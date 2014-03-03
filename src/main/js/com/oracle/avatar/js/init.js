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

var gc = global.gc;

// isolate NativeModule
(function() {
    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var avatarPermission = new java.lang.RuntimePermission("avatar-js");
    var avatarContext = __avatar.controlContext;

    function NativeModule(id) {
        this.filename = id + '.js';
        this.id = id;
        this.loaded = false;
        this.exports = {};
    }

    NativeModule._cache = {};

    NativeModule.require = function(id) {
        if (id === 'native_module') {
            return NativeModule;
        }

        var cached = NativeModule.getCached(id);
        if (cached) {
            return cached.exports;
        }

        if (!NativeModule.exists(id)) {
            throw new Error('No such native module ' + id);
        }

        var nativeModule = new NativeModule(id);

        nativeModule.cache();
        nativeModule.compile();

        return nativeModule.exports;
    };

    NativeModule.getCached = function(id) {
        return NativeModule._cache[id];
    }

    NativeModule.exists = function(id) {
        var exists = AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                return __avatar.loader.exists(id);
            }
        }, avatarContext, avatarPermission);
        return exists;
    }

    NativeModule.wrap = function(script) {
        return Packages.com.oracle.avatar.js.Loader.wrap(script);
    };

    NativeModule.getURL = function(id) {
        var url = AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                return __avatar.loader.getURL(id);
            }
        });
        return url;
    }

    NativeModule.prototype.compile = function() {
        var url = NativeModule.getURL(this.id);
        var that = this;
        AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                var fn = load(url);
                fn(that.exports, NativeModule.require, that, that.filename);
            }
        });
        __avatar.loader.loadSucceeded(this.id);
        this.loaded = true;
    };

    NativeModule.prototype.cache = function() {
        NativeModule._cache[this.id] = this;
    };

    __avatar.installNativeModule(NativeModule);

    var process = NativeModule.require('process');
    global.process = process;

    var Buffer = NativeModule.require('buffer').Buffer;

    global.Buffer = Buffer;

    global.setTimeout = function() {
        var t = NativeModule.require('timers');
        return t.setTimeout.apply(this, arguments);
    };

    global.setInterval = function() {
        var t = NativeModule.require('timers');
        return t.setInterval.apply(this, arguments);
    };

    global.clearTimeout = function() {
        var t = NativeModule.require('timers');
        return t.clearTimeout.apply(this, arguments);
    };

    global.clearInterval = function() {
        var t = NativeModule.require('timers');
        return t.clearInterval.apply(this, arguments);
    };

    global.setImmediate = function() {
        var t = NativeModule.require('timers');
        return t.setImmediate.apply(this, arguments);
    };

    global.clearImmediate = function() {
        var t = NativeModule.require('timers');
        return t.clearImmediate.apply(this, arguments);
    };

    // console require timers that require setTimeout.
    var console = NativeModule.require('console');
    global.console = console;

    // utility method to print stack dumps
    Error.prototype.dumpStack = function() {
        if (!this.stack) {
            Error.captureStackTrace(this);
        }
        console.log(this.stack);
    };

    var fatalProcessing = function(er) {
        var caught = false;
        if (process.domain) { // From nodejs
            var domain = process.domain;
            var domainModule = NativeModule.require('domain');
            var domainStack = domainModule._stack;

            // ignore errors on disposed domains.
            //
            // XXX This is a bit stupid.  We should probably get rid of
            // domain.dispose() altogether.  It's almost always a terrible
            // idea.  --isaacs
            if (domain._disposed)
                return true;

            er.domain = domain;
            er.domainThrown = true;
            // wrap this in a try/catch so we don't get infinite throwing
            try {
                // One of three things will happen here.
                //
                // 1. There is a handler, caught = true
                // 2. There is no handler, caught = false
                // 3. It throws, caught = false
                //
                // If caught is false after this, then there's no need to exit()
                // the domain, because we're going to crash the process anyway.
                caught = domain.emit('error', er);
                // Exit all domains on the stack.  Uncaught exceptions end the
                // current tick and no domains should be left on the stack
                // between ticks.
                var domainModule = NativeModule.require('domain');
                domainStack.length = 0;
                domainModule.active = process.domain = null;
            } catch (er2) {
                // The domain error handler threw!  oh no!
                // See if another domain can catch THIS error,
                // or else crash on the original one.
                // If the user already exited it, then don't double-exit.
                if (domain === domainModule.active)
                    domainStack.pop();
                if (domainStack.length) {
                    var parentDomain = domainStack[domainStack.length - 1];
                    process.domain = domainModule.active = parentDomain;
                    caught = fatalProcessing(er2);
                } else
                    caught = false;
            }
        } else {
            caught = process.emit('uncaughtException', er);
        }

        // Avatar-js specific handling,
        // exit is handled in the class that is catching the rethrown er
        if (!caught) {
            throw er;
        } else {
            // if we handled an error, then make sure any ticks get processed
            process._needTickCallback();
        }

        return caught;
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
            fatalProcessing(e);
        }
    );

    // If we were spawned with env NODE_CHANNEL_FD then load that up and
    // start parsing data from that stream.
    if (process.env.NODE_CHANNEL_FD) {
        var fd = parseInt(process.env.NODE_CHANNEL_FD, 10);

        // Make sure it's not accidentally inherited by child processes.
        delete process.env.NODE_CHANNEL_FD;

        var cp = NativeModule.require('child_process');

        cp._forkChild(fd);
    }

    // with v8, splice called with a single argument means remove from index to length
    // with nashorn, splice called with a single argument means remove nothing
    // this is a redefinition of splice to emulate v8 behavior
    var current = Array.prototype.splice;
    Array.prototype.splice = function() {
        var args = arguments.length === 1 ? [arguments[0], this.length] : arguments;
        return current.apply(this, args);
    }

    if (process._eval !== null) { // to handle empty string
        // User passed '-e' or '--eval' arguments to Node.
        evalScript('[eval]');
    } else if (process.argv[1]) {
        // make process.argv[1] into a full path
        var path = NativeModule.require('path');
        process.argv[1] = path.resolve(process.argv[1]);

        // If this is a worker in cluster mode, start up the communiction
        // channel.
        if (process.env.NODE_UNIQUE_ID) {
            var cluster = NativeModule.require('cluster');
            cluster._setupWorker();

            // Make sure it's not accidentally inherited by child processes.
            delete process.env.NODE_UNIQUE_ID;
        }

        NativeModule.require('module').runMain();
    } else {
        var Module = NativeModule.require('module');

        // If -i or --interactive were passed, or stdin is a TTY.
        if (process._forceRepl || NativeModule.require('tty').isatty(0)) {
            // REPL
            var opts = {
                useGlobal: true,
                ignoreUndefined: false
            };
            if (parseInt(process.env['NODE_NO_READLINE'], 10)) {
                opts.terminal = false;
            }
            if (parseInt(process.env['NODE_DISABLE_COLORS'], 10)) {
                opts.useColors = false;
            }
            var repl = Module.requireRepl().start(opts);
            repl.on('exit', function() {
                process.exit();
            });
         } else {
            // Read all of stdin - execute it.
            process.stdin.setEncoding('utf8');

            var code = '';
            process.stdin.on('data', function(d) {
                code += d;
            });

            process.stdin.on('end', function() {
                process._eval = code;
                evalScript('[stdin]');
            });
        }
    }

    function evalScript(name) {
        var Module = NativeModule.require('module');
        var path = NativeModule.require('path');
        var cwd = process.cwd();

        var module = new Module(name);
        module.filename = path.join(cwd, name);
        module.paths = Module._nodeModulePaths(cwd);
        var script = process._eval;
        if (!Module._contextLoad) {
            var body = script;
            script = 'global.__filename = ' + JSON.stringify(name) + ';\n' +
                     'global.exports = exports;\n' +
                     'global.module = module;\n' +
                     'global.__dirname = __dirname;\n' +
                     'global.require = require;\n' +
                     'return require("vm").runInThisContext(' +
                     JSON.stringify(body) + ', ' +
                     JSON.stringify(name) + ', true);\n';
        }
        var result = module._compile(script, name + '-wrapper');
        if (process._print_eval) console.log(result);
    }
} )();
