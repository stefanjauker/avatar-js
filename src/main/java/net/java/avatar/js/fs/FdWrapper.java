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

import java.nio.channels.FileChannel;
import java.nio.file.Path;

final class FdWrapper {

    private final int fd;
    private final Path path;
    private final FileChannel channel;

    FdWrapper(final int fd, final Path path, final FileChannel channel) {
        this.fd = fd;
        this.path = path;
        this.channel = channel;
    }

    int getFd() {
        return fd;
    }

    Path getPath() {
        return path;
    }

    FileChannel getChannel() {
        return channel;
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof FdWrapper) {
            final FdWrapper otherfd = (FdWrapper) other;
            return otherfd.fd == fd;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fd;
    }

    @Override
    public String toString() {
        return "{ " +
                "fd: " + fd + ", " +
                "path: " + path + ", " +
                "channel: " + channel +
                "}";
    }

    @Override
    protected void finalize() throws Throwable {
        if (channel != null) {
            channel.close();
        }
    }

}

