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

package net.java.avatar.js.zlib;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.java.avatar.js.eventloop.EventLoop;

/**
 * Read GZIP compressed input and write uncompressed to output.
 */
public final class Gunzip extends UncompressWriter {

    public Gunzip(final EventLoop eventLoop) {
        super(eventLoop);
    }

    @Override
    protected InputStream createInputStream(final byte[] rawChunk, final InputStream istream) throws IOException {
        return new ExtendedGZIPInputStream(istream);
    }
    /**
     * The GZIPInputStream doesn't offer a constructor with initialization
     * parameter. This one does a best effort to map input parameters with
     * internal Deflater capabilities.
     */
    private final class ExtendedGZIPInputStream extends GZIPInputStream {

        public ExtendedGZIPInputStream(final InputStream out) throws IOException {
            super(out);
            if (getDictionary() != null) {
                inf.setDictionary(getDictionary().array());
            }
        }
    }
}
