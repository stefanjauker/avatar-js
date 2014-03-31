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

(function(exports) {

var eventloop = __avatar.eventloop;
var System = java.lang.System;
var Runtime = java.lang.Runtime.getRuntime();

var LibUV = Packages.com.oracle.libuv.LibUV;
var CheckHandle = Packages.com.oracle.libuv.handles.CheckHandle;
var IdleHandle = Packages.com.oracle.libuv.handles.IdleHandle;
var SignalHandle = Packages.com.oracle.libuv.handles.SignalHandle;
var Map = java.util.HashMap;
var Process = Packages.com.oracle.avatar.js.os.Process;
var Server = Packages.com.oracle.avatar.js.Server;
var Constants = Packages.com.oracle.libuv.Constants;
var ScriptUtils = Packages.jdk.nashorn.api.scripting.ScriptUtils;

var separator = exports.platform === 'win32' ? ';' : ':';
var pathSeparator = exports.platform === 'win32' ? '\\' : '/';

'use strict';

// load definitions of __defineGetter__, __defineSetter__, __proto__, etc
load("nashorn:mozilla_compat.js");

// injected reference becomes readonly.
Object.defineProperty(this, '__avatar',  { writable: false,  configurable: false, enumerable: false});

// global is a reference to the current global scope.
var global = this;

global.gc = function() {
    java.lang.System.gc();
};

// utility method to print stack dumps
Error.prototype.dumpStack = function() {
    if (!this.stack) {
        Error.captureStackTrace(this);
    }
    print(this.stack);
};

__avatar.eventloop.setUncaughtExceptionHandler(
    function(name, args) {
        var listeners = process.listeners('uncaughtException');
        if (listeners.length > 0) {
            args[0] = true; // return non-null value to indicate user handler present
        }
    },
    function(name, args) {
        var e = new Error();
        var ex = args[0];
        for (var k in ex) {
            e[k] = ex[k];
        }
        process._fatalException(e);
    }
);

// with v8, splice called with a single argument means remove from index to length
// with nashorn, splice called with a single argument means remove nothing
// this is a redefinition of splice to emulate v8 behavior
var current = Array.prototype.splice;
Array.prototype.splice = function() {
    var args = arguments.length === 1 ? [arguments[0], this.length] : arguments;
    return current.apply(this, args);
};

exports._setupAsyncListener = function(asyncFlags, runAsyncQueue, loadAsyncQueue, unloadAsyncQueue) {
    // TODO
};

exports._setupNextTick = function(tickInfo, _tickCallback) {
    // TODO
};

exports._setupDomainUse = function(domain, domain_flag) {
    // TODO
};

exports.moduleLoadList = [];

exports._rawDebug = function(s) {
    print(s);
};

Object.defineProperty(exports, 'throwDeprecation', {
    enumerable: true,
    value: __avatar.throwDeprecation
});

Object.defineProperty(exports, 'traceDeprecation', {
    enumerable: true,
    value: __avatar.traceDeprecation
});

Object.defineProperty(exports, 'noDeprecation', {
    enumerable: true,
    value: __avatar.noDeprecation
});

Object.defineProperty(exports, '_forceRepl', {
    enumerable: true,
    value: __avatar.forceRepl
});

Object.defineProperty(exports, '_print_eval', {
    enumerable: true,
    value: __avatar.printEval
});

Object.defineProperty(exports, '_eval', {
    enumerable: true,
    value: __avatar.evalString
});

var _execPath;
Object.defineProperty(exports, 'execPath', {
    enumerable: true,
    get: function() {
        if (!_execPath) {
            var libPath = System.getProperty('java.library.path').split(separator);
            var libs = "";

            for (var i = 0; i < libPath.length; i++) {
                if (libPath[i].endsWith(pathSeparator)) {
                    libPath[i] = libPath[i].substr(0, libPath[i].length - 1);
                }
                if (libPath[i].indexOf(' ') >= 0 && libPath[i].indexOf('\"') === -1) {
                    libs += '\"' + libPath[i] + '\"' ;
                } else {
                    libs += libPath[i];
                }
                libs += (i !== libPath.length - 1) ? separator : '';
            }

            _execPath = 'java ' +
               (libs ? '-Djava.library.path=' + libs : '') + ' ' +
               '-cp ' +
               System.getProperty('java.class.path') + ' ' +
               Server.class.getName();
        } else {
            // Not Leaking library.path and class.path
            var sm = System.getSecurityManager();
            if (sm) {
                sm.checkPropertyAccess('java.library.path');
                sm.checkPropertyAccess('java.class.path');
            }
        }
        return _execPath;
    }
});

var _argv;
Object.defineProperty(exports, 'argv', {
    enumerable: true,
    get: function() {
        if (!_argv) {
            _argv = [];
            _argv.push(exports.execPath);
            _argv.push(__avatar.userFile);

            var ua = __avatar.userArgs;
            var alen = ua ? ua.length : 0;
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
            var alen = aa ? aa.length : 0;
            for (var j=0; j < alen; j++) {
                _execArgv.push(aa[j]);
            }
        }
        return _execArgv;
    }
});

var _env = {};
(function() {
    var pi = System.getenv().entrySet().iterator();
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
        var arch =  System.getProperty('os.arch').toLowerCase();
        if (arch === 'x86_64') {
            return 'x64';
        }
        return arch;
    }
});

Object.defineProperty(exports, 'platform', {
    enumerable: true,
    get: function() {
        var osname = System.getProperty('os.name').toLowerCase();
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
        eventloop.setWorkDir(dir);
    }
});

Object.defineProperty(exports, '_exiting', { writable: true, value: false });
exports.exit = function(status) {
    var code = status ? status : 0;
    if (!exports._exiting) {
        exports._exiting = true;
        exports.emit('exit', code);
    }
    __avatar.eventloop.stop();
    exports.reallyExit(code);
};

exports.reallyExit = function(code) {
    System.exit(code);
};

Object.defineProperty(exports, 'memoryUsage', {
    enumerable: true,
    value: function(native) {
        var total = native ? LibUV.getTotalMem() : Runtime.maxMemory();
        var free = native ? LibUV.getFreeMem() : Runtime.freeMemory();
        return {
            heapTotal: total,
            heapUsed: total - free,
            rss: LibUV.rss()
        };
    }
});

var startTime = LibUV.getUptime();
Object.defineProperty(exports, 'uptime', {
    enumerable: true,
    value: function() {
        return LibUV.getUptime() - startTime;
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
    configurable: true,
    value: function(callback) {
        if (this._exiting) {
            return;
        }
        eventloop.nextTick(function(name, args) {
            callback();
        });
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
        var file = '/com/oracle/avatar/js/' + module + '.js';
        var exports = {};
        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                var url = Server.getResource(file);
                if (url === null) {
                    throw new Error('binding not found for module ' + module);
                }
                var f = load(url);
                var NativeModule = __avatar.require('native_module');
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
});

exports.getuid = function() {
    return Process.getUid();
};

exports.setuid = function(value) {
    Process.setUid(value);
};

exports.getgid = function() {
    return Process.getGid();
};

exports.setgid = function(value) {
    Process.setGid(value);
};

exports.dlopen = function(module) {
    throw new Error('dlopen is not supported, cannot load ' + module);
};

exports.versions = {openssl: true, node: eventloop.version()};

// tls_sni will be true, tls_npn is not supported by Java binding
exports.features = {tls_sni: true};

exports.umask  = function(mask) {
    var oct = 0;
    if (mask !== undefined) {
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
};

exports._usingDomains = function() {
    // redefine nextTick at this time to speedup
    // domain event posting
    Object.defineProperty(exports, 'nextTick', {
        enumerable: true,
        value: function(callback) {
            if (this._exiting) {
                return;
            }
            eventloop.nextTickWithDomain(function(name, args) {
                callback();
            }, process.domain);
        }
    });
};

Object.defineProperty(exports, 'domain', {
    enumerable : true,
    configurable: true,
    set : function(domain) {
        if (domain) {
            eventloop.domain = domain;
        } else {
            eventloop.domain = null;
        }
    },
    get : function() {
        return ScriptUtils.unwrap(eventloop.domain);
    }
});

var checkHandle = new CheckHandle(eventloop.loop());
checkHandle.setCheckCallback(checkImmediate);
// this handle should not keep the event loop from terminating
checkHandle.unref();

// During the lifetime of an immediate callback, we want to keep the loop running.
// This is why an IdleHandle is started/stopped when the unrefed CheckHandle is started/stopped
// Nodejs core.cc
var idleHandle = new IdleHandle(eventloop.loop());
idleHandle.setIdleCallback(IdleCallbackHandler);

// Handle the nextTick in a UV callback
var spinnerHandle = new IdleHandle(eventloop.loop());
spinnerHandle.setIdleCallback(spin);
var need_tick_cb = false;
function spin() {
    if (!need_tick_cb) {
        return;
    }
    need_tick_cb = false;
    eventloop.enableSyncEventsProcessing(true);
    spinnerHandle.stop();
    exports._tickCallback();
}

exports._needTickCallback = function() {
    need_tick_cb = true;
    eventloop.enableSyncEventsProcessing(false);
    spinnerHandle.start();
};

function checkImmediate() {
    // This needs to be treated as a callback, all ticked events need to be handled.
    if (exports._immediateCallback) {
        exports._immediateCallback();
    }
}

function IdleCallbackHandler() {
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

// do not install any signal handlers by default
// some generate EINVAL (invalid argument)
// and the JVM installs some of its own and we do not want to cause conflicts
// an app can install signal handlers as needed using
//   process.signals.start('SIGUSR1');
// or
//   process.signals.start(43);
exports.signals = { cache: new Map() };
Object.defineProperty(exports.signals, 'start', {
    enumerable: true,
    value: function(signal) {
        var signalHandle = exports.signals.cache.get(signal);
        if (!signalHandle) {

            signalHandle = new SignalHandle(eventloop.loop());
            // this handle should not keep the event loop from terminating
            signalHandle.unref();
            signalHandle.signalCallback = function(signum) {
                exports.emit(Constants.getConstantsString().get(signum));
            };
            signalHandle.start(signal);
            exports.signals.cache.put(signal, signalHandle);
        }
    }
});

if (exports.platform !== 'win32') {
    exports.getgroups = function() {
        var groupArray = Process.getGroups();
        var ret = [];
        for(var group in groupArray) {
            ret.push(groupArray[group]);
        }
        return ret;
    };

    exports.setgroups = function(groups) {
        var groupArray = Java.to(groups, "java.lang.String[]");
        Process.setGroups(groupArray);
    };

    exports.initgroups = function(user, group) {
        if (!user || !group) {
            throw new Error("Invalid parameters");
        }
        Process.initGroups(user, group);
    };
}

});
