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

/* 
 * Http server treats responses in a thread
 * and post an event to send back the reponse
 * up to maxClients are handled in parallel.
*/
var perf = require("../perf/common-perf");
var maxClients = 50;

var http = require('http');
var PORT = 9999;
var srvCount = 0;
var server = http.createServer(function(req, res) {
    print("Num treated requests  in // " + srvCount);
    print(__avatar.eventloop);
    srvCount++;
    var handle = __avatar.eventloop.acquire();
    var captureResponse = function(response) {
        return function() {
            srvCount --;
            response.end("DONE");
        }
    }
    var captured = captureResponse(res);
    __avatar.eventloop.submit(function() {
        java.lang.Thread.sleep(1);
        for(var i = 0; i < 10000000; i++) {
            var j = i;
        } 
        for(var i = 0; i < 1000; i++) {
            __avatar.eventloop.post(function() {});
        } 
        __avatar.eventloop.post(captured);
        handle.release();
    });
});

server.listen(PORT, function() {
    console.log("listen");
    perf.startPerf(startClient, 100)
});
var count = 0;
function startClient() {
    if(count < maxClients) {
        perf.actionStart();
        count++;
        http.get({
            port: PORT,
            agent:new http.Agent({
                maxSockets:maxClients
            })
        }, function(res) {
            res.on("data", function(d) {
            })
            res.on("end", function(d) {
                count--;
                if (perf.canContinue()) {
                    startClient();
                }
            })
        });
    
        setTimeout(function() { 
            if (perf.canContinue()) {
                startClient();
            }
        }, 20);
    }

}

