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

    var Parser = Packages.com.oracle.httpparser.HttpParser;
    var ParserSettings = Packages.com.oracle.avatar.js.http.ParserSettings;

    var debug;
    if (process.env.NODE_DEBUG && /http/.test(process.env.NODE_DEBUG)) {
      debug = function(x) { console.error('HTTP: %s', x); };
    } else {
      debug = function() { };
    }

    function HTTPParser(type) {
        this.reinitialize(type);
        Object.defineProperty(this, 'minor', {
            get : function() {
                this._parser.minor()
            }
        });
        Object.defineProperty(this, 'major', {
            get : function() {
                this._parser.major()
            }
        });

        var that = this;

        Object.defineProperty(this, '_settings', {
            value : new ParserSettings(
            // on_headers_complete
            function(url, jheaders) {
                var headers = Java.from(jheaders);
                var info = {
                    headers : headers,
                    versionMinor : that._parser.minor(),
                    versionMajor : that._parser.major(),
                    shouldKeepAlive : that._parser.shouldKeepAlive(),
                    upgrade : that._parser.upgrade()
                };

                if (that._type === HTTPParser.REQUEST) {
                    info.url = url;
                    info.method = that._parser.method();
                }
                if (that._type === HTTPParser.RESPONSE) {
                    info.statusCode = that._parser.statusCode();
                }

                var response;
                try {
                    response = that.onHeadersComplete(info);
                } catch (e) {
                    debug(e.stack);
                    that._got_exception = e;
                    return -1;
                }
                return response ? 1 : 0;
            },
            // on_body
            function(offset, length) {
                try {
                    var bodyOffset = that._dataStart + offset;
                    that.onBody(that._data, bodyOffset, length);
                } catch (e) {
                    debug(e.stack);
                    that._got_exception = e;
                    return -1;
                }
                return 0;
            },
            // on_message_complete
            function() {
                try {
                    that.onMessageComplete();
                } catch (e) {
                    debug(e.stack);
                    that._got_exception = e;
                    return -1;
                }
                return 0;
            },
            // on_headers
            function(url, jheaders) {
               that.onHeaders(Java.from(jheaders), url);
            })
        });
    }

    exports.HTTPParser = HTTPParser;
    HTTPParser.REQUEST = Parser.Type.REQUEST.name();
    HTTPParser.RESPONSE = Parser.Type.RESPONSE.name();
    var typeClass = Packages.com.oracle.httpparser.HttpParser.Type.class;
    var valueOf = java.lang.Enum.valueOf;

    HTTPParser.prototype.reinitialize = function(type) {
        if (this._parser) {
            this._parser.free();
        }
        delete this._parser;
        this._got_exception = null;

        Object.defineProperty(this, '_parser', {
            value : new Parser(),
            configurable : true
        });
        Object.defineProperty(this, '_type', {
            value : type,
            writable : true
        });
        this._parser.init(valueOf(typeClass, type));
    }

    HTTPParser.prototype.execute = function(data, start, length) {
        if (!Buffer.isBuffer(data)) {
            var err = new Error();
            err.message = "Argument should be a buffer";
            return err;
        }

        if (start >= data.length) {
            var err = new Error();
            err.message = "Offset is out of bounds";
            return err;
        }

        if (start + length > data.length) {
            var err = new Error();
            err.message = "off + len > buffer.length";
            return err;
        }

        this._got_exception = null;
        this._data = data;
        this._dataStart = start;
        var nparsed = this._parser.execute(this._settings, data._impl.underlying(), start, length);
        this._data = null;
        this._dataStart = -1;
        if (this._got_exception)
            throw this._got_exception;
        if (!this._parser.upgrade() && nparsed != length) {
            return newError(this._parser, nparsed);
        } else {
            return nparsed;
        }
    }

    function newError(parser, nparsed) {
        var e = new Error();
        e.code = parser.errnoName();
        e.message = "Parse Error " + e.code;
        e.bytesParsed = nparsed;
        return e;
    }

    var FINISH_BUFFER = new Buffer(0)._impl.toByteBuffer()
    HTTPParser.prototype.finish = function() {
        this._got_exception = null;
        var nparsed = this._parser.execute(this._settings, FINISH_BUFFER, 0, 0);
        if (this._got_exception) {
            throw this._got_exception;
        }
        if (nparsed != 0) {
            return newError(this._parser, nparsed);
        }
        return undefined;
    }

    HTTPParser.prototype.pause = function() {
       this._parser.pause();
    }

    HTTPParser.prototype.resume = function() {
       this._parser.resume();
    }
});
