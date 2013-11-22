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


var perf = require("../perf/common-perf");
var module = require("module");
var path = require("path");
var fs = require("fs");
var file = path.join(__dirname, '../crypto/test-crypto.js');
var cryptoRoot = path.join(__dirname, '../../../../test/crypto');
var files = fs.readdirSync(cryptoRoot);
var cryptoFiles = [];
files.map(function(item) {
    if(item.indexOf("test-crypto") == 0) {
        cryptoFiles.push(path.join(cryptoRoot, item));
    }
})
perf.startPerf(crypto, 20);

function crypto() {
    perf.actionStart();
    global.silent = true;
    // load/execute all crypto tests
    cryptoFiles.map(function(f) {
        print(f);
        require(f);
        delete module.Module._cache[f];
    })
    // load continuously the test-crypto.js, it is synchronous.
    require(file);
    // otherwise common will find it and fail.
    delete global.silent;
    delete module.Module._cache[file];
    if (perf.canContinue()) {
        setImmediate(crypto);
    }
}