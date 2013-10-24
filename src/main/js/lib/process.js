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

// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

var events = require("events");
var NativeModule = require('native_module');

// process is an EventEmitter - copy EventEmitter methods to process
for (var f in events.EventEmitter.prototype) {
    exports[f] = events.EventEmitter.prototype[f];
}

// events export are re-exported by process
for (var f in events) {
    exports[f] = events[f];
}

var eventloop = __avatar.eventloop;
var LibUV = Packages.net.java.libuv.LibUV;
var Process = Packages.net.java.avatar.js.os.Process;
var Server = Packages.net.java.avatar.js.Server;
var Event = Packages.net.java.avatar.js.eventloop.Event
var Constants = Packages.net.java.avatar.js.constants.Constants;
var Signals = Packages.net.java.libuv.handles.SignalHandle;

Object.defineProperty(exports, 'throwDeprecation', {
    enumerable: true,
    value: __avatar.throwDeprecation
});

Object.defineProperty(exports, 'traceDeprecation', {
    enumerable: true,
    value: __avatar.traceDeprecation
});

var stdin;
Object.defineProperty(exports, 'stdin', {
    enumerable: true,
    get: function() {
        if (!stdin) {
            // From Node.js
            var tty_wrap = process.binding('tty_wrap');
            var fd = 0;

            switch (tty_wrap.guessHandleType(fd)) {
                case 'TTY':
                  var tty = NativeModule.require('tty');
                  stdin = new tty.ReadStream(fd, {
                      highWaterMark: 0,
                      readable: true,
                      writable: false
                  });
                  break;
                case 'FILE':
                  var fs = NativeModule.require('fs');
                  stdin = new fs.ReadStream(null, { fd: fd });
                  break;
                case 'PIPE':
                case 'TCP':
                  var net = NativeModule.require('net');
                  stdin = new net.Socket({
                      fd: fd,
                      readable: true,
                      writable: false
                  });
                  break;

                default:
                    // Probably an error on in uv_guess_handle()
                    throw new Error('Unknown stdin file type!');
            }

            // For supporting legacy API we put the FD here.
            stdin.fd = fd;

            // stdin starts out life in a paused state, but node doesn't
            // know yet.  Explicitly to readStop() it to put it in the
            // not-reading state.
            if (stdin._handle && stdin._handle.readStop) {
                stdin._handle.reading = false;
                stdin._readableState.reading = false;
                stdin._handle.readStop();
            }

            // if the user calls stdin.pause(), then we need to stop reading
            // immediately, so that the process can close down.
            stdin.on('pause', function() {
                if (!stdin._handle) {
                    return;
                }
                stdin._readableState.reading = false;
                stdin._handle.reading = false;
                stdin._handle.readStop();
            });
            return stdin;
        }
        return stdin;
    },
    set: function(value) {
        stdin = value;
    }
});

var wrapStdOutputStream = function(fd) {
    // From Node.js
    var stream;
    var tty_wrap = process.binding('tty_wrap');
    switch(tty_wrap.guessHandleType(fd)) {
        case 'TTY':
            var tty = NativeModule.require('tty');
            stream = new tty.WriteStream(fd);
            stream._type = 'tty';

            // Hack to have stream not keep the event loop alive.
            // See https://github.com/joyent/node/issues/1726
            if (stream._handle && stream._handle.unref) {
              stream._handle.unref();
            }
            break;
        case 'FILE':
            var fs = NativeModule.require('fs');
            stream = new fs.SyncWriteStream(fd);
            stream._type = 'fs';
            break;
        case 'TCP':
        case 'PIPE':
            var net = NativeModule.require('net');
            stream = new net.Socket({
              fd: fd,
              readable: false,
              writable: true
            });

            // FIXME Should probably have an option in net.Socket to create a
            // stream from an existing fd which is writable only. But for now
            // we'll just add this hack and set the `readable` member to false.
            // Test: ./node test/fixtures/echo.js < /etc/passwd
            stream.readable = false;
            stream.read = null;
            stream._type = 'pipe';

            // FIXME Hack to have stream not keep the event loop alive.
            // See https://github.com/joyent/node/issues/1726
            if (stream._handle && stream._handle.unref) {
              stream._handle.unref();
            }
            stream.fd = fd;
            stream._isStdio = true;
            break;
        default:
            throw new Error('Unknow stream type');
    }
    stream.fd = fd;
    stream._isStdio = true;
    return stream;
}

var stdout;
Object.defineProperty(exports, 'stdout', {
    enumerable: true,
    get: function() {
        if (stdout) return stdout;
        stdout = wrapStdOutputStream(1);
        stdout.destroy = stdout.destroySoon = function(er) {
            er = er || new Error('process.stdout cannot be closed.');
            stdout.emit('error', er);
        };
        if (stdout.isTTY) {
            process.on('SIGWINCH', function() {
                stdout._refreshSize();
            });
        }
        return stdout;
    },
    set: function(value) {
        stdout = value;
    }
});

var stderr;
Object.defineProperty(exports, 'stderr', {
    enumerable: true,
    get: function() {
        if (stderr) return stderr;
        stderr = wrapStdOutputStream(2);
        stderr.destroy = stderr.destroySoon = function(er) {
            er = er || new Error('process.stderr cannot be closed.');
            stderr.emit('error', er);
        };
        return stderr;
    },
    set: function(value) {
        stderr = value;
    }
});

Object.defineProperty(exports, 'execPath', {
    enumerable: true,
    get: function() {
        var libPath = java.lang.System.getProperty('java.library.path');
        return 'java ' +
               (libPath ? '-Djava.library.path=' + libPath : '') + ' ' +
               '-cp ' +
               java.lang.System.getProperty('java.class.path') + ' ' +
               Server.class.getName();
    }
});

var _argv;
Object.defineProperty(exports, 'argv', {
    enumerable: true,
    get: function() {
        if (!_argv) {
            _argv = [];
            _argv.push(exports.execPath);
            var uf = __avatar.userFiles;
            var flen = uf.length;
            for (var i=0; i < flen; i++) {
                _argv.push(uf[i]);
            }
            var ua = __avatar.userArgs;
            var alen = ua.length;
            for (var j=0; j < alen; j++) {
                _argv.push(ua[j]);
            }
        }
        return _argv;
    },
    set: function(value) {
        _argv = value;
    }
});

var _execArgv;
Object.defineProperty(exports, 'execArgv', {
    enumerable: true,
    get: function() {
        if (!_execArgv) {
            _execArgv = [];
            var aa = __avatar.avatarArgs;
            var alen = aa.length;
            for (var j=0; j < alen; j++) {
                _execArgv.push(aa[j]);
            }
        }
        return _execArgv;
    }
});

var _env = {};
(function() {
    var pi = java.lang.System.getenv().entrySet().iterator();
    while (pi.hasNext()) {
        var p = pi.next();
        _env[p.key] = p.value;
    }
    // override to load modules into the current context (see module.js)
    _env['NODE_MODULE_CONTEXTS'] = 0;
})();
Object.defineProperty(exports, 'env', {
    enumerable: true,
    get: function() {
        Server.checkGetEnv();
        return _env;
    },
    set: function(value) {
        _env = value;
    }
});

Object.defineProperty(exports, 'arch', {
    enumerable: true,
    get: function() {
        var arch =  java.lang.System.getProperty('os.arch').toLowerCase();
        if (arch === 'x86_64') {
            return 'x64';
        }
        return arch;
    }
});

Object.defineProperty(exports, 'platform', {
    enumerable: true,
    get: function() {
        var osname = java.lang.System.getProperty('os.name').toLowerCase();
        if (osname.startsWith('windows')) {
            return 'win32';
        } else {
            if (osname.startsWith('mac os x')) {
                return 'darwin';
            }
        }
        return osname;
    }
});

Object.defineProperty(exports, 'cwd', {
    enumerable: true,
    value: function() {
        return eventloop.getWorkDir();
    }
});

Object.defineProperty(exports, '_tickCallback', {
    value: function() {
        // Permission is required to tick callbacks,
        // user code is not expected to call this method.
        __avatar.eventloop.processQueuedEvents();
    }
});

Object.defineProperty(exports, 'chdir', {
    enumerable: true,
    value: function(dir) {
        var fs = java.nio.file.FileSystems.getDefault();
        var path = fs.getPath(dir);
        var newpath = path.isAbsolute() ? path : fs.getPath(process.cwd(), path.toString()).normalize();
        eventloop.setWorkDir(newpath.toString());
    }
});

Object.defineProperty(exports, '_exiting', { writable: true, value: false });
exports.exit = function(status) {
    var code = status ? status : 0;
    // permission is required to stop the eventloop.
    __avatar.eventloop.drain();
    if (!exports._exiting) {
        exports._exiting = true;
        exports.emit('exit', code);
    }
    __avatar.eventloop.stop();
    java.lang.System.exit(code);
}

Object.defineProperty(exports, 'memoryUsage', {
    enumerable: true,
    value: function() {
        var total = LibUV.getTotalMem();
        return {
            heapTotal: total,
            heapUsed: total - LibUV.getFreeMem(),
            rss: LibUV.rss(),
        };
    }
});

var startTime = java.lang.System.nanoTime();
Object.defineProperty(exports, 'uptime', {
    enumerable: true,
    value: function() {
        return LibUV.getUptime();
    }
});

Object.defineProperty(exports, 'hrtime', {
    enumerable: true,
    value: function(start) {
        if (start && !Array.isArray(start)) {
            throw new Error("Only Array may be passed to process.hrtime()");
        }
        var now = Process.hrTime();
        return start ?
            [now.seconds - start[0], now.nanoseconds - start[1]] :
            [now.seconds, now.nanoseconds];
    }
});

Object.defineProperty(exports, 'nextTick', {
    enumerable: true,
    value: function(callback) {
        eventloop.nextTick(
            new Event('nextTick', function(name, args) {
                    callback();
                }
            )
        );
    }
});

Object.defineProperty(exports, 'version', {
    enumerable: true,
    get: function() {
        return eventloop.version();
    }
});

var _bindings_cache = {};
var AccessController = java.security.AccessController;
var PrivilegedAction = java.security.PrivilegedAction;

Object.defineProperty(exports, 'binding', {
    enumerable: true,
    value: function(module) {
        if (module === 'buffer') {
        // loaded on demand to avoid dependency cycles
            return {SlowBuffer: require('buffer').SlowBuffer};
        }
        var cached = _bindings_cache[module];
        if (cached) {
            return cached;
        }

        // append '_wrap' unless is is already present
        if (!module.match(/._wrap$/)) {
            module += '_wrap';
        }
        var file = '/net/java/avatar/js/' + module + '.js';
        var exports = {};
        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                var url = Server.getResource(file);
                if (url === null) {
                    throw new Error('binding not found for module ' + module);
                }
                var f = load(url);
                f(exports, NativeModule.require);
            }
        });
        _bindings_cache[module] = exports;
        return exports;
    }
});

Object.defineProperty(exports, 'abort', {
    enumerable: true,
    value: function() {
        Process.abort();
    }
});

Object.defineProperty(exports, 'title', {
    enumerable: true,
    get: function() {
        return LibUV.getTitle();
    },
    set: function(value) {
        LibUV.setTitle(value);
    }
});

Object.defineProperty(exports, 'pid', {
    enumerable: true,
    get: function() {
        return Process.getPid();
    }
});

Object.defineProperty(exports, 'kill', {
    enumerable: true,
    value: function(pid, sig) {
      var r;

      // preserve null signal
      if (0 === sig) {
        r = LibUV.kill(pid, 0);
      } else {
        sig = sig || 'SIGTERM';
        var constants = process.binding('constants');
        if (constants[sig]) {
          r = LibUV.kill(pid, constants[sig]);
        } else {
          throw new Error('Unknown signal: ' + sig);
        }
      }
      if (r) {
        throw errnoException(errno, 'kill');
      }
      return true;
    }
})

exports.getuid = function() {
    return Process.getUid();
}

exports.setuid = function(value) {
    Process.setUid(value);
}

exports.getgid = function() {
    return Process.getGid();
}

exports.setgid = function(value) {
    Process.setGid(value);
}

exports.dlopen = function(module) {
    throw new Error('dlopen is not supported, cannot load ' + module);
}

exports.versions = {openssl: true, node: eventloop.version()};

// tls_sni will be true, tls_npn is not supported by Java binding
exports.features = {tls_sni: true};

exports.umask  = function(mask) {
    var oct = 0
    if (mask != undefined) {
        if (typeof mask === 'number') {
            oct = mask;
        } else if (typeof mask === 'string') {
            // Parse the octal string.
            for (var i = 0; i < mask.length; i++) {
              var c = mask[i];
              if (c > '7' || c < '0') {
                throw new Error("invalid octal string");
              }
              oct *= 8;
              oct += c - '0';
            }
        }
        return Process.umask(oct);
    } else {
        return Process.getUmask();
    }
};

exports.openStdin = function() {
    process.stdin.resume();
    return process.stdin;
}

var Check = Packages.net.java.libuv.handles.CheckHandle;
var Idle = Packages.net.java.libuv.handles.IdleHandle;
var checkHandle = new Check(eventloop.loop());
checkHandle.setCheckCallback(checkImmediate);
// this handle should not keep the event loop from terminating
checkHandle.unref();

// During the lifetime of an immediate callback, we want to keep the loop running.
// This is why an Idle is started/stopped when the unrefed Check is started/stopped
// Nodejs core.cc 
var idleHandle = new Idle(eventloop.loop());
idleHandle.setIdleCallback(IdleImmediateDummy);

function checkImmediate() {
    // This needs to be treated as a callback, all ticked events need to be handled.
    if (exports._immediateCallback) {
        exports._immediateCallback();
    }
}

function IdleImmediateDummy() {
    // Just to keep the loop running.
}

Object.defineProperty(exports, '_needImmediateCallback', {
    enumerable : true,
    set : function(need) {
        if (checkHandle) {
            if (need) {
                checkHandle.start();
                idleHandle.start();
            } else {
                checkHandle.stop();
                idleHandle.stop();
            }
        }
    }
});
    
var signalHandle = new Signals(eventloop.loop());
// this handle should not keep the event loop from terminating
signalHandle.unref();
signalHandle.signalCallback = function(signum) {
    exports.emit(Constants.getConstantsString().get(signum));
}
// do not install any signal handlers by default
// some generate EINVAL (invalid argument)
// and the JVM installs some of its own and we do not want to cause conflicts
// an app can install signal handlers as needed using
//   process.signals.start('SIGUSR1');
// or
//   process.signals.start(43);
Object.defineProperty(exports, 'signals', {
    enumerable: true,
    value: signalHandle
});

if (exports.platform !== 'win32') {
    exports.getgroups = function() {
        var jarr = Process.getGroups();
        var ret = [];
        for(var a in jarr) {
            ret.push(jarr[a]);
        }
        return ret;
    }

    exports.setgroups = function(groups) {
        var jarr = Java.to(groups, "java.lang.String[]");
        Process.setGroups(jarr);
    }

    exports.initgroups = function(user, group) {
        if (!user || !group) {
            throw new Error("Invalid parameters");
        }
        Process.initGroups(user, group);
    }
}
