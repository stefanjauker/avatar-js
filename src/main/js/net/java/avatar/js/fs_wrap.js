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
    var PendingOperations = Packages.net.java.avatar.js.fs.PendingOperations;
    var JavaBuffer = Packages.net.java.avatar.js.buffer.Buffer;
    var loop = __avatar.eventloop.loop();
    var fs = new Files(loop);

    Object.defineProperty(this, '_closeCallbacks', { value: new PendingOperations() });

    fs.setCloseCallback(function(id, args) {
        var cb = _closeCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.close = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = _closeCallbacks.push(callback);
            return fs.close(fd, id);
        } else {
            try {
                return fs.close(fd);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_openCallbacks', { value: new PendingOperations() });

    fs.setOpenCallback(function(id, args) {
        var cb = _openCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            var fd = args[0];
            cb(undefined, args[0]);
        }
    });

    exports.open = function(path, flags, mode, callback) {
        if (typeof callback === 'function') {
            var id = _openCallbacks.push(callback);
            return fs.open(path, flags, mode, id);
        } else {
            try {
                return fs.open(path, flags, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_readCallbacks', { value: new PendingOperations() });

    fs.setReadCallback(function(id, bytesRead, data, nativeException) {
        var cb = _readCallbacks.shift(id);
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
            var id = _readCallbacks.push(callback);
            return fs.read(fd, buffer._impl.array(), offset, length, position, id);
        } else {
            try {
                return fs.read(fd, buffer._impl.array(), offset, length, position);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_unlinkCallbacks', { value: new PendingOperations() });

    fs.setUnlinkCallback(function(id, args) {
        var cb = _unlinkCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.unlink = function(path, callback) {
        if (typeof callback === 'function') {
            var id = _unlinkCallbacks.push(callback);
            return fs.unlink(path, id);
        } else {
            try {
                return fs.unlink(path);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_writeCallbacks', { value: new PendingOperations() });

    fs.setWriteCallback(function(id, bytesWritten, nativeException) {
        var cb = _writeCallbacks.shift(id);
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
            var id = _writeCallbacks.push(callback);
            var r = fs.write(fd, buffer._impl.array(), offset, length, position, id);
        } else {
            try {
                return fs.write(fd, buffer._impl.array(), offset, length, position);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_mkdirCallbacks', { value: new PendingOperations() });

    fs.setMkdirCallback(function(id, args) {
        var cb = _mkdirCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.mkdir = function(path, mode, callback) {
        if (typeof callback === 'function') {
            var id = _mkdirCallbacks.push(callback);
            return fs.mkdir(path, mode, id);
        } else {
            try {
                return fs.mkdir(path, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_rmdirCallbacks', { value: new PendingOperations() });

    fs.setRmdirCallback(function(id, args) {
        var cb = _rmdirCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.rmdir = function(path, callback) {
        if (typeof callback === 'function') {
            var id = _rmdirCallbacks.push(callback);
            return fs.rmdir(path, id);
        } else {
            try {
                return fs.rmdir(path);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_readdirCallbacks', { value: new PendingOperations() });

    fs.setReaddirCallback(function(id, args) {
        var cb = _readdirCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException), undefined);
        } else {
            var dirs = [];
            var names = args[1];
            for (var i = 0; i < names.length; i++) {
                dirs.push(names[i]);
            }
            cb(undefined, dirs);
        }
    });

    exports.readdir = function(path, callback) {
        if (typeof callback === 'function') {
            var id = _readdirCallbacks.push(callback);
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
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_statCallbacks', { value: new PendingOperations() });

    fs.setStatCallback(function(id, args) {
        var cb = _statCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, new exports.Stats(args[1]));
        }
    });

    exports.stat = function(path, callback) {
        if (typeof callback === 'function') {
            var id = _statCallbacks.push(callback);
            return fs.stat(path, id);
        } else {
            try {
                var stats = fs.stat(path);
                return new exports.Stats(stats);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_fstatCallbacks', { value: new PendingOperations() });

    fs.setFStatCallback(function(id, args) {
        var cb = _fstatCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, new exports.Stats(args[1]));
        }
    });

    exports.fstat = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = _fstatCallbacks.push(callback);
            return fs.fstat(fd, id);
        } else {
            try {
                var stats = fs.fstat(fd);
                return new exports.Stats(stats);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_renameCallbacks', { value: new PendingOperations() });

    fs.setRenameCallback(function(id, args) {
        var cb = _renameCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.rename = function(oldPath, newPath, callback) {
        if (typeof callback === 'function') {
            var id = _renameCallbacks.push(callback);
            return fs.rename(oldPath, newPath, id);
        } else {
            try {
                return fs.rename(oldPath, newPath);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_fsyncCallbacks', { value: new PendingOperations() });

    fs.setFSyncCallback(function(id, args) {
        var cb = _fsyncCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fsync = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = _fsyncCallbacks.push(callback);
            return fs.fsync(fd, id);
        } else {
            try {
                return fs.fsync(fd);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_fdatasyncCallbacks', { value: new PendingOperations() });

    fs.setFDatasyncCallback(function(id, args) {
        var cb = _fdatasyncCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fdatasync = function(fd, callback) {
        if (typeof callback === 'function') {
            var id = _fdatasyncCallbacks.push(callback);
            return fs.fdatasync(fd, id);
        } else {
            try {
                return fs.fdatasync(fd);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_ftruncateCallbacks', { value: new PendingOperations() });

    fs.setFTruncateCallback(function(id, args) {
        var cb = _ftruncateCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.ftruncate = function(fd, length, callback) {
        if (typeof callback === 'function') {
            var id = _ftruncateCallbacks.push(callback);
            return fs.ftruncate(fd, length, id);
        } else {
            try {
                return fs.ftruncate(fd, length);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_chmodCallbacks', { value: new PendingOperations() });

    fs.setChmodCallback(function(id, args) {
        var cb = _chmodCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.chmod = function(path, mode, callback) {
        if (typeof callback === 'function') {
            var id = _chmodCallbacks.push(callback);
            return fs.chmod(path, mode, id);
        } else {
            try {
                return fs.chmod(path, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_utimeCallbacks', { value: new PendingOperations() });

    fs.setUtimeCallback(function(id, args) {
        var cb = _utimeCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, args[0]);
        }
    });

    exports.utimes = function(path, atime, mtime, callback) {
        if (typeof callback === 'function') {
            var id = _utimeCallbacks.push(callback);
            return fs.utime(path, atime, mtime, id);
        } else {
            try {
                return fs.utime(path, atime, mtime);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_futimeCallbacks', { value: new PendingOperations() });

    fs.setFUtimeCallback(function(id, args) {
        var cb = _futimeCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, args[0]);
        }
    });

    exports.futimes = function(fd, atime, mtime, callback) {
        if (typeof callback === 'function') {
            var id = _futimeCallbacks.push(callback);
            return fs.futime(fd, atime, mtime, id);
        } else {
            try {
                return fs.futime(fd, atime, mtime);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_lstatCallbacks', { value: new PendingOperations() });

    fs.setLStatCallback(function(id, args) {
        var cb = _lstatCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, new exports.Stats(args[1]));
        }
    });

    exports.lstat = function(path, callback) {
        if (typeof callback === 'function') {
            var id = _lstatCallbacks.push(callback);
            return fs.lstat(path, id);
        } else {
            try {
                var stats = fs.lstat(path);
                return new exports.Stats(stats);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_linkCallbacks', { value: new PendingOperations() });

    fs.setLinkCallback(function(id, args) {
        var cb = _linkCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.link = function(srcpath, dstpath, callback) {
        if (typeof callback === 'function') {
            var id = _linkCallbacks.push(callback);
            return fs.link(srcpath, dstpath, id);
        } else {
            try {
                return fs.link(srcpath, dstpath);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_symlinkCallbacks', { value: new PendingOperations() });

    fs.setSymlinkCallback(function(id, args) {
        var cb = _symlinkCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, args[0]);
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
            var id = _symlinkCallbacks.push(callback);
            return fs.symlink(destination, path, flags, id);
        } else {
            try {
                return fs.symlink(destination, path, flags);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_readlinkCallbacks', { value: new PendingOperations() });

    fs.setReadlinkCallback(function(id, args) {
        var cb = _readlinkCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb(undefined, args[1]);
        }
    });

    exports.readlink = function(path, callback) {
        if (typeof callback === 'function') {
            var id = _readlinkCallbacks.push(callback);
            return fs.readlink(path, id);
        } else {
            try {
                return fs.readlink(path);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_fchmodCallbacks', { value: new PendingOperations() });

    fs.setFChmodCallback(function(id, args) {
        var cb = _fchmodCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fchmod = function(fd, mode, callback) {
        if (typeof callback === 'function') {
            var id = _fchmodCallbacks.push(callback);
            return fs.fchmod(fd, mode, id);
        } else {
            try {
                return fs.fchmod(fd, mode);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_chownCallbacks', { value: new PendingOperations() });

    fs.setChownCallback(function(id, args) {
        var cb = _chownCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.chown = function(path, uid, gid, callback) {
        if (typeof callback === 'function') {
            var id = _chownCallbacks.push(callback);
            return fs.chown(path, uid, gid, id);
        } else {
            try {
                return fs.chown(path, uid, gid);
            } catch(e) {
                throw newError(e);
            }
        }
    }

    Object.defineProperty(this, '_fchownCallbacks', { value: new PendingOperations() });

    fs.setFChownCallback(function(id, args) {
        var cb = _fchownCallbacks.shift(id);
        var status = args[0];
        if (status == -1) {
            var nativeException = args[1];
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.fchown = function(fd, uid, gid, callback) {
        if (typeof callback === 'function') {
            var id = _fchownCallbacks.push(callback);
            return fs.fchown(path, uid, gid, id);
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
                that.onchange(current, previous, status);
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
