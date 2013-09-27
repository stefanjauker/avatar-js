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

/**
 * Zlib constants defined in zlib-1.2.5/zlib.h
 */
public interface ZlibConstants {

    // Allowed flush values
    public static final int Z_NO_FLUSH = 0;
    public static final int Z_PARTIAL_FLUSH = 1;
    public static final int Z_SYNC_FLUSH = 2;
    public static final int Z_FULL_FLUSH = 3;
    public static final int Z_FINISH = 4;
    public static final int Z_BLOCK = 5;
    public static final int Z_TREES = 6;

    // Return codes for the compression/decompression functions
    public static final int Z_OK = 0;
    public static final int Z_STREAM_END = 1;
    public static final int Z_NEED_DICT = 2;

    // File system related errors. Never returned by the module
    public static final int Z_ERRNO = -1;

    // Invalid compression level, Invalid option, Invalid stream
    // This is the error code that makes the more sense in this module.
    public static final int Z_STREAM_ERROR = -2;

    // Never returned by the module
    public static final int Z_DATA_ERROR = -3;
    // Never returned by the module
    public static final int Z_MEM_ERROR = -4;
    // Never returned by the module
    public static final int Z_BUF_ERROR = -5;
    // Invalid Library version error.
    // Never returned by the module.
    public static final int Z_VERSION_ERROR = -6;

    // Compression levels.
    public static final int Z_NO_COMPRESSION = 0;
    public static final int Z_BEST_SPEED = 1;
    public static final int Z_BEST_COMPRESSION = 9;
    public static final int Z_DEFAULT_COMPRESSION = -1;

    // Compression strategy
    public static final int Z_FILTERED = 1;
    public static final int Z_HUFFMAN_ONLY = 2;
    public static final int Z_RLE = 3;
    public static final int Z_FIXED = 4;
    public static final int Z_DEFAULT_STRATEGY = 0;

    // Possible values of the data_type field (though see inflate()).
    public static final int Z_BINARY = 0;
    public static final int Z_TEXT = 1;
    public static final int Z_ASCII = Z_TEXT;   /* for compatibility with 1.2.2 and earlier */
    public static final int Z_UNKNOWN = 2;

    // The deflate compression method (the only one supported in this version).
    public static final int Z_DEFLATED = 8;

    // For initializing zalloc, zfree, opaque.
    public static final int Z_NULL = 0;
}
