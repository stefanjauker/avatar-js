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

import java.util.concurrent.Callable;

import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.eventloop.Callback;
import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

/**
 * Base class for compress and uncompress.
 * Subclass are writing as expected by the zLib.js
 */
public abstract class Writer {

    protected final net.java.avatar.js.log.Logger LOG;
    protected final EventLoop eventLoop;

    private int windowBits;
    private int level;
    private int memLevel;
    private int strategy;
    private Buffer dictionary;
    private Callback errorCb;
    private byte[] content;

    protected Writer(final EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        this.LOG = eventLoop.logger("zlib");
    }

        public int getWindowBits() {
        return windowBits;
    }

    public void close() {
        // Sub class can close any resource
    }

    public int getLevel() {
        return level;
    }

    public int getMemLevel() {
        return memLevel;
    }

    public int getStrategy() {
        return strategy;
    }

    public Buffer getDictionary() {
        return dictionary;
    }

    protected byte[] getContent() {
        return content;
    }

    protected void setContent(final byte[] content) {
        this.content = content;
    }

    // Called by Javascript wrapper
    public void onError(final Callback errorCb) {
        this.errorCb = errorCb;
    }

    public Callback getErrorCallback() {
        return errorCb;
    }

    public void init(final int windowBits,
            final int level,
            final int memLevel,
            final int strategy,
            final Buffer dictionary) {
        try {
            if (level < -1 || level > 9) {
                throw new Exception("Invalid compression level");
            }
            this.windowBits = windowBits;
            this.level = level;
            this.memLevel = memLevel;
            this.strategy = strategy;
            this.dictionary = dictionary;
        } catch (final Exception exception) {
            if (LOG.enabled()) {
                LOG.log("Invalid paremeters " + exception);
                //exception.printStackTrace();
            }
            eventLoop.post(new Event("zlib.error", getErrorCallback(),
                    exception.getMessage(), ZlibConstants.Z_STREAM_ERROR));
        }
    }

    /**
     * Writing input to output.
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
    public abstract void write(final int flush,
            final Buffer chunk,
            final int inOff,
            final int availInBefore,
            final Buffer buffer,
            final int outOff,
            final int availOutBefore,
            final Callback callback);

    protected boolean hasContent() {
        return content != null;
    }

    /**
     * Fill the passed buffer with the content. If there is overflow, the content
     * is recomputed for the next write.
     * @param buffer Output buffer
     * @param outOffset Out offset
     * @param remainingOutput Free space in buffer
     * @return The new remaining output
     */
    protected int fillBufferWithContent(final Buffer buffer, int outOffset, int remainingOutput) {
        if (LOG.enabled()) {
            LOG.log("Collecting, compressed length " + content.length);
        }
        while (remainingOutput > 0 && content != null) {
            if (remainingOutput - content.length >= 0) {
                outOffset = writeBuffer(content, buffer, outOffset);
                remainingOutput = remainingOutput - content.length;
                content = null;
            } else {
                final byte[] newContent = new byte[content.length - remainingOutput];
                System.arraycopy(content, remainingOutput, newContent, 0, content.length - remainingOutput);
                outOffset = writeBuffer(content, buffer, remainingOutput, outOffset);
                remainingOutput = 0;
                content = newContent;
            }
        }
        return remainingOutput;
    }

    public int writeBuffer(final byte[] src, final Buffer dest, final int offset) {
        return writeBuffer(src, dest, src.length, offset);
    }

    public int writeBuffer(final byte[] src, final Buffer dest, final int size, int offset) {
        for (int i = 0; i < size; i++) {
            dest.writeUInt8(src[i], offset);
                // TODO: eclipse warning: parameter should not be assigned
            offset += 1;
        }
        return offset;
    }

    public void submitToLoop(final Callable<?> callable, final Callback cb) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (final Exception e) {
                    if (LOG.enabled()) {
                        LOG.log(e);
                    }
                    eventLoop.post(new Event("zlib.error", cb, e.toString(), ZlibConstants.Z_STREAM_ERROR));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public void callback(final Callback callback, final int availInAfter,
                         final int availOutAfter) throws Exception {
        if (LOG.enabled()) {
            LOG.log("DONE, Remaining input "
                    + availInAfter + " remainingOutput " + availOutAfter);
        }
        final Object[] args = {availInAfter, availOutAfter};
        eventLoop.post(new Event("zlib.callback", callback, args));
    }

    void checkWriteParameters(final int flush,
            final Buffer chunk,
            final int inOff,
            final int availInBefore,
            final Buffer buffer,
            final int outOff,
            final int availOutBefore,
            final Callback callback) {
        checkBuffer(chunk, inOff, availInBefore);
        checkBuffer(buffer, outOff, availOutBefore);

        if (callback == null) {
            throw new IllegalArgumentException("Null callback");
        }
    }

    private static void checkBuffer(final Buffer b, final int offset, final int available) {
        if (b == null) {
            throw new IllegalArgumentException("Invalid buffer");
        }
        if (offset < 0 || available < 0) {
            throw new IllegalArgumentException("Invalid buffer values");
        }
    }
}
