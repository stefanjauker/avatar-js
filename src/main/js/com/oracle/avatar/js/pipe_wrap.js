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
    var net = require("net");
    var dgram = require("dgram");
    var TCP = process.binding('tcp_wrap').TCP;
    var UDP = process.binding('udp_wrap').UDP;

    var JavaBuffer = Packages.com.oracle.avatar.js.buffer.Buffer;
    var PipeHandle = Packages.com.oracle.libuv.handles.PipeHandle;
    var TCPHandle = Packages.com.oracle.libuv.handles.TCPHandle;
    var UDPHandle = Packages.com.oracle.libuv.handles.UDPHandle;
    var loop = __avatar.eventloop.loop();

    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.com.oracle.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;

    exports.Pipe = Pipe;

    function Pipe(ipc, pipe) {
        events.EventEmitter.call(this);
        var that = this;

        // User context, used to check accept permission.
        Object.defineProperty(this, '_callerContext', { value: AccessController.getContext() });

        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                Object.defineProperty(that, '_pipe',
                    { value: pipe ? pipe : new PipeHandle(loop, ipc) });
            }
        }, avatarContext, LibUVPermission.HANDLE);

        this._pipe.readCallback = function(status, error, byteBuffer, handle, type) {
            if (byteBuffer) {
               var data = new Buffer(new JavaBuffer(byteBuffer));
               // Defined in uv.h (see UV_HANDLE_TYPE_MAP)
               var UV_NAMED_PIPE = 7;
               var UV_TCP = 12;
               var UV_UDP = 15;
               if (type === UV_NAMED_PIPE) {
                   var pipeHandle = AccessController.doPrivileged(new PrivilegedAction() {
                        run: function() {
                            return new PipeHandle(loop, handle, true);
                        }
                    }, avatarContext, LibUVPermission.HANDLE);
                   var p = new Pipe(true, pipeHandle);
                   that.onread(status, data, 0, p);
               } else if (type === UV_TCP) {
                   var socket = AccessController.doPrivileged(new PrivilegedAction() {
                        run: function() {
                            return new TCPHandle(loop, handle);
                        }
                    }, avatarContext, LibUVPermission.HANDLE);
                   var tcp = new TCP(socket);
                   tcp._connected = true;
                   that.onread(status, data, 0, tcp);
               } else if (type === UV_UDP) {
                   var datagram = AccessController.doPrivileged(new PrivilegedAction() {
                        run: function() {
                            return new UDPHandle(loop, handle);
                        }
                    }, avatarContext, LibUVPermission.HANDLE);
                   var udp = new UDP(datagram);
                   that.onread(status, data, 0, udp);
               } else {
                   that.onread(status, data);
               }
            } else {
                that.onread(status); // assert(status < 0);
            }
        };

        this._pipe.writeCallback = function(status, nativeException, req) {
            req.oncomplete(status, that, req);
        };

        this._pipe.connectCallback = function(status, nativeException, req) {
            if (status >= 0) {
                that._pipe.readStart();
            }
            req.oncomplete(status, that, req, true, true);
        };

        this._pipe.connectionCallback = function(status, nativeException) {
            var clientHandle = new Pipe();
            AccessController.doPrivileged(new PrivilegedAction() {
                run: function() {
                    that._pipe.accept(clientHandle._pipe);
                }
            }, that._callerContext);
            clientHandle._pipe.readStart();
            that.onconnection(status, clientHandle);
        };

        this._pipe.closeCallback = function() {
            if (that._closeCallback) {
                process.nextTick(that._closeCallback);
            }
        };

        this._pipe.shutdownCallback = function(status, nativeException, req) {
            req.oncomplete(status, that, req);
        };

        Object.defineProperty(this, 'writeQueueSize', { enumerable: true,
            get : function() {  return that._pipe ? that._pipe.writeQueueSize() : 0; }
        });
    }

    util.inherits(Pipe, events.EventEmitter);

    Pipe.prototype.open = function(fd) {
        this._pipe.open(fd);
    };

    Pipe.prototype.connect = function(req, name) {
        return this._pipe.connect(name, req);
    };

    Pipe.prototype.bind = function(address) {
        return this._pipe.bind(address);
    };

    Pipe.prototype.listen = function(backlog) {
        return this._pipe.listen(backlog);
    };

    Pipe.prototype.readStart = function() {
        return this._pipe.readStart();
    };

    Pipe.prototype.readStop = function() {
        return this._pipe.readStop();
    };

    Pipe.prototype.writeUtf8String = function(req, message, handle) {
        if (handle) {
            var send_handle;
            if (handle instanceof net.Socket ||
                handle instanceof net.Server ||
                handle instanceof process.binding('tcp_wrap').TCP) {
                send_handle = handle._connection;
            } else if (handle instanceof process.binding('pipe_wrap').Pipe) {
                send_handle = handle._pipe;
            } else if (handle instanceof dgram.Socket ||
                handle instanceof process.binding('udp_wrap').UDP) {
                send_handle = handle._udp;
            }
            var buffer  = new JavaBuffer(message, 'utf8');
            return this._pipe.write2(buffer.toStringContent(), send_handle, req);
        }
        return this._writeString(req, message, 'utf8');
     };

    Pipe.prototype.writeBuffer = function(req, message) {
        if (message._impl) message = message._impl; // unwrap if necessary
        return this._pipe.write(message.underlying(), req);
    };

    Pipe.prototype._writeString = function(req, string, encoding) {
        return this.writeBuffer(req, new JavaBuffer(string, encoding));
    };

    Pipe.prototype.writeAsciiString = function(req, data) {
        return this._writeString(req, data, 'ascii');
    };

    Pipe.prototype.writeUcs2String = function(req, data) {
        return this._writeString(req, data, 'ucs2');
    };

    Pipe.prototype.setBlocking = function(blocking) {
        return this._pipe.setBlocking(blocking);
    };

    Pipe.prototype.close = function(callback) {
        if (this._pipe) {
            this._pipe.readStop();
            var r = this._pipe.close();
            if (callback) {
                Object.defineProperty(this, '_closeCallback', {value: callback});
            }
            return r;
        }
    };

    Pipe.prototype.shutdown = function(req) {
        return this._pipe.shutdown(req);
    };

    Pipe.prototype.ref = function() {
        return this._pipe.ref();
    };

    Pipe.prototype.unref = function() {
        return this._pipe.unref();
    };

    var newError = function(exception) {
        var error = new Error(exception.getMessage());
        error.errno = exception.errno();
        error.code = exception.errnoString();
        return error;
    };

});
