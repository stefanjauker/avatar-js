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
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import net.java.avatar.js.dns.DNS;
import net.java.avatar.js.log.Logger;
import net.java.avatar.js.log.Logging;

import net.java.libuv.LibUV;
import net.java.libuv.cb.Callback;
import net.java.libuv.cb.CallbackExceptionHandler;
import net.java.libuv.cb.CallbackHandler;
import net.java.libuv.cb.CheckCallback;
import net.java.libuv.cb.FileCallback;
import net.java.libuv.cb.FileEventCallback;
import net.java.libuv.cb.IdleCallback;
import net.java.libuv.cb.ProcessCallback;
import net.java.libuv.cb.SignalCallback;
import net.java.libuv.cb.StreamCallback;
import net.java.libuv.cb.TimerCallback;
import net.java.libuv.cb.UDPCallback;
import net.java.libuv.handles.IdleHandle;
import net.java.libuv.handles.LoopHandle;

public final class EventLoop {

    private static final int DEFAULT_QUEUE_SIZE = Integer.MAX_VALUE;
    private static final int DEFAULT_CORE_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private static final int DEFAULT_MAX_THREADS = Integer.MAX_VALUE;
    private static final long DEFAULT_THREAD_TIMEOUT_SECONDS = 15;

    private static final String PACKAGE = EventLoop.class.getPackage().getName() + ".";
    private static final String QUEUE_SIZE_PROPERTY = PACKAGE + "queueSize";
    private static final String CORE_THREAD_PROPERTY = PACKAGE + "coreThreads";
    private static final String MAX_THREADS_PROPERTY = PACKAGE + "maxThreads";
    private static final String THREAD_TIMEOUT_PROPERTY = PACKAGE + "threadTimeout";

    private final String version;
    private final String uvVersion;
    private final Logging logging;
    private final DNS dns;
    private final LoopHandle uvLoop;
    private final int instanceNumber;

    private final Logger LOG;
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger hooks = new AtomicInteger(0);

    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>(
            Integer.getInteger(QUEUE_SIZE_PROPERTY, DEFAULT_QUEUE_SIZE));

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Integer.getInteger(CORE_THREAD_PROPERTY, DEFAULT_CORE_THREADS),
            Integer.getInteger(MAX_THREADS_PROPERTY, DEFAULT_MAX_THREADS),
            Long.getLong(THREAD_TIMEOUT_PROPERTY, DEFAULT_THREAD_TIMEOUT_SECONDS), TimeUnit.SECONDS,
            tasks,
            new DaemonThreadFactory("avatar-js.bg.task"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private Callback isHandlerRegistered = null;
    private Callback uncaughtExceptionHandler = null;
    private final Thread mainThread;
    private Exception pendingException = null;
    private final IdleHandle idleHandle;
    private final AtomicBoolean idleHandleStarted = new AtomicBoolean(false);

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
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        eventQueue.add(event);
    }

    public void post(final Event event) {
        eventQueue.add(event);
    }

    public void run() throws Exception {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        executor.allowCoreThreadTimeOut(true);
        uvLoop.run();
        // throw pending exception, if any
        if (pendingException != null) {
            final Exception pex = pendingException;
            pendingException = null;
            throw pex;
        }
    }

    /**
     * This is called after the main module has been loaded, after each native callback and
     * when background threads have posted events.
     */
    public void processQueuedEvents() throws Exception {
        if (eventQueue.peek() != null) {
            // process current events and all events added by processed events
            for (Event event = eventQueue.poll();
                 event != null;
                 event = eventQueue.poll()) {
                processEvent(event);
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
                stop();
                throw ex;
            }
        }
    }

    public void stop() {
        executor.shutdown();
        uvLoop.stop();
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
        // If submit is not called from main thread, there is no guarantee that idleHandle.start()
        // will be taken into account by uv loop.
        if (Thread.currentThread() != mainThread) {
            assert idleHandleStarted.get() : "idleHandle not started although called "
                    + "from non-event thread " + Thread.currentThread().getName();
        }
        if (idleHandleStarted.compareAndSet(false, true)) {
            idleHandle.start();
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
               "activeThreads: " + executor.getActiveCount() + ", " +
               "threads: " + executor.getPoolSize() + ", " +
               "pending: " + eventQueue.size() + ", " +
               "idleHandleStarted: " + idleHandleStarted.get() +
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
        mainThread = Thread.currentThread();
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
                    if (pendingException == null) {
                        pendingException = ex;
                    } else {
                        pendingException.addSuppressed(ex);
                    }
                    stop();
                }
            }
        }, new CallbackHandler() {
            @Override
            public void handleCheckCallback(final CheckCallback cb, final int status) {
                try {
                    cb.call(status);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleProcessCallback(final ProcessCallback cb, final Object[] args) {
                try {
                    cb.call(args);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleSignalCallback(final SignalCallback cb, final int signum) {
                try {
                    cb.call(signum);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleStreamCallback(final StreamCallback cb, final Object[] args) {
                try {
                    cb.call(args);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleFileCallback(final FileCallback cb, final int id, final Object[] args) {
                try {
                    cb.call(id, args);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleFileEventCallback(FileEventCallback cb, int status, String event, String filename) {
                try {
                    cb.call(status, event, filename);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleTimerCallback(final TimerCallback cb, final int status) {
                try {
                    cb.call(status);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleUDPCallback(final UDPCallback cb, final Object[] args) {
                try {
                    cb.call(args);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }

            @Override
            public void handleIdleCallback(IdleCallback cb, int status) {
                try {
                    cb.call(status);
                    processQueuedEvents();
                } catch (Exception ex) {
                    uvLoop.getExceptionHandler().handle(ex);
                }
            }
        });

        this.instanceNumber = instanceNumber;

        LibUV.chdir(workDir);
        LOG = logger("eventloop");
        idleHandle = new IdleHandle(uvLoop);
        idleHandle.setIdleCallback(new IdleCallback() {
            @Override
            public void call(int status) throws Exception {
                // process pending events in this cycle
                // have been posted by background threads
                processQueuedEvents();
                // No more bg task and no more Events to process, stop idleHandle
                if (hooks.get() == 0 && eventQueue.peek() == null) {
                    idleHandle.stop();
                    idleHandleStarted.set(false);
                }
            }
        });

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
