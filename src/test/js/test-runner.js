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

var fs = require('fs');
var path = require('path');
var spawn = require('child_process').spawn;
var exec = require('child_process').exec;
var stdout = process.stdout;
var stderr = process.stderr;

var args = process.argv;
if (args.length < 3) {
    stderr.write('Usage: ' + args[0] + ' ' + args[1] + ' [options] <args>\n');
    stderr.write(' where options may be\n');
    stderr.write('  -nocolor to disable colorized output (for non-interactive runs)\n');
    stderr.write('  -noassert to disable assertions (assertions are enabled by default)\n');
    stderr.write('  -deprecation to throw on deprecations (disabled by default)\n');
    stderr.write('  -noredirect to disable stdout/stderr redirection to files\n');
    stderr.write('  -secure to run tests with a security manager\n');
    stderr.write('  -delay ms to pause between each test to give the OS time to cleanup (default: 100)\n');
    stderr.write('  -timeout s to specify the timeout for each test (default: 60s)\n');
    stderr.write('  -drip to enable use of drip to reduce jvm startup time (disables assertions and logging)\n');
    stderr.write(' <args> is some combination of node tests and/or directories\n');
    stderr.write('directories are searched for node tests matching "test-*.js"\n');
    process.exit(-1);
}

var secure = false;
var drip = false;
var delay = 100;
var timeout = 60 * 1000;
var assertions = true;
var deprecations = false;
var redirect = true;
// ansi tty escape codes only work with node.js on windows
var colorize = !(process.platform === 'win32' && typeof java != 'undefined');

var testNames = [];
var exclusions = [];
try {
    exclusions = fs.readFileSync(process.platform +
        '-test-exclusions.txt').toString('utf8').split('\n');
} catch (ignore) {}

stderr.write('\n');
var tests = args.slice(2);
var testlen = tests.length;
for (var i=0; i < testlen; i++) {
    var root = tests[i];
    if (root.match('^-\w*')) {
        switch (root) {
            case '-noassert': assertions = false; break;
            case '-deprecation': deprecation = true; break;
            case '-nocolor': colorize = false; break;
            case '-noredirect': redirect = false; break;
            case '-secure': secure = true; break;
            case '-drip': drip = true; break;
            case '-delay': delay = Number(tests[++i]); break;
            case '-timeout': timeout = Number(tests[++i]) * 1000; break;
        }
        continue;
    }
    var stats = fs.statSync(root);
    if (stats.isDirectory()) {
        fs.readdirSync(root).filter(function(file) {
            return /^test-.+\.js$/.test(file);
        }).sort().forEach(function(test) {
            if (exclusions.indexOf(test) >= 0) {
                stderr.write('excluding ' + test + '\n');
            } else {
                testNames.push(path.join(root, test).toString());
            }
        });
    } else if (stats.isFile()) {
        var test = path.basename(root);
        if (exclusions.indexOf(test) >= 0) {
            stderr.write('excluding ' + test + '\n');
        } else {
            testNames.push(root.toString());
        }
    }
}

// a recursive implementation of mkdir -p
function mkdirsSync(p, target) {
    if (path.relative(p, target) == '') {
        return; // do not traverse above the target
    }
    try {
        fs.mkdirSync(p);
    } catch (e) {
        // probably failed due to our parent not existing
        // recursively attempt to create our parent dir(s)
        mkdirsSync(path.dirname(p), target);
        try {
            fs.mkdirSync(p); // retry
        } catch (ignore) {
        }
    }
}

// ensure tmp dir exists, needed for running fs tests
var tmp = path.join(process.cwd(), 'test', 'tmp');
mkdirsSync(tmp, process.cwd());

var target = path.join(process.cwd(), 'dist');
var jar = path.join(target, 'avatar-js.jar');
var results = path.join(process.cwd(), 'test-output');
mkdirsSync(results, process.cwd());

var testsRun = 0;
var testsToRun = testNames.length;
var testsFailed = 0;
var failedTests = [];

var escape = '\033[';
var red = colorize ? escape + 31 + 'm' : '';
var green = colorize ? escape + 32 + 'm' : '';
var magenta = colorize ? escape + 35 + 'm' : '';
var cyan = colorize ? escape + 36 + 'm' : '';
var bold = colorize ? escape + 1 + 'm' : '';
var reset = colorize ? escape + 0 + 'm' : '';

var args = ['-server', (assertions ? '-ea' : '-da'), '-Djava.awt.headless=true'];
args.push('-Djava.library.path=' + target);
var jarArgs = ['-jar', jar.toString()];
if (!deprecations) {
    jarArgs.push('--no-deprecation');
}
if (secure) {
    args.push('-Djava.security.manager');
    args.push('-Djava.security.policy=' + path.join(process.cwd(), 'dist/avatar-js.policy'));
}
var options = {
    env: process.env
};

// use drip if it is available on the path
// https://github.com/flatland/drip
var command = 'java';
if (drip) {
    exec('drip -version', function(error) {
        if (!error) {
            command = 'drip';
            options.env['DRIP_SHUTDOWN'] = '1'; // shutdown drip process after 1 min
            // disable logging if drip is enabled - otherwise logs end up in incorrect folders
            assertions = false;
        }
    });
}

var totalStart = Date.now();
function runNextTest() {
    if (testNames.length == 0) {
        var completion = testsToRun == 0 ? 0 : (100 * (testsRun - testsFailed)) / testsToRun;
        stderr.write('\nTotal tests attempted: ' + bold + testsRun + reset +
                ', failed: ' + bold + testsFailed + reset +
                ', completion rate: ' + bold + completion.toFixed(0) + '%' + reset +
                ', total time: ' + bold + ((Date.now() - totalStart) / 1000).toFixed(0) + reset + 's' +
                '\n');
        if (failedTests.length > 0) {
            stderr.write('Failed tests: ' + '\n');
            var i = 0;
            failedTests.forEach(function(test) {
                stderr.write((++i) + ' ' + test + '\n');
            });
        }
        stderr.write('\n');
        process.exit(testsFailed);
    }
    var testName = testNames.shift();
    stderr.write(++testsRun + '/' + testsToRun + ' ' + testName + ' ... ');

    var dir = path.join(results, testName).replace(/\.js$/, '');
    mkdirsSync(dir, results);
    if (redirect) {
        var out = fs.openSync(path.join(dir, 'stdout.txt'), 'w');
        var err = fs.openSync(path.join(dir, 'stderr.txt'), 'w');
    }

    var restArgs = [].concat(args, '-Davatar-js.log.output.dir=' + dir, jarArgs, testName);
    var start = Date.now();
    var java = spawn(command, restArgs, options);
    var failed = false;
    var timeoutId = setTimeout(function() {
        stderr.write(cyan + 'TIMEOUT ' + reset);
        failed = true;
        java.kill();
    }, timeout);
    java.stdout.setEncoding('utf8');
    java.stderr.setEncoding('utf8');
    java.stdout.on('data', function(data) {
        if (redirect) {
            fs.writeSync(out, data);
        } else {
            stdout.write(data);
        }
    });
    java.stderr.on('data', function(data) {
        if (redirect) {
            fs.writeSync(err, data);
        } else {
            stderr.write(data);
        }
    });
    java.on('exit', function(code) {
        clearTimeout(timeoutId);
        stderr.write(((Date.now() - start) / 1000).toFixed(0) + 's ');
        if (redirect) {
            fs.closeSync(out);
            fs.closeSync(err);
        }
        if (code != 0 || failed) {
            testsFailed++;
            stderr.write(bold + red + 'FAILED');
            failedTests.push(testName);
        } else {
            stderr.write(bold + green + 'OK');
        }
        stderr.write(reset + '\n');
        setTimeout(function() {
            runNextTest();
        }, delay);
    });
}

stderr.write('\n');
runNextTest();
