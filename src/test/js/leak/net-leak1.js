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


var perf = require("../perf/common-perf");

var net = require('net');

var body = 'hello world\n';
var PORT;

if (process.argv[2] === '-pipe') {
    PORT = require("../../../../test/common.js").PIPE;
    process.on('exit', function() {
        require('fs').unlinkSync(PORT);
    })
} else {
    PORT = 9999;
}

var server = net.createServer(function(res) {
    res.end(body);
});

server.listen(PORT, function() {
    console.log("listen");
    perf.startPerf(startClient, 100)
});

function startClient() {
    perf.actionStart();
    var client = net.connect(PORT, function() {
        client.on('data', function(d) {
            
        });
        client.on('end', function(d) {
            if (perf.canContinue()) {
                startClient();
            }
        });
    });
}

