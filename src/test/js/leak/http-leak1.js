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

var perf = require("../perf/common-perf");

var http = require('http');

var body = '';
if(process.argv[2] === '-large') {
    print('building large body...');
    for (var i = 0; i < 1024 * 1024; i++) {
        body += 'hello worldçoié\uD83D\uDC4D\n';
    }
    print('done building body');
} else {
    print('building small body.');
    body = 'hello worldçoié\uD83D\uDC4D\n';
}
var PORT = 9999;

var server = http.createServer(function(req, res) {
    res.writeHead(200, {
        'content-type': 'text/plain'
    });
    res.end(new Buffer(body));
});

server.listen(PORT, function() {
    console.log("listen");
    perf.startPerf(startClient, 100)
});

function startClient() {
    perf.actionStart();
    http.get({
        port: PORT
    }, function(res) {
        res.on('data', function(d) {
            console.log(d.toString());
        });
        res.on('end', function(d) {
            if (perf.canContinue()) {
                startClient();
            }
        });
    });
}

