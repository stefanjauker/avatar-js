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

    var JavaBuffer = Packages.net.java.avatar.js.buffer.Buffer;
    var UDPHandle = Packages.net.java.libuv.handles.UDPHandle;
    var loop = __avatar.eventloop.loop();
    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.net.java.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;
    
    exports.UDP = UDP;

    function UDP(dgram) {
        var that = this;
        
        Object.defineProperty(this, '_writeWrappers', { value: [] });
        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                Object.defineProperty(that, '_udp', 
                { value: dgram ? dgram : new UDPHandle(loop) });
            }
        }, avatarContext, Java.to([LibUVPermission.HANDLE], "java.security.Permission[]"));
        

        this._udp.recvCallback = function(args) {
            var nread = args[0];
            var buffer = new Buffer(new JavaBuffer(args[1]));
            var rinfo = args[2];
            that.onmessage(that, buffer, 0, buffer.length, { address: rinfo.getIp(), port: rinfo.getPort() })
        }

        this._udp.sendCallback = function(args) {
            var status = args[0];
            if (status == -1) {
                var nativeException = args[1];
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            var wrapper = that._writeWrappers.shift();
            if (wrapper && wrapper.oncomplete) {
                wrapper.oncomplete(status, that, wrapper, wrapper._buffer);
            }
        }
    }

    UDP.prototype.bind = function(address, port, flags) {
        return this._udp.bind(port, address);
    }

    UDP.prototype.bind6 = function(address, port, flags) {
        return this._udp.bind6(port, address);
    }

    UDP.prototype.send = function(buffer, offset, length, port, ip) {
        var wrapper = {_buffer: buffer};
        this._writeWrappers.push(wrapper);
        this._udp.send(buffer._impl.array(), offset, length, port, ip);
        return wrapper;
    }

    UDP.prototype.send6 = function(buffer, offset, length, port, ip) {
        var wrapper = {_buffer: buffer};
        this._writeWrappers.push(wrapper);
        this._udp.send6(buffer._impl.array(), offset, length, port, ip);
        return wrapper;
    }

    UDP.prototype.recvStart = function() {
        this._udp.recvStart();
    }

    UDP.prototype.recvStop = function() {
        this._udp.recvStop();
    }

    UDP.prototype.close = function() {
        this._udp.close();
    }

    UDP.prototype.getsockname = function() {
        var address = this._udp.address();
        return {
            address: address.getIp(),
            port: address.getPort(),
            family: address.getFamily()
        };
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
        this._udp.ref();
    }

    UDP.prototype.unref = function() {
        this._udp.unref();
    }
});
