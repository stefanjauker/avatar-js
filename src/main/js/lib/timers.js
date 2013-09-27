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

// Simple re-export of globals

exports.setTimeout = setTimeout;

exports.setInterval = setInterval;

exports.clearTimeout = clearTimeout;

exports.clearInterval = clearInterval;

exports.setImmediate = setImmediate;

exports.clearImmediate = clearImmediate;

function enroll(item, delay, repeat) {
    var that = item;
    unenroll(item);
    item._timerId = setTimeout(function() {
        that._onTimeout();
    }, delay);
    item._idleTimeout = delay;
    item._enrolled = true;

}

exports.enroll = enroll;

function unenroll(item) {
    if (item._timerId) {
        clearTimeout(item._timerId);
        item._enrolled = false;
        item._idleTimeout = -1;
    }
}

exports.unenroll = unenroll;

function _unrefActive(item) {
    var msecs = item._idleTimeout;
    if (msecs >= 0 && !item._enrolled) {
        clearTimeout(item._timerId);
        var that = item;
        item._timerId = setTimeout(function() {
            that._onTimeout();
        }, msecs);
        item._idleTimeout = msecs;
    }
}

exports._unrefActive = _unrefActive;
