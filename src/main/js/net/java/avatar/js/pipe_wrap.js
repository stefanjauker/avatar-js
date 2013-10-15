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

    var JavaBuffer = Packages.net.java.avatar.js.buffer.Buffer;
    var PipeHandle = Packages.net.java.libuv.handles.PipeHandle;
    var TCPHandle = Packages.net.java.libuv.handles.TCPHandle;
    var UDPHandle = Packages.net.java.libuv.handles.UDPHandle;
    var loop = __avatar.eventloop.loop();

    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.net.java.libuv.LibUVPermission;
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
        }, avatarContext, [LibUVPermission.HANDLE]);

        this._pipe.readCallback = function(args) {
            if (args && args.length > 0 && args[0]) {
               process._errno = undefined;
               var data = new Buffer(new JavaBuffer(args[0]));
               if (args.length == 3) {
                   // Defined in uv.h
                   var UV_NAMED_PIPE = 7
                   var UV_TCP = 12;
                   var UV_UDP = 15;
                   if (args[2] == UV_NAMED_PIPE) {
                       var pipeHandle = AccessController.doPrivileged(new PrivilegedAction() {
                            run: function() {
                                return new PipeHandle(loop, args[1], true);
                            }
                        }, avatarContext, [LibUVPermission.HANDLE]);
                       var p = new Pipe(pipeHandle);
                       that.onread(data, 0, data.length, p);
                   } else if (args[2] == UV_TCP) {
                       var socket = AccessController.doPrivileged(new PrivilegedAction() {
                            run: function() {
                                return new TCPHandle(loop, args[1]);
                            }
                        }, avatarContext, [LibUVPermission.HANDLE]);
                       var tcp = new TCP(socket);
                       that.onread(data, 0, data.length, tcp);
                   } else if (args[2] == UV_UDP) {
                       var datagram = AccessController.doPrivileged(new PrivilegedAction() {
                            run: function() {
                                return new UDPHandle();
                            }
                        }, avatarContext, [LibUVPermission.HANDLE]);
                       var udp = new dgram.UDP(datagram);
                       that.onread(data, 0, data.length, udp);
                   } else {
                       that.onread(data, 0, data.length);
                   }
               } else {
                   that.onread(data, 0, data.length);
               }
            } else {
                var errno = 'EOF';
                process._errno = errno;
                that.onread(undefined, 0, 0);
            }
        }

        this._pipe.writeCallback = function(args) {
            var status = args[0];
            if (status == -1) {
                var nativeException = args[1];
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            var wrapper = that._writeWrappers.shift();
            if (wrapper && wrapper.oncomplete) {
                wrapper.oncomplete(status, that, wrapper);
            }
        }

        this._pipe.connectCallback = function(args) {
            var status = args[0];
            if (status == -1) {
                var nativeException = args[1];
                var errno = nativeException.errnoString();
                process._errno = errno;
            } else {
                that._pipe.readStart();
            }
            that._connectWrapper.oncomplete(status, that, that, true, true);
        }

        this._pipe.connectionCallback = function(args) {
            var status = args[0];
            if (status == -1) {
                var nativeException = args[1];
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            var clientHandle = new Pipe();
            AccessController.doPrivileged(new PrivilegedAction() {
                run: function() {
                    that._pipe.accept(clientHandle._pipe);
                }
            }, that._callerContext);
            clientHandle._pipe.readStart();
            that.onconnection(status == -1 ? undefined : clientHandle);
        }

        this._pipe.closeCallback = function(args) {
            if (that._closeCallback) {
                // net.js, line 422, fireErrorCallbacks uses nextTick to do
                // error handling. error handling MUST be handled before this close callback is called
                // otherwise all error handlers are removed and error handlers are not called.
                process.nextTick(that._closeCallback);
            }
        }

        this._pipe.shutdownCallback = function(args) {
            var status = args[0];
            if (status == -1) {
                var nativeException = args[1];
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            if (that._shutdownWrapper) {
                that._shutdownWrapper.oncomplete(status, that, undefined);
            }
        }

        Object.defineProperty(this, '_writeWrappers', { value: [] });

        Object.defineProperty(this, 'writeQueueSize', { enumerable: true,
            get : function() {  return that._pipe ? that._pipe.writeQueueSize() : 0 } } );
    }

    util.inherits(Pipe, events.EventEmitter);

    Pipe.prototype.open = function(fd) {
        this._pipe.open(fd);
    }

    Pipe.prototype.connect = function(name) {
        var wrapper =  {};
        Object.defineProperty(this, '_connectWrapper', {value: wrapper});
        this._pipe.connect(name);
        return this._connectWrapper;
    }

    Pipe.prototype.bind = function(address) {
        this._pipe.bind(address);
    }

    Pipe.prototype.listen = function(backlog) {
        try {
            this._pipe.listen(backlog);
        } catch (err) {
            process._errno = err.errnoString();
            this._pipe = undefined;
            return err;
        }
    }

    Pipe.prototype.readStart = function() {
        this._pipe.readStart();
    }

    Pipe.prototype.readStop = function() {
        this._pipe.readStop();
    }

    Pipe.prototype.writeUtf8String = function(message, handle) {
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
            var wrapper = {bytes: buffer.array().length};
            this._writeWrappers.push(wrapper);
            this._pipe.write2(buffer.toStringContent(), send_handle);
            return wrapper;
        }
        return this._writeString(message, 'utf8');
     }

    Pipe.prototype.writeBuffer = function(message) {
        if (message._impl) message = message._impl; // unwrap if necessary
        var wrapper = {bytes: message.array().length};
        this._writeWrappers.push(wrapper);
        this._pipe.write(message.array());
        return wrapper;
    }

    Pipe.prototype._writeString = function(string, encoding) {
        return this.writeBuffer(new JavaBuffer(string, encoding));
    }

    Pipe.prototype.writeAsciiString = function(data) {
        return this._writeString(data, 'ascii');
    }

    Pipe.prototype.writeUcs2String = function(data) {
        return this._writeString(data, 'ucs2');
    }

    Pipe.prototype.close = function(callback) {
        if (this._pipe) {
            this._pipe.readStop();
            this._pipe.close();
            if (callback) {
                Object.defineProperty(this, '_closeCallback', {value: callback});
            }
        }
    }

    Pipe.prototype.shutdown = function() {
        var wrapper = {};
        Object.defineProperty(this, '_shutdownWrapper', { value: wrapper });
        this._pipe.closeWrite();
        return wrapper;
    }

    Pipe.prototype.ref = function() {
      this._pipe.ref();
    }

    Pipe.prototype.unref = function() {
      this._pipe.unref();
    }

});
