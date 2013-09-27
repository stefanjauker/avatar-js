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

(function(exports) {

    // Allowed flush values
    var clazz = Packages.net.java.avatar.js.zlib.ZlibConstants;
    exports.Z_NO_FLUSH      = clazz.Z_NO_FLUSH;
    exports.Z_PARTIAL_FLUSH = clazz.Z_PARTIAL_FLUSH;
    exports.Z_SYNC_FLUSH    = clazz.Z_SYNC_FLUSH;
    exports.Z_FULL_FLUSH    = clazz.Z_FULL_FLUSH;
    exports.Z_FINISH        = clazz.Z_FINISH;
    exports.Z_BLOCK         = clazz.Z_BLOCK;
    exports.Z_TREES         = clazz.Z_TREES;

    // Return codes for the compression/decompression functions
    exports.Z_OK            = clazz.Z_OK;
    exports.Z_STREAM_END    = clazz.Z_STREAM_END;
    exports.Z_NEED_DICT     = clazz.Z_NEED_DICT;
    exports.Z_ERRNO         = clazz.Z_ERRNO;
    exports.Z_STREAM_ERROR  = clazz.Z_STREAM_ERROR;
    exports.Z_DATA_ERROR    = clazz.Z_DATA_ERROR;
    exports.Z_MEM_ERROR     = clazz.Z_MEM_ERROR;
    exports.Z_BUF_ERROR     = clazz.Z_BUF_ERROR;
    exports.Z_VERSION_ERROR = clazz.Z_VERSION_ERROR;

    // Compression levels.
    exports.Z_NO_COMPRESSION      = clazz.Z_NO_COMPRESSION;
    exports.Z_BEST_SPEED          = clazz.Z_BEST_SPEED;
    exports.Z_BEST_COMPRESSION    = clazz.Z_BEST_COMPRESSION;
    exports.Z_DEFAULT_COMPRESSION = clazz.Z_DEFAULT_COMPRESSION;

    //Compression strategy
    exports.Z_FILTERED            = clazz.Z_FILTERED;
    exports.Z_HUFFMAN_ONLY        = clazz.Z_HUFFMAN_ONLY;
    exports.Z_RLE                 = clazz.Z_RLE;
    exports.Z_FIXED               = clazz.Z_FIXED;
    exports.Z_DEFAULT_STRATEGY    = clazz.Z_DEFAULT_STRATEGY;

    // Possible values of the data_type field (though see inflate()).
    exports.Z_BINARY   = clazz.Z_BINARY;
    exports.Z_TEXT     = clazz.Z_TEXT;
    exports.Z_ASCII    = clazz.Z_ASCII;
    exports.Z_UNKNOWN  = clazz.Z_UNKNOWN;

    // The deflate compression method (the only one supported in this version).
    exports.Z_DEFLATED   = clazz.Z_DEFLATED;

    // For initializing zalloc, zfree, opaque.
    exports.Z_NULL  = clazz.Z_NULL;

    function Gzip() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.Gzip(__avatar.eventloop);
        }
    }

    function Deflate() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.Deflate(__avatar.eventloop);
        }
    }

    function DeflateRaw() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.DeflateRaw(__avatar.eventloop);
        }
    }

    function Gunzip() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.Gunzip(__avatar.eventloop);
        }
    }

    function Inflate() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.Inflate(__avatar.eventloop);
        }
    }

    function InflateRaw() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.InflateRaw(__avatar.eventloop);
        }
    }
    function Unzip() {
        this.createPeer = function() {
            return new Packages.net.java.avatar.js.zlib.Unzip(__avatar.eventloop);
        }
    }
    exports.GZIP    = new Gzip();
    exports.DEFLATE = new Deflate();
    exports.DEFLATERAW = new DeflateRaw();
    exports.GUNZIP = new Gunzip();
    exports.INFLATE = new Inflate();
    exports.INFLATERAW = new InflateRaw();
    exports.UNZIP    = new Unzip();

    function init(windowBits,
        level,
        memLevel,
        strategy,
        dictionary) {
        if (!dictionary) {
            dictionary = new Buffer(0);
        }
        this.peer.init(windowBits, level, memLevel, strategy, dictionary._impl);
    }

    function reset() {
        this.peer.reset();
    }

    /*
     * _flush is the _flush type.
     * chunk is the input to compress.
     * inOff, offset inside the input.
     * availInBefore input Length
     * _buffer, output
     * t_offset, out offset
     * availOutBefore output length
     */
    function write(_flush,
        chunk,
        inOff,
        availInBefore,
        _buffer,
        t_offset,
        availOutBefore) {

        var res = new Object();
        var localCallback = function(name, args) {
            process.nextTick(function() {
                var availInAfter = args[0];
                var availOutAfter = args[1];
                res.callback(availInAfter, availOutAfter);
            });
        }

        if (chunk === null) { // No more input
            chunk = new Buffer(0);
        }

        this.peer.write(_flush, chunk._impl, inOff, availInBefore,
            _buffer._impl, t_offset, availOutBefore, localCallback);

        // This object will receive the callback from the caller (@see zlib.js)
        return res;
    }

    function Zlib(mode) {
        this.peer = mode.createPeer();
        var that = this;
        this.peer.onError(function(name, args) {
            var msg = args[0];
            var errno =args[1];
            that.onerror(msg, errno);
        });
        this.init = init;
        this.reset = reset;
        this.write = write;
    }

    Zlib.prototype.close = function() {
        this.peer.close();
    }
    exports.Zlib = Zlib;

});
