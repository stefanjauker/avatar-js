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

package com.oracle.avatar.js.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class FileLogger implements Logger {

    private static final String FORMAT =
            "%d %d %s" +
            (File.separatorChar == '\\' ? "\r\n" : "\n");

    private final Logging logging;
    private final long start;
    private final PrintWriter writer;
    private final AtomicInteger sequence = new AtomicInteger(0);

    private volatile boolean enabled;

    @SuppressWarnings("resource")
    FileLogger(final Logging logging, final File dir, final String name, final int id) {
        this.logging = logging;
        this.enabled = true; // enabled by default

        OutputStream os = null;
        try {
            // aesthetic - do not number the first instance
            final String fileName = name + (id == 0 ? "" : id) + ".log";
            os = new FileOutputStream(new File(dir, fileName));
        } catch (final FileNotFoundException ignore) {
            // fallback to logging to stdout if we failed to create the log file
            os = System.out;
        }
        writer = new PrintWriter(os);
        start = System.currentTimeMillis();
    }

    @Override
    public void log(final Throwable throwable) {
        final StringWriter sw = new StringWriter(64 * 1024);
        try (PrintWriter inMemoryWriter = new PrintWriter(sw)) {
            throwable.printStackTrace(inMemoryWriter);
            logging.log(new LogEvent(sw.toString(), writer));
        }
    }

    @Override
    public void log(final String message) {
        if (enabled) {
            final String m = String.format(FORMAT,
                            sequence.incrementAndGet(), // log event sequence number
                            System.currentTimeMillis() - start, // ms since start
                            message);
            logging.log(new LogEvent(m, writer));
        }
    }

    @Override
    public void log(final String format, final Object... args) {
        if (enabled) {
            log(String.format(format, args));
        }
    }

    @Override
    public boolean enable() {
        final boolean was = enabled;
        enabled = true;
        return was;
    }

    @Override
    public boolean disable() {
        final boolean was = enabled;
        enabled = false;
        return was;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

}
