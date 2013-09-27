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
        return __avatar.loader.exists(id);
    }

    NativeModule.getSource = function(id) {
        return __avatar.loader.load(id);
    }

    NativeModule.wrap = function(script) {
        return Packages.net.java.avatar.js.Loader.wrap(script);
    };

    NativeModule.getURL = function(id) {
        return __avatar.loader.getURL(id);
    }

    NativeModule.prototype.compile = function() {
        var url = NativeModule.getURL(this.id);
        var fn = load(url);
        fn(this.exports, NativeModule.require, this, this.filename);

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

    // timer functions
    var timer = __avatar.eventloop.timer();

    var _setTimeout = function() {
        var repeated = arguments[0];
        var callargs = arguments[1];

        var len = callargs.length;
        if (len < 1) {
            throw new Error('timer callback not specified');
        }
        var after = callargs[1];
        if (len < 2) {
            after = 1; // default to 1ms if not specified
        }

        var callback = callargs[0];
        if (typeof callback !== 'function') {
            throw new Error('timer callback is not a function (' + (typeof callback) + ')');
        }

        after *= 1; // coalesce to number or NaN
        if (!(after >= 1 && after <= java.lang.Integer.MAX_VALUE)) {
            after = 1;
        }

        var callbackargs = [];
        for (var i = 2; i < len; i++) {
            callbackargs.push(callargs[i]);
        }
        var id = timer.setTimeout(repeated, after, function(name, args) {
              callback.apply(id, callbackargs);
        });
        // defer starting the timer until the next tick
        process.nextTick(function() {
            timer.start(id);
        });
        return id;
    };

    Object.defineProperty(global, 'setTimeout', {
        enumerable : true,
        value : function() {
            return _setTimeout(false, arguments);
        }
    });

    Object.defineProperty(global, 'clearTimeout', {
        enumerable : true,
        value : function() {
            if (arguments.length > 0) {
                if (arguments[0]) {
                    timer.clearTimeout(arguments[0]);
                }
            } else {
                timer.clearAll();
            }
        }
    });

    Object.defineProperty(global, 'setInterval', {
        enumerable : true,
        value : function() {
            return _setTimeout(true, arguments);
        }
    });

    Object.defineProperty(global, 'clearInterval', {
        enumerable : true,
        value : function() {
            if (arguments.length > 0) {
                if (arguments[0]) {
                    timer.clearTimeout(arguments[0]);
                }
            } else {
                timer.clearAll();
            }
        }
    });

    Object.defineProperty(global, 'setImmediate', {
        enumerable : true,
        value : function() {
            var cb = arguments[0]
            var callbackargs = [];
            for (var i = 1; i < arguments.length; i++) {
                callbackargs.push(arguments[i]);
            }
            var ctx = {};
            var wrapper = function() {
                cb.apply(ctx, callbackargs);
            }

            ctx.id = timer.setImmediate(wrapper);
            return ctx;
        }
    });

    Object.defineProperty(global, 'clearImmediate', {
        enumerable : true,
        value : function() {
            if (arguments.length > 0) {
                if (arguments[0]) {
                    timer.clearImmediate(arguments[0].id);
                }
            }
        }
    });

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

    __avatar.eventloop.setUncaughtExceptionHandler(
        function(name, args) {
            var listeners = process.listeners('uncaughtException');
            if (listeners.length > 0) {
                args[0] = true; // return non-null value to indicate user handler present
            }
        },
        function(name, args) {
            var ctx = Packages.net.java.avatar.js.eventloop.EventLoop;
            var e = new Error();
            var ex = args[0];
            e.filename = ex[ctx.EXCEPTION_FILE];
            e.line = ex[ctx.EXCEPTION_LINE];
            e.column = ex[ctx.EXCEPTION_COLUMN];
            e.name = ex[ctx.EXCEPTION_NAME];
            e.message = ex[ctx.EXCEPTION_MESSAGE];
            e.stack = ex[ctx.EXCEPTION_STACK];
            process.emit(name, e);
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

    process.nextTick(NativeModule.require('module').runMain);

} )();
