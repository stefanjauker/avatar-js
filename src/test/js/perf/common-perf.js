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
var start = 0;
var count = 0;
var go = true;
var util = require('util');
var memStart;
var log = console.log;

function startPerf(fstart, time) {
    start = process.hrtime();
    setTimeout(function() {
        go = false;
    }, time * 1000);
    fstart();
    log("Perf test started.");
}
exports.startPerf = startPerf;
function round(n) {
    return Math.floor(n * 100) / 100;
}
function memToString(mem) {
    if (java) {
        return mem.toString();
    } else {
        return util.inspect(mem)
    }
}
function captureMemory() {
    if (java) {
        return java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()
    } else {
        return process.memoryUsage();
    }
}

function diffMemory(start, end) {
    if (java) {
        return end.used - start.used;
    } else {
        return end.heapUsed - start.heapUsed;
    }
}
function forceGC() {
    for (var i = 0; i < 10; i++) {
        global.gc();
    }
}
function dumpResults() {
    var end = process.hrtime();
    process.stdout.write('\n');
    log('num of actions %d ', count);
    var elapsed = [end[0] - start[0], end[1] - start[1]];
    var ns = elapsed[0] * 1E9 + elapsed[1];
    var nsper = round(ns / count);
    log('%d ns per action (lower is better)', nsper);
    var readsper = round(count / (ns / 1E9));
    log('%d actions per sec (higher is better)', readsper);
}
exports.dumpResults = dumpResults;
function canContinue() {
    if (!go) {
       doExit(); 
    }
    return true;
}

function doExit() {
    dumpResults();
    log("Ending...");
    forceGC();
    var memEnd = captureMemory();
    log("Memory after gc \n" + memToString(memEnd));
    log("Heap Diff " + diffMemory(memStart, memEnd));
    process.exit(0);
}

exports.canContinue = canContinue;
function actionStart() {
    if(count == 5) {
        log("Capturing mem...")
        forceGC();
        memStart = captureMemory()
        log("mem " + memToString(memStart));
    }
    count++;
    if (!(count % 1000)) {
        process.stdout.write('.');
    }
}
exports.actionStart = actionStart;

// Automated execution of exiting tests
var fs = require("fs")
var path = require("path")
var module = require("module");
var testFiles = [];

function startPerfFromTests(rootDirs, radicals, time) {
    if(!Array.isArray(rootDirs)) {
        rootDirs = [rootDirs]
    }
    if(!Array.isArray(radicals)) {
        radicals = [radicals]
    }
    rootDirs.map(function(rootDir) {
        var files = fs.readdirSync(rootDir);
        files.map(function(item) {
            radicals.map(function(radical) {
                if(item.indexOf(radical) == 0) {
                    testFiles.push(path.join(rootDir, item));
                }
            });
        })
    });
    log("Run files:\n" + testFiles);
    console.log = function() {}
    startPerf(run, time)
}

exports.startPerfFromTests = startPerfFromTests;

function run() {
    actionStart();
    // load/execute all tests
    testFiles.map(function(f) {
        require(f);
        delete module.Module._cache[f];
    })
    if (canContinue()) {
        setImmediate(run);
    }
}
// Killing the process dumps current status
process.signals.setSignalCallback(doExit);
process.signals.start('SIGINT');
