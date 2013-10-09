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

    var FileHandle = Packages.net.java.libuv.handles.FileHandle;
    var PendingOperations = Packages.net.java.libuv.handles.PendingOperations;
    var JavaBuffer = Packages.net.java.avatar.js.buffer.Buffer;
    var loop = __avatar.eventloop.loop();
    var fs = new FileHandle(loop);

    Object.defineProperty(exports, '_closeCallback', { value: new PendingOperations() });

    exports.close = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = exports._closeCallback.push(callback);
            fs.setCloseCallback(function(id, args) {
                var cb = exports._closeCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.close(fd, id);
        } else {
            try {
                return fs.close(fd);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_openCallback', { value: new PendingOperations() });

    exports.open = function(path, flags, mode, callback) {
        if (typeof callback === 'function') {
            var id = exports._openCallback.push(callback);
            var cb = callback;
            fs.setOpenCallback(function(id, args) {
                var cb = exports._openCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    var fd = args[0];
                    cb(undefined, args[0]);
                }
            });
            return fs.open(path, flags, mode, id);
        } else {
            try {
                return fs.open(path, flags, mode);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_readCallback', { value: new PendingOperations() });

    exports.read = function(fd, buffer, offset, length, position, callback) {
        if (position == null || position == undefined) {
            position = -1;
        }
        if (typeof callback === 'function') {
            var id = exports._readCallback.push(callback);
            fs.setReadCallback(function(id, args) {
                var cb = exports._readCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException), undefined, -1);
                } else {
                    var bytesRead = args[0];
                    var data = new Buffer(new JavaBuffer(args[1]));
                    cb(undefined, bytesRead, data);
                }
            });
            return fs.read(fd, buffer._impl.array(), offset, length, position, id);
        } else {
            try {
                return fs.read(fd, buffer._impl.array(), offset, length, position);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_unlinkCallback', { value: new PendingOperations() });

    exports.unlink = function(path, callback) {
        if (typeof callback === 'function') {
            var id = exports._unlinkCallback.push(callback);
            fs.setUnlinkCallback(function(id, args) {
                var cb = exports._unlinkCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.unlink(path, id);
        } else {
            try {
                return fs.unlink(path);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_writeCallback', { value: new PendingOperations() });

    exports.write = function(fd, buffer, offset, length, position, callback) {
        if (position == null || position == undefined) {
            position = -1;
        }
        if (typeof callback === 'function') {
            var id = exports._writeCallback.push(callback);
            fs.setWriteCallback(function(id, args) {
                var cb = exports._writeCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException), -1, undefined);
                } else {
                    var bytesWritten = args[0];
                    cb(undefined, bytesWritten, buffer);
                }
            });
            return fs.write(fd, buffer._impl.array(), offset, length, position, id);
        } else {
            try {
                return fs.write(fd, buffer._impl.array(), offset, length, position);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_mkdirCallback', { value: new PendingOperations() });

    exports.mkdir = function(path, mode, callback) {
        if (typeof callback === 'function') {
            var id = exports._mkdirCallback.push(callback);
            fs.setMkdirCallback(function(id, args) {
                var cb = exports._mkdirCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.mkdir(path, mode, id);
        } else {
            try {
                return fs.mkdir(path, mode);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_rmdirCallback', { value: new PendingOperations() });

    exports.rmdir = function(path, callback) {
        if (typeof callback === 'function') {
            var id = exports._rmdirCallback.push(callback);
            fs.setRmdirCallback(function(id, args) {
                var cb = exports._rmdirCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.rmdir(path, id);
        } else {
            try {
                return fs.rmdir(path);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_readdirCallback', { value: new PendingOperations() });

    exports.readdir = function(path, callback) {
        if (typeof callback === 'function') {
            var id = exports._readdirCallback.push(callback);
            fs.setReaddirCallback(function(id, args) {
                var cb = exports._readdirCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException), undefined);
                } else {
                    var dirs = [];
                    for (var i = 0; i < args.length; i++) {
                        dirs.push(args[i]);
                    }
                    cb(undefined, dirs);
                }
            });
            return fs.readdir(path, 0, id);
        } else {
            try {
                var args = fs.readdir(path, 0);
                var dirs = [];
                for (var i = 0; i < args.length; i++) {
                    dirs.push(args[i]);
                }
                return dirs;
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_statCallback', { value: new PendingOperations() });

    exports.stat = function(path, callback) {
        if (typeof callback === 'function') {
            var id = exports._statCallback.push(callback);
            fs.setStatCallback(function(id, args) {
                var cb = exports._statCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, new exports.Stats(args[0]));
                }
            });
            return fs.stat(path, id);
        } else {
            try {
                var stats = fs.stat(path);
                return new exports.Stats(stats);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_fstatCallback', { value: new PendingOperations() });

    exports.fstat = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = exports._fstatCallback.push(callback);
            fs.setFStatCallback(function(id, args) {
                var cb = exports._fstatCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, new exports.Stats(args[0]));
                }
            });
            return fs.fstat(fd, id);
        } else {
            try {
                var stats = fs.fstat(fd);
                return new exports.Stats(stats);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_renameCallback', { value: new PendingOperations() });

    exports.rename = function(oldPath, newPath, callback) {
        if (typeof callback === 'function') {
            var id = exports._renameCallback.push(callback);
            fs.setRenameCallback(function(id, args) {
                var cb = exports._renameCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.rename(oldPath, newPath, id);
        } else {
            try {
                return fs.rename(oldPath, newPath);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_fsyncCallback', { value: new PendingOperations() });

    exports.fsync = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = exports._fsyncCallback.push(callback);
            fs.setFSyncCallback(function(id, args) {
                var cb = exports._fsyncCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.fsync(fd, id);
        } else {
            try {
                return fs.fsync(fd);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_fdatasyncCallback', { value: new PendingOperations() });

    exports.fdatasync = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = exports._fdatasyncCallback.push(callback);
            fs.setFDatasyncCallback(function(id, args) {
                var cb = exports._fdatasyncCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.fdatasync(fd, id);
        } else {
            try {
                return fs.fdatasync(fd);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_ftruncateCallback', { value: new PendingOperations() });

    exports.ftruncate = function(fd, length, callback) {
        if (typeof callback === 'function') {
            var id = exports._ftruncateCallback.push(callback);
            fs.setFTuncateCallback(function(id, args) {
                var cb = exports._ftruncateCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.ftruncate(fd, length, id);
        } else {
            try {
                return fs.ftruncate(fd, length);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_chmodCallback', { value: new PendingOperations() });

    exports.chmod = function(path, mode, callback) {
        if (typeof callback === 'function') {
            var id = exports._chmodCallback.push(callback);
            fs.setChmodCallback(function(id, args) {
                var cb = exports._chmodCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.chmod(path, mode, id);
        } else {
            try {
                return fs.chmod(path, mode);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_utimeCallback', { value: new PendingOperations() });

    exports.utimes = function(path, atime, mtime, callback) {
        if (typeof callback === 'function') {
            var id = exports._utimeCallback.push(callback);
            fs.setUtimeCallback(function(id, args) {
                var cb = exports._utimeCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, args[0]);
                }
            });
            return fs.utime(path, atime, mtime, id);
        } else {
            try {
                return fs.utime(path, atime, mtime);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_futimeCallback', { value: new PendingOperations() });

    exports.futimes = function(fd, atime, mtime, callback) {
        if (typeof callback === 'function') {
            var id = exports._futimeCallback.push(callback);
            fs.setFUtimeCallback(function(id, args) {
                var cb = exports._futimeCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, args[0]);
                }
            });
            return fs.futime(fd, atime, mtime, id);
        } else {
            try {
                return fs.futime(fd, atime, mtime);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_lstatCallback', { value: new PendingOperations() });

    exports.lstat = function(path, callback) {
        if (typeof callback === 'function') {
            var id = exports._lstatCallback.push(callback);
            fs.setLStatCallback(function(id, args) {
                var cb = exports._lstatCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, new exports.Stats(args[0]));
                }
            });
            return fs.lstat(path, id);
        } else {
            try {
                var stats = fs.lstat(path);
                return new exports.Stats(stats);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_linkCallback', { value: new PendingOperations() });

    exports.link = function(srcpath, dstpath, callback) {
        if (typeof callback === 'function') {
            var id = exports._linkCallback.push(callback);
            fs.setLinkCallback(function(id, args) {
                var cb = exports._linkCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.link(srcpath, dstpath, id);
        } else {
            try {
                return fs.link(srcpath, dstpath);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_symlinkCallback', { value: new PendingOperations() });

    exports.symlink = function(destination, path, type, callback) {
        var flags = 0;
        if (type === 'dir') {
            flags |= 0x0001;  //UV_FS_SYMLINK_DIR
        } else if (type === 'junction') {
            flags |= 0x0002; //UV_FS_SYMLINK_JUNCTION
        } else if (type != 'file') {
            throw new Error('Unknown symlink type');
        }

        if (typeof callback === 'function') {
            var id = exports._symlinkCallback.push(callback);
            fs.setSymlinkCallback(function(id, args) {
                var cb = exports._symlinkCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, args[0]);
                }
            });
            return fs.symlink(destination, path, flags, id);
        } else {
            try {
                return fs.symlink(destination, path, flags);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_readlinkCallback', { value: new PendingOperations() });

    exports.readlink = function(path, callback) {
        if (typeof callback === 'function') {
            var id = exports._readlinkCallback.push(callback);
            fs.setReadlinkCallback(function(id, args) {
                var cb = exports._readlinkCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb(undefined, args[0]);
                }
            });
            return fs.readlink(path, id);
        } else {
            try {
                return fs.readlink(path);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_fchmodCallback', { value: new PendingOperations() });

    exports.fchmod = function(fd, mode, callback) {
        if (typeof callback === 'function') {
            var id = exports._fchmodCallback.push(callback);
            fs.setFChmodCallback(function(id, args) {
                var cb = exports._fchmodCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.fchmod(fd, mode, id);
        } else {
            try {
                return fs.fchmod(fd, mode);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_chownCallback', { value: new PendingOperations() });

    exports.chown = function(path, uid, gid, callback) {
        if (typeof callback === 'function') {
            var id = exports._chownCallback.push(callback);
            fs.setChownCallback(function(id, args) {
                var cb = exports._chownCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.chown(path, uid, gid, id);
        } else {
            try {
                return fs.chown(path, uid, gid);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    Object.defineProperty(exports, '_fchownCallback', { value: new PendingOperations() });

    exports.fchown = function(fd, uid, gid, callback) {
        if (typeof callback === 'function') {
            var id = exports._fchownCallback.push(callback);
            fs.setFChownCallback(function(id, args) {
                var cb = exports._fchownCallback.shift(id);
                var status = args[0];
                if (status == -1) {
                    var nativeException = args[1];
                    cb(exports._error(nativeException));
                } else {
                    cb();
                }
            });
            return fs.fchown(path, uid, gid, id);
        } else {
            try {
                return fs.fchown(path, uid, gid);
            } catch(e) {
                throw exports._error(e);
            }
        }
    }

    exports.Stats = function(stats) {
        Object.defineProperty(this, 'dev', { enumerable: true, get: function() {  return stats.getDev(); } });
        Object.defineProperty(this, 'ino', { enumerable: true, get: function() {  return stats.getIno(); } });
        Object.defineProperty(this, 'mode', { enumerable: true, get: function() {  return stats.getMode(); } });
        Object.defineProperty(this, 'nlink', { enumerable: true, get: function() {  return stats.getNlink(); } });
        Object.defineProperty(this, 'uid', { enumerable: true, get: function() {  return stats.getUid(); } });
        Object.defineProperty(this, 'gid', { enumerable: true, get: function() {  return stats.getGid(); } });
        Object.defineProperty(this, 'rdev', { enumerable: true, get: function() {  return stats.getRdev(); } });
        Object.defineProperty(this, 'size', { enumerable: true, get: function() {  return stats.getSize(); } });
        Object.defineProperty(this, 'blksize', { enumerable: true, get: function() {  return stats.getBlksize(); } });
        Object.defineProperty(this, 'blocks', { enumerable: true, get: function() {  return stats.getBlocks(); } });
        Object.defineProperty(this, 'atime', { enumerable: true, get: function() {  return new Date(stats.getAtime()); } });
        Object.defineProperty(this, 'mtime', { enumerable: true, get: function() {  return new Date(stats.getMtime()); } });
        Object.defineProperty(this, 'ctime', { enumerable: true, get: function() {  return new Date(stats.getCtime()); } });
    }

    Object.defineProperty(exports, '_error', {
        value : function(exception) {
                  var error = new Error(exception.getErrnoMessage());
                  error.errno = exception.errno()
                  error.code = exception.errnoString();
                  error.message = exception.errnoString() + ', ' + exception.getErrnoMessage() + ' \'' + exception.path() +'\'';
                  error.path = exception.path();
                  process._errno = error.code;
                  return error;
              }
    });
});
