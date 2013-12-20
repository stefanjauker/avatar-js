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

// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

// net based benchmark similar to benchmark/http_simple.js
// call it with ab -c 100 -n 10000  http://127.0.0.1:8000/buffer/{buff size}

var path = require('path'),
net = require('net');

var port = parseInt(process.env.PORT || 8000);

var fixed = makeString(20 * 1024, 'C'),
storedBytes = {},
storedBuffer = {},
storedUnicode = {};
var HTTPParser = process.binding('http_parser').HTTPParser;
var CRLF = '\r\n';
var server = module.exports = net.createServer(function (con) {  
    con.on('error', function(e) {
        print("GOT ERROR " + e);
    });
    con.on('data', function(b) {
        var parser = new HTTPParser(HTTPParser.REQUEST);
        var req;
        parser.onMessageComplete = function() {
            var commands = req.url.split('/');
            var command = commands[1];
            var body = '';
            var arg = commands[2];
            if (command == 'buffer') {
                var n = ~~arg;
                if (n <= 0)
                    throw new Error('buffer called with n <= 0');
                if (storedBuffer[n] === undefined) {
                    storedBuffer[n] = new Buffer(n);
                    for (var i = 0; i < n; i++) {
                        storedBuffer[n][i] = 'C'.charCodeAt(0);
                    }
                }
                body = storedBuffer[n];
        
            } else {
                status = 404;
                body = 'not found\n';
            }
        
            var content_length = body.length.toString();
            var statusLine = 'HTTP/1.1 ' + '200' + ' ' +
            'OK' + CRLF;
            statusLine += 'Content-Type: text/plain' + CRLF + 'Content-Length: ' + content_length ;
            statusLine += CRLF;
            var slBuffer = new Buffer(statusLine);
            var complete = new Buffer(slBuffer.length + body.length);
            slBuffer.copy(complete, 0, 0, slBuffer.length);
            body.copy(complete, slBuffer.length, 0,  body.length);
            con.end(complete);
            parser.reinitialize(HTTPParser.REQUEST);
        }
        
        parser.onHeadersComplete = function(info) {
            req = info;          
        }
        
        parser.execute(b, 0, b.length);
    });

});

function makeString(size, c) {
    var s = '';
    while (s.length < size) {
        s += c;
    }
    return s;
}

server.listen(port, function () {
    if (module === require.main)
        console.error('Listening at http://127.0.0.1:'+port+'/');
});
