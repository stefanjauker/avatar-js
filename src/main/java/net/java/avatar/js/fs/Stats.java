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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.java.avatar.js.constants.Constants;

public final class Stats {

    private final Path path;
    private final boolean isLink;

    private BasicFileAttributes basicFileAttributes;
    private PosixFileAttributes posixFileAttributes;
    private DosFileAttributes dosFileAttributes;
    private Map<String, Object> unixFileAttributes;

    private final int mode;
    private final boolean isWindows;

    @SuppressWarnings("unchecked")
    public Stats(final Path path, final boolean isLink, final boolean isWindows) {
        this.path = path;
        this.isLink = isLink;
        this.isWindows = isWindows;

        final BasicFileAttributeView basicView = this.isLink ?
            Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) :
            Files.getFileAttributeView(path, BasicFileAttributeView.class);
        try {
            basicFileAttributes = basicView.readAttributes();
        } catch (IOException x) {
            basicFileAttributes = null;
        }

        if (isWindows) {
            final DosFileAttributeView dosView = this.isLink ?
                Files.getFileAttributeView(path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) :
                Files.getFileAttributeView(path, DosFileAttributeView.class);
            try {
                dosFileAttributes = dosView.readAttributes();
            } catch (IOException x) {
                dosFileAttributes = null;
            }
            posixFileAttributes = null;
        } else {
            dosFileAttributes = null;
            final PosixFileAttributeView posixView = this.isLink ?
                Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) :
                Files.getFileAttributeView(path, PosixFileAttributeView.class);
            try {
                posixFileAttributes = posixView.readAttributes();
            } catch (IOException x) {
                posixFileAttributes = null;
            }
        }

        int mod = 0;
        if (basicFileAttributes != null && basicFileAttributes.isDirectory()) {
            mod |= Constants.S_IFDIR;
        }
        if (basicFileAttributes != null && basicFileAttributes.isRegularFile()) {
            mod |= Constants.S_IFREG;
        }
        if (basicFileAttributes != null && basicFileAttributes.isSymbolicLink()) {
            mod |= Constants.S_IFLNK;
        }
        if (isWindows) {
            if (dosFileAttributes != null && dosFileAttributes.isReadOnly()) {
                // 0555
                mod |= Constants.S_IRUSR;
                mod |= Constants.S_IRGRP;
                mod |= Constants.S_IROTH;

                mod |= Constants.S_IXUSR;
                mod |= Constants.S_IXGRP;
                mod |= Constants.S_IXOTH;
            } else {
                // 0777
                mod |= Constants.S_IRWXU;
                mod |= Constants.S_IRWXG;
                mod |= Constants.S_IRWXO;
            }
        } else {
            mod |= FileSystem.mapPermissionsToMode(
                    posixFileAttributes != null ?
                            posixFileAttributes.permissions() :
                            Collections.EMPTY_SET);
        }
        this.mode = mod;
    }

    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public String toString() {
        return "{" +
            "path: " + path.toString() +
            ", isLink: " + Boolean.toString(isLink) +
            ", basicFileAttributes: " + basicFileAttributesToString() +
            ", dosFileAttributes: " + dosFileAttributesToString() +
            ", posixFileAttributes: " + posixFileAttributesToString() +
            ", unixFileAttributes: " + unixFileAttributesToString() +
            "}";
    }

    public BasicFileAttributes getBasicFileAttributes() {
        return basicFileAttributes;
    }

    public PosixFileAttributes getPosixFileAttributes() {
        return posixFileAttributes;
    }

    public DosFileAttributes getDosFileAttributes() {
        return dosFileAttributes;
    }

    public int getMode() {
        return mode;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUnixFileAttributes() {
        if (unixFileAttributes == null) {
            if (isWindows) {
                unixFileAttributes = Collections.EMPTY_MAP;
            } else {
                try {
                    unixFileAttributes = Files.readAttributes(path, "unix:*",
                            LinkOption.NOFOLLOW_LINKS);
                } catch (IOException x) {
                    unixFileAttributes = null;
                }
            }
        }
        return unixFileAttributes;
    }

    private String basicFileAttributesToString() {
        if (basicFileAttributes == null) {
            return "null";
        }
        return "{" +
               "isRegularFile: " + basicFileAttributes.isRegularFile() +
                ", isDirectory: " + basicFileAttributes.isDirectory() +
                ", isSymbolicLink: " + basicFileAttributes.isSymbolicLink() +
                ", isOther: " + basicFileAttributes.isOther() +
                ", size: " + basicFileAttributes.size() +
                ", lastModifiedTime: " + basicFileAttributes.lastModifiedTime().toString() +
                ", lastAccessTime: " + basicFileAttributes.lastAccessTime().toString() +
                ", creationTime: " + basicFileAttributes.creationTime().toString() +
                "}";
    }

    private String dosFileAttributesToString() {
        if (dosFileAttributes == null) {
            return "null";
        }
        return (dosFileAttributes.isArchive() ? "a" : "-") +
               (dosFileAttributes.isHidden() ? "h" : "-") +
               (dosFileAttributes.isReadOnly() ? "-" : "w") +
               (dosFileAttributes.isSystem() ? "s" : "-");
    }

    private String posixFileAttributesToString() {
        if (posixFileAttributes == null) {
            return "null";
        }
        final Set<PosixFilePermission> permissions = posixFileAttributes.permissions();
        return (permissions.contains(PosixFilePermission.OWNER_READ) ? "r" : "-") +
               (permissions.contains(PosixFilePermission.OWNER_WRITE) ? "w" : "-") +
               (permissions.contains(PosixFilePermission.OWNER_EXECUTE) ? "x" : "-") +
               (permissions.contains(PosixFilePermission.GROUP_READ) ? "r" : "-") +
               (permissions.contains(PosixFilePermission.GROUP_WRITE) ? "w" : "-") +
               (permissions.contains(PosixFilePermission.GROUP_EXECUTE) ? "x" : "-") +
               (permissions.contains(PosixFilePermission.OTHERS_READ) ? "r" : "-") +
               (permissions.contains(PosixFilePermission.OTHERS_WRITE) ? "w" : "-") +
               (permissions.contains(PosixFilePermission.OTHERS_EXECUTE) ? "x" : "-");
    }

    private String unixFileAttributesToString() {
        if (unixFileAttributes == null) {
            return "null";
        }
        final StringBuilder sb = new StringBuilder(4096);
        sb.append("{");
        final Iterator<Map.Entry<String, Object>> entries = unixFileAttributes.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<String, Object> entry = entries.next();
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            if (entries.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
