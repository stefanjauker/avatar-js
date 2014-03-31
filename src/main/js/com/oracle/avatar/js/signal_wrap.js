/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

    var SignalHandle = Packages.com.oracle.libuv.handles.SignalHandle;
    var Constants = Packages.com.oracle.libuv.Constants;
    var loop = __avatar.eventloop.loop();

    function Signal() {
        Object.defineProperty(this, '_signal', { value: new SignalHandle(loop) });
        var that = this;
        this._signal.setSignalCallback(function(signum) {
            if (that.onsignal) {
                if (that.domain) {
                    that.domain.enter();
                }
                var signal = Constants.getConstantsString().get(signal);
                that.onsignal(signal);
                if (that.domain) {
                    that.domain.exit();
                }
            }
        });
    }

    Signal.prototype.start = function(signum) {
        return this._signal.start(signum);
    };

    Signal.prototype.stop = function() {
        return this._signal.stop();
    };

    Signal.prototype.ref = function() {
        return this._signal.ref();
    };

    Signal.prototype.unref = function() {
        return this._signal.unref();
    };

    Signal.prototype.close = function() {
        if (!this.closed) {
            this.closed = true;
            return this._signal.close();
        }
    };

    exports.Signal = Signal;

});
