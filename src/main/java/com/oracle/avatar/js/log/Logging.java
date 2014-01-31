/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the GNU General
 * Public License Version 2 only ("GPL"). You may not use this file except
 * in compliance with the License.  You can obtain a copy of the License at
 * https://avatar.java.net/license.html or legal/LICENSE.txt.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.avatar.js.log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Logging {

    // each logger instance within a category is given a unique id
    private final ConcurrentHashMap<String, AtomicInteger> NEXT_ID = new ConcurrentHashMap<>();

    private final LogQueue queue;

    private final boolean sharedQueue;

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

    public Logging(final boolean enabled) {
        this(null, enabled);
    }

    public Logging(final File logDirectory, final boolean enabled) {
        this(new LogQueue(), false, logDirectory, enabled);
    }

    public Logging(final LogQueue queue, final boolean sharedQueue, final File logDirectory, final boolean enabled) {
        this.queue = queue;
        this.sharedQueue = sharedQueue;
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
        this.defaultLogger = enabled ? create("avatar-js") : NULL_LOGGER;
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

    public void log(final LogEvent event) {
        queue.log(event);
    }

    public void shutdown() {
        if (!sharedQueue) {
            queue.shutdown();
        }
    }

    private Logger create(final String name) {
        NEXT_ID.putIfAbsent(name, new AtomicInteger(0));
        return new FileLogger(this, logDirectory, name, NEXT_ID.get(name).getAndIncrement());
    }
}
