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

package net.java.avatar.js.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * A String decoder used to handle Buffer characters decoding.
 * In order to check for char validity, charCodeAt called on the Java String
 * returns the character 0XFFFD (Incomplete char). So we are loosing the actual
 * codepoint.
 * The way to implement byte per byte decoding of multi byte char is to use
 * CharsetDecoder.
 *
 */
public class StringDecoder {

    private CharsetDecoder decoder;
    private ByteBuffer remaining;
    private CharBuffer out;
    private final String charSet;

    public StringDecoder(String encoding) {
        charSet = encoding;
        init();
    }

    private void init() {
        decoder = Charset.forName(charSet).newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        remaining = null;
    }

    public String write(Buffer buffer) {
        // The received buffer can be fully consumed
        ByteBuffer buff = ByteBuffer.allocate(buffer.capacity());
        buff.put(buffer.array());
        buff.flip();
        ByteBuffer nBuffer = buff;
        if (remaining != null) {
            nBuffer = ByteBuffer.allocate(buff.limit() + remaining.limit());
            nBuffer.put(remaining);
            nBuffer.put(buff);
            nBuffer.flip();
            remaining = null;
        }
        CoderResult res = decode(nBuffer);
        if (nBuffer.remaining() != 0) {
            // keep all that has not been read.
            remaining = ByteBuffer.allocate(nBuffer.remaining());
            remaining.put(nBuffer);
            remaining.flip();
        }
        out.flip();

        return out.toString();
    }

    private CoderResult decode(ByteBuffer input) {
        CoderResult res = null;
        out = CharBuffer.allocate(input.capacity());
        do {
            res = decoder.decode(input, out, true);
            if (res.isOverflow()) {
                out = CharBuffer.allocate(out.capacity() + input.limit());
            }
        } while (res.isOverflow());
        return res;
    }

    public String end() {
        // Keep invalid/incomplete when ending
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decode(remaining == null ? ByteBuffer.allocate(0) : remaining);
        out.flip();
        init();
        return out.toString();
    }
}
