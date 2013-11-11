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

package net.java.avatar.js.zlib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.eventloop.Callback;
import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

public abstract class CompressWriter extends Writer {

    private ByteArrayOutputStream buffOutStream;
    private OutputStream compressStream;

    public CompressWriter(final EventLoop eventLoop) {
        super(eventLoop);
    }

    @Override
    public void close() {
        if (compressStream != null) {
            try {
                compressStream.close();
            } catch (IOException ex) {
                if (LOG.enabled()) {
                    LOG.log("compressStream close exception " + ex);
                }
            }
        }

        if (buffOutStream != null) {
            try {
                buffOutStream.close();
            } catch (IOException ex) {
                if (LOG.enabled()) {
                    LOG.log("buffOutStream close exception " + ex);
                }
            }
        }
    }

    @Override
    public void init(final int windowBits,
            final int level,
            final int memLevel,
            final int strategy,
            final Buffer dictionary) {
        super.init(windowBits, level, memLevel, strategy, dictionary);
        buffOutStream = new ByteArrayOutputStream();
        try {
            compressStream = createCompressionStream(buffOutStream);
        } catch(final IOException ex) {
            // Mapping IOException in this case is not perfect.
            // No IOException should be thrown BUT the Java API can... for undocumented reason
            // (eg: GZIPOutputStream constructor).
            eventLoop.post(new Event("zlib.error", getErrorCallback(),
                    ex.getMessage(), ZlibConstants.Z_STREAM_ERROR));
        }
        if (LOG.enabled()) {
            LOG.log("New Compression stream " + compressStream);
        }
    }

    public void reset() throws IOException {
        if (compressStream == null) {
            return;
        }

        compressStream.close();
        buffOutStream.close();

        buffOutStream = new ByteArrayOutputStream();
        compressStream = createCompressionStream(buffOutStream);
        if (LOG.enabled()) {
            LOG.log("Reset, new outputstream " + compressStream);
        }
    }

    protected abstract OutputStream createCompressionStream(ByteArrayOutputStream out) throws IOException;

    /**
     * Writing compressed data onto the output buffer. The complexity of this
     * algorithm is related to having a finite output buffer in which the
     * compressed content is added. When the buffer is full and there is still
     * some content in the input buffer, this method is called again by the
     * zlib.js script. It is called until there is no more input.
     *
     * Algorithm steps:
     * 1) Compressing input.
     * 2) Flushing, create the compressed content.
     * 3) Collecting, fill in the output buffer(s) with the compressed content. When output
     * buffer is full, the zlib.js will emit a data event with the content. Then
     * will pass a new Buffer
     *
     * @param flush The flush state.
     * @param chunk The input buffer
     * @param inOff The offset in the input buffer
     * @param availInBefore The remaining content to compress
     * @param buffer The output buffer
     * @param outOff The offset in the output
     * @param availOutBefore The remaining available content in the output
     * buffer
     * @param callback The callback to call with (availableInAfter,
     * availableOutAfter)
     */
    @Override
    public void write(final int flush,
            final Buffer chunk,
            final int inOff,
            final int availInBefore,
            final Buffer buffer,
            final int outOff,
            final int availOutBefore,
            final Callback callback) {
        submitToLoop(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                checkWriteParameters(flush, chunk, inOff, availInBefore, buffer, outOff, availOutBefore, callback);
                int remainingOutput = availOutBefore;
                final int inOffset = inOff;
                final int outOffset = outOff;

                if (LOG.enabled()) {
                    LOG.log("Compressing, input length" + chunk.array().length);
                }
                if (hasContent()) {
                    remainingOutput = fillBufferWithContent(buffer, outOffset, remainingOutput);
                } else {
                    switch(flush) {
                        case ZlibConstants.Z_NO_FLUSH: {
                            assert chunk != null;
                            // Compress all
                            compressStream.write(chunk.array(), inOffset, availInBefore);
                            break;
                        }
                        case ZlibConstants.Z_FINISH: {
                            assert chunk != null;
                            if (chunk.capacity() > 0) {
                                compressStream.write(chunk.array(), inOffset, availInBefore);
                            }
                            compressStream.close();
                            setContent(buffOutStream.toByteArray());
                            buffOutStream.close();
                            if (LOG.enabled()) {
                                LOG.log("Closing, ["+ flush +"] data length to collect " + getContent().length);
                            }
                            remainingOutput = fillBufferWithContent(buffer, outOffset, remainingOutput);
                            break;
                        }
                        case ZlibConstants.Z_SYNC_FLUSH:
                        case ZlibConstants.Z_FULL_FLUSH:
                        case ZlibConstants.Z_PARTIAL_FLUSH:
                        case ZlibConstants.Z_BLOCK:
                        case ZlibConstants.Z_TREES: {
                            // XXX jfdenise, this part has to evolve. SYNC flush is set at construction time
                            // for Java outputstream and can't be changed.
                            // I am implementing as a flush on compression, and reset the output, but will need to be possibly revisited.
                            compressStream.flush();
                            setContent(buffOutStream.toByteArray());
                            buffOutStream.reset();
                            remainingOutput = fillBufferWithContent(buffer, outOffset, remainingOutput);
                            break;
                        }

                    }
                }
                callback(callback, 0, remainingOutput);

                return null;
            }
        }, getErrorCallback());
    }
}

