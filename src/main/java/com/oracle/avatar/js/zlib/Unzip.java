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

import java.io.IOException;
import java.io.InputStream;

import com.oracle.avatar.js.eventloop.EventLoop;

/**
 * Read GZIP or Deflate compressed input and write uncompressed to output.
 */
public final class Unzip extends UncompressWriter {

    public Unzip(final EventLoop eventLoop) {
        super(eventLoop);
    }

    @Override
    protected InputStream createInputStream(final byte[] rawChunk, final InputStream istream) throws IOException {
        if (rawChunk == null || rawChunk.length < 2) {
            throw new IllegalArgumentException("Invalid chunk");
        }
        if (isGZIP(rawChunk)) {
            return new Gunzip(eventLoop).createInputStream(rawChunk, istream);
        }
        return new Inflate(eventLoop).createInputStream(rawChunk, istream);
    }

     private static boolean isGZIP(final byte[] rawChunk) throws IOException {
         // Defined in GZIP specification
         return rawChunk[0] == (byte) 0x1f && rawChunk[1] == (byte) 0x8b;
        }
}
