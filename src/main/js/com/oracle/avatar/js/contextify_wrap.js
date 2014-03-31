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

    var Server = Packages.com.oracle.avatar.js.Server;

    function ContextifyScript(code, options) {
        this._code = code;
        this._filename = options.filename;
    }

    exports.ContextifyScript = ContextifyScript;

    ContextifyScript.prototype.runInThisContext = function(options) {
       if (this._code === null) {
           return load(__avatar.loader.wrapURL(this._filename));
       } else {
           return load({script: this._code, name: this._filename});
       }
    };

    ContextifyScript.prototype.runInContext = function(sandbox, options) {
        print('contextify.runInContext sandbox ' + JSON.stringify(sandbox) + ', options: ' + JSON.stringify(options));
    };

    ContextifyScript.prototype.makeContext = function(sandbox) {
        print('contextify.makeContext sandbox ' + JSON.stringify(sandbox));
    };

    ContextifyScript.prototype.isContext = function(sandbox) {
        print('contextify.isContext sandbox ' + JSON.stringify(sandbox));
    };

});
