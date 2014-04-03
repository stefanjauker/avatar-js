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

// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

var assert = require('assert');
var JavaBuffer = Packages.com.oracle.avatar.js.buffer.Buffer;
var setIndexedPropertiesToExternalArrayData = Object.setIndexedPropertiesToExternalArrayData;

function Buffer(subject, encoding, offset) {

    if (!(this instanceof Buffer)) {
        return new Buffer(subject, encoding);
    }

    // Are we slicing?
    if (typeof offset === 'number') {
        if (!Buffer.isBuffer(subject)) {
            throw new TypeError('First argument must be a Buffer when slicing');
        }
        var start = +encoding > 0 ? Math.ceil(encoding) : 0;
        var end = offset;
        if (offset < 0) { // from end of subject
            end = subject._impl.capacity() + offset;
        }
        if (end <= 0) {
            throw new RangeError("Invalid slice end " + end);
        }
        this._impl = subject._impl.slice(start, end);
    } else {
        // Find the length
        switch (typeof subject) {
            case 'number':
                var size = +subject > 0 ? Math.ceil(subject) : 0;
                if (size < 0) {
                    throw new TypeError('Buffer size negative (' + size + ')');
                } else if (size > java.lang.Integer.MAX_VALUE && size <= 0xffffffff) {
                    throw new RangeError('Buffer size too large for signed integer (' + size + ')');
                } else if (size > 0xffffffff) {
                    throw new TypeError('Buffer size too large (' + size + ')');
                }
                this._impl = new JavaBuffer(size);
                break;

            case 'string':
                if (typeof encoding != 'undefined' && typeof encoding != 'string') {
                    throw new TypeError('invalid encoding: ' + encoding);
                }
                this._impl = new JavaBuffer(subject, Buffer._javaEncoding(encoding));
                break;

            case 'object':
                if (subject.length == undefined) {
                    // this is a Java Buffer from slice()
                    this._impl = subject;
                } else { // subject is an array or a Buffer
                    // round up and truncate to integer if fractional
                    var len = +subject.length > 0 ? Math.ceil(subject.length) : 0;
                    var a = java.lang.reflect.Array.newInstance(java.lang.Double.class, len);
                    for (var i=0; i < len; i++) {
                        a[i] = subject[i];
                    }
                    this._impl = new JavaBuffer(a);
                }
                break;

            default:
                throw new TypeError('First argument needs to be a number, ' +
                    'array or string.');
        }
    }

    if (setIndexedPropertiesToExternalArrayData) {
        // this function is available in jdk8u20+ and jdk9+ (JDK-8011964)
        setIndexedPropertiesToExternalArrayData(this, this._impl.underlying());
        this.length = this._impl.capacity();
    } else {
        // use slower JSAdapter wrapper in earlier releases
        return this._init();
    }
}

// make certain Array methods available on Buffers
// only those Array methods that make no structural changes are supported
if (setIndexedPropertiesToExternalArrayData) {
    require('util').inherits(Buffer, Array);
}

exports.Buffer = Buffer;

exports.SlowBuffer = function(str, encoding) {
    var sb = Buffer(str, encoding);
    sb._slow = true;
    return sb;
};
exports.SlowBuffer.prototype = Buffer;

Buffer.prototype._init = function() {
    var that = this;
    // provide frequently-used final properties as overrides for speed
    // Side effect of this is that Object.getOwnPropertyNames(buffer)
    // https://kenai.com/jira/browse/NODEJS-110
    // returns the content of overrides otherwise it is not true (eg: valueOf and toString)
    var overrides = {
        _impl: that._impl,
        _this: that,
        length: that._impl.capacity(),
        checkOffset: that.checkOffset,
        checkInt: that.checkInt,
        prototype: that.prototype,
        valueOf: that.valueOf,
        toString: that.toString,
        slice: that.slice,
        copy: that.copy,
        write: that.write
// Tradeoff. All in overrides impacts construction time (JDK-8012593)
//        fill: that.fill,
//        readInt8: that.readInt8,
//        readUInt8: that.readUInt8,
//        readInt16LE: that.readInt16LE,
//        readUInt16LE: that.readUInt16LE,
//        readInt16BE: that.readInt16BE,
//        readUInt16BE: that.readUInt16BE,
//        readInt32LE: that.readInt32LE,
//        readUInt32LE: that.readUInt32LE,
//        readInt32BE: that.readInt32BE,
//        readUInt32BE: that.readUInt32BE,
//        readFloatLE: that.readFloatLE,
//        readFloatBE: that.readFloatBE,
//        readDoubleLE: that.readDoubleLE,
//        readDoubleBE: that.readDoubleBE,
//        writeInt8: that.writeInt8,
//        writeUInt8: that.writeUInt8,
//        writeInt16LE: that.writeInt16LE,
//        writeUInt16LE: that.writeUInt16LE,
//        writeInt16BE: that.writeInt16BE,
//        writeUInt16BE: that.writeUInt16BE,
//        writeInt32LE: that.writeInt32LE,
//        writeUInt32LE: that.writeUInt32LE,
//        writeInt32BE: that.writeInt32BE,
//        writeUInt32BE: that.writeUInt32BE,
//        writeFloatLE: that.writeFloatLE,
//        writeFloatBE: that.writeFloatBE,
//        writeDoubleLE: that.writeDoubleLE,
//        writeDoubleBE: that.writeDoubleBE,
//        inspect: that.inspect
    };
    // JSAdapter is needed to intercept calls that use the array syntax
    // (such as b[0] = 32)
    return new JSAdapter(Buffer.prototype, overrides, {
        __get__: function(name) {
            if (typeof name == 'number') {
                if (name >= that._impl.capacity()) {
                    return undefined;
                } else {
                    return that._impl.getByteAt(name);
                }
            } else if (name == 'INSPECT_MAX_BYTES') {
                return _INSPECT_MAX_BYTES;
            } else if (name == "__proto__") {
                return Object.getPrototypeOf(that);
            } else {
                return that[name];
            }
        },

        __has__: function(name) { // called by slice and var exists = "x" in obj;
            if (typeof name == 'number') {
                return name < that._impl.capacity();
            } else {
                // Overrides properties are handled by nashorn runtime.
                // Checking the Buffer prototype, "in" goes up the chain
                return Object.keys(Buffer.prototype).indexOf(name) > -1
            }
        },

        __put__: function(name, value) {
            if (typeof name == 'number') {
                that._impl.setByteAt(name, value);
            } else if (name == 'INSPECT_MAX_BYTES') {
                _INSPECT_MAX_BYTES = value;
            } else if (name == '_slow') {
                that._slow = value;
            } else if (name == "__proto__") {
                Object.setPrototypeOf(that, value);
            } else {
                that[name] = value;
            }
        },

        __call__: function(name, arg1, arg2, arg3, arg4, arg5, arg6, arg7) {
            // TODO: __call__ should support 'arguments' and more than 7 args
            var method = that[name];
            if (typeof method == 'function') {
                // 'this' is the wrapped Buffer, make the call on 'that', the unwrapped version
                return method.call(that, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
            } else {
                throw new Error('Buffer: unsupported method ' + name);
            }
        },

        __getIds__: function() {
            // Called when for in is called.
            // The list of properties must contain: all indexes, overrides and Buffer.prototype.
            // Getting all indexes would kill perf, but we can add Buffer.prototype.
            return [ 'length', 'toString', 'write', 'copy',
                'isBuffer', 'slice', 'fill' ].concat(Object.keys(Buffer.prototype));
        }

    });
}

Buffer.prototype.write = function(string, offset, length, encoding) {
    if (typeof offset == 'string' && isNaN(offset)) {
        var old_encoding = encoding;
        encoding = offset;
        offset = length;
        length = old_encoding;
    } else if (typeof length == 'string' && isNaN(length)) {
        encoding = length;
        length = encoding;
    }
    var capacity = this._impl.capacity();
    var off = offset || 0;
    var len = length == undefined || isNaN(length) ? capacity - off : length;

    var remaining = capacity - off;
    var len = length == undefined || isNaN(length) ? remaining : length;
    if (len > remaining) {
        len = remaining;
    }

    if (string.length > 0 && (len < 0 || off < 0)) {
        throw new RangeError('attempt to write beyond buffer bounds');
    }

    if (off >= capacity) {
        return 0;
    }

    var writtenBytes = this._impl.write(string, off, len, Buffer._javaEncoding(encoding));
    _writtenChars = this._impl.getCharsWritten();
    return writtenBytes;
}

Buffer.prototype.copy = function(targetBuffer, targetStart, sourceStart, sourceEnd) {
    var len = this._impl.capacity();
    var ts = targetStart || 0;
    var ss = sourceStart || 0;
    var se = sourceEnd == undefined ? len : sourceEnd;
    if (se - ss == 0) {
        return 0;
    }

    if (ss < 0) {
        ss = 0;
    }

    if (se > len) {
        se = len;
    }

    if (se < ss) {
        throw new RangeError('sourceEnd < sourceStart');
    }
    if (ts < 0 || ts >= targetBuffer.length) {
        throw new RangeError('targetStart out of bounds');
    }
    if (ss < 0 || ss >= se || ss >= len) {
        throw new RangeError('sourceStart out of bounds');
    }
    if (se > len) {
        throw new RangeError('sourceEnd out of bounds');
    }

    if (targetBuffer.length - ts < se - ss) {
        se = targetBuffer.length - ts + ss;
    }

    return this._impl.copy(targetBuffer._impl, ts, ss, se);
}

Buffer.prototype.toJSON = function() {
    var js = [];
    var impl = this._impl;
    impl.rewind();
    var length = impl.remaining();
    impl.mark();
    for (var i=0; i < length; i++) {
        js.push(impl.getByteAt(i));
    }
    impl.reset();
    return js;
};

Buffer.prototype.toString = function(encoding, start, end) {
    var capacity = this._impl.capacity();
    var off = start || 0;
    var endpos = end == undefined ? capacity : (end > capacity ? capacity : end);
    var len = endpos - off;
    if (off < 0 || off >= capacity || endpos < 0 || endpos > capacity || len <= 0 || len > capacity) {
        return "";
    }
    return this._impl.toString(Buffer._javaEncoding(encoding), off, len);
}

Buffer.prototype.slice = function(start, end) {
    var capacity = this._impl.capacity();
    if (start < 0 && end == undefined) { // in this case, the negative value is the offset from end
        end = capacity
        start = capacity + start;
    }

    var off = start || 0;
    if (off < 0) {
        off = 0;
    }
    var endpos = end == undefined ? capacity : (end > capacity ? capacity : end);
    if (endpos < 0) { // negative offset starts from end of Buffer.
        endpos = capacity + endpos;
    }
    var len = endpos - off;
    if (off >= capacity || len <= 0 || len > capacity) {
        return new Buffer(0);
    }
    return new Buffer(this._impl.slice(off, endpos));
}

Buffer.prototype.fill = function(value, start, end) {
    var capacity = this._impl.capacity();
    var off = start || 0;
    var endpos = end == undefined ? capacity : (end > capacity ? capacity : end);
    if (typeof value == 'string') {
        value = value.charCodeAt(0);
    } else if (!(typeof value == 'number') || isNaN(value)) {
        throw new Error('value is not a number');
    }
    this._impl.fill(value, off, endpos);
}

Buffer.concat = function(list, length) {
  if (!Array.isArray(list)) {
    throw new Error('Usage: Buffer.concat(list, [length])');
  }

  if (list.length === 0) {
    return new Buffer(0);
  } else if (list.length === 1) {
    return list[0];
  }

  if (typeof length !== 'number') {
    length = 0;
    for (var i = 0; i < list.length; i++) {
      var buf = list[i];
      length += buf.length;
    }
  }

  var buffer = new Buffer(length);
  var pos = 0;
  for (var i = 0; i < list.length; i++) {
    var buf = list[i];
    buf.copy(buffer, pos);
    pos += buf.length;
  }
  return buffer;
};

Buffer.prototype.readInt8 = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 1, this._impl.capacity());
    }
    return this._impl.readInt8(offset);
}

Buffer.prototype.readUInt8 = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 1, this._impl.capacity());
    }
    return this._impl.readUInt8(offset);
}

Buffer.prototype.readInt16LE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 2, this._impl.capacity());
    }
    return this._impl.readInt16LE(offset);
}

Buffer.prototype.readUInt16LE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 2, this._impl.capacity());
    }
    return this._impl.readUInt16LE(offset);
}

Buffer.prototype.readInt16BE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 2, this._impl.capacity());
    }
    return this._impl.readInt16BE(offset);
}

Buffer.prototype.readUInt16BE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 2, this._impl.capacity());
    }
    return this._impl.readUInt16BE(offset);
}

Buffer.prototype.readInt32LE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    return this._impl.readInt32LE(offset);
}

Buffer.prototype.readUInt32LE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    return this._impl.readUInt32LE(offset);
}

Buffer.prototype.readInt32BE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    return this._impl.readInt32BE(offset);
}

Buffer.prototype.readUInt32BE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    return this._impl.readUInt32BE(offset);
}

Buffer.prototype.readFloatLE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    return this._impl.readFloatLE(offset);
}

Buffer.prototype.readFloatBE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    return this._impl.readFloatBE(offset);
}

Buffer.prototype.readDoubleLE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 8, this._impl.capacity());
    }
    return this._impl.readDoubleLE(offset);
}

Buffer.prototype.readDoubleBE = function(offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 8, this._impl.capacity());
    }
    return this._impl.readDoubleBE(offset);
}

Buffer.prototype.writeInt8 = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 1, 0x7f, -0x80);
    }
    if (value < 0) value = 0xff + value + 1;
    this._impl.writeInt8(value, offset);
}

Buffer.prototype.writeUInt8 = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 1, 0xff, 0);
    }
    this._impl.writeUInt8(value, offset);
}

Buffer.prototype.writeInt16LE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 2, 0x7fff, -0x8000);
    }
    if (value < 0) value = 0xffff + value + 1;
    this._impl.writeInt16LE(value, offset);
}

Buffer.prototype.writeUInt16LE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 2, 0xffff, 0);
    }
    this._impl.writeUInt16LE(value, offset);
}

Buffer.prototype.writeInt16BE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 2, 0x7fff, -0x8000);
    }
    if (value < 0) value = 0xffff + value + 1;
    this._impl.writeInt16BE(value, offset);
}

Buffer.prototype.writeUInt16BE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 2, 0xffff, 0);
    }
    this._impl.writeUInt16BE(value, offset);
}

Buffer.prototype.writeInt32LE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000);
    }
    if (value < 0) value = 0xffffffff + value + 1;
    this._impl.writeInt32LE(value, offset);
}

Buffer.prototype.writeUInt32LE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 4, 0xffffffff, 0);
    }
    this._impl.writeUInt32LE(value, offset);
}

Buffer.prototype.writeInt32BE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000);
    }
    if (value < 0) value = 0xffffffff + value + 1;
    this._impl.writeInt32BE(value, offset);
}

Buffer.prototype.writeUInt32BE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkInt(this, value, offset, 4, 0xffffffff, 0);
    }
    this._impl.writeUInt32BE(value, offset);
}

Buffer.prototype.writeFloatLE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    this._impl.writeFloatLE(value, offset);
}

Buffer.prototype.writeFloatBE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 4, this._impl.capacity());
    }
    this._impl.writeFloatBE(value, offset);
}

Buffer.prototype.writeDoubleLE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 8, this._impl.capacity());
    }
    this._impl.writeDoubleLE(value, offset);
}

Buffer.prototype.writeDoubleBE = function(value, offset, noAssert) {
    if (!noAssert) {
        checkOffset(offset, 8, this._impl.capacity());
    }
    this._impl.writeDoubleBE(value, offset);
}

var INSPECT_MAX_BYTES = 50;

Buffer.prototype.inspect = function() {
    return this._impl.inspect(INSPECT_MAX_BYTES, this._slow ? true : false);
}

Buffer.isBuffer = function(obj) {
    return obj && obj._impl && (obj._impl instanceof JavaBuffer);
}

Buffer.byteLength = function(string, encoding) {
    return JavaBuffer.byteLength(string, Buffer._javaEncoding(encoding));
}

Buffer.isEncoding = function(encoding) {
    encoding = String(encoding).toLowerCase();
    return ['raw',
            'ascii',
            'binary',
            'base64',
            'hex',
            'utf8',
            'utf-8',
            'ucs2',
            'ucs-2',
            'utf16',
            'utf-16',
            'utf16le',
            'utf-16le',
            'utf16be',
            'utf-16be'
            ].indexOf(encoding) >= 0;
}

Object.defineProperty(Buffer, '_charsWritten', {
    enumerable : true,
    get: function() {
        return _writtenChars;
    },
    set: undefined,
    configurable: false
});

var _writtenChars = 0;

Buffer._javaEncoding = function(encoding) {
    encoding = encoding || 'utf8';
    if (typeof encoding !== 'string') {
        return "utf-8";
    }
    encoding = String(encoding).toLowerCase();
    if (!Buffer.isEncoding(encoding)) {
        throw new TypeError('Unknown encoding: ' + encoding);
    }
    switch (encoding) {
    case 'ascii':
        // Java ascii is 7-bit, use iso-8859-1 instead. Node.js need 8-bit.
        return 'iso-8859-1';
    case 'ucs2':
    case 'ucs-2':
        return 'utf-16le';
    }
    return encoding.replace(/^utf(\d+)(le|be)?$/, 'utf-$1$2'); // hyphenate
};

function checkOffset(offset, ext, length) {
  if ((offset % 1) !== 0 || offset < 0)
    throw new RangeError('offset is not uint');
  if (offset > java.lang.Integer.MAX_VALUE || offset + ext > length)
    throw new RangeError('Trying to access beyond buffer length');
}

function checkInt(buffer, value, offset, ext, max, min) {
  if ((value % 1) !== 0 || value > max || value < min)
    throw TypeError('value is out of bounds');
  if ((offset % 1) !== 0 || offset < 0)
    throw TypeError('offset is not uint');
  if (offset > java.lang.Integer.MAX_VALUE || offset + ext > buffer.length || buffer.length + offset < 0)
    throw RangeError('Trying to write outside buffer length');
}
