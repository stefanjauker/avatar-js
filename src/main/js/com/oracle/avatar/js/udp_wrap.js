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
    var UDPHandle = Packages.com.oracle.libuv.handles.UDPHandle;
    var loop = __avatar.eventloop.loop();
    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.com.oracle.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;

    exports.UDP = UDP;

    function UDP(dgram) {
        var that = this;

        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                Object.defineProperty(that, '_udp',
                { value: dgram ? dgram : new UDPHandle(loop) });
            }
        }, avatarContext, LibUVPermission.HANDLE);


        this._udp.recvCallback = function(nread, data, rinfo) {
            var buffer = new Buffer(new JavaBuffer(data));
            that.onmessage(nread, that, buffer, { address: rinfo.getIp(), port: rinfo.getPort() })
        }

        this._udp.sendCallback = function(status, nativeException, req) {
            var callback = req.oncomplete;
            if (callback) {
                callback.call(req, status);
            }
        }
    }

    UDP.prototype.bind = function(address, port, flags) {
        return this._udp.bind(port, address);
    }

    UDP.prototype.bind6 = function(address, port, flags) {
        return this._udp.bind6(port, address);
    }

    UDP.prototype.send = function(req, buffer, offset, length, port, ip, callback) {
        return this._udp.send(buffer._impl.underlying(), offset, length, port, ip, req);
    }

    UDP.prototype.send6 = function(req, buffer, offset, length, port, ip, callback) {
        return this._udp.send6(buffer._impl.underlying(), offset, length, port, ip, req);
    }

    UDP.prototype.recvStart = function() {
        return this._udp.recvStart();
    }

    UDP.prototype.recvStop = function() {
        return this._udp.recvStop();
    }

    UDP.prototype.close = function() {
        return this._udp.close();
    }

    UDP.prototype.getsockname = function(out) {
        var address = this._udp.address();
        out.address = address.getIp();
        out.port = address.getPort();
        out.family = address.getFamily();
    }

    UDP.prototype.setMulticastTTL = function(ttl) {
        return this._udp.setMulticastTTL(ttl);
    }

    UDP.prototype.setMulticastLoopback = function(flag) {
        return this._udp.setMulticastLoop(flag);
    }

    UDP.prototype.setBroadcast = function(flag) {
        return this._udp.setBroadcast(flag);
    }

    UDP.prototype.setTTL = function(ttl) {
        return this._udp.setTTL(ttl);
    }

    UDP.prototype.addMembership = function(multicastAddress, interfaceAddress) {
        return this._udp.setMembership(multicastAddress, interfaceAddress, this._udp.Membership.JOIN_GROUP);
    }

    UDP.prototype.dropMembership = function(multicastAddress, interfaceAddress) {
        return this._udp.setMembership(multicastAddress, interfaceAddress, this._udp.Membership.LEAVE_GROUP);
    }

    UDP.prototype.ref = function() {
        return this._udp.ref();
    }

    UDP.prototype.unref = function() {
        return this._udp.unref();
    }

    var newError = function(exception) {
        var error = new Error(exception.getMessage());
        error.errno = exception.errno()
        error.code = exception.errnoString();
        return error;
    }
});
