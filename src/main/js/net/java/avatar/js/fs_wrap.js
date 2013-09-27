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

(function(exports) {

    var fileSystem = __avatar.eventloop.fs();

    var mapJavaException = function(e, isfd) {
        if (e === null) {
            return null;
        }
        var code;
        var message;
        var path;
        if (e instanceof java.nio.file.NoSuchFileException) {
            code = 'ENOENT';
            message = 'no such file or directory';
            path = e.getFile();
        } else if (e instanceof java.nio.file.NotDirectoryException) {
            code = 'ENOTDIR';
            path = e.getFile();
            message = 'not a directory';
        } else if (e instanceof java.io.FileNotFoundException) {
            code = isfd ? 'EBADF' : 'ENOENT';
            message = 'file not found';
            // Best effort. This is not specified but this message contains the path
            path = e.getMessage();
        } else if (e instanceof java.nio.file.FileAlreadyExistsException) {
            code = 'EEXIST';
            message = 'file already exists, EEXIST';
            path = e.getFile();
        } else if (e instanceof java.nio.file.FileSystemException &&
                   e.getReason() === 'Operation not permitted') {
            code = 'EPERM';
            message = 'Operation not permitted';
            path = e.getFile();
        } else {
            // fall-through to generic IO error
            code = 'EIO';
            message = 'i/o error';
            if (Packages.net.java.avatar.js.Server.assertions()) {
                e.printStackTrace();
            }
        }
        var error = new Error(code + ', ' + message + ' \'' + e.message + '\'');
        error.code = code;
        if (path) {
            error.path = path;
        }
        return error;
    }

    var getStats = function(stat) {
        if (stat) {
            return new exports.Stats(stat);
        }
    }

    exports.stat = function(path, callback) {
        if (typeof callback === 'function') {
            fileSystem.stat(path, function(name, args) {
                callback(mapJavaException(args[0]), getStats(args[1]));
            });
        } else {
            try {
                return new exports.Stats(fileSystem.stat(path));
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.lstat = function(path, callback) {
        if (typeof callback === 'function') {
            fileSystem.lstat(path, function(name, args) {
                callback(mapJavaException(args[0]), getStats(args[1]));
            });
        } else {
            try {
                return new exports.Stats(fileSystem.lstat(path));
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.fstat = function(fd, callback) {
        if (typeof callback === 'function') {
            fileSystem.fstat(fd, function(name, args) {
                callback(mapJavaException(args[0], true), getStats(args[1]));
            });
        } else {
            try {
                return new exports.Stats(fileSystem.fstat(fd));
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.open = function(path, flags, mode, callback) {
        if (typeof callback === 'function') {
            return fileSystem.open(path, flags, mode, function(name, args) {
                callback(mapJavaException(args[0]), args[1]);
            });
        } else {
            try {
                return fileSystem.open(path, flags, mode);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.unlink = function(path, callback) {
        if (typeof callback === 'function') {
            return fileSystem.unlink(path, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.unlink(path);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.close = function(fd, callback) {
        if (typeof callback === 'function') {
            fileSystem.close(fd, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                fileSystem.close(fd);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.write = function(fd, buffer, offset, length, position, callback) {
        if (position === null || position === undefined) {
            position = -1;
        }
        if (typeof callback === 'function') {
            return fileSystem.write(fd, buffer._impl, offset, length, position,
                    function(name, args) {
                callback(mapJavaException(args[0], true), args[1], args[2]);
            });
        } else {
            try {
                return fileSystem.write(fd, buffer._impl, offset, length, position);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.read = function(fd, buffer, offset, length, position, callback) {
        if (position === null || position === undefined) {
            position = -1;
        }
        if (typeof callback === 'function') {
            return fileSystem.read(fd, buffer._impl, offset, length, position,
                    function(name, args) {
                callback(mapJavaException(args[0], true), args[1], args[2]);
            });
        } else {
            try {
                return fileSystem.read(fd, buffer._impl, offset, length, position);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.chmod = function(path, mode, callback) {
        if (typeof callback === 'function') {
            return fileSystem.chmod(path, mode, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.chmod(path, mode);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.fchmod = function(fd, mode, callback) {
        if (typeof callback === 'function') {
            return fileSystem.fchmod(fd, mode, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                return fileSystem.fchmod(fd, mode);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.readdir = function(path, callback) {
        if (typeof callback === 'function') {
            return fileSystem.readdir(path, function(name, args) {
                var ar = [];
                if (! args[0]) {
                    var it = args[1].iterator();
                    while (it.hasNext()) {
                        var e = it.next();
                        ar.push(e);
                    }
                }
                callback(mapJavaException(args[0]), ar);
            })
        } else {
            try {
                var ar = [];
                var list = fileSystem.readdir(path);
                var it = list.iterator();
                while (it.hasNext()) {
                    var e = it.next();
                    ar.push(e);
                }
                return ar;
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

/*
  { dev: 2114,
  ino: 48064969,
  mode: 33188,
  nlink: 1,
  uid: 85,
  gid: 100,
  rdev: 0,
  size: 527,
  blksize: 4096, <== Not retrievable in Java
  blocks: 8, <== Not retrievable in Java
  atime: Mon, 10 Oct 2011 23:24:11 GMT,
  mtime: Mon, 10 Oct 2011 23:24:11 GMT,
  ctime: Mon, 10 Oct 2011 23:24:11 GMT }
  */
    exports.Stats = function(stats) {
        // Needed first place
        var basic = stats.basicFileAttributes;
        if (basic) {
            this.size = basic.size();
        }
        this.mode = stats.mode;
        var that = this;

        Object.defineProperty(that, "atime",
        { get: function() {
                if (!that.__atime && basic) {
                    that.__atime = new Date(basic.lastAccessTime().toMillis());
                }
                return that.__atime;
            },
          set: undefined,
          enumerable: true,
          configurable: false
        });
        Object.defineProperty(that, "ctime",
        { get: function() {
                if (!that.__ctime && basic) {
                    that.__ctime = new Date(basic.creationTime().toMillis());
                }
                return that.__ctime;
            },
          set: undefined,
          enumerable: true,
          configurable: false
        });
        Object.defineProperty(that, "mtime",
        { get: function() {
                if (!that.__mtime && basic) {
                    that.__mtime = new Date(basic.lastModifiedTime().toMillis());
                }
                return that.__mtime;
            },
          set: undefined,
          enumerable: true,
          configurable: false
        });

        // We are deferring access to unix attributes. This is a perf killer.
        // We can't use __no_such_property, Stats properties MUST be enumerable.
        if (fileSystem.isWindows()) {
            that.uid = 0;
            that.gid = 0;
            that.dev = 0;
            that.ino = 0;
            that.nlink = 0;
        } else {
            Object.defineProperty(that, "uid",
            { get: function() {
                    var unix = stats.unixFileAttributes;
                    if (!that.__uid && unix) {
                        that.__uid = unix.get("uid");
                    }
                    return that.__uid == null ? undefined : that.__uid;
                },
              set: undefined,
              enumerable: true,
              configurable: false
            });
            Object.defineProperty(that, "gid",
            { get: function() {
                    var unix = stats.unixFileAttributes;
                    if (!that.__gid && unix) {
                        that.__gid = unix.get("gid");
                    }
                    return that.__gid == null ? undefined : that.__gid;
                },
              set: undefined,
              enumerable: true,
              configurable: false
            });
            Object.defineProperty(that, "dev",
            { get: function() {
                    var unix = stats.unixFileAttributes;
                    if (!that.__dev && unix) {
                        that.__dev = unix.get("dev");
                    }
                    return that.__dev == null ? undefined : that.__dev;
                },
              set: undefined,
              enumerable: true,
              configurable: false
            });
            Object.defineProperty(that, "ino",
            { get: function() {
                    var unix = stats.unixFileAttributes;
                    if (!that.__ino && unix) {
                        that.__ino = unix.get("ino");
                    }
                    return that.__ino == null ? undefined : that.__ino;
                },
              set: undefined,
              enumerable: true,
              configurable: false
            });
            Object.defineProperty(that, "nlink",
            { get: function() {
                    var unix = stats.unixFileAttributes;
                    if (!that.__nlink && unix) {
                        that.__nlink = unix.get("nlink");
                    }
                    return that.__nlink == null ? undefined : that.__nlink;
                },
              set: undefined,
              enumerable: true,
              configurable: false
            });
        }
    };

    exports.fsync = function(fd, callback) {
        if (typeof callback === 'function') {
            return fileSystem.fsync(fd, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                return fileSystem.fsync(fd);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.fdatasync = function(fd, callback) {
        if (typeof callback === 'function') {
            return fileSystem.fdatasync(fd, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                return fileSystem.fdatasync(fd);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.mkdir = function(path, mode, callback) {
        if (typeof callback === 'function') {
            return fileSystem.mkdir(path, mode, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.mkdir(path, mode);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.rmdir = function(path, callback) {
        if (typeof callback === 'function') {
            return fileSystem.rmdir(path, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.rmdir(path);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.rename = function(oldPath, newPath, callback) {
        if (typeof callback === 'function') {
            return fileSystem.rename(oldPath, newPath, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.rename(oldPath, newPath);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.ftruncate = function(fd, length, callback) {
        if (typeof callback === 'function') {
            return fileSystem.truncate(fd, length, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                return fileSystem.truncate(fd, length);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.fchown = function(fd, uid, gid, callback) {
        if (typeof callback === 'function') {
            return fileSystem.fchown(fd, uid, gid, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                return fileSystem.fchown(fd, uid, gid);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    exports.chown = function(path, uid, gid, callback) {
        if (typeof callback === 'function') {
            return fileSystem.chown(path, uid, gid, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.chown(path, uid, gid);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.link = function(srcpath, dstpath, callback) {
        if (typeof callback === 'function') {
            return fileSystem.link(srcpath, dstpath, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.link(srcpath, dstpath);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.symlink = function(destination, path, type_, callback) {
        if (typeof callback === 'function') {
            return fileSystem.symlink(destination, path, type_, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.symlink(destination, path, type_);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.readlink = function(path, callback) {
        if (typeof callback === 'function') {
            return fileSystem.readlink(path, function(name, args) {
                callback(mapJavaException(args[0]), args[1]);
            });
        } else {
            try {
                return fileSystem.readlink(path);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.utimes = function(path, atime, mtime, callback) {
        if (typeof callback === 'function') {
            return fileSystem.utimes(path, atime, mtime, function(name, args) {
                callback(extractArgs(args));
            });
        } else {
            try {
                return fileSystem.utimes(path, atime, mtime);
            } catch (e) {
                throw mapJavaException(e);
            }
        }
    }

    exports.futimes = function(fd, atime, mtime, callback) {
        if (typeof callback === 'function') {
            return fileSystem.futimes(fd, atime, mtime, function(name, args) {
                callback(extractArgs(args, true));
            });
        } else {
            try {
                return fileSystem.futimes(fd, atime, mtime);
            } catch (e) {
                throw mapJavaException(e, true);
            }
        }
    }

    function extractArgs(args, isfd) {
        return args && args.length > 0 ? mapJavaException(args[0], isfd) : undefined;
    }
});
