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

    var Files = Packages.com.oracle.libuv.Files;
    var FilePollHandle = Packages.com.oracle.libuv.handles.FilePollHandle;
    var JavaBuffer = Packages.com.oracle.avatar.js.buffer.Buffer;
    var JavaStats = Packages.com.oracle.libuv.Stats;
    var ByteBuffer = java.nio.ByteBuffer;
    var loop = __avatar.eventloop.loop();
    var fs = new Files(loop);
    var uv = process.binding('uv_wrap');

    function Stats() {
    }

    // update self from instance of java stats
    Stats.prototype.update = function(stats) {
        this.dev = stats ? stats.getDev() : undefined;
        this.ino = stats ? stats.getIno() : undefined;
        this.mode = stats ? stats.getMode() : undefined;
        this.nlink = stats ? stats.getNlink() : undefined;
        this.uid = stats ? stats.getUid() : undefined;
        this.gid = stats ? stats.getGid() : undefined;
        this.rdev = stats ? stats.getRdev() : undefined;
        this.size = stats ? stats.getSize() : undefined;
        this.blksize = stats ? stats.getBlksize() : undefined;
        this.blocks = stats ? stats.getBlocks() : undefined;
        this.atime = stats ? new Date(stats.getAtime()) : undefined;
        this.mtime = stats ? new Date(stats.getMtime()) : undefined;
        this.ctime = stats ? new Date(stats.getMtime()) : undefined;
        return this;
    }

    exports.Stats = Stats;

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
            var r = fs.close(fd);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
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
            var r = fs.open(path, flags, mode);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            return fs.read(fd, buffer._impl.underlying(), offset, length, position, callback);
        } else {
            var r = fs.read(fd, buffer._impl.underlying(), offset, length, position);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
        }
    }

    fs.setUnlinkCallback(function(cb, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb();
        }
    });

    exports.unlink = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.unlink(path, callback);
        } else {
            var r = fs.unlink(path);
            if (r < 0) throw newErrnoError(r, path);
            return r;
        }
    }

    fs.setWriteCallback(function(cb, bytesWritten, nativeException) {
        if (nativeException) {
            cb(newError(nativeException), bytesWritten);
        } else {
            cb(undefined, bytesWritten);
        }
    });

    exports.writeBuffer = function(fd, buffer, offset, length, position, callback) {
        if (position == null || position == undefined) {
            position = -1;
        } else if (position % 1 != 0) {
            throw new TypeError("Not an integer");
        }

        if (typeof callback === 'function') {
            return fs.write(fd, buffer._impl.underlying(), offset, length, position, callback);
        } else {
            var r = fs.write(fd, buffer._impl.underlying(), offset, length, position);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
        }
    }

    exports.writeString = function(fd, string, position, encoding, callback) {
        if (position == null || position == undefined) {
            position = -1;
        } else if (position % 1 != 0) {
            throw new TypeError("Not an integer");
        }

        encoding = encoding || 'utf-8';
        var buffer = ByteBuffer.wrap(String(string).getBytes(encoding));
        var length = buffer.capacity();

        if (typeof callback === 'function') {
            return fs.write(fd, buffer, 0, length, position, callback);
        } else {
            var r = fs.write(fd, buffer, 0, length, position);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
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
            var r = fs.mkdir(path, mode);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            var r = fs.rmdir(path);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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

    var _stats = new exports.Stats();
    fs.setStatCallback(function(cb, stats, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, _stats.update(stats)); // stats === _jstats
        }
    });

    var _jstats = new JavaStats();
    exports.stat = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.stat(path, _jstats, callback);
        } else {
            var r = fs.stat(path, _jstats);
            _stats.update(_jstats);
            if (r < 0) throw newErrnoError(r, path);
            return _stats;
        }
    }

    var _fstats = new exports.Stats();
    fs.setFStatCallback(function(cb, stats, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, _fstats.update(stats)); // stats === _fjstats
        }
    });

    var _fjstats = new JavaStats();
    exports.fstat = function(fd, callback) {
        if (typeof callback === 'function') {
            return fs.fstat(fd, _fjstats, callback);
        } else {
            var r = fs.fstat(fd, _fjstats);
            _fstats.update(_fjstats);
            if (r < 0) throw newErrnoError(r, fd);
            return _fstats;
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
            var r = fs.rename(oldPath, newPath);
            if (r < 0) throw newErrnoError(r, oldPath, newPath);
            return r;
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
            var r = fs.fsync(fd);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
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
            var r = fs.fdatasync(fd);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
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
            var r = fs.ftruncate(fd, length);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
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
            var r = fs.chmod(path, mode);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            print('utimeSync atime '+atime+' mtime '+mtime+' path '+path);
            var r = fs.utime(path, atime, mtime);
            print('utimeSync atime '+atime+' mtime '+mtime+' path '+path+' r '+r);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            var r = fs.futime(fd, atime, mtime);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
        }
    }

    var _lstats = new exports.Stats();
    fs.setLStatCallback(function(cb, stats, nativeException) {
        if (nativeException) {
            cb(newError(nativeException));
        } else {
            cb(undefined, _lstats.update(stats)); // stats === _ljstats
        }
    });

    var _ljstats = new JavaStats();
    exports.lstat = function(path, callback) {
        if (typeof callback === 'function') {
            return fs.lstat(path, _ljstats, callback);
        } else {
            var r = fs.lstat(path, _ljstats);
            _lstats.update(_ljstats);
            if (r < 0) throw newErrnoError(r, path);
            return _lstats;
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
            var r = fs.link(srcpath, dstpath);
            if (r < 0) throw newErrnoError(r, srcpath, dstpath);
            return r;
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
            var r =fs.symlink(destination, path, flags);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            var r = fs.readlink(path);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            var r = fs.fchmod(fd, mode);
            if (r < 0) throw newErrnoError(r, fd);
            return r;
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
            var r = fs.chown(path, uid, gid);
            if (r < 0) throw newErrnoError(r, path);
            return r;
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
            var r = fs.fchown(path, uid, gid);
            if (r < 0) throw newErrnoError(r, path);
            return r;
        }
    }

    exports.StatWatcher = StatWatcher;

    function StatWatcher() {
        var that = this;
        this._fsPoll = new FilePollHandle(loop);
        this._previous = new exports.Stats();
        this._current = new exports.Stats();

        this._fsPoll.setFilePollCallback(function(status, previous, current) {
            if (that.onchange) {
                that._current.update(current);
                that._previous.update(previous);
                that.onchange(that._current, that._previous, status);
            }
        });

        this._fsPoll.setStopCallback(function() {
            if (that.onstop) {
                that.onstop();
            }
        });
    }

    StatWatcher.prototype.start = function(filename, persistent, interval) {
        return this._fsPoll.start(filename, persistent, interval);
    }

    StatWatcher.prototype.stop = function() {
        this._fsPoll.stop();
    }

    var newError = function(exception) {
        var error = new Error();
        error.errno = exception.errno();
        error.code = exception.errnoString();
        error.message = exception.errnoString() + ', ' + exception.getErrnoMessage() + ' \'' + exception.path() +'\'';
        error.path = exception.path();
        return error;
    }

    var newErrnoError = function(errno, path, path2) {
        var error = new Error();
        var code = uv.errname(errno);
        var msg = uv.errmsg(errno);
        error.errno = errno;
        error.code = code;
        error.message = code + ', ' + msg + (path ? ' \'' + path +'\'' : '') + (path2 ? ' \'' + path2 +'\'' : '');
        error.path = path;
        return error;
    }

});
