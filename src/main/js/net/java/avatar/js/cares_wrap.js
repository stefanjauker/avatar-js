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

// this module wraps the java networking APIs instead of c-ares

(function(exports, require) {

    var net = require('net');

    var LibUV = Packages.net.java.libuv.LibUV;
    var dns = __avatar.eventloop.dns();

    var mapJavaException = function(e) {
        if (e === null) {
            return null;
        }
        var code;
        var message;
        if (e instanceof java.net.UnknownHostException) {
            code = 'ENOTFOUND';
            message = 'domain name not found.';
        } else {
            code = 'ENODATA';
            message = 'domain has no data.'
        }
        var error = new Error(code + ', ' + message + ' \'' + e.message + '\'');
        process._errno = error.code = code;
        return error;
    }

    exports.queryA = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'A', function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                values[i] = results[i];
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    exports.queryAaaa = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'AAAA', function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                values[i] = results[i];
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    exports.queryCname = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'CNAME', function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                values[i] = results[i];
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    exports.queryMx = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'MX', function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                var record = results[i].split(" ");
                var priority = parseInt(record[0]);
                var exchange = record[1];
                values[i] = new MxRecord(priority, exchange);
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    function MxRecord(priority, exchange) {
        this.priority = priority;
        this.exchange = exchange;
    }

    exports.queryNs = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'NS', function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                values[i] = results[i];
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    exports.queryTxt = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'TXT', function(name, args) {
            var results = args[1];
            var values = [];
            var txtvalues = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                var txt = results[i].split(" ");
                for (var j = 0; j < txt.length; j++) {
                    txtvalues[j] = txt[j];
                }
                values[i] = txtvalues;
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    exports.querySrv = function(address, callback) {
        var wrapper = new RequestWrapper();
        dns.query(address, 'SRV', function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                var srv = results[i].split(" ");
                var priority = parseInt(srv[0]);
                var weight = parseInt(srv[1]);
                var port = parseInt(srv[2]);
                var target = srv[3];
                values[i] = new Srv(priority, weight, port, target);
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    function Srv(priority, weight, port, target) {
        this.priority = priority;
        this.weight = weight;
        this.port = port;
        this.name = target;
    }

    exports.getHostByAddr = function(address, callback) {
        if (!net.isIP(address)) {
            var error = new Error(address);
            error.errno = 'ENOTIMP';
            throw error;
        }
        var wrapper = new RequestWrapper();
        dns.getHostByAddress(address, function(name, args) {
            var results = args[1];
            var values = [];
            var len = results ? results.length : 0;
            for (var i = 0; i < len; i++) {
                values[i] = results[i];
            }
            callback(mapJavaException(args[0]), values);
        });
        return wrapper;
    }

    exports.getaddrinfo = function(hostname) {
        var wrapper = new RequestWrapper();
        dns.getAddressByHost(hostname, function(name, args) {
            var callback = wrapper.oncomplete;
            var ex = args[0];
            if (ex) {
                mapJavaException(ex);
                callback();
            } else {
                var results = args[1];
                var values = [];
                var len = results ? results.length : 0;
                for (var i = 0; i < len; i++) {
                    values[i] = results[i];
                }
                callback(values);
            }
        });
        return wrapper;
    }

    exports.isIP = function(input) {
        if (!input) {
            return 0;
        } else if (/^(\d?\d?\d)\.(\d?\d?\d)\.(\d?\d?\d)\.(\d?\d?\d)$/.test(input)) {
            var parts = input.split('.');
            for (var i = 0; i < parts.length; i++) {
                var part = parseInt(parts[i]);
                if (part < 0 || 255 < part) {
                    return 0;
                }
            }
            return 4;
        } else {
            if (LibUV.isIPv6(input)) {
                return 6;
            }
            return 0;
        }
    }

    function RequestWrapper() {
    }

});
