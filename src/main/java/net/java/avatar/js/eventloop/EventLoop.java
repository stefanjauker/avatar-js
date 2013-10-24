/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package net.java.avatar.js.eventloop;

import jdk.nashorn.api.scripting.NashornException;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.script.ScriptException;

import net.java.avatar.js.dns.DNS;
import net.java.avatar.js.log.Logger;
import net.java.avatar.js.log.Logging;

import net.java.libuv.Callback;
import net.java.libuv.CallbackExceptionHandler;
import net.java.libuv.CallbackHandler;
import net.java.libuv.CheckCallback;
import net.java.libuv.FileCallback;
import net.java.libuv.IdleCallback;
import net.java.libuv.LibUV;
import net.java.libuv.ProcessCallback;
import net.java.libuv.SignalCallback;
import net.java.libuv.StreamCallback;
import net.java.libuv.TimerCallback;
import net.java.libuv.UDPCallback;
import net.java.libuv.handles.LoopHandle;

public final class EventLoop {

    private static final int DEFAULT_QUEUE_SIZE = Integer.MAX_VALUE;
    private static final int DEFAULT_CORE_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private static final int DEFAULT_MAX_THREADS = Integer.MAX_VALUE;
    private static final long DEFAULT_THREAD_TIMEOUT_SECONDS = 15;
    private static final long MAX_IDLE_PAUSE_INTERVAL_MILLISECONDS = 100;
    // Should be configurable, seems a good trade-off between perf and cpu consumption.
    private static final long NUM_ITERATIONS_BEFORE_INC_DELAY = 100;

    private static final String PACKAGE = EventLoop.class.getPackage().getName() + ".";
    private static final String QUEUE_SIZE_PROPERTY = PACKAGE + "queueSize";
    private static final String CORE_THREAD_PROPERTY = PACKAGE + "coreThreads";
    private static final String MAX_THREADS_PROPERTY = PACKAGE + "maxThreads";
    private static final String THREAD_TIMEOUT_PROPERTY = PACKAGE + "threadTimeout";

    // a marker event that indicates the end of the current tick
    // a tick is one cycle that processes queued events as of this moment
    // a call to nextTick() starts a new cycle
    private static final Event TICK_MARKER = new Event("tick.marker", null);

    private final String version;
    private final String uvVersion;
    private final Logging logging;
    private final DNS dns;
    private final LoopHandle uvLoop;
    private final int instanceNumber;

    private final Logger LOG;
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

    private final Semaphore idleWait = new Semaphore(1);
    private final AtomicInteger hooks = new AtomicInteger(0);
    private final AtomicBoolean maybeIdle = new AtomicBoolean(true);

    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>(
            Integer.getInteger(QUEUE_SIZE_PROPERTY, DEFAULT_QUEUE_SIZE));

    private final AtomicInteger activeTasks = new AtomicInteger(0);

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Integer.getInteger(CORE_THREAD_PROPERTY, DEFAULT_CORE_THREADS),
            Integer.getInteger(MAX_THREADS_PROPERTY, DEFAULT_MAX_THREADS),
            Long.getLong(THREAD_TIMEOUT_PROPERTY, DEFAULT_THREAD_TIMEOUT_SECONDS), TimeUnit.SECONDS,
            tasks,
            new DaemonThreadFactory("avatar-js.bg.task"),
            new ThreadPoolExecutor.CallerRunsPolicy()) {

        @Override
        protected void beforeExecute(final Thread t, final Runnable r) {
            activeTasks.incrementAndGet();
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(final Runnable r, final Throwable t) {
            super.afterExecute(r, t);
            activeTasks.decrementAndGet();
        }
    };

    private final EventLoopStatsMBean STATS_MBEAN =
            new EventLoopStats(eventQueue, hooks, tasks, activeTasks, executor);

    private Callback isHandlerRegistered = null;
    private Callback uncaughtExceptionHandler = null;
    private Thread mainThread = null;
    private Exception pendingException = null;
    private volatile boolean closed;
    
    public static final class Handle implements AutoCloseable {

        private final AtomicInteger hooks;

        public Handle(final AtomicInteger hooks) {
            this.hooks = hooks;
            hooks.incrementAndGet();
        }

        @Override
        public void close() {
            hooks.decrementAndGet();
        }

        @Override
        public String toString() {
            return Integer.toString(hooks.get());
        }
    }

    public Handle grab() {
        return new Handle(hooks);
    }

    public void setUncaughtExceptionHandler(final Callback registered, final Callback handler) {
        isHandlerRegistered = registered;
        uncaughtExceptionHandler = handler;
    }

    public void nextTick(final Event event) {
        eventQueue.add(TICK_MARKER);
        postEvent(event, eventQueue);
    }

    public void post(final Event event) {
        postEvent(event, eventQueue);
    }

    private void postEvent(final Event event, final BlockingQueue<Event> queue) {
        maybeIdle.set(false);
        queue.add(event);
        idleWait.drainPermits();
        idleWait.release();
    }

    public void run() throws Exception {
        mainThread = Thread.currentThread();
        executor.allowCoreThreadTimeOut(true);
        registerStatsMBean();

        boolean idle = false;
        long delay = 0;
        long nativeIdleCount = 0;
        while (!closed &&
               (uvLoop.runNoWait() ||
                hooks.get() != 0 ||
                !maybeIdle.get() ||
                activeTasks.get() != 0 ||
                // LinkedBlockingQueue.isEmpty() is quick but not always accurate
                // peek first element to make sure the queues are really empty
                // do this last to avoid unnecessary iteration
                tasks.peek() != null ||
                eventQueue.peek() != null)) {
            
            // throw pending exception, if any
            if (pendingException != null) {
                final Exception pex = pendingException;
                pendingException = null;
                throw pex;
            }

            // pause until an event arrives to avoid spinning on idle
            if (maybeIdle.get()) {
                // increase delay as long as idle on consecutive polls
               delay = Math.min(nativeIdleCount++ / NUM_ITERATIONS_BEFORE_INC_DELAY, MAX_IDLE_PAUSE_INTERVAL_MILLISECONDS);
               if (delay > 1) {
                    idleWait.tryAcquire(delay, TimeUnit.MILLISECONDS);
               }
            } else {
                nativeIdleCount = 0;
            }

            // process pending events in this cycle
            for (Event event = eventQueue.poll();
                 event != null && event != TICK_MARKER;
                 event = eventQueue.poll()) {
                processEvent(event);
            }

            idle = eventQueue.peek() == null;
            maybeIdle.set(idle);
        }
    }

    /**
     * This is called once the main module has been loaded
     * (module.js runMain, process._tickCallback();).
     * All posted events are processed until the event queue is empty.
     * With the native binding, IO callbacks (eg: connect, connection, ...)
     * are processed when runNoWait is called. runNoWait is called in each iteration
     * and before the eventQueue is polled. So processing all nextTicks added during main
     * module loading before calling runNoWait guarantees that ticked events are
     * called prior any IO events are fired.
     */
    public void processQueuedEvents() throws Exception {
        if (eventQueue.size() != 0) {
            // process current events and all events added by processed events
            for (Event event = eventQueue.poll();
                 event != null;
                 event = eventQueue.poll()) {
                if (event != TICK_MARKER) {
                    processEvent(event);
                }
            }
        }
    }

    private void processEvent(final Event event) throws Exception {
        processEvent(event.getName(), event.getCallback(), event.getContext(), event.getArgs());
    }

    private void processEvent(final String name, final Callback callback,
            final AccessControlContext context, final Object... args) throws Exception {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        assert callback != null : "callback is null for event " + name;
        try {
            if (LOG.enabled()) { LOG.log(Event.toString(name, args)); }
            if (context != null) {
                // Reuses the caller context when executing the callback.
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        callback.call(name, args);
                        return null;
                    }
                }, context);
            } else {
                callback.call(name, args);
            }
        } catch (final Exception ex) {
            if (!handleCallbackException(ex)) {
                executor.shutdown();
                throw ex;
            }
        }
    }

    public void drain() throws Exception {
        assert Thread.currentThread() == mainThread : "drain called from non-event thread " + Thread.currentThread().getName();
        // don't spin forever in case there is a large backlog of native events
        final long start = System.currentTimeMillis();
        final long timeout = 1000;
        int times = 1000;
        while (uvLoop.runNoWait() && --times > 0 && System.currentTimeMillis() - start < timeout) {
        }
    }

    public void stop() {
        closed = true;
        executor.shutdown();
        uvLoop.stop();
    }

    private synchronized void registerStatsMBean()
            throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        final String value = instanceNumber == 0 ? "stats" : ("stats." + instanceNumber);
        final ObjectName STATS_MBEAN_NAME =
                new ObjectName("net.java.avatar.js.eventloop.EventLoop", "key", value);
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (!mBeanServer.isRegistered(STATS_MBEAN_NAME)) {
            mBeanServer.registerMBean(STATS_MBEAN, STATS_MBEAN_NAME);
        }
    }

    public void release() {
        hooks.set(0);
        tasks.clear();
    }

    // filename, line, column, name, message
    public static final String EXCEPTION_FILE = "file";
    public static final String EXCEPTION_LINE = "line";
    public static final String EXCEPTION_COLUMN = "column";
    public static final String EXCEPTION_NAME = "name";
    public static final String EXCEPTION_MESSAGE = "message";
    public static final String EXCEPTION_STACK = "stack";

    private static final String UNCAUGHT_EXCEPTION_NAME = "uncaughtException";

    public boolean handleCallbackException(final Exception ex) {

        // callback to check if an uncaught exception handler has been registered by the user
        final Object[] registeredArgs = {null};
        if (isHandlerRegistered != null) {
            try {
                isHandlerRegistered.call(UNCAUGHT_EXCEPTION_NAME, registeredArgs);
            } catch (final Exception e) {
                return false;
            }
        }

        if (registeredArgs[0] == null || uncaughtExceptionHandler == null) {
            // no handler registered - rethrow uncaught exceptions
            return false;
        }

        final Map<String, Object> args = new HashMap<>();
        args.put(EXCEPTION_STACK, NashornException.getScriptStackString(ex));
        if (LOG.enabled()) { LOG.log("uncaught %s", ex.toString()); }

        NashornException nex = null;
        if (ex instanceof NashornException) {
            nex = (NashornException) ex;
        } else if (ex instanceof ScriptException) {
            // unwrap one level to see if we have a NashornException as the cause
            final ScriptException sx = (ScriptException) ex;
            final Throwable cause = sx.getCause();
            if (cause instanceof NashornException) {
                nex = (NashornException) cause;
            }
        }
        if (nex == null) {
            // unknown/unhandled exception - rethrow
            return false;
        }

        // extract exception file, line, column, name and message, if available
        args.put(EXCEPTION_FILE, nex.getFileName());
        args.put(EXCEPTION_LINE, nex.getLineNumber());
        args.put(EXCEPTION_COLUMN, nex.getColumnNumber());

        final String message = nex.getMessage();
        final String[] EMPTY_ARRAY = {};
        final String[] parts = message == null ? EMPTY_ARRAY : message.split(":");
        switch (parts.length) {
            case 2:
                args.put(EXCEPTION_NAME, parts[0].trim());
                args.put(EXCEPTION_MESSAGE, parts[1].trim());
                break;
            case 1:
                args.put(EXCEPTION_MESSAGE, parts[0].trim());
                break;
            default:
        }

        // dispatch exception to user handler, returning true on success
        try {
            final Object[] arr = {args};
            uncaughtExceptionHandler.call(UNCAUGHT_EXCEPTION_NAME, arr);
        } catch (final Exception e) {
            return false;
        }

        return true;
    }

    public Future<?> submit(final Runnable runnable) {
        Runnable toSubmit = runnable;
        if (System.getSecurityManager() != null) {
            // snapshot to be reused at execution time.
            final AccessControlContext context = AccessController.getContext();
            toSubmit = new Runnable() {
                @Override
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            runnable.run();
                            return null;
                        }
                    }, context);
                }
            };
        }
        return executor.submit(toSubmit);
    }

    @Override
    public String toString() {
        return "EventLoop." + instanceNumber + " " +
                "{config: {" +
                QUEUE_SIZE_PROPERTY + "=" + tasks.remainingCapacity() + ", " +
                CORE_THREAD_PROPERTY + "=" + executor.getCorePoolSize() + ", " +
                MAX_THREADS_PROPERTY + "=" + executor.getMaximumPoolSize() +
               "}, runtime: {" +
               "executor: " + executor + ", " +
               "hooks: " + hooks.get() + ", " +
               "events: " + eventQueue.size() + ", " +
               "tasks: " + tasks.size() + ", " +
               "activeTasks: " + activeTasks.get() + ", " +
               "activeThreads: " + executor.getActiveCount() + ", " +
               "threads: " + executor.getPoolSize() + ", " +
               "pending: " + eventQueue.size() + ", " +
               "}}";
    }

    public boolean isMainThread() {
        return mainThread == Thread.currentThread();
    }

    public EventLoop(final String version,
                     final String uvVersion,
                     final Logging logging,
                     final String workDir,
                     final int instanceNumber) throws IOException {
        final String uv = LibUV.version();
        if (!uvVersion.equals(uv)) {
            throw new LinkageError(String.format("libuv version mismatch: expected '%s', found '%s'",
                    uvVersion,
                    uv));
        }

        this.version = version;
        this.uvVersion = uvVersion;
        this.logging = logging;
        this.dns = new DNS(this);
        this.uvLoop = new LoopHandle(new CallbackExceptionHandler() {
            @Override
            public void handle(final Exception ex) {
                if (!handleCallbackException(ex)) {
                    executor.shutdown();
                    if (pendingException == null) {
                        pendingException = ex;
                    } else {
                        pendingException.addSuppressed(ex);
                    }
                }
            }
        }, new CallbackHandler() {
            @Override
            public void handleCheckCallback(final CheckCallback cb, final int status) {
                maybeIdle.set(false);
                try {
                    cb.call(status);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleProcessCallback(final ProcessCallback cb, final Object[] args) {
                maybeIdle.set(false);
                try {
                    cb.call(args);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleSignalCallback(final SignalCallback cb, final int signum) {
                maybeIdle.set(false);
                try {
                    cb.call(signum);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleStreamCallback(final StreamCallback cb, final Object[] args) {
                maybeIdle.set(false);
                try {
                    cb.call(args);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleFileCallback(final FileCallback cb, final int id, final Object[] args) {
                maybeIdle.set(false);
                try {
                    cb.call(id, args);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleTimerCallback(final TimerCallback cb, final int status) {
                maybeIdle.set(false);
                try {
                    cb.call(status);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleUDPCallback(final UDPCallback cb, final Object[] args) {
                maybeIdle.set(false);
                try {
                    cb.call(args);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleIdleCallback(IdleCallback cb, int status) {
                maybeIdle.set(false);
                try {
                    cb.call(status);
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }
        });
        
        this.instanceNumber = instanceNumber;

        LibUV.chdir(workDir);
        LOG = logger("eventloop");
        closed = false;
    }

    public String version() {
        return version;
    }

    public String uvVersion() {
        return uvVersion;
    }

    public Logger logger() {
        return logging.getDefault();
    }

    public Logger logger(final String name) {
        return logging.get(name);
    }

    public DNS dns() {
        return dns;
    }

    public String getWorkDir() {
        return LibUV.cwd();
    }

    public void setWorkDir(final String newDir) {
        LibUV.chdir(newDir);
    }

    public LoopHandle loop() {
        return uvLoop;
    }

}
