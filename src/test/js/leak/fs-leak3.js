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
var path = require('path');
var fs = require('fs');

var tmpDir = require("../../../../test/common.js").tmpDir;
var filename = path.join(tmpDir, 'fs-leak3.txt');
var content = 'hello worldçoié\uD83D\uDC4D\n';

if(process.argv[2] === '-large') {
    print('building large content...');
    for (var i = 0; i < 1024 * 1024; i++) {
        content += 'hello worldçoié\uD83D\uDC4D\n';
    }
    print('done building content');
}

process.on('exit', function() {
    fs.unlinkSync(filename);
})

var perf = require("../perf/common-perf");
perf.startPerf(writeFile);

function writeFile() {
    perf.actionStart();
    var fd = fs.openSync(filename, 'w+');
    var buff = new Buffer(content);
    fs.write(fd, buff, 0, buff.length, 0, then);
    
    function then(er) {
        fs.close(fd);
        if (perf.canContinue()) {
            writeFile();
        }
    }
}
