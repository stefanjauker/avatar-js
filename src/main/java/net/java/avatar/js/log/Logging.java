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

package net.java.avatar.js.log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.java.avatar.js.eventloop.DaemonThreadFactory;

public final class Logging {

    private final BlockingQueue<LogEvent> logQueue = new LinkedBlockingQueue<>();

    private final ThreadFactory logThreadFactory = new DaemonThreadFactory("avatar-js.log");

    private final AtomicBoolean logThreadStarted = new AtomicBoolean(false);

    private final Thread logThread = logThreadFactory.newThread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    logQueue.take().write();
                } catch (final IOException | InterruptedException ignore) {
                    return;
                }
            }
        }
    });

    // each logger instance within a category is given a unique id
    private final ConcurrentHashMap<String, AtomicInteger> NEXT_ID = new ConcurrentHashMap<>();

    private final File logDirectory;

    private volatile boolean enabled;

    private final Logger defaultLogger;

    private static final Logger NULL_LOGGER = new Logger() {
        @Override
        public void log(final Throwable throwable) {
        }

        @Override
        public void log(final String message) {
        }

        @Override
        public void log(final String format, final Object... args) {
        }

        @Override
        public boolean enable() {
            return false;
        }

        @Override
        public boolean disable() {
            return false;
        }

        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void close() throws IOException {
        }
    };

    void log(final LogEvent event) {
        logQueue.add(event);

        // start log thread lazily on the very first log event
        if (logThreadStarted.compareAndSet(false, true)) {
            logThread.start();
        }
    }

    public Logging(final boolean enabled) {
        this(null, enabled);
    }

    public Logging(final File logDirectory, final boolean enabled) {
        if (logDirectory != null && logDirectory.exists() && logDirectory.isDirectory()) {
            this.logDirectory = logDirectory;
        } else {
            this.logDirectory = new File(System.getProperty("user.dir"));
            if (logDirectory != null) {
                System.err.println("invalid log directory specified: " + logDirectory +
                    ", falling back to " + this.logDirectory);
            }
        }
        this.enabled = enabled;
        this.defaultLogger = enabled ? create("node") : NULL_LOGGER;
    }

    public boolean setEnabled(final boolean enabled) {
        final boolean was = this.enabled;
        this.enabled = enabled;
        return was;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Logger get(final String name) {
        if (enabled) {
            return create(name);
        }
        return NULL_LOGGER;
    }

    public Logger getDefault() {
        return defaultLogger;
    }

    public void shutdown() {
        logThread.interrupt();
    }

    private Logger create(final String name) {
        NEXT_ID.putIfAbsent(name, new AtomicInteger(0));
        return new FileLogger(this, logDirectory, name, NEXT_ID.get(name).getAndIncrement());
    }
}
