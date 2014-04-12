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

        private final byte[] content;
        private int offset = 0;

        private ContentWrapper(final byte[] content) {
            this.content = content;
        }

        private boolean hasMore() {
            return offset < content.length;
        }

        private int read() {
            return content[offset++];
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
        private static final byte[] WRAP_PREFIX = Loader.PREFIX.getBytes();
        private static final byte[] WRAP_SUFFIX = Loader.SUFFIX.getBytes();
        private static final int WRAP_LENGTH = WRAP_PREFIX.length + WRAP_SUFFIX.length;

        private final ContentWrapper prefix = new ContentWrapper(WRAP_PREFIX);
        private final ContentWrapper suffix = new ContentWrapper(WRAP_SUFFIX);
        private final InputStream wrapped;
        private boolean wrappedHasMore = true;

        public WrapperInputStream(final InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            if (prefix.hasMore()) {
                return prefix.read();
            } else if (wrappedHasMore) {
                final int read = wrapped.read();
                if (read >= 0) {
                    return read;
                } else {
                    wrappedHasMore = false;
                    return suffix.read();
                }
            } else {
                if (suffix.hasMore()) {
                    return suffix.read();
                } else {
                    return -1;
                }
            }
        }

        // read method has to be overridden for performance purpose.
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (prefix.hasMore()) {
                return prefix.read(b, off, len);
            } else if (wrappedHasMore) {
                final int read = wrapped.read(b, off, len);
                if (read >= 0) {
                    return read;
                } else {
                    wrappedHasMore = false;
                    return suffix.read(b, off, len);
                }
            } else {
                if (suffix.hasMore()) {
                    return suffix.read(b, off, len);
                } else {
                    return -1;
                }
            }
        }
    }

    private final class WrappedURLConnection extends URLConnection {
        private final URLConnection wrapped;

        WrappedURLConnection(final URL url, final URLConnection wrapped) {
            super(url);
            this.wrapped = wrapped;
        }

        @Override
        public void connect() throws IOException {
            //NOOP
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new WrapperInputStream(wrapped.getInputStream());
        }

        @Override
        public int getContentLength() {
            return wrapped.getContentLength() + WrapperInputStream.WRAP_LENGTH;
        }

        @Override
        public long getLastModified() {
            return wrapped.getLastModified();
        }
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        // Reconstruct a URL without handler.
        final URL withoutHandler = new URL(u.toExternalForm());
        return new WrappedURLConnection(withoutHandler, withoutHandler.openConnection());
    }
}
