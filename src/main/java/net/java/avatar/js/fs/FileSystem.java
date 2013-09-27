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

import net.java.libuv.Callback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.SuppressWarnings;
import java.nio.channels.FileChannel;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.constants.Constants;
import net.java.avatar.js.eventloop.DaemonThreadFactory;
import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

public final class FileSystem {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private final EventLoop eventLoop;
    private final FdWrapperCache fdWrapperCache;
    private final WatchService watchService;

    private volatile boolean closed;

    public FileSystem(final EventLoop eventLoop) throws IOException {
        this.eventLoop = eventLoop;
        this.fdWrapperCache = new FdWrapperCache();
        this.watchService = FileSystems.getDefault().newWatchService();
        closed = false;
    }

    public boolean isWindows() {
        return IS_WINDOWS;
    }

    public void shutdown() throws IOException {
        fdWrapperCache.clear();
        watchService.close();
        closed = true;
    }

    @SuppressWarnings("try")
    public Stats stat(final String path) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Stats stats = new Stats(getPath(path), false, IS_WINDOWS);
            if (!stats.exists()) {
                throw new FileNotFoundException(getPath(path).toString());
            }
            return stats;
        }
    }

    public void stat(final String path,
                     final Callback callback) throws Exception {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final Stats stats = stat(path);
                    eventLoop.post(new Event("fs.stat", callback, null, stats, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.stat.error", callback, e, null, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public Stats lstat(final String path) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Stats stats = new Stats(getPath(path), true, IS_WINDOWS);
            if (!stats.exists()) {
                throw new FileNotFoundException(path);
            }
            return stats;
        }
    }

    public void lstat(final String path,
                      final Callback callback) throws Exception {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final Stats stats = lstat(path);
                    eventLoop.post(new Event("fs.lstat", callback, null, stats, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.lstat.error", callback, e, null, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public Stats fstat(final int fd) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            final Stats stats = new Stats(wrapper.getPath(), false, IS_WINDOWS);
            if (!stats.exists()) {
                throw new FileNotFoundException(wrapper.getPath().toString());
            }
            return stats;
        }
    }

    public void fstat(final int fd,
                      final Callback callback) throws Exception {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final Stats stats = fstat(fd);
                    eventLoop.post(new Event("fs.fstat", callback, null, stats, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.fstat.error", callback, e, null, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public int open(final String path,
                    final Object flags,
                    final Object mode) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Path p = getPath(path);
            final FileChannel channel = IS_WINDOWS ?
                FileChannel.open(p, mapFlagsToOpenOptions(flags)) : // use defaults on windows
                FileChannel.open(p, mapFlagsToOpenOptions(flags),
                        PosixFilePermissions.asFileAttribute(mapModeToPermissions(mode)));
            return fdWrapperCache.wrap(p, channel);
        }
    }

    public void open(final String path,
                     final Object flags,
                     final Object mode,
                     final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final int fd = open(path, flags, mode);
                    eventLoop.post(new Event("fs.open", callback, null, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.open.error", callback, e, null));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public int write(final int fd,
                     final Buffer buffer,
                     final long offset,
                     final long length,
                     final long position) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            final FileChannel channel = wrapper.getChannel();
            if (position >= 0) {
                channel.position(position);
            }
            return channel.write(buffer.toByteBuffer((int) offset, (int) (offset + length)));
        }
    }

    public void write(final int fd,
                      final Buffer buffer,
                      final long offset,
                      final long length,
                      final long position,
                      final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                int nwritten = 0;
                try {
                    nwritten = write(fd, buffer, offset, length, position);
                    eventLoop.post(new Event("fs.write", callback, null, nwritten, buffer, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.write.error", callback, e, nwritten, buffer, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public int read(final int fd,
                    final Buffer buffer,
                    final long offset,
                    final long length,
                    final long position) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            final FileChannel channel = wrapper.getChannel();
            if (position >= 0) {
                channel.position(position);
            }

            final int nread = channel.read(buffer.toByteBuffer((int) offset, (int) (offset + length)));
            if (nread > 0) {
                buffer.position(buffer.position() + nread);
            }
            return nread < 0 ? 0 : nread; // 0 is EOF
        }
    }

    public void read(final int fd,
                     final Buffer buffer,
                     final long offset,
                     final long length,
                     final long position,
                     final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                int nread = 0;
                try {
                    nread = read(fd, buffer, offset, length, position);
                    eventLoop.post(new Event("fs.read", callback, null, nread, buffer, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.read.error", callback, e, nread, buffer, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void rmdir(final String path) throws IOException {
        final Path p = getPath(path);
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            if (! Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                throw new NotDirectoryException(path);
            }
            Files.delete(p);
        }
    }

    public void rmdir(final String path,
                      final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    rmdir(path);
                    eventLoop.post(new Event("fs.rmdir", callback, null, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.rmdir.error", callback, e, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void mkdir(final String path,
                      final Object mode) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            if (IS_WINDOWS) {
                Files.createDirectory(getPath(path)); // use default permissions on windows
            } else {
                Files.createDirectory(getPath(path),
                        PosixFilePermissions.asFileAttribute(mapModeToPermissions(mode)));
            }
        }
    }

    public void mkdir(final String path,
                      final Object mode,
                      final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mkdir(path, mode);
                    eventLoop.post(new Event("fs.mkdir", callback, null, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.mkdir.error", callback, e, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public List<String> readdir(final String path) throws IOException {
        final List<String> result = new ArrayList<>();
        final Path p = getPath(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
            for (final Path entry : stream) {
                result.add(entry.getFileName().toString());
            }
        } catch (final DirectoryIteratorException ex) {
            // I/O error encountered during the iteration, the cause is an IOException
            throw ex.getCause();
        } catch (final NotDirectoryException nde) {
            throw nde;
        } catch (final IOException ioe) {
            // In case NotDirectoryException is not supported
            if (p.toFile().isDirectory()) {
                throw new NotDirectoryException(p.toString());
            }
            throw ioe;
        }
        return result;
    }

    public void readdir(final String path,
                        final Callback cb) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<String> list = readdir(path);
                    eventLoop.post(new Event("fs.readdir", cb, null, list, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.readdir.error", cb, e, null, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void link(final String srcpath,
                     final String dstpath) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            Files.createLink(getPath(dstpath), getPath(srcpath));
        }
    }

    public void link(final String srcpath,
                     final String dstpath,
                     final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    link(srcpath, dstpath);
                    eventLoop.post(new Event("fs.link", callback, null, srcpath, dstpath));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.link.error", callback, e, srcpath, dstpath));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void symlink(final String srcpath,
                        final String dstpath,
                        final String type) throws IOException {
        // Windows requires administrator privileges to create a symbolic link.
        if (!IS_WINDOWS) {
            try (final EventLoop.Handle handle = eventLoop.grab()) {
                final Path src = getPath(srcpath);
                final Path dst = getPath(dstpath);
                Files.createSymbolicLink(dst, src);
            }
        }
    }

    public void symlink(final String srcpath,
                        final String dstpath,
                        final String type,
                        final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    symlink(srcpath, dstpath, type);
                    eventLoop.post(new Event("fs.symlink", callback, null, srcpath, dstpath));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.symlink.error", callback, e, srcpath, dstpath));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public String readlink(final String path) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Path p = getPath(path);
            final String link = Files.readSymbolicLink(p).toString();
            // compatibility - dir paths end with a separator
            return Files.isDirectory(p) ? link + File.separator : link;
        }
    }

    public void readlink(final String path,
                         final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final String link = readlink(path);
                    eventLoop.post(new Event("fs.readlink", callback, null, link, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.readlink.error", callback, e, null, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void unlink(final String path) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Path p = getPath(path);
            Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public void unlink(final String path,
                       final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    unlink(path);
                    eventLoop.post(new Event("fs.unlink", callback, null, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.unlink.error", callback, e, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public boolean close(final int fd) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            return fdWrapperCache.close(fd);
        }
    }

    public void close(final int fd,
                      final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (close(fd)) {
                        eventLoop.post(new Event("fs.close", callback, null, fd));
                    } else {
                        // fd not found, likely already closed
                    }
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.close.error", callback, e, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    private void chmod(final Path path,
                       final Object mode) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Set<PosixFilePermission> permissions = mapModeToPermissions(mode);
            if (IS_WINDOWS) {
                Files.setAttribute(path, "dos:readonly",
                        !permissions.contains(PosixFilePermission.OWNER_WRITE));
            } else {
                Files.setPosixFilePermissions(path, permissions);
            }
        }
    }

    private void chmod(final Path path,
                       final Object mode,
                       final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    chmod(path, mode);
                    eventLoop.post(new Event("fs.chmod", callback, null, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.chmod.error", callback, e, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public void chmod(final String path,
                      final Object mode) throws IOException {
        chmod(getPath(path), mode);
    }

    public void chmod(final String path,
                      final Object mode,
                      final Callback callback) {
        chmod(getPath(path), mode, callback);
    }

    public void fchmod(final int fd,
                       final Object mode) throws IOException {
        final FdWrapper wrapper = fdWrapperCache.find(fd);
        chmod(wrapper.getPath(), mode);
    }

    public void fchmod(final int fd,
                       final Object mode,
                       final Callback callback) {
        try {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            chmod(wrapper.getPath(), mode);
            eventLoop.post(new Event("fs.fchmod", callback, null, fd));
        } catch (final IOException e) {
            eventLoop.post(new Event("fs.fchmod.error", callback, e, fd));
        }
    }

    @SuppressWarnings("try")
    private void chown(final Path path,
                       final int uid,
                       final int gid) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            Files.setAttribute(path, "unix:uid", uid, LinkOption.NOFOLLOW_LINKS);
            Files.setAttribute(path, "unix:gid", gid, LinkOption.NOFOLLOW_LINKS);        }
    }

    private void chown(final Path path,
                       final int uid,
                       final int gid,
                       final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    chown(path, uid, gid);
                    eventLoop.post(new Event("fs.chown", callback, null, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.chown.error", callback, e, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public void chown(final String path,
                      final int uid,
                      final int gid) throws IOException {
        chown(getPath(path), uid, gid);
    }

    public void chown(final String path,
                      final int uid,
                      final int gid, final Callback callback) {
        chown(getPath(path), uid, gid, callback);
    }

    public void fchown(final int fd,
                       final int uid,
                       final int gid) throws IOException {
        final FdWrapper wrapper = fdWrapperCache.find(fd);
        chown(wrapper.getPath(), uid, gid);
    }

    public void fchown(final int fd,
                       final int uid,
                       final int gid,
                       final Callback callback) {
        try {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            chown(wrapper.getPath(), uid, gid);
            eventLoop.post(new Event("fs.fchown", callback, null, fd));
        } catch (final IOException ex) {
            eventLoop.post(new Event("fs.fchown.error", callback, ex, fd));
        }
    }

    @SuppressWarnings("try")
    public void rename(final String oldPath,
                       final String newPath) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            Files.move(getPath(oldPath), getPath(newPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void rename(final String oldPath,
                       final String newPath,
                       final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    rename(oldPath, newPath);
                    eventLoop.post(new Event("fs.rename", callback, null, oldPath, newPath));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.rename.error", callback, e, oldPath, newPath));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void truncate(final int fd,
                         final long length) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            wrapper.getChannel().truncate(length);
        }
    }

    public void truncate(final int fd,
                         final long length,
                         final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    truncate(fd, length);
                    eventLoop.post(new Event("fs.truncate", callback, null, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.truncate.error", callback, e, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void utimes(final String path,
                       final double atime,
                       final double mtime) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            // We need to convert atime and mtime back to milliseconds
            // because fs.toUnixTimeStamp converts them to seconds.
            final FileTime accessFileTime = FileTime.fromMillis((long)(atime * 1000));
            final FileTime modifiedFileTime = FileTime.fromMillis((long)(mtime * 1000));
            Files.setAttribute(getPath(path), "lastAccessTime", accessFileTime);
            Files.setAttribute(getPath(path), "lastModifiedTime", modifiedFileTime);
        }
    }

    public void utimes(final String path,
                       final double atime,
                       final double mtime,
                       final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    utimes(path, atime, mtime);
                    eventLoop.post(new Event("fs.utimes", callback, null, path));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.utimes.error", callback, e, path));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    public void futimes(final int fd,
                        final double atime,
                        final double mtime) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final FdWrapper wrapper = fdWrapperCache.find(fd);
            utimes(wrapper.getPath().toString(), atime, mtime);
        }
    }

    public void futimes(final int fd,
                        final double atime,
                        final double mtime,
                        final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    futimes(fd, atime, mtime);
                    eventLoop.post(new Event("fs.futimes", callback, null, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.futimes.error", callback, e, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    @SuppressWarnings("try")
    private void force(final int fd,
                       final boolean meta) throws IOException {
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final FdWrapper w = fdWrapperCache.find(fd);
            final FileChannel ch = w.getChannel();
            ch.force(meta);
        }
    }

    private void force(final int fd,
                       final boolean meta,
                       final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final FdWrapper w = fdWrapperCache.find(fd);
                    final FileChannel ch = w.getChannel();
                    ch.force(meta);
                    eventLoop.post(new Event("fs.force", callback, null, fd));
                } catch (final IOException e) {
                    eventLoop.post(new Event("fs.force.error", callback, e, fd));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public void fsync(final int fd) throws IOException {
        force(fd, true);
    }

    public void fsync(final int fd, final Callback callback) {
        force(fd, true, callback);
    }

    public void fdatasync(final int fd) throws IOException {
        force(fd, false);
    }

    public void fdatasync(final int fd, final Callback callback) {
        force(fd, false, callback);
    }

    private final Map<WatchKey, Callback> watchedPaths = new HashMap<>();
    private final ThreadFactory watchThreadFactory = new DaemonThreadFactory("node.fs.watch");
    private final AtomicBoolean watchThreadStarted = new AtomicBoolean(false);

    private final Thread watchThread = watchThreadFactory.newThread(new Runnable() {
        @Override
        public void run() {
            WatchKey key;
            Callback callback;
            while (!closed) {
                try {
                    /* quoting fs.watchFile, which hard-codes the interval */
                    // Poll interval in milliseconds. 5007 is what libev used to use. It's
                    // a little on the slow side but let's stick with it for now to keep
                    // behavioral changes to a minimum.
                    key = watchService.poll(5007, TimeUnit.MILLISECONDS);
                    if (key != null && key.isValid()) {
                        callback = watchedPaths.get(key);
                        assert callback != null : "callback not found for " + key.watchable();
                        if (callback != null) {
                            eventLoop.post(new Event("fs.watch", callback, key.watchable()));
                        }
                    }
                } catch (InterruptedException ignore) {
                } catch (ClosedWatchServiceException done) {
                    return;
                }
            }
        }
    });

    @SuppressWarnings("try")
    public WatchKey watch(final String filename, final Callback callback) throws IOException {
        // start watch thread lazily on the very first registration
        if (watchThreadStarted.compareAndSet(false, true)) {
            watchThread.start();
        }
        try (final EventLoop.Handle handle = eventLoop.grab()) {
            final Path path = getPath(filename);
            final WatchKey watchKey = path.register(watchService,
                                                    StandardWatchEventKinds.ENTRY_CREATE,
                                                    StandardWatchEventKinds.ENTRY_DELETE,
                                                    StandardWatchEventKinds.ENTRY_MODIFY);
            watchedPaths.put(watchKey, callback);
            return watchKey;
        }
    }

    public void unwatch(final WatchKey watchKey) {
        if (watchKey != null) {
            watchKey.cancel();
            watchedPaths.remove(watchKey);
        }
    }

    private static Set<PosixFilePermission> mapModeToPermissions(final Object mode) {
        if (mode != null) {
            final String smode = mode.toString();
            if (!"undefined".equals(smode)) {
                final int imode = Double.valueOf(smode).intValue();
                final Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
                if ((imode & Constants.S_IRUSR) != 0) {
                    permissions.add(PosixFilePermission.OWNER_READ);
                }
                if ((imode & Constants.S_IWUSR) != 0) {
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                }
                if ((imode & Constants.S_IXUSR) != 0) {
                    permissions.add(PosixFilePermission.OWNER_EXECUTE);
                }

                if ((imode & Constants.S_IRGRP) != 0) {
                    permissions.add(PosixFilePermission.GROUP_READ);
                }
                if ((imode & Constants.S_IWGRP) != 0) {
                    permissions.add(PosixFilePermission.GROUP_WRITE);
                }
                if ((imode & Constants.S_IXGRP) != 0) {
                    permissions.add(PosixFilePermission.GROUP_EXECUTE);
                }

                if ((imode & Constants.S_IROTH) != 0) {
                    permissions.add(PosixFilePermission.OTHERS_READ);
                }
                if ((imode & Constants.S_IWOTH) != 0) {
                    permissions.add(PosixFilePermission.OTHERS_WRITE);
                }
                if ((imode & Constants.S_IXOTH) != 0) {
                    permissions.add(PosixFilePermission.OTHERS_EXECUTE);
                }
                return permissions;
            }
        }
        return null;
    }

    static int mapPermissionsToMode(final Set<PosixFilePermission> permissions) {
        int mode = 0;
        if (permissions.contains(PosixFilePermission.OWNER_READ)) {
            mode |= Constants.S_IRUSR;
        }
        if (permissions.contains(PosixFilePermission.OWNER_WRITE)) {
            mode |= Constants.S_IWUSR;
        }
        if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
            mode |= Constants.S_IXUSR;
        }
        if (permissions.contains(PosixFilePermission.GROUP_READ)) {
            mode |= Constants.S_IRGRP;
        }
        if (permissions.contains(PosixFilePermission.GROUP_WRITE)) {
            mode |= Constants.S_IWGRP;
        }
        if (permissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
            mode |= Constants.S_IXGRP;
        }
        if (permissions.contains(PosixFilePermission.OTHERS_READ)) {
            mode |= Constants.S_IROTH;
        }
        if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
            mode |= Constants.S_IWOTH;
        }
        if (permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
            mode |= Constants.S_IXOTH;
        }
        return mode;
    }

    private static Set<? extends OpenOption> mapFlagsToOpenOptions(final Object flags) {
        final EnumSet<StandardOpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
        if (flags != null) {
            final String sflags = flags.toString();
            if (!"undefined".equals(sflags)) {
                final int iflags = Double.valueOf(sflags).intValue();
                if ((iflags & Constants.O_APPEND) != 0) {
                    options.add(StandardOpenOption.APPEND);
                }
                if ((iflags & Constants.O_CREAT) != 0) {
                    options.add(StandardOpenOption.CREATE);
                }
                if ((iflags & Constants.O_TRUNC) != 0) {
                    options.add(StandardOpenOption.TRUNCATE_EXISTING);
                }
                if (iflags == Constants.O_RDONLY) { // Constants.O_RDONLY == 0
                    options.add(StandardOpenOption.READ);
                }
                if ((iflags & Constants.O_RDWR) != 0) {
                    options.add(StandardOpenOption.READ);
                    options.add(StandardOpenOption.WRITE);
                }
                if ((iflags & Constants.O_WRONLY) != 0) {
                    options.add(StandardOpenOption.WRITE);
                }
            }
        }
        return options;
    }

    // java.nio.file APIs do not like Long UNC paths on Windows (as of Java7)
    // strip the leading '\\?\'
    private static String shortenLongUNCPath(final String path) {
        if (IS_WINDOWS) {
            final String longUNCPrefix = "\\\\?\\";
            if (path.startsWith(longUNCPrefix)) {
                return path.substring(longUNCPrefix.length());
            }
        }
        return path;
    }

    // fix incorrect paths such as 'D:\D:\foo', if present
    private static String fixDoubleDriveLetters(final String path) {
        if (IS_WINDOWS) {
            if (path.matches("^[A-Za-z]:\\\\[A-Za-z]:\\\\.*")) {
                return path.substring(3);
            }
        }
        return path;
    }

    private static Path getPath(final String p) {
        return IS_WINDOWS ?
            FileSystems.getDefault().getPath(fixDoubleDriveLetters(shortenLongUNCPath(p))) :
            FileSystems.getDefault().getPath(p);
    }

}
