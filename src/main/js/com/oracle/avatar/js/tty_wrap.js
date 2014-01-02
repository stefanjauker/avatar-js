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

    var JavaBuffer = Packages.com.oracle.avatar.js.buffer.Buffer;
    var Mode = Packages.com.oracle.libuv.handles.TTYHandle.Mode;
    var TTYHandle = Packages.com.oracle.libuv.handles.TTYHandle;
    var loop = __avatar.eventloop.loop();

    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.com.oracle.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;

    exports.TTY = TTY;

    exports.isTTY = function(fd) {
        return TTYHandle.isTTY(fd);
    }

    exports.guessHandleType = function(fd) {
        return TTYHandle.guessHandleType(fd);
    }

    function TTY(fd, readable) {

        var that = this;
        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                Object.defineProperty(that, '_tty',
                    { value: new TTYHandle(loop, fd, readable) });
            }
        }, avatarContext, LibUVPermission.HANDLE);

        this._tty.readCallback = function(byteBuffer) {
            if (byteBuffer) {
                var buffer = new Buffer(new JavaBuffer(byteBuffer));
                that.onread(buffer, 0, buffer.length);
            } else {
                var errno = loop.getLastError().errnoString();
                process._errno = errno;
                that.onread(undefined, 0, 0);
            }
        }

        this._tty.writeCallback = function(status, nativeException) {
            if (status == -1) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            var wrapper = that._writeWrappers.shift();
            if (wrapper && wrapper.oncomplete) {
                wrapper.oncomplete(status, that, wrapper);
            }
        }

        Object.defineProperty(this, '_writeWrappers', { value: [] });

        Object.defineProperty(this, 'writeQueueSize', { enumerable: true,
            get : function() {  return that._tty ? that._tty.writeQueueSize() : 0 } } );
    }

    TTY.prototype.readStart = function() {
        this._tty.readStart();
    }

    TTY.prototype.readStop = function() {
        this._tty.readStop();
    }

    TTY.prototype.writeBuffer = function(data) {
        if (data._impl) data = data._impl; // unwrap if necessary
        var wrapper = {bytes: data.underlying().capacity()};
        this._writeWrappers.push(wrapper);
        this._tty.write(data.underlying());
        return wrapper;
    }

    TTY.prototype._writeString = function(string, encoding) {
        return this.writeBuffer(new JavaBuffer(string, encoding));
    }

    TTY.prototype.writeUtf8String = function(string) {
        return this._writeString(string, 'utf8');
    }

    TTY.prototype.writeAsciiString = function(data) {
        return this._writeString(data, 'ascii');
    }

    TTY.prototype.writeUcs2String = function(data) {
        return this._writeString(data, 'ucs2');
    }

    TTY.prototype.close = function(callback) {
        if (this._tty) {
            this._tty.readStop();
            this._tty.close();
            if (callback) {
                callback();
            }
        }
    }

    TTY.prototype.setRawMode = function(mode) {
        if (mode == 0) {
            this._tty.setMode(Mode.NORMAL);
        } else if (mode == 1) {
            this._tty.setMode(Mode.RAW);
        }
    }

    TTY.prototype.getWindowSize = function() {
        return this._tty.getWindowSize();
    }

    TTY.prototype.ref = function() {
        this._tty.ref();
    }

    TTY.prototype.unref = function() {
        this._tty.unref();
    }

});
