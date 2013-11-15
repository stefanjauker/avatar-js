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

    var Files = Packages.net.java.libuv.Files;
    var FilePollHandle = Packages.net.java.libuv.handles.FilePollHandle;
    var JavaBuffer = Packages.net.java.avatar.js.buffer.Buffer;
    var loop = __avatar.eventloop.loop();
    var fs = new Files(loop);

    fs.setCloseCallback(function(cb, fd, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.close = function(fd, callback) {
        if (typeof callback === 'function') {
            return fs.close(fd, callback);
        } else {
            try {
                return fs.close(fd);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setOpenCallback(function(cb, fd, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, fd);
        }
    });

    exports.open = function(path, flags, mode, callback) {
        if (typeof callback === 'function') {
            return fs.open(path, flags, mode, callback);
        } else {
            try {
                return fs.open(path, flags, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setReadCallback(function(cb, bytesRead, data, nativeException) {
        if (nativeException) {
            cb(newError(nativeException), bytesRead, data);
        } else {
            cb(undefined, bytesRead, new Buffer(new JavaBuffer(data)));
        }
    });

    exports.read = function(fd, buffer, offset, length, position, callback) {
        if (position == null || position == undefined) {
            position = -1;
        }
        if (typeof callback === 'function') {
            return fs.read(fd, buffer._impl.array(), offset, length, position, callback);
        } else {
            try {
                return fs.read(fd, buffer._impl.array(), offset, length, position);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setUnlinkCallback(function(cb, nativeException) {
        if(nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.unlink = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.unlink(path, callback);
        } else {
            try {
                return fs.unlink(path);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setWriteCallback(function(cb, bytesWritten, nativeException) {
        if (nativeException) {
            cb(newError(nativeException), bytesWritten);
        } else {
            cb(undefined, bytesWritten);
        }
    });

    exports.write = function(fd, buffer, offset, length, position, callback) {
        if (position == null || position == undefined) {
            position = -1;
        } else if (position % 1 != 0) {
            throw new TypeError("Not an integer");
        }

        if (typeof callback === 'function') {
            var r = fs.write(fd, buffer._impl.array(), offset, length, position, callback);
        } else {
            try {
                return fs.write(fd, buffer._impl.array(), offset, length, position);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setMkDirCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.mkdir = function(path, mode, callback) {
        if (typeof callback === 'function') {
            return fs.mkdir(path, mode, callback);
        } else {
            try {
                return fs.mkdir(path, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setRmDirCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.rmdir = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.rmdir(path, callback);
        } else {
            try {
                return fs.rmdir(path);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setReadDirCallback(function(cb, names, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            var dirs = [];
            var files = names;
            for (var i = 0; i < files.length; i++) {
                dirs.push(files[i]);
            }
            cb(undefined, dirs);
        }
    });

    exports.readdir = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.readdir(path, 0, callback);
        } else {
            try {
                var args = fs.readdir(path, 0);
                var dirs = [];
                for (var i = 0; i < args.length; i++) {
                    dirs.push(args[i]);
                }
                return dirs;
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setStatCallback(function(cb, stats, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, new exports.Stats(stats));
        }
    });

    exports.stat = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.stat(path, callback);
        } else {
            try {
                var stats = fs.stat(path);
                return new exports.Stats(stats);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFStatCallback(function(cb, stats, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, new exports.Stats(stats));
        }
    });

    exports.fstat = function(fd, callback) {
        if (typeof callback === 'function') {
            return fs.fstat(fd, callback);
        } else {
            try {
                var stats = fs.fstat(fd);
                return new exports.Stats(stats);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setRenameCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.rename = function(oldPath, newPath, callback) {
        if (typeof callback === 'function') {
            return fs.rename(oldPath, newPath, callback);
        } else {
            try {
                return fs.rename(oldPath, newPath);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFSyncCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fsync = function(fd, callback) {
        if (typeof callback === 'function') {
            return fs.fsync(fd, callback);
        } else {
            try {
                return fs.fsync(fd);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFDatasyncCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fdatasync = function(fd, callback) {
        if (typeof callback === 'function') {
            return fs.fdatasync(fd, callback);
        } else {
            try {
                return fs.fdatasync(fd);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFTruncateCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.ftruncate = function(fd, length, callback) {
        if (typeof callback === 'function') {
            return fs.ftruncate(fd, length, callback);
        } else {
            try {
                return fs.ftruncate(fd, length);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setChmodCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.chmod = function(path, mode, callback) {
        if (typeof callback === 'function') {
            return fs.chmod(path, mode, callback);
        } else {
            try {
                return fs.chmod(path, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setUTimeCallback(function(cb, time, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, time);
        }
    });

    exports.utimes = function(path, atime, mtime, callback) {
        if (typeof callback === 'function') {
            return fs.utime(path, atime, mtime, callback);
        } else {
            try {
                return fs.utime(path, atime, mtime);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFUTimeCallback(function(cb, time, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, time);
        }
    });

    exports.futimes = function(fd, atime, mtime, callback) {
        if (typeof callback === 'function') {
            return fs.futime(fd, atime, mtime, callback);
        } else {
            try {
                return fs.futime(fd, atime, mtime);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setLStatCallback(function(cb, stats, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, new exports.Stats(stats));
        }
    });

    exports.lstat = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.lstat(path, callback);
        } else {
            try {
                var stats = fs.lstat(path);
                return new exports.Stats(stats);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setLinkCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.link = function(srcpath, dstpath, callback) {
        if (typeof callback === 'function') {
            return fs.link(srcpath, dstpath, callback);
        } else {
            try {
                return fs.link(srcpath, dstpath);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setSymLinkCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.symlink = function(destination, path, type, callback) {
        var flags = 0;
        if (!type) {
            type = 'file';
        }
        if (type === 'dir') {
            flags |= 0x0001;  //UV_FS_SYMLINK_DIR
        } else if (type === 'junction') {
            flags |= 0x0002; //UV_FS_SYMLINK_JUNCTION
        } else if (type != 'file') {
            throw new Error('Unknown symlink type');
        }

        if (typeof callback === 'function') {
            return fs.symlink(destination, path, flags, callback);
        } else {
            try {
                return fs.symlink(destination, path, flags);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setReadLinkCallback(function(cb, name, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, name);
        }
    });

    exports.readlink = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.readlink(path, callback);
        } else {
            try {
                return fs.readlink(path);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFChmodCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fchmod = function(fd, mode, callback) {
        if (typeof callback === 'function') {
            return fs.fchmod(fd, mode, callback);
        } else {
            try {
                return fs.fchmod(fd, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setChownCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.chown = function(path, uid, gid, callback) {
        if (typeof callback === 'function') {
            return fs.chown(path, uid, gid, callback);
        } else {
            try {
                return fs.chown(path, uid, gid);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    fs.setFChownCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fchown = function(fd, uid, gid, callback) {
        if (typeof callback === 'function') {
            return fs.fchown(path, uid, gid, callback);
        } else {
            try {
                return fs.fchown(path, uid, gid);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    exports.StatWatcher = StatWatcher;

    function StatWatcher() {
        var that = this;
        this._fsPoll = new FilePollHandle(loop);

        this._fsPoll.setFilePollCallback(function(status, previous, current) {
            if (that.onchange) {
                that.onchange(new exports.Stats(current), new exports.Stats(previous), status);
            }
        });

        this._fsPoll.setStopCallback(function() {
            if (that.onstop) {
                that.onstop();
            }
        });
    }

    StatWatcher.prototype.start = function(filename, persistent, interval) {
        try {
            var status = this._fsPoll.start(filename, persistent, interval);
        } catch (err) {
            if(!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
        }
        return status;
    }

    StatWatcher.prototype.stop = function() {
        this._fsPoll.stop();
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

    var newError = function(exception) {
        var error = new Error(exception.getErrnoMessage());
        error.errno = exception.errno()
        error.code = exception.errnoString();
        error.message = exception.errnoString() + ', ' + exception.getErrnoMessage() + ' \'' + exception.path() +'\'';
        error.path = exception.path();
        process._errno = error.code;
        return error;
    }
});
