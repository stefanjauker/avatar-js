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
var net = require('net');
var assert = require('assert');
var connected = false;
var closed = false;
var server = net.Server(function(connection) {
  console.error('SERVER got connection');
  connected = true;
  connection.end();
  server.close();
});

server.on('close', function() {closed = true;});
console.log("TCP PORT TO LISTEN TO " + global.SCRIPT_PORT);
server.listen(global.SCRIPT_PORT, function() {
    var c = net.createConnection({
            port: global.SCRIPT_PORT
        });
    c.end();
});

if(SCRIPT_ON_EXIT) {
    process.on('exit', function() {
        assert.ok(connected);
        SCRIPT_OK = connected;
        if (!closed) {
            server.close();
        }
    });
} else {
    process.on('exit', function() {
        if (!closed) {
            server.close();
        }
    });
}
