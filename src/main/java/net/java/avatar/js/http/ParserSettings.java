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

import net.java.httpparser.HttpParserSettings;

public final class ParserSettings extends HttpParserSettings {

    public interface ParserDataFunction {
        public int call(int offset, int length);
    }

    public interface ParserFunction {
        public int call();
    }
    
     public interface ParserHeadersFunction {
        public int call(String url, String[] headers);
    }

    final ParserHeadersFunction onHeadersComplete;
    final ParserDataFunction onBody;
    final ParserFunction onMessageComplete;
    final ParserHeadersFunction onHeaders;

    public ParserSettings(final ParserHeadersFunction onHeadersComplete,
                          final ParserDataFunction onBody,
                          final ParserFunction onMessageComplete,
                          final ParserHeadersFunction onHeaders) {
        this.onHeadersComplete = onHeadersComplete;
        this.onBody = onBody;
        this.onMessageComplete = onMessageComplete;
        this.onHeaders = onHeaders;
    }

    @Override
    public int onHeadersComplete(String url, String[] headers) {
        return onHeadersComplete.call(url, headers);
    }
    @Override
    public int onHeaders(String url, String[] headers) {
        return onHeaders.call(url, headers);
    }

    @Override
    public int onBody(int offset, int length) {
        return onBody.call(offset, length);
    }

    @Override
    public int onMessageComplete() {
        return onMessageComplete.call();
    }
}
