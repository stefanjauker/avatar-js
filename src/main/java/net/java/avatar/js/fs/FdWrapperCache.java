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

package net.java.avatar.js.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class FdWrapperCache {

    private final AtomicInteger NEXT_FD = new AtomicInteger(1);
    private final Map<Integer, FdWrapper> OPEN_CHANNELS = new ConcurrentHashMap<>();

    int wrap(final Path path, final FileChannel channel) {
        final FdWrapper wrapper = new FdWrapper(NEXT_FD.getAndIncrement(), path, channel);
        OPEN_CHANNELS.put(wrapper.getFd(), wrapper);
        return wrapper.getFd();
    }

    boolean close(final int fd) throws IOException {
        final FdWrapper wrapper = OPEN_CHANNELS.remove(fd);
        if (wrapper == null) {
            return false;
        } else {
            wrapper.getChannel().close();
            return true;
        }
    }

    FdWrapper find(final int fd) throws FileNotFoundException {
        final FdWrapper wrapper = OPEN_CHANNELS.get(fd);
        if (wrapper == null) {
            throw new FileNotFoundException(Integer.toString(fd));
        }
        return wrapper;
    }

    void clear() {
        for (final FdWrapper wrapper : OPEN_CHANNELS.values()) {
            try {
                wrapper.getChannel().close();
            } catch (final IOException ignore) {}
        }
        OPEN_CHANNELS.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        clear();
    }

}
