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

import net.java.libuv.cb.Callback;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.eventloop.EventLoop;

/**
 * Read compressed input and write uncompressed to output.
 */
public abstract class UncompressWriter extends Writer {

    public UncompressWriter(final EventLoop eventLoop) {
        super(eventLoop);
    }

    /**
     * The compression stream must be continuous. We received discrete content
     * (chunks) This InputStream creates a continuous stream on top of
     * chunks. XXX jfdenise: WARNING: WHAT ABOUT PERFORMANCE? HOW TO PROPERLY SUBCLASS InpuStream.
     */
    private class ChunkInputStream extends InputStream {

        private ByteArrayInputStream bi;
        private boolean empty = true;

        @Override
        public int read() throws IOException {
            return bi.read();
        }

        // read method has to be overriden for performance purpose.
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return bi.read(b, off, len);
        }

        void setEmpty() {
            empty = true;
        }

        boolean isEmpty() {
            return empty;
        }

        void setChunk(final byte[] chunk) {
            bi = new ByteArrayInputStream(chunk);
            empty = false;
        }
    }
    private final ChunkInputStream istream = new ChunkInputStream();
    InputStream uncompressStream;

    protected abstract InputStream createInputStream(byte[] rawChunk, InputStream istream) throws IOException;

    protected boolean shouldRetry() {
        return false;
    }

    public void reset() throws IOException {
        istream.setEmpty();
        uncompressStream.close();
        uncompressStream = null;
    }

    /**
     * Writing uncompressed data onto the output buffer.
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
                int remainingInput = availInBefore;
                int outOffset = outOff;
                if (LOG.enabled()) {
                    LOG.log("flush " + flush + "inOff "+ inOff + " availInBefore" + availInBefore);
                }
                if (hasContent()) {
                    remainingOutput = fillBufferWithContent(buffer, outOffset, remainingOutput);
                    // increment the outOffset with the written length;
                    outOffset = outOffset + (availOutBefore - remainingOutput);
                }
                // Occurs with any flush value.
                if (remainingInput > 0) {
                    assert chunk != null;
                    if (istream.isEmpty()) {// Initialize for new chunk
                        if (LOG.enabled()) {
                            LOG.log("New chunk to read " + availInBefore + " offset is " + inOff);
                        }
                        istream.setChunk(chunk.array());
                    }
                    if (LOG.enabled()) {
                        LOG.log("Uncompressing, input length" + remainingInput);
                    }
                    // This has to be done there, we need an header to open the stream.
                    if (uncompressStream == null) {
                        uncompressStream = createInputStream(chunk.array(), new BufferedInputStream(istream));
                        if (LOG.enabled()) {
                            LOG.log("Initializing new InputStream " + uncompressStream);
                        }
                    }
                    int size = 0;
                    // Whatever the size of the buffer the uncompressStream read small blocks (512 bytes)
                    final byte[] buff = new byte[1024];
                    try {
                        boolean retry = true;
                        while (retry) {
                            while (remainingOutput != 0 && (size = uncompressStream.read(buff)) >= 0) {
                                if (LOG.enabled()) {
                                    LOG.log("Read uncompressed " + size + ", remainingOuput " + remainingOutput);
                                }
                                if (remainingOutput - size >= 0) {
                                    outOffset = writeBuffer(buff, buffer, size, outOffset);
                                    remainingOutput = remainingOutput - size;
                                } else {
                                    // read too much, need to keep it for next call.
                                    final int remainSize = size - remainingOutput;
                                    final byte[] newUnCompressedContent = new byte[remainSize];
                                    System.arraycopy(buff, remainingOutput, newUnCompressedContent, 0, remainSize);
                                    outOffset = writeBuffer(buff, buffer, remainingOutput, outOffset);
                                    remainingOutput = 0;
                                    setContent(newUnCompressedContent);
                                }
                            }
                            if (size == -1) {
                                // The inputstream can need some initialization based on first chunk (eg: needs a dictionary
                                retry = shouldRetry();
                            } else {
                                retry = false;
                            }
                        }
                    } catch (final EOFException ex) {
                        if (LOG.enabled()) {
                            LOG.log("Exception " + ex.getMessage());
                        }
                        // This can happen (reason unknown yet), doesn't corrupt the stream.
                        size = -1;
                    }
                    if (size == -1) {
                        if (LOG.enabled()) {
                            LOG.log("Chunk read");
                        }
                        remainingInput = 0;
                        istream.setEmpty();
                    }
                }
                switch (flush) {
                    case ZlibConstants.Z_NO_FLUSH: {
                        if (LOG.enabled()) {
                            LOG.log("Z_NO_FLUSH");
                        }
                        break;
                    }
                    case ZlibConstants.Z_FINISH:
                    case ZlibConstants.Z_PARTIAL_FLUSH:
                    case ZlibConstants.Z_SYNC_FLUSH:
                    case ZlibConstants.Z_FULL_FLUSH:
                    case ZlibConstants.Z_BLOCK:
                    case ZlibConstants.Z_TREES: {
                        if (LOG.enabled()) {
                            LOG.log("Finish closing stream " + flush);
                        }
                        if (remainingInput == 0 && uncompressStream != null) {
                            uncompressStream.close();
                            uncompressStream = null;
                        }
                        break;
                    }
                }

                callback(callback, remainingInput, remainingOutput);

                return null;
            }
        }, getErrorCallback());
    }
}
