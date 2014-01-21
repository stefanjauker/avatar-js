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

// requires avatar-js permission
testThrow(function() { process._tickCallback(); } );

// System permission
testThrow(function() { process.abort(); } );

process.arch;

process.argv;

process.binding("natives");

// require LibUV permission
testThrow(function() { process.chdir("roro"); } );

// require LibUV permission
testThrow(function() { process.cwd(); } );

// requires System property read
testThrow(function() { process.env } );

process.execArgv;

// requires System property read
testThrow(function() { process.execPath; } );

// requires avatar-js permission
testThrow(function() { process.exit(); } );

process.features;

// requires process permission
testThrow(function() { process.getgid(); } );

// requires process permission
if (typeof(__test_windows) === 'undefined') {
    print("Non windows checks");
    testThrow(function() { process.getgroups(); } );
    // requires process permission
    testThrow(function() { process.initgroups(999, 999); } );
    // requires process permission
    testThrow(function() { process.setgroups([999]); } );
}

// requires process permission
testThrow(function() { process.getuid(); } );

process.hrtime;

// requires libUV permission
testThrow(function() { process.kill(999, 0); } );

process.memoryUsage

process.nextTick(function(){});

process.openStdin;

process.pid;

// requires process permission
testThrow(function() { process.setgid(999); } );

// requires process permission
testThrow(function() { process.setuid(999); } );

process.stderr;

// When run in TestNG, these are seen as socket requiring permissions.
// If run outside TestNG, no permission required.
testThrow(function() { process.stdin; } );

process.stdout;

// requires LibUV permission
testThrow(function() { process.title; } );

// requires LibUV permission
testThrow(function() { process.title = 'toto'; } );

process.throwDeprecation;

process.traceDeprecation;

// requires process permission
testThrow(function() { process.umask(); } );

// requires process permission
testThrow(function() { process.umask(999); } );

process.uptime;

process.versions;

process.version;

process.on('exit', function() {
    SCRIPT_OK = true;
});

function testThrow(f) {
    try {
        f();
        throw new Error("Should have thrown! " + f);
    }catch(ex) {
        if (ex instanceof java.security.AccessControlException) {
            print("Caught expected exception " + ex);
        } else {
            print("Root exception " + ex.stack);
            throw ex;
        }
    }
}
