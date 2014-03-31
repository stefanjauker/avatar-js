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

    var LibUV = Packages.com.oracle.libuv.LibUV;
    var OS = Packages.com.oracle.avatar.js.os.OS;

    exports.getHostname = function() {
        return java.net.InetAddress.getLocalHost().getHostName();
    };

    exports.getOSType = function() {
        var r = OS.getType();
        return r ? r : undefined;
    };

    exports.getOSRelease = function() {
        var r = OS.getRelease();
        return r ? r : undefined;
    };

    exports.getUptime = function() {
        var r = LibUV.getUptime();
        return r ? r : undefined;
    };

    exports.getLoadAvg = function() {
        var r = LibUV.getLoadAvg();
        return r ? Java.from(r) : undefined;
    };

    exports.getTotalMem = function() {
        var r = LibUV.getTotalMem();
        return r < 0 ? undefined : r;
    };

    exports.getFreeMem = function() {
        var r = LibUV.getFreeMem();
        return r < 0 ? undefined : r;
    };

    exports.getCPUs = function() {
        var cpuinfoarray = [];
        var cpuinfo = LibUV.getCPUs();
        if (!cpuinfo) {
        	return undefined;
        }
        var numcpu = cpuinfo.length/7;
        var i = 0;
        for (var cpu = 0; cpu < numcpu; cpu++) {
            cpuinfoarray.push({
               model: cpuinfo[i++],
               speed: cpuinfo[i++],
               times: {
                   user: cpuinfo[i++],
                   nice: cpuinfo[i++],
                   sys: cpuinfo[i++],
                   idle: cpuinfo[i++],
                   irq: cpuinfo[i++]
               }
            });
        }
        return cpuinfoarray;
    };

    exports.getInterfaceAddresses = function() {
        var ret = {};
        var interfaces = java.net.NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            var iface = interfaces.nextElement();
            var addressArray = [];
            var addressIterator = iface.getInterfaceAddresses().iterator();
            while (addressIterator.hasNext()) {
                var interfaceAddress = addressIterator.next();
                var inetAddress = interfaceAddress.getAddress();

                var inetFamily;
                if (inetAddress instanceof java.net.Inet4Address) {
                    inetFamily = 'IPv4';
                } else if (inetAddress instanceof java.net.Inet6Address) {
                    inetFamily = 'IPv6';
                }

                addressArray.push({
                    address: inetAddress.getHostAddress(),
                    family: inetFamily,
                    internal: inetAddress.isLoopbackAddress()
                });
            }
            ret[iface.getName()] = addressArray;
        }
        return ret;
    };

    exports.getEndianness = function() {
        return OS.getEndianness();
    };

});
