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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;

public final class Buffer {

    private static final String BASE64_ENCODING = "base64";
    private static final String HEX_ENCODING = "hex";
    private static final String BINARY_ENCODING = "binary";
    private static final byte[] EMPTY_BYTE_ARRAY = {};
    private static final String EMPTY_STRING = "";

    private final ByteBuffer byteBuffer;

    public static Buffer wrap(final byte[] array) {
        return new Buffer(ByteBuffer.wrap(array));
    }

    public static Buffer wrap(final byte[] array, final int offset, final int length) {
        return new Buffer(ByteBuffer.wrap(array, offset, length));
    }

    public Buffer(final ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public Buffer(final int size) {
        byteBuffer = ByteBuffer.allocateDirect(size);
    }

    public Buffer(final Double[] numbers) {
        this(numbers.length);
        for (final Double number : numbers) {
            byteBuffer.put(number.byteValue());
        }
        byteBuffer.rewind();
    }

    public Buffer(final String str, final String encoding) throws UnsupportedEncodingException {
        this(Buffer.toBytes(str, encoding));
    }

    public Buffer(final byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public Buffer(final byte[] bytes, final int offset, final int length) {
        this(length - offset);
        byteBuffer.put(bytes, offset, length);
        byteBuffer.rewind();
    }

    public int capacity() {
        return byteBuffer.capacity();
    }

    public Buffer clear() {
        byteBuffer.clear();
        return this;
    }

    public Buffer flip() {
        byteBuffer.flip();
        return this;
    }

    public boolean hasRemaining() {
        return byteBuffer.hasRemaining();
    }

    public int limit() {
        return byteBuffer.limit();
    }

    public Buffer limit(final int limit) {
        byteBuffer.limit(limit);
        return this;
    }

    public Buffer mark() {
        byteBuffer.mark();
        return this;
    }

    public int position() {
        return byteBuffer.position();
    }

    public Buffer position(final int position) {
        byteBuffer.position(position);
        return this;
    }

    public int remaining() {
        return byteBuffer.remaining();
    }

    public Buffer reset() {
        byteBuffer.reset();
        return this;
    }

    public Buffer rewind() {
        byteBuffer.rewind();
        return this;
    }

    public Buffer slice() {
        return new Buffer(byteBuffer.slice());
    }

    public Buffer duplicate() {
        return new Buffer(byteBuffer.duplicate());
    }

    public byte[] array() {
        if (byteBuffer.hasArray()) {
            return byteBuffer.array();
        } else {
            final ByteBuffer dup = byteBuffer.duplicate();
            final byte[] data = new byte[dup.capacity()];
            dup.clear();
            dup.get(data);
            return data;
        }
    }

    public ByteBuffer underlying() {
        return byteBuffer;
    }

    public ByteBuffer toByteBuffer() {
        return byteBuffer;
    }

    public ByteBuffer toByteBuffer(final int position, final int limit) {
        // A sliced buffer has its position set at the position of this buffer
        int currentPos = byteBuffer.position();
        try {
            byteBuffer.position(0);
            final ByteBuffer slice = byteBuffer.slice();
            slice.position(position);
            slice.limit(limit);
            return slice;
        } finally {
            byteBuffer.position(currentPos);
        }
    }

    public String toStringContent() {
        return new String(array());
    }

    public String toStringContent(final Charset charset) {
        return new String(array(), charset);
    }

    public String toStringContent(final Charset charset, final int position, final int limit) {
        final ByteBuffer slice = toByteBuffer(position, limit);
        return new String(slice.array(), charset);
    }

    public Buffer get(final byte[] dest) {
        byteBuffer.get(dest);
        return this;
    }

    public Buffer get(final byte[] dest, final int offset, final int length) {
        byteBuffer.get(dest, offset, length);
        return this;
    }

    public Buffer put(final byte[] src) {
        byteBuffer.put(src);
        return this;
    }

    public Buffer put(final byte[] src, final int offset, final int length) {
        byteBuffer.put(src, offset, length);
        return this;
    }

    public int getByteAt(final int index) {
        return byteBuffer.get(index) & 0xff;
    }

    public void setByteAt(final int index, final int value) {
        byteBuffer.put(index, (byte) (value & 0xff));
    }

    @Override
    public int hashCode() {
        return byteBuffer.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Buffer) {
            final Buffer otherBuffer = (Buffer) other;
            return byteBuffer.equals(otherBuffer.byteBuffer);
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + "pos:" + position() + ", lim:" + limit() + ", cap:" + capacity() + "}";
    }

    public int write(final String str, final int off, final int length, final String encoding, final Integer[] charsWrittenReturn)
            throws UnsupportedEncodingException {

        // these encodings will not write partial characters
        if ("us-ascii".equals(encoding) || "binary".equals(encoding) || "iso-8859-1".equals(encoding)) {
            final byte[] src = Buffer.toBytes(str, encoding);
            final int towrite = Math.min(byteBuffer.capacity() - off, Math.min(src.length, length));
            byteBuffer.position(off);
            byteBuffer.put(src, 0, towrite);
            charsWrittenReturn[0] = towrite;
            return towrite;
        }

        // for (hex, base64), try to write as many whole chars as we can fit (slow, inefficient, TODO)
        final boolean hex = "hex".equals(encoding);
        final int canwrite = Math.min(byteBuffer.capacity() - off, length);
        int fit = str.length();
        // our hex decoder chokes on odd-sized input, so we ensure we pass an even number of characters if hex is used
        if (hex) {
            fit = (fit / 2) * 2;
        }
        for (; fit > 0; fit = hex ? fit-2 : fit-1) {
            final byte[] bytes = Buffer.toBytes(str.substring(0, fit), encoding);
            if (bytes.length <= canwrite) {
                byteBuffer.position(off);
                byteBuffer.put(bytes, 0, bytes.length);
                charsWrittenReturn[0] = isEncoded(encoding) ?
                        bytes.length :
                        new String(bytes, encoding).toCharArray().length;
                return bytes.length;
            }
        }

        return 0;
    }

    public int copy(final Buffer targetBuffer, final int targetStart, final int sourceStart, final int sourceEnd) {
        final int len = Math.min(sourceEnd - sourceStart, targetBuffer.byteBuffer.capacity());
        if (byteBuffer.hasArray() && targetBuffer.byteBuffer.hasArray()) {
            System.arraycopy(byteBuffer.array(), sourceStart, targetBuffer.byteBuffer.array(), targetStart, len);
        } else if (targetBuffer.byteBuffer.hasArray()) {
            System.arraycopy(array(), sourceStart, targetBuffer.byteBuffer.array(), targetStart, len);
        } else {
            final int pos = targetBuffer.byteBuffer.position();
            try {
                targetBuffer.byteBuffer.position(targetStart);
                targetBuffer.byteBuffer.put(array(), sourceStart, len);
            } finally {
                targetBuffer.byteBuffer.position(pos);
            }
        }
        return len;
    }

    public String toString(final String encoding, final int start, final int length)
            throws UnsupportedEncodingException {
        return Buffer.fromBytes(Arrays.copyOfRange(array(), start, start + length), encoding);
    }

    public Buffer slice(final int position, final int end) {
        return new Buffer(Arrays.copyOfRange(array(), position, end));
    }

    public void fill(final Double value, final int start, final int end) {
        if (byteBuffer.hasArray()) {
            Arrays.fill(byteBuffer.array(), start, end, value.byteValue());
        } else {
            final byte[] data = new byte[end - start];
            Arrays.fill(data, value.byteValue());
            final ByteBuffer dup = byteBuffer.duplicate();
            dup.clear();
            dup.position(start);
            dup.put(data, 0, data.length);
        }
    }

    public int readInt8(final int off) {
        return byteBuffer.get(off);
    }

    public int readUInt8(final int off) {
        return byteBuffer.get(off) & 0xff;
    }

    public int readInt16LE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getShort(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public int readUInt16LE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getShort(off) & 0xffff;
        } finally {
            byteBuffer.order(obo);
        }
    }

    public int readInt16BE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getShort(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public int readUInt16BE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getShort(off) & 0xffff;
        } finally {
            byteBuffer.order(obo);
        }
    }

    public long readInt32LE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getInt(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public long readUInt32LE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getInt(off) & 0xffffffffL;
        } finally {
            byteBuffer.order(obo);
        }
    }

    public long readInt32BE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getInt(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public long readUInt32BE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getInt(off) & 0xffffffffL;
        } finally {
            byteBuffer.order(obo);
        }
    }

    public float readFloatLE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getFloat(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public float readFloatBE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getFloat(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public double readDoubleLE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getDouble(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public double readDoubleBE(final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getDouble(off);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeInt8(final int value, final int off) {
        byteBuffer.put(off, (byte) value);
    }

    public void writeUInt8(final int value, final int off) {
        byteBuffer.put(off, (byte) (value & 0xff));
    }

    public void writeInt16LE(final int value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putShort(off, (short) value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeUInt16LE(final int value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putShort(off, (short) (value & 0xffff));
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeInt16BE(final int value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putShort(off, (short) value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeUInt16BE(final int value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putShort(off, (short) (value & 0xffff));
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeInt32LE(final long value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt(off, (int) value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeUInt32LE(final long value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt(off, (int) (value & 0xffffffff));
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeInt32BE(final long value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putInt(off, (int) value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeUInt32BE(final long value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putInt(off, (int) (value & 0xffffffff));
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeFloatLE(final float value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putFloat(off, value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeFloatBE(final float value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putFloat(off, value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeDoubleLE(final double value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putDouble(off, value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public void writeDoubleBE(final double value, final int off) {
        final ByteOrder obo = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putDouble(off, value);
        } finally {
            byteBuffer.order(obo);
        }
    }

    public String inspect(final int maxBytes, final boolean slow) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(slow ? "Slow" : "");
        sb.append("Buffer ");
        for (int i = 0; i < byteBuffer.limit(); i++) {
            sb.append(String.format("%02x", byteBuffer.get(i)));
            final int nexti = i + 1;
            if (nexti < maxBytes) {
                if (nexti < byteBuffer.limit()) {
                    sb.append(' ');
                }
            } else {
                sb.append(" ...");
                break;
            }
        }
        sb.append('>');
        return sb.toString();
    }

    public static int byteLength(final String str, final String encoding) throws UnsupportedEncodingException {
        return Buffer.toBytes(str, encoding).length;
    }

    public static byte[] toBytes(final String str, final String encoding) throws UnsupportedEncodingException {
        byte[] bytes;
        switch (encoding) {
        case Buffer.BASE64_ENCODING:
            bytes = Base64Decoder.decode(str);
            break;
        case Buffer.HEX_ENCODING:
            bytes = HexUtils.decode(str);
            break;
        case Buffer.BINARY_ENCODING:
            final char[] chars = str.toCharArray();
            bytes = new byte[chars.length];
            for (int i=0; i < chars.length; i++) {
                bytes[i] = (byte) chars[i];
            }
            break;
        default:
            bytes = str.getBytes(encoding);
        }
        return bytes == null ? Buffer.EMPTY_BYTE_ARRAY : bytes;
    }

    private static boolean isEncoded(final String encoding) {
        switch (encoding) {
            case Buffer.BASE64_ENCODING:
            case Buffer.HEX_ENCODING:
            case Buffer.BINARY_ENCODING: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    public static String fromBytes(final byte[] bytes, final String encoding) throws UnsupportedEncodingException {
        return fromBytes(bytes, 0, bytes.length, encoding);
    }

    public static String fromBytes(final byte[] b, final int off, final int length, final String encoding)
            throws UnsupportedEncodingException {
        // avoid copying in the common case where slicing is not required
        final byte[] bytes = off == 0 && length == b.length ? b : Arrays.copyOfRange(b, off, off + length);
        String str;
        switch (encoding) {
        case Buffer.BASE64_ENCODING:
            str = Base64.getEncoder().encodeToString(bytes);
            break;
        case Buffer.HEX_ENCODING:
            str = HexUtils.encode(bytes);
            break;
        case Buffer.BINARY_ENCODING:
            final char[] chars = new char[bytes.length];
            for (int i=0; i < chars.length; i++) {
                chars[i] = (char) (bytes[i] & 0xff);
            }
            str = new String(chars);
            break;
        default:
            str = new String(bytes, encoding);
        }
        return str == null ? Buffer.EMPTY_STRING : str;
    }

    public static Buffer copyBytes(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new Buffer(bytes);
    }

    public static double _valueOf(final Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        } else if (obj instanceof Double) {
            return ((Double) obj).doubleValue();
        } else if (obj instanceof String) {
            return Double.valueOf((String) obj);
        }
        return 0;
    }

}
