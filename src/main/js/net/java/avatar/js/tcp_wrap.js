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

(function(exports, require) {

    var util = require("util");
    var events = require("events");

    var JavaBuffer = Packages.net.java.avatar.js.buffer.Buffer;
    var TCPHandle = Packages.net.java.libuv.handles.TCPHandle;
    var loop = __avatar.eventloop.loop();

    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.net.java.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;

    exports.TCP = TCP;

    function TCP(socket) {

        Object.defineProperty(this, '_writeWrappers', { value: [] });

        // User context, used to check accept permission.
        Object.defineProperty(this, '_callerContext', { value: AccessController.getContext() });

        var that = this;
        Object.defineProperty(this, 'writeQueueSize', { enumerable: true,
            get : function() { return that._connection ? that._connection.writeQueueSize() : 0 } });

        var clientHandle = AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                Object.defineProperty(that, '_connection', { value: socket ? socket : new TCPHandle(loop) });
            }
        }, avatarContext, LibUVPermission.HANDLE);

        this._connection.connectionCallback = function(status, nativeException) {
            if (status == -1) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            var clientHandle = new TCP();
            AccessController.doPrivileged(new PrivilegedAction() {
                run: function() {
                    that._connection.accept(clientHandle._connection);
                }
            }, that._callerContext);

            Object.defineProperty(clientHandle, '_connected', {value: true});
            clientHandle._connection.readStart();
            that.onconnection(status == -1 ? undefined : clientHandle);
        }

        this._connection.connectCallback = function(status, nativeException) {
            if (status == -1) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            } else {
                that._connection.readStart();
                Object.defineProperty(that, '_connected', {value: true});
            }
            that._connectWrapper.oncomplete(status, that, that._connectWrapper, true, true);
        }

        this._connection.readCallback = function(byteBuffer) {
            if (byteBuffer) {
                var buffer = new Buffer(new JavaBuffer(byteBuffer));
                that.onread(buffer, 0, buffer.length);
            } else {
                var errno = loop.getLastError().errnoString();
                process._errno = errno;
                that.onread(undefined, 0, 0);
            }
        }

        this._connection.writeCallback = function(status, nativeException) {
            if (status == -1) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            var wrapper = that._writeWrappers.shift();
            if (wrapper && wrapper.oncomplete) {
                wrapper.oncomplete(status, that, wrapper);
            }
        }

        this._connection.closeCallback = function() {
            if (that._closeCallback) {
                // net.js, line 422, fireErrorCallbacks uses nextTick to do
                // error handling. error handling MUST be handled before this close callback is called
                // otherwise all error handlers are removed and error handlers are not called.
                process.nextTick(that._closeCallback);
            }
        }

        this._connection.shutdownCallback = function(status, nativeException) {
            if (status == -1) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            if (that._shutdownWrapper) {
                that._shutdownWrapper.oncomplete(status, that, that._shutdownWrapper);
            }
        }
    }

    util.inherits(TCP, events.EventEmitter);

    TCP.prototype.bind = function(address, port) {
        try {
            this._connection.bind(address, port);
        } catch (err) {
            if(!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            this._connection = undefined;
            return -1;
        }
        return 0;
    }

    TCP.prototype.bind6 = function(address, port) {
        this.bind(address, port);
    }

    TCP.prototype.listen = function(backlog) {
        try {
            this._connection.listen(backlog);
        } catch (err) {
            if(!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            this._connection = undefined;
            return -1;
        }
        return 0;
    }

    TCP.prototype.connect6 = function(address, port) {
        return this.connect(address, port);
    }

    TCP.prototype.connect = function(address, port) {
        var wrapper = {address: address, port: port};
        Object.defineProperty(this, '_connectWrapper', {value: wrapper});
        try {
            this._connection.connect(address, port);
        }catch(err) {
            if(!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            return null;
        }
        return this._connectWrapper;
    }

    TCP.prototype.open = function(fd) {
        try {
            this._connection.open(fd);
        } catch (err) {
            if(!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            this._connection = undefined;
            return -1;
        }
        return 0;
    }

    TCP.prototype.readStart = function() {
        if (this._connected) {
            this._connection.readStart();
        }
    }

    TCP.prototype.readStop = function() {
        this._connection.readStop();
    }

    TCP.prototype.writeBuffer = function(data) {
        if (data._impl) data = data._impl; // unwrap if necessary
        var wrapper = {bytes: data.underlying().capacity()};
        this._writeWrappers.push(wrapper);
        Object.defineProperty(wrapper, '_socketHandle', { value: this.owner });
        this._connection.write(data.underlying());
        return wrapper;
    }

    TCP.prototype._writeString = function(string, encoding) {
        return this.writeBuffer(new JavaBuffer(string, encoding));
    }

    TCP.prototype.writeUtf8String = function(string) {
        return this._writeString(string, 'utf8');
    }

    TCP.prototype.writeAsciiString = function(data) {
        return this._writeString(data, 'ascii');
    }

    TCP.prototype.writeUcs2String = function(data) {
        return this._writeString(data, 'ucs2');
    }

    TCP.prototype.setNoDelay = function(enable) {
        this._connection.setNoDelay(enable);
    }

    TCP.prototype.setKeepAlive = function(enable, initialDelay) {
        this._connection.setKeepAlive(enable, initialDelay);
    }

    TCP.prototype._addressToJS = function(address) {
        return {
            port: address ? address.getPort() : 0,
            address: address ? address.getIp() : undefined,
            family: address ? address.getFamily() : undefined
        };
    }

    TCP.prototype.getsockname = function() {
        return this._addressToJS(this._connection.getSocketName());
    }

    TCP.prototype.getpeername = function() {
        return this._addressToJS(this._connected ? this._connection.getPeerName() : null);
    }

    TCP.prototype.setSimultaneousAccepts = function(enable) {
        return this._connection.setSimultaneousAccepts(enable);
    }

    TCP.prototype.close = function(cb) {
        if (this._connection) {
            this._connection.readStop();
            this._connection.close();
            if (cb) {
                Object.defineProperty(this, '_closeCallback', {value: cb});
            }
        }
    }

    TCP.prototype.shutdown = function() {
        var wrapper = {};
        Object.defineProperty(wrapper, '_socketHandle', { value: this.owner });
        Object.defineProperty(this, '_shutdownWrapper', { value: wrapper });
        this._connection.closeWrite();
        return wrapper;
    }

    TCP.prototype.ref = function() {
        this._connection.ref();
    }

    TCP.prototype.unref = function() {
        this._connection.unref();
    }

});
