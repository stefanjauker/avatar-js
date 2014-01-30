/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.avatar.js.log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.avatar.js.eventloop.DaemonThreadFactory;

public class LogQueue {

    private final BlockingQueue<LogEvent> logQueue = new LinkedBlockingQueue<>();

    private final ThreadFactory logThreadFactory = new DaemonThreadFactory("avatar-js.log");

    private final AtomicBoolean logThreadStarted = new AtomicBoolean(false);

    private final Thread logThread = logThreadFactory.newThread(() -> {
        while (true) {
            try {
                logQueue.take().write();
            } catch (final IOException | InterruptedException ignore) {
                return;
            }
        }
    });

    private final AtomicBoolean logThreadStopped = new AtomicBoolean(false);

    public void log(final LogEvent event) {
        logQueue.add(event);

        // start log thread lazily on the very first log event
        if (logThreadStarted.compareAndSet(false, true)) {
            logThread.start();
        }
    }

    public void shutdown() {
        if (logThreadStopped.compareAndSet(false, true)) {
            logThread.interrupt();
        }
    }
}
