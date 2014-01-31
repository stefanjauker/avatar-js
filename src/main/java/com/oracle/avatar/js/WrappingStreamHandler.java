/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the GNU General
 * Public License Version 2 only ("GPL"). You may not use this file except
 * in compliance with the License.  You can obtain a copy of the License at
 * https://avatar.java.net/license.html or legal/LICENSE.txt.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.avatar.js;

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
public class WrappingStreamHandler extends URLStreamHandler {

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
