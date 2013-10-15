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

(function(exports, require) {

    var Module = require("module");
    var stream = Module.prototype.require('stream');
    var util = require("util");
    var events = require('events');

    var ProcessHandle = Packages.net.java.libuv.handles.ProcessHandle;
    var Constants = Packages.net.java.libuv.Constants;
    var loop = __avatar.eventloop.loop();
    
    var AccessController = java.security.AccessController;
    var PrivilegedAction = java.security.PrivilegedAction;
    var LibUVPermission = Packages.net.java.libuv.LibUVPermission;
    // From this context, only the libuv.handle permission will be granted.
    var avatarContext = __avatar.controlContext;
    
    exports.Process = function() {
        return new Process();
    }

    function Process() {
        var that = this;
        this.process = AccessController.doPrivileged(new PrivilegedAction() {
            run: function() {
                return new ProcessHandle(loop);
            }
        }, avatarContext, LibUVPermission.HANDLE);
        
        this.process.exitCallback = function(args) {
            var status = args[0];
            var signal = args[1];
            if (typeof signal == 'number' && signal > 0) {
                signal = Constants.getConstantsString().get(signal);
            }
            if (status == -1) {
                var nativeException = args[2];
                var errno = nativeException.errnoString();
                process._errno = errno;
            }
            that.onexit(status, signal);
        };
    }

    Process.prototype.spawn = function(options) {
        var Array = Java.type("java.lang.String[]");
        var args = new Array(options.args.length);
        for (var i = 0; i < args.length; i++) {
            args[i] = options.args[i];
        }
        var env = null;
        if (options.envPairs) {
            env = new Array(options.envPairs.length);
            for (var i = 0; i < env.length; i++) {
                env[i] = options.envPairs[i];
            }
        }
        var cwd = options.cwd ? options.cwd : null;
        var uid = options.uid ? options.uid : -1;
        var gid = options.gid ? options.gid : -1;

        var ProcessFlags = Java.type("net.java.libuv.handles.ProcessHandle.ProcessFlags");
        var processFlagSet = java.util.EnumSet.noneOf(ProcessFlags.class);

        if (options.windowsVerbatimArguments) {
            processFlagSet.add(ProcessFlags.WINDOWS_VERBATIM_ARGUMENTS);
        }

        if (options.detached) {
            processFlagSet.add(ProcessFlags.DETACHED);
        }

        var StdioOptions = Java.type("net.java.libuv.handles.StdioOptions");
        var StdioOptionsArray = Java.type("net.java.libuv.handles.StdioOptions[]");
        var stdioOptions = new StdioOptionsArray(options.stdio.length);
        for (var i = 0; i < stdioOptions.length; i++) {
            var type = options.stdio[i].type;
            if (type == 'ignore') {
                stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.IGNORE, null, -1);
            } else if (type == 'pipe') {
                stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.CREATE_PIPE, options.stdio[i].handle._pipe, -1);
            } else if (type == 'wrap') {
                var wrapType = options.stdio[i].wrapType;
                if (wrapType == "pipe") {
                    stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.INHERIT_STREAM, options.stdio[i].handle._pipe, -1);
                } else if (wrapType == "tty") {
                    stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.INHERIT_STREAM, options.stdio[i].handle._tty, -1);
                } else if (wrapType == "tcp") {
                    stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.INHERIT_STREAM, options.stdio[i].handle._connection, -1);
                } else if (wrapType == "udp") {
                    stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.INHERIT_STREAM, options.stdio[i].handle._udp, -1);
                }
            } else {
                stdioOptions[i] = new StdioOptions(StdioOptions.StdioType.INHERIT_FD, null, options.stdio[i].fd);
            }
        }

        this.pid = this.process.spawn(
                    options.file,
                    args,
                    env,
                    cwd,
                    processFlagSet,
                    stdioOptions,
                    uid,
                    gid);
        return this.pid == -1 ? -1 : 0;
    }

    Process.prototype.close = function() {
        this.process.close();
    }

    Process.prototype.kill = function(signal) {
        return this.process.kill(signal);
    }

    Process.prototype.ref = function() {
      this.process.ref();
    }

    Process.prototype.unref = function() {
      this.process.unref();
    }
});

