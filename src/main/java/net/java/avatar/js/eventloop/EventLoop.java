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
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.internal.runtime.ECMAException;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.java.avatar.js.dns.DNS;
import net.java.avatar.js.log.Logger;
import net.java.avatar.js.log.Logging;
import net.java.libuv.LibUV;
import net.java.libuv.cb.AsyncCallback;
import net.java.libuv.cb.CallbackExceptionHandler;
import net.java.libuv.cb.CallbackDomainProvider;
import net.java.libuv.cb.CallbackHandler;
import net.java.libuv.cb.CallbackHandlerFactory;
import net.java.libuv.handles.AsyncHandle;
import net.java.libuv.handles.LoopHandle;

public final class EventLoop {

    private final String version;
    private final String uvVersion;
    private final Logging logging;
    private final DNS dns;
    private final LoopHandle uvLoop;
    private final int instanceNumber;
    private final ThreadPool executor;
    private final Logger LOG;
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger hooks = new AtomicInteger(0);
    private final AsyncHandle asyncHandle;
    private final Thread mainThread;

    private Callback isHandlerRegistered = null;
    private Callback uncaughtExceptionHandler = null;
    private Exception pendingException = null;

    private ScriptObjectMirror domain;

    public static final class Handle implements AutoCloseable {

        private final AtomicInteger hooks;
        private final AsyncHandle asyncHandle;
        public Handle(final AtomicInteger hooks, final AsyncHandle asyncHandle) {
            this.hooks = hooks;
            this.asyncHandle = asyncHandle;
            if (hooks.incrementAndGet() == 1) {
                asyncHandle.ref();
            }
        }

        @Override
        public void close() {
            hooks.decrementAndGet();
            asyncHandle.send();
        }
        
        public void release() {
            close();
        }

        @Override
        public String toString() {
            return Integer.toString(hooks.get());
        }
    }

    public Handle acquire() {
        return new Handle(hooks, asyncHandle);
    }

    public void setUncaughtExceptionHandler(final Callback registered, final Callback handler) {
        isHandlerRegistered = registered;
        uncaughtExceptionHandler = handler;
    }

    public void nextTick(final Callback cb) {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        eventQueue.add(new Event("nextTick", cb));
    }

    public void nextTickWithDomain(final Callback cb, ScriptObjectMirror evtDomain) {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        eventQueue.add(new Event("nextTickWithDomain", evtDomain, cb));
    }

    public void post(final Callback cb, Object... args) {
        eventQueue.add(new Event(null, cb, args));
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
                ScriptObjectMirror evtDomain = event.getDomain();
                if (evtDomain != null) {
                    if (isDisposed(evtDomain)) {
                        continue;
                    }
                    enterDomain(evtDomain);
                    processEvent(event);
                    exitDomain(evtDomain);
                } else {
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
                stop();
                throw ex;
            }
        }
    }

    public void stop() {
        executor.shutdown();
        asyncHandle.close();
        uvLoop.stop();
    }

    public void release() {
        hooks.set(0);
        executor.clearQueuedTasks();
    }

    // filename, line, column, name, message
    public static final String EXCEPTION_FILE = "filename";
    public static final String EXCEPTION_LINE = "line";
    public static final String EXCEPTION_COLUMN = "column";
    public static final String EXCEPTION_NAME = "name";
    public static final String EXCEPTION_MESSAGE = "message";
    public static final String EXCEPTION_STACK = "stack";

    private static final String UNCAUGHT_EXCEPTION_NAME = "uncaughtException";

    public boolean handleCallbackException(final Exception ex) {
        boolean handled = true;
        // callback to check if an uncaught exception handler has been registered by the user
        final Object[] registeredArgs = {null};
        if (isHandlerRegistered != null) {
            try {
                isHandlerRegistered.call(UNCAUGHT_EXCEPTION_NAME, registeredArgs);
            } catch (final Exception e) {
                handled = false;
            }
        }

        if (registeredArgs[0] == null || uncaughtExceptionHandler == null) {
            // no handler registered - rethrow uncaught exceptions
            handled = false;
        }

        if (!handled && domain == null) {
           // No domain and no uncaughtException Handler registered
           // rethrowing
            return false;
        }

        final Map<String, Object> args = new HashMap<>();
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
                
        // extract exception file, line, column, stack, name and message, if available
        args.put(EXCEPTION_FILE, nex.getFileName());
        args.put(EXCEPTION_LINE, nex.getLineNumber());
        args.put(EXCEPTION_COLUMN, nex.getColumnNumber());
        args.put(EXCEPTION_STACK, NashornException.getScriptStackString(nex));

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
        // Workaround of https://bugs.openjdk.java.net/browse/JDK-8029364
        // To be replaced with public API when available.
        if (nex instanceof ECMAException) {
            ECMAException ecma = (ECMAException) nex;
            if (ecma.getThrown() instanceof ScriptObject) {
                ScriptObject so = (ScriptObject) ecma.getThrown();
                for (String m : so.getOwnKeys(false)) {
                    if (!args.keySet().contains(m)) {
                        args.put(m, so.get(m));
                    }
                }
            }
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
        return "EventLoop." + instanceNumber + " " + "}, runtime: {" +
               "executor: " + executor + ", " +
               "hooks: " + hooks.get() + ", " +
               "events: " + eventQueue.size() + ", " +
               "tasks: " + executor.queuedTasksCount() + ", " +
               "activeThreads: " + executor.getActiveCount() + ", " +
               "threads: " + executor.getPoolSize() + ", " +
               "pending: " + eventQueue.size() +
               "}}";
    }

    public boolean isMainThread() {
        return mainThread == Thread.currentThread();
    }

    public EventLoop(final String version,
                     final String uvVersion,
                     final Logging logging,
                     final String workDir,
                     final int instanceNumber,
                     final ThreadPool executor) throws IOException {
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

        final LoopCallbackHandler defaultHandler = new LoopCallbackHandler(this);
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
        },
        new CallbackHandlerFactory() {
            @Override
            public CallbackHandler newCallbackHandlerWithDomain(Object domain) {
                return new LoopCallbackHandler(EventLoop.this, domain);
            }
            @Override
            public CallbackHandler newCallbackHandler() {
                return defaultHandler;
            }
        },
        new CallbackDomainProvider() {
            @Override
            public Object getDomain() {
                return EventLoop.this.getDomain();
            }
        });
        this.instanceNumber = instanceNumber;
        this.executor = executor;

        LibUV.chdir(workDir);
        LOG = logger("eventloop");
        asyncHandle = new AsyncHandle(uvLoop);
        asyncHandle.setAsyncCallback(new AsyncCallback() {
            @Override
            public void onSend(int status) throws Exception {
                // The side effect of this callback being called is that posted
                // events have been processed.
                if (hooks.get() <= 0) {
                    asyncHandle.unref();
                }
            }
        });
        asyncHandle.unref();

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

    public void setDomain(ScriptObjectMirror obj) {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        domain = obj;
    }

    public ScriptObjectMirror getDomain() {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        return domain;
    }

    public boolean isDisposed(ScriptObjectMirror domain) {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        return Boolean.TRUE.equals(domain.getMember("_disposed"));
    }

    public void enterDomain(ScriptObjectMirror domain) {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
         domain.callMember("enter");
    }

    public void exitDomain(ScriptObjectMirror domain) {
        assert Thread.currentThread() == mainThread : "called from non-event thread " + Thread.currentThread().getName();
        domain.callMember("exit");
    }

    public boolean isDisposed(Object domain) {
        if (domain instanceof ScriptObjectMirror) {
            isDisposed((ScriptObjectMirror) domain);
        }
        return false;
    }

    public void enterDomain(Object domain) {
        if (domain instanceof ScriptObjectMirror) {
            enterDomain((ScriptObjectMirror) domain);
        }
    }

    public void exitDomain(Object domain) {
        if (domain instanceof ScriptObjectMirror) {
            exitDomain((ScriptObjectMirror) domain);
        }
    }
}
