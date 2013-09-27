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

package net.java.avatar.js;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL WrappingStreamHandler to deal with script boxing. A script is isolated inside
 * a function object. This wrapping is not present in node apps, it has to be done on-the-fly.
 * To keep the original codeBase, we do the wrapping when the script is loaded by nashorn.
 * URL examples:
 * <ul>
 * <li>avatar:file://{path to script file}</li>
 * <li>avatar:jar:file:{path to script file inside jar file}</li>
 * </ul>
 */
final class WrappingStreamHandler extends URLStreamHandler {

    private static final class ContentWrapper {

        private int offset = 0;
        private final byte[] content;

        private ContentWrapper(byte[] content) {
            this.content = content;
        }

        private boolean needsContent() {
            return offset < content.length;
        }

        private int read() {
            int read = content[offset];
            offset++;
            return read;
        }

        public int read(final byte[] b, final int o, final int len) {
            int off = o;
            int currentOffset = offset;
            if (len >= offset) {
                while (offset < content.length) {
                    b[off] = content[offset];
                    off++;
                    offset++;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    b[off] = content[offset];
                    off++;
                    offset++;
                    if (offset == content.length - 1) {
                        break;
                    }
                }
            }
            return offset - currentOffset;
        }
    }

    protected static final class WrapperInputStream extends InputStream {
        private static final byte[] PREFIX = Loader.PREFIX.getBytes();
        private static final byte[] SUFFIX = Loader.SUFFIX.getBytes();

        private final InputStream real;
        private boolean endReached;
        private ContentWrapper prefix = new ContentWrapper(PREFIX);
        private ContentWrapper suffix = new ContentWrapper(SUFFIX);

        public WrapperInputStream(InputStream real) {
            this.real = real;
        }

        @Override
        public int read() throws IOException {
            if (prefix.needsContent()) {
                return prefix.read();
            } else {
                if (endReached) {
                    if (suffix.needsContent()) {
                        return suffix.read();
                    } else {
                        return -1;
                    }
                } else {
                    int r = real.read();
                    if (r == -1) {
                        endReached = true;
                        return suffix.read();
                    } else {
                        return r;
                    }
                }
            }
        }

        // read method has to be overridden for performance purpose.
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            int offset = off;
            if (prefix.needsContent()) {
                return prefix.read(b, offset, len);
            } else {
                if (endReached) {
                    if (suffix.needsContent()) {
                        return suffix.read(b, offset, len);
                    } else {
                        return -1;
                    }
                } else {
                    int read = real.read(b, offset, len);
                    if (read == -1) {
                        endReached = true;
                        return suffix.read(b, offset, len);
                    } else {
                        return read;
                    }
                }
            }
        }
    }

    private final class WrappedURLConnection extends URLConnection {

        private final URLConnection real;

        WrappedURLConnection(URL url, URLConnection real) {
            super(url);
            this.real = real;
        }

        @Override
        public void connect() throws IOException {
            //NOOP
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new WrapperInputStream(real.getInputStream());
        }
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        // Reconstruct an URL without handler.
        final URL withoutHandler = new URL(u.toExternalForm());
        return new WrappedURLConnection(withoutHandler, withoutHandler.openConnection());
    }
}
