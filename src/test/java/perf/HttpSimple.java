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

package perf;

import java.nio.ByteBuffer;
import com.oracle.httpparser.HttpParser;
import com.oracle.httpparser.HttpParserSettings;
import com.oracle.libuv.LibUV;
import com.oracle.libuv.cb.StreamCloseCallback;
import com.oracle.libuv.cb.StreamConnectionCallback;
import com.oracle.libuv.cb.StreamReadCallback;
import com.oracle.libuv.handles.LoopHandle;
import com.oracle.libuv.handles.TCPHandle;

/* Java based benchmark similar to benchmark/http_simple.js
 * call it with ab -c 100 -n 10000  http://127.0.0.1:8000/buffer/{buff size}
 * no support for chunks.
 */
public class HttpSimple {

    private static class Req {

        private String url;
        private String[] headers;
    }

    public static void main(String[] args) throws Exception {
        LibUV.cwd();
        final LoopHandle loop = new LoopHandle();
        final TCPHandle server = new TCPHandle(loop);
        final String CRLF = "\r\n";
        server.setConnectionCallback(new StreamConnectionCallback() {
            @Override
            public void onConnection(int status, Exception error) throws Exception {
                final TCPHandle peer = new TCPHandle(loop);

                server.accept(peer);
                peer.setReadCallback(new StreamReadCallback() {
                    @Override
                    public void onRead(final ByteBuffer data) throws Exception {
                        final Req req = new Req();
                        if (data == null) {
                            peer.close();
                        } else {
                            HttpParser parser = new HttpParser();
                            parser.init(HttpParser.Type.REQUEST);

                            HttpParserSettings settings = new HttpParserSettings() {
                                @Override
                                public int onHeadersComplete(String url, String[] headers) {
                                    req.url = url;
                                    req.headers = headers;
                                    return 0;
                                }

                                @Override
                                public int onMessageComplete() {
                                    try {
                                        String[] paths = req.url.split("/");
                                        int size = Integer.valueOf(paths[2]);
                                        String statusLine = "HTTP/1.1 " + "200" + " "
                                                + "OK" + CRLF;
                                        statusLine += "Content-Type: text/plain" + CRLF + "Content-length: " + size + CRLF;
                                        statusLine += CRLF;
                                        peer.write(statusLine);
                                        ByteBuffer body = ByteBuffer.allocate(size);
                                        for (int i = 0; i < body.capacity(); i++) {
                                            body.put((byte) 67);
                                        }
                                        body.rewind();
                                        ByteBuffer l = ByteBuffer.wrap(statusLine.getBytes());
                                        ByteBuffer complete = ByteBuffer.allocate(statusLine.length() + body.capacity());
                                        complete.put(l);
                                        complete.put(body);
                                        peer.write(complete);
                                        peer.close();
                                        return 0;
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    return 0;
                                }
                            };
                            parser.execute(settings, data, 0, data.capacity());
                            parser.free();
                        }
                    }
                });
                peer.setCloseCallback(new StreamCloseCallback() {
                    @Override
                    public void onClose() throws Exception { // close
                    }
                });
                peer.readStart();
            }
        });

        server.bind("127.0.0.1", 8000);
        server.listen(512);

        System.out.println("Listening at http://127.0.0.1:8000/");
        while (true) {
            loop.run();
        }
    }
}
