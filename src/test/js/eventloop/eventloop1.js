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
var evtloop = __avatar.eventloop;
var count  = new java.util.concurrent.atomic.AtomicInteger(0);
var NUM_THREADS = 1000;
var POSTED = 12;

function f2() {
    count.incrementAndGet();
}

function createClosure(i) {
    return function() {
        java.lang.Thread.sleep(500);
        print("Thread " + i + " started");
        for(var j = 0; j < 10; j++) {
            evtloop.post(f2);
        }
        java.lang.Thread.sleep(100);
        evtloop.post(new Packages.net.java.avatar.js.eventloop.Event("", f2));
        print("Thread " + i + " done");
        evtloop.post(new Packages.net.java.avatar.js.eventloop.Event("", f2));
        handle.release();
    }
}

for(var i = 0 ; i < NUM_THREADS; i++) {
    var handle = evtloop.acquire();
    var thr = new java.lang.Thread(createClosure(i));
    thr.setDaemon(true);
    thr.start();
}

console.log("End synchronous script");
process.on('exit', function(e) {
    print("Exiting ");
    if(count.get() != NUM_THREADS * POSTED) {
        throw new Error("COunt is not the expected one " + count.get());
    }
})