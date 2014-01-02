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

package com.oracle.avatar.js.zlib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import com.oracle.avatar.js.eventloop.EventLoop;

/**
 * Gzip compressor.
 */
public final class Gzip extends CompressWriter {

    public Gzip(final EventLoop eventLoop) {
        super(eventLoop);
    }

    @Override
    protected OutputStream createCompressionStream(final ByteArrayOutputStream out) throws IOException {
        return new ExtendedGZIPOutputStream(out);
    }

    /**
     * The GZIPOutputStream doesn't offer a constructor with initialization
     * parameter. This one does a best effort to map input parameters with
     * internal Deflater capabilities.
     */
    private final class ExtendedGZIPOutputStream extends GZIPOutputStream {

        public ExtendedGZIPOutputStream(final OutputStream out) throws IOException {
            super(out, true);
            def.setLevel(getLevel());
            def.setStrategy(getStrategy());
            if (getDictionary() != null) {
                def.setDictionary(getDictionary().array());
            }
        }
    }
}
