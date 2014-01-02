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

(function(exports) {
    var TimerHandle = Packages.com.oracle.libuv.handles.TimerHandle;
    var loop = __avatar.eventloop.loop();

    function Timer() {
        Object.defineProperty(this, '_timer', { value: new TimerHandle(loop) });
        var that = this;
        this._timer.setTimerFiredCallback(function(status) {
            if (that.ontimeout) {
                // When a timer is unref, the domain is set on the wrap.
                if (that.domain) {
                    that.domain.enter();
                }
                that.ontimeout();
                if (that.domain) {
                    that.domain.exit();
                }
            }
        });
    }

    Timer.prototype.start = function(timeout, repeat) {
        try {
            this._timer.start(timeout, repeat);
        } catch(err) {
            if (!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            throw err;
        }
    }

    Timer.prototype.stop = function() {
        try {
            this._timer.stop();
        } catch(err) {
            if (!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            throw err;
        }
    }

    Timer.prototype.again = function() {
        try {
            this._timer.again();
        } catch(err) {
            if (!err.errnoString) {
                throw err;
            }
            process._errno = err.errnoString();
            throw err;
        }
    }

    Timer.prototype.setRepeat = function(repeat) {
        this._timer.setRepeat(repeat);
    }

    Timer.prototype.getRepeat = function() {
        return this._timer.getRepeat();
    }

    Timer.prototype.ref = function() {
        this._timer.ref();
    }

    Timer.prototype.unref = function() {
        this._timer.unref();
    }

    Timer.prototype.close = function() {
        if (!this.closed) {
            this.closed = true;
            this._timer.close();
        }
    }

    exports.Timer = Timer;

});
