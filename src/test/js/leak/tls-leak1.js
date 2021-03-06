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
var common = require('../../../../test/common');
var tls = require('tls');
var fs = require('fs');
var PORT = 9999;
var body = 'hello worldçoié\uD83D\uDC4D\n';
for(var arg in process.argv) {
    switch(process.argv[arg]) {
        case '-large': {
            print('building large body...');
            for (var i = 0; i < 1024 * 1024; i++) {
                body += 'hello worldçoié\uD83D\uDC4D\n';
            }
            print('done building body');  
        }
    }
}
var options = {
  key: fs.readFileSync(common.fixturesDir + '/keys/agent1-key.pem'),
  cert: fs.readFileSync(common.fixturesDir + '/keys/agent1-cert.pem')
};

var server = tls.createServer(options, function(res) {
    res.end(body);
});

server.listen(PORT, function() {
    console.log("listen");
    perf.startPerf(startClient, 100)
});

function startClient() {
    perf.actionStart();
    var client = tls.connect({rejectUnauthorized: false, port:PORT}, function() {
        client.on('data', function(d) {
            
        });
        client.on('end', function(d) {
            if (perf.canContinue()) {
                startClient();
            }
        });
    });
}

