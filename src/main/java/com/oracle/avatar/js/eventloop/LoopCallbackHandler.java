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

package com.oracle.avatar.js.eventloop;

import java.nio.ByteBuffer;

import com.oracle.libuv.Address;
import com.oracle.libuv.Stats;
import com.oracle.libuv.cb.AsyncCallback;
import com.oracle.libuv.cb.CallbackHandler;
import com.oracle.libuv.cb.CheckCallback;
import com.oracle.libuv.cb.FileCallback;
import com.oracle.libuv.cb.FileEventCallback;
import com.oracle.libuv.cb.FilePollCallback;
import com.oracle.libuv.cb.FilePollStopCallback;
import com.oracle.libuv.cb.FileCloseCallback;
import com.oracle.libuv.cb.FileOpenCallback;
import com.oracle.libuv.cb.FileReadCallback;
import com.oracle.libuv.cb.FileReadDirCallback;
import com.oracle.libuv.cb.FileReadLinkCallback;
import com.oracle.libuv.cb.FileStatsCallback;
import com.oracle.libuv.cb.FileUTimeCallback;
import com.oracle.libuv.cb.FileWriteCallback;
import com.oracle.libuv.cb.IdleCallback;
import com.oracle.libuv.cb.ProcessCloseCallback;
import com.oracle.libuv.cb.ProcessExitCallback;
import com.oracle.libuv.cb.SignalCallback;
import com.oracle.libuv.cb.StreamCloseCallback;
import com.oracle.libuv.cb.StreamConnectCallback;
import com.oracle.libuv.cb.StreamConnectionCallback;
import com.oracle.libuv.cb.StreamRead2Callback;
import com.oracle.libuv.cb.StreamReadCallback;
import com.oracle.libuv.cb.StreamShutdownCallback;
import com.oracle.libuv.cb.StreamWriteCallback;
import com.oracle.libuv.cb.TimerCallback;
import com.oracle.libuv.cb.UDPCloseCallback;
import com.oracle.libuv.cb.UDPRecvCallback;
import com.oracle.libuv.cb.UDPSendCallback;

final class LoopCallbackHandler implements CallbackHandler {

    private final EventLoop eventLoop;
    private final Object domain;

    public LoopCallbackHandler(EventLoop eventLoop) {
        this(eventLoop, null);
    }

    public LoopCallbackHandler(EventLoop eventLoop, Object domain) {
        this.eventLoop = eventLoop;
        this.domain = domain;
    }

    private boolean shouldCall() {
        if (domain != null) {
            if (eventLoop.isDisposed(domain)) {
                return false;
            }
            eventLoop.enterDomain(domain);
        }

        return true;
    }

    private void post() throws Exception {
        if (domain != null) {
            eventLoop.exitDomain(domain);
        }
        eventLoop.processQueuedEvents();
    }

    @Override
    public void handleAsyncCallback(final AsyncCallback cb, final int status) {
        try {
            if (shouldCall()) {
                cb.onSend(status);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleCheckCallback(final CheckCallback cb, final int status) {
        try {
            if (shouldCall()) {
                cb.onCheck(status);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleSignalCallback(final SignalCallback cb, final int signum) {
        try {
            if (shouldCall()) {
                cb.onSignal(signum);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamReadCallback(final StreamReadCallback cb, final ByteBuffer data) {
        try {
            if (shouldCall()) {
                cb.onRead(data);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamRead2Callback(final StreamRead2Callback cb, final ByteBuffer data, final long handle, final int type) {
        try {
            if (shouldCall()) {
                cb.onRead2(data, handle, type);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamWriteCallback(final StreamWriteCallback cb, final int status, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onWrite(status, error);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamConnectCallback(final StreamConnectCallback cb, final int status, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onConnect(status, error);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamConnectionCallback(final StreamConnectionCallback cb, final int status, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onConnection(status, error);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamCloseCallback(final StreamCloseCallback cb) {
        try {
            if (shouldCall()) {
                cb.onClose();
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamShutdownCallback(final StreamShutdownCallback cb, final int status, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onShutdown(status, error);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileCallback(final FileCallback cb, final Object context, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onDone(context, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }


    @Override
    public void handleFileCloseCallback(final FileCloseCallback cb, final Object context, final int fd, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onClose(context, fd, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileOpenCallback(final FileOpenCallback cb, final Object context, final int fd, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onOpen(context, fd, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileReadCallback(final FileReadCallback cb, final Object context, final int bytesRead, final ByteBuffer data, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onRead(context, bytesRead, data, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileReadDirCallback(final FileReadDirCallback cb, final Object context, final String[] names, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onReadDir(context, names, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileReadLinkCallback(final FileReadLinkCallback cb, final Object context, final String name, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onReadLink(context, name, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileStatsCallback(final FileStatsCallback cb, final Object context, final Stats stats, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onStats(context, stats, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileUTimeCallback(final FileUTimeCallback cb, final Object context, final long time, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onUTime(context, time, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileWriteCallback(final FileWriteCallback cb, final Object context, final int bytesWritten, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onWrite(context, bytesWritten, error);
                post();
            }
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileEventCallback(FileEventCallback cb, int status, String event, String filename) {
        try {
            if (shouldCall()) {
                cb.onEvent(status, event, filename);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }


    @Override
    public void handleFilePollCallback(FilePollCallback cb, int status, Stats previous, Stats current) {
        try {
            if (shouldCall()) {
                cb.onPoll(status, previous, current);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFilePollStopCallback(FilePollStopCallback cb) {
        try {
            if (shouldCall()) {
                cb.onStop();
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleProcessCloseCallback(ProcessCloseCallback cb) {
        try {
            if (shouldCall()) {
                cb.onClose();
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleProcessExitCallback(ProcessExitCallback cb, long status, int signal) {
        try {
            if (shouldCall()) {
                cb.onExit(status, signal);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleTimerCallback(final TimerCallback cb, final int status) {
        try {
            if (shouldCall()) {
                cb.onTimer(status);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleUDPRecvCallback(final UDPRecvCallback cb, final int nread, final ByteBuffer data, final Address address) {
        try {
            if (shouldCall()) {
                cb.onRecv(nread, data, address);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleUDPSendCallback(final UDPSendCallback cb, final int status, final Exception error) {
        try {
            if (shouldCall()) {
                cb.onSend(status, error);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleUDPCloseCallback(final UDPCloseCallback cb) {
        try {
            if (shouldCall()) {
                cb.onClose();
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleIdleCallback(IdleCallback cb, int status) {
        try {
            if (shouldCall()) {
                cb.onIdle(status);
                post();
            }
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }
}
