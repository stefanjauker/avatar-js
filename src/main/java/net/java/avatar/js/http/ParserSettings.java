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

package net.java.avatar.js.http;

import java.nio.charset.Charset;

import net.java.httpparser.HttpParserSettings;

import net.java.avatar.js.buffer.Buffer;

public final class ParserSettings extends HttpParserSettings {

    public interface ParserFunction {
        public int call(Object arg);
    }

    final ParserFunction onMessageBegin;
    final ParserFunction onURL;
    final ParserFunction onHeaderField;
    final ParserFunction onHeaderValue;
    final ParserFunction onHeadersComplete;
    final ParserFunction onBody;
    final ParserFunction onMessageComplete;

    public ParserSettings(final ParserFunction onMessageBegin,
                          final ParserFunction onURL,
                          final ParserFunction onHeaderField,
                          final ParserFunction onHeaderValue,
                          final ParserFunction onHeadersComplete,
                          final ParserFunction onBody,
                          final ParserFunction onMessageComplete) {
        this.onMessageBegin = onMessageBegin;
        this.onURL = onURL;
        this.onHeaderField = onHeaderField;
        this.onHeaderValue = onHeaderValue;
        this.onHeadersComplete = onHeadersComplete;
        this.onBody = onBody;
        this.onMessageComplete = onMessageComplete;
    }

    @Override
    public int onMessageBegin() {
        return onMessageBegin.call(null);
    }
    @Override
    public int onURL(final byte[] data) {
        return onURL.call(new Buffer(data));
    }

    @Override
    public int onHeaderField(final byte[] data) {
        return onHeaderField.call(toString(data));
    }

    @Override
    public int onHeaderValue(final byte[] data) {
        return onHeaderValue.call(toString(data));
    }

    @Override
    public int onHeadersComplete() {
        return onHeadersComplete.call(null);
    }

    @Override
    public int onBody(final byte[] data) {
        return onBody.call(new Buffer(data));
    }

    @Override
    public int onMessageComplete() {
        return onMessageComplete.call(null);
    }

    private static String toString(final byte[] content) {
        return new String(content, Charset.forName("utf-8"));
    }
}
