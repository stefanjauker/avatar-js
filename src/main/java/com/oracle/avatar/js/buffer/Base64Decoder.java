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

package com.oracle.avatar.js.buffer;

import java.util.Arrays;

public final class Base64Decoder {

    private static final char[] CA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] IA = new int[256];

    static {
        Arrays.fill(IA, -1);
        for (int i = 0, iS = CA.length; i < iS; i++)
            IA[CA[i]] = i;
        IA['='] = -2;
        // RFC-4648 table 2: URL and filename safe
        IA['-'] = IA['+'];
        IA['_'] = IA['/'];
    }

    public static byte[] decode(final String str) {
        if (str.isEmpty()) {
            return new byte[0];
        }

        // estimate size, multiple of 4 with max 2 paddings
        final int len = ((str.length() + 2) >> 2) * 3;
        final byte[] buf = new byte[len];
        int pos = 0;
        int decode_buf = 0;
        int rem = 4;

        for (int i = 0; i < str.length(); i++) {
            final int c = IA[str.charAt(i)];
            assert (rem > 0);
            if (c >= 0) {
                decode_buf = (decode_buf << 6) | (c & 63);
                rem--;
            } else if (c == -2) {
                // padding encountered, end of encoded data
                break;
            } else {
                // skip invalid characters, including CR/RF
            }
            // put down 3 octets with 4 characters
            if (rem == 0) {
                buf[pos++] = (byte) (decode_buf >> 16);
                buf[pos++] = (byte) (decode_buf >> 8);
                buf[pos++] = (byte) (decode_buf);
                rem = 4;
                decode_buf = 0;
            }
        }

        // No more data, take care of padding
        if (rem == 4) {
            // just match, no padding needed
        } else if (rem == 3) {
            // invalid data, even the last octet encode into 2 chars
            // discard last byte to be more tolerate.
        } else {
            if (rem == 1) {
                // two octets with rem == 1, 18 bits
                decode_buf >>= 2;
                buf[pos++] = (byte) (decode_buf >> 8);
            } else {
                // one octet with rem == 2, 12 bits
                decode_buf >>= 4;
            }
            buf[pos++] = (byte) (decode_buf);
        }
        return Arrays.copyOf(buf, pos);
    }
}
