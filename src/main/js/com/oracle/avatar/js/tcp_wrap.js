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

    var JavaBuffer = Packages.com.oracle.avatar.js.buffer.Buffer;
    var TCPHandle = Packages.com.oracle.libuv.handles.TCPHandle;
    var loop = __avatar.eventloop.loop();

    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.com.oracle.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;

    exports.TCP = TCP;

    function TCP(socket) {

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
            if (status < 0) {
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
            that.onconnection(status, clientHandle);
        }

        this._connection.connectCallback = function(status, nativeException, req) {
            if (status < 0) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            } else {
                that._connection.readStart();
                Object.defineProperty(that, '_connected', {value: true});
            }
            req.oncomplete(status, that, req, true, true);
        }

        this._connection.readCallback = function(status, nativeException, byteBuffer) {
            if (byteBuffer) {
                var buffer = new Buffer(new JavaBuffer(byteBuffer));
                that.onread(status, buffer);
            } else {
                process._errno = nativeException.errnoString();
                that.onread(status);
            }
        }

        this._connection.writeCallback = function(status, nativeException, req) {
            if (status < 0) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            if (req && req.oncomplete) {
                req.oncomplete(status, that, req);
            }
        }

        this._connection.closeCallback = function() {
            if (that._closeCallback) {
                process.nextTick(that._closeCallback);
            }
        }

        this._connection.shutdownCallback = function(status, nativeException, req) {
            if (status < 0) {
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            req.oncomplete(status, that, req);
        }
    }

    util.inherits(TCP, events.EventEmitter);

    TCP.prototype.bind = function(address, port) {
        try {
            this._connection.bind(address, port);
        } catch (err) {
            if (!err.errnoString) {
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
            if (!err.errnoString) {
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

    TCP.prototype.connect = function(req, address, port) {
        try {
            return this._connection.connect(address, port, req);
        } catch (err) {
            if (!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            return err;
        }
    }

    TCP.prototype.open = function(fd) {
        try {
            this._connection.open(fd);
        } catch (err) {
            if (!err.errnoString) {
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

    TCP.prototype.writeBuffer = function(req, data) {
        if (data._impl) data = data._impl; // unwrap if necessary
        return this._connection.write(data.underlying(), req);
    }

    TCP.prototype._writeString = function(req, string, encoding) {
        return this.writeBuffer(req, new JavaBuffer(string, encoding));
    }

    TCP.prototype.writeUtf8String = function(req, string) {
        return this._writeString(req, string, 'utf8');
    }

    TCP.prototype.writeAsciiString = function(req, data) {
        return this._writeString(req, data, 'ascii');
    }

    TCP.prototype.writeUcs2String = function(req, data) {
        return this._writeString(req, data, 'ucs2');
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

    TCP.prototype.shutdown = function(req) {
        return this._connection.shutdown(req);
    }

    TCP.prototype.ref = function() {
        this._connection.ref();
    }

    TCP.prototype.unref = function() {
        this._connection.unref();
    }

});
