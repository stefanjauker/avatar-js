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
var PORT = 9999;
var assert = require('assert');
var tls = require('tls');
var fs = require('fs');

var clientConnected = 0;
var serverConnected = 0;
var NUM_CONNECTED = 50;
var hosterr = 'Hostname/IP doesn\'t match certificate\'s altnames';
var expectedData=[];
var receivedData=[];
var dataReceived = 0;
for(var i = 0; i < NUM_CONNECTED; i++){
    expectedData[i] = "hello";
}

var options = {
    key: fs.readFileSync("test/fixtures/keys/agent1-key.pem"),
    cert: fs.readFileSync("test/fixtures/keys/agent1-cert.pem")
};

var server = tls.Server(options, function(socket) {
    socket.end("hello");
    if (++serverConnected === NUM_CONNECTED) {
        server.close();
    }
});

server.listen(PORT, function() {
    for(var i = 0; i < NUM_CONNECTED; i++){
        doTest();

    }

});
function doTest() {
    var client = tls.connect({ ca: [fs.readFileSync("test/fixtures/keys/ca1-cert.pem")],
        port: PORT, rejectUnauthorized:false
    }, function() {
        ++clientConnected;
        var authorized = client.authorized ||
                       client.authorizationError === hosterr;
        print("Client " + clientConnected);
        assert.equal(authorized, true);
    });
    client.on('data', function(data){
        receivedData[dataReceived] = data;
        assert.equal(data, "hello");
        ++dataReceived;
        client.end();
    });
    client.on('error', function(e) {
        print('ERROR ' + e);
    });
}
process.on('exit', function() {
    assert.equal(clientConnected, NUM_CONNECTED);
    assert.equal(serverConnected, NUM_CONNECTED);
    assert.equal(receivedData.length, expectedData.length);
});
