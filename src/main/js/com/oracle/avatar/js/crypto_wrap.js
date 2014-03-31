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

    var kMaxLength = 0x3fffffff;
    var jBinding = new Packages.com.oracle.avatar.js.crypto.Crypto(__avatar.eventloop);
    var defaultEncoding = "binary";

    function toArray(set) {
        var ret = [];
        var it = set.iterator();
        while(it.hasNext()) {
            ret.push(it.next());
        }
        return ret;
    }

    function getCiphers() {
        var cipherSet = jBinding.getCiphers();
        return toArray(cipherSet);
    }

    function getHashes() {
        var hashSet = jBinding.getHashes();
        return toArray(hashSet);
    }

    function getSSLCiphers() {
        var set = jBinding.getSSLCiphers();
        return toArray(set);
    }

    exports.getCiphers = getCiphers;
    exports.getHashes = getHashes;
    exports.getSSLCiphers = getSSLCiphers;

    function toBuffer(obj, encoding) {
        var buff = obj;
        var enc = defaultEncoding;
        if (encoding) {
            enc = encoding;
        }
        if (typeof obj === 'string') {
            if (enc === 'buffer') {// provide a string with no encoding
                enc = undefined;
            }
            buff = new Buffer(obj, enc);
        } else {
            if (!Buffer.isBuffer(obj)) {
                throw new Error("Invalid format, must be string or buffer");
            }
        }
        return buff;
    }

    function toBufferorString(buffer, encoding) {
        if (!encoding || encoding === 'buffer') {
            return buffer;
        }
        return buffer.toString(encoding);
    }

    //makes sense only with TLS
    function SecureContext() {
        var that = this;

        // extension, required for SSL session
        this.setAddress = function(host, port) {
            that.peer.setAddress(host, port);
        };

        this.init = function() {
            var secureProtocol = null;
            if (arguments.length === 1) {
                secureProtocol = arguments[0];
            }
            that.peer = jBinding.newSecureContext(secureProtocol);
        };

        this.setCert = function(pemCertString) {
            that.peer.setPemCertificate(pemCertString);
        };

        this.setCiphers = function(ciphersString) {
            that.peer.setCipherSuites(ciphersString);
        };

        this.addCACert = function(pemCertificate) {
            that.peer.addTrustedPemCertificate(pemCertificate);
        };

        this.addRootCerts = function() {
            /// XXX TODO
            /*
             *  If no 'ca' details are given, then use the default
             *  publicly trusted list of CAs as given in
                http://mxr.mozilla.org/mozilla/source/security/nss/lib/ckfw/builtins/certdata.txt.
             */
        };

        this.addCRL = function(pemCRL) {
            that.peer.addPemCRL(pemCRL);
        };

        this.setSessionIdContext = function(sessionId) {
            that.peer.setSessionId(sessionId);
        };

        this.loadPKCS12 = function(pfx) {
            var buffer = toBuffer(pfx);
            var passPhrase = null;
            if (arguments.length === 2) {
                passPhrase = arguments[1];
            }
            try {
                that.peer.loadPKCS12(buffer._impl, passPhrase);
            } catch (e) {
                // Expected by test
                throw new Error(e.getMessage() + ' mac verify failure');
            }
        };

        this.setOptions = function(secureOptions) {
            that.peer.setOptions(secureOptions);
        };

        this.setKey = function() {
            var pass = null;
            if (arguments.length === 2) {
                pass = arguments[1];
            }
            that.peer.setKey(arguments[0], pass);
        };
    }

    function Sign(algo) {
        var that = this;
        this.init = function(algo) {
            this.peer = jBinding.newSignature(algo);
            return that;
        };

        this.update = function(content) {
            jBinding.update(that.peer, content);
            return that;
        };

        this.sign = function(privKey) {
            return new Buffer(jBinding.sign(that.peer, privKey.toString()));
        };
    }

    function Verify(algo) {
        var that = this;
        this.init = function(algo) {
            this.peer = jBinding.newSignature(algo);
            return that;
        };

        this.update = function(content) {
            jBinding.update(that.peer, content);
            return that;
        };

        this.verify = function(pubKey, signature) {
            var ret = false;
            try {
                ret = jBinding.verify(that.peer, pubKey.toString(), signature._impl);
            } catch(e) {
                // exception, returns false
            }
            return ret;
        };
    }

    function Hmac() {
        var that = this;
        this.init = function(algo, key) {
            this.peer = jBinding.newHmac(algo, key._impl);
            return that;
        };

        this.update = function(content, input_encoding) {
            if (!input_encoding) {
                input_encoding = defaultEncoding;
            }
            var contentBuffer = toBuffer(content, input_encoding);
            jBinding.update(that.peer, contentBuffer._impl);
            return that;
        };

        this.digest = function(encoding) {
            return toBufferorString(new Buffer(jBinding.doFinal(that.peer)), encoding);
        };
    }

    function Hash(algo) {
        var that = this;
        this.peer = jBinding.newMessageDigest(algo);
        this.update = function(content, input_encoding) {
            var buff = toBuffer(content, input_encoding);
            jBinding.update(that.peer, buff._impl);
            return that;
        };

        this.digest = function(encoding) {
            return toBufferorString(new Buffer(jBinding.digest(that.peer)), encoding);
        };
    }

    function Cipher() {
        var that = this;
        this.initiv = function(cipher, key, iv) {
            var buffKey = toBuffer(key);
            var buffParam = toBuffer(iv);
            that.peer = jBinding.initivEncrypt(cipher, buffKey._impl, buffParam._impl);
            return that;
        };

        this.init = function(cipher, key) {
            var buffKey = toBuffer(key);
            that.peer = jBinding.initEncrypt(cipher, buffKey._impl);
            return that;
        };

        this.update = function(content, input_encoding) {
            var buff = toBuffer(content, input_encoding);
            return new Buffer(jBinding.update(that.peer, buff._impl));
        };

        this.final = function(encoding) {
            return toBufferorString(new Buffer(jBinding.doFinal(that.peer), encoding));
        };

        this.setAutoPadding = function(pad) {
            that.peer.setAutoPadding(pad);
        };
    }

    function Decipher() {
        var that = this;
        this.initiv = function(cipher, key, iv) {
            var buffKey = toBuffer(key);
            var buffParam = toBuffer(iv);
            that.peer = jBinding.initivDecrypt(cipher, buffKey._impl, buffParam._impl);
            return that;
        };

        this.init = function(cipher, key) {
            var buffKey = toBuffer(key);
            that.peer = jBinding.initDecrypt(cipher, buffKey._impl);
            return that;
        };

        this.update = function(content, input_encoding) {
            var buff = toBuffer(content, input_encoding);
            return new Buffer(jBinding.update(that.peer, buff._impl));
        };

        this.final = function(encoding) {
            return toBufferorString(new Buffer(jBinding.doFinal(that.peer)), encoding);
        };

        this.setAutoPadding = function(pad) {
            that.peer.setAutoPadding(pad);
        };
    }

    function DH(peer) {
        this.peer = peer;
        this.generateKeys = function() {
            return new Buffer(jBinding.generateKeys(this.peer));
        };

        this.computeSecret = function(other_public_key) {
            return new Buffer(jBinding.computeSecret(this.peer, other_public_key._impl));
        };

        this.getPrime = function() {
            return new Buffer(jBinding.getPrime(this.peer));
        };

        this.getGenerator = function() {
            return new Buffer(jBinding.getGenerator(this.peer));
        };

        this.getPublicKey = function() {
            return new Buffer(jBinding.getPublicKey(this.peer));
        };

        this.getPrivateKey = function() {
            return new Buffer(jBinding.getPrivateKey(this.peer));
        };
    }

    function DiffieHellmanGroup(name) {
        var peer = jBinding.getDHGroup(name);
        return new DH(peer);
    }

    function DiffieHellman(size_or_key) {
        var isBuffer = Buffer.isBuffer(size_or_key);
        if (!size_or_key ||
            (!isBuffer && typeof size_or_key !== 'number')) {
            throw new Error("Invalid prime or size");
        }
        var peer;
        if (isBuffer) {
            peer = jBinding.createDH(size_or_key._impl);
        } else {
            peer = jBinding.createDH(size_or_key);
        }

        var dh = new DH(peer);

        dh.setPublicKey = function(public_key) {
            jBinding.setPublicKey(dh.peer, public_key._impl);
        };

        dh.setPrivateKey = function(private_key) {
            jBinding.setPrivateKey(dh.peer, private_key._impl);
        };

        return dh;
    }

    function PBKDF2(password, salt, iterations, keylen, callback) {
        if (!password || !salt || !iterations || !keylen) {
            throw new Error("Invalid parameter");
        }
        if (callback) {
            var cb = function(name, args) {
                var ex = args[0];
                var buf = args[1];
                callback(ex, new Buffer(buf));
            };
            jBinding.pbkdf2(password, salt, iterations, keylen, cb);
        } else {
            return new Buffer(jBinding.pbkdf2(password, salt, iterations, keylen));
        }
    }

    function randomBytes(size, callback) {
        checkSize(size);
        var buffer;
        if (callback) {
            var cb = function(name, args) {
                var ex = args[0];
                var buf = new Buffer(args[1]);
                callback(ex, buf);
            };
            jBinding.randomBytes(size, cb);
        } else {
            buffer = jBinding.randomBytes(size);
            return new Buffer(buffer);
        }
    }

    function pseudoRandomBytes(size, callback) {
        checkSize(size);
        var buffer;
        if (callback) {
            var cb = function(name, args) {
                var ex = args[0];
                var buf = new Buffer(args[1]);
                callback(ex, buf);
            };
            jBinding.pseudoRandomBytes(size, cb);
        } else {
            buffer = jBinding.pseudoRandomBytes(size);
            return new Buffer(buffer);
        }
    }

    exports.SecureContext = SecureContext;
    exports.Hmac = Hmac;
    exports.Hash = Hash;
    exports.Cipher = Cipher;
    exports.Decipher = Decipher;
    exports.Sign = Sign;
    exports.Verify    = Verify;
    exports.DiffieHellman = DiffieHellman;
    exports.DiffieHellmanGroup = DiffieHellmanGroup;
    exports.PBKDF2 = PBKDF2;
    exports.randomBytes = randomBytes;
    exports.pseudoRandomBytes = pseudoRandomBytes;

    function checkSize(size) {
        if ((typeof size === "number")) {
            if (size >= 0) {
                if (size > kMaxLength) {
                    throw new TypeError("Invalid size");
                }
                return true;
            }
        }
        // All other cases, throw Exception;
        throw new RangeError("Invalide size " + size);
    }


    // TLS support
    // reqCert means client authentication, server only
    // serverName is for client side
    // callbacks: onhandshakestart, onhandshakedone,
    // properties: handshakes, timer
    function Connection(secureContext, isServer, reqCertOrServerName, rejectUnauthorized) {
        var that = this;
        var connection = Packages.com.oracle.avatar.js.crypto.SecureConnection;
        if (!isServer && !reqCertOrServerName) { // null String for SNI server name.
            reqCertOrServerName = null;
        }

        var renegoStart = function() {
            if (that.onhandshakestart) {
                that.onhandshakestart();
            }
        };
        this.peer = new connection(__avatar.eventloop,
                    secureContext.peer, reqCertOrServerName, rejectUnauthorized, renegoStart);
        Object.defineProperty(this, 'peer',  { writable: false,  enumerable: false });
        this.setSNICallback = function(SNICallback) {
            var cb = function(name, args) {
                 var serverName = args[0];
                 var retCb = args[1];
                 var secureContext = SNICallback(serverName);
                 var ctx = null;
                 if (secureContext) {
                     ctx = secureContext.peer;
                 }
                 var arr = java.lang.reflect.Array.newInstance(java.lang.Object.class, 1);
                 java.lang.reflect.Array.set(arr, 0, ctx);
                 retCb.call("", arr);
             };
             that.peer.setSNICallback(cb);
        };

        this.setNPNProtocols = function(NPNProtocols) {
            print("NPN Protocols not supported");
        };

        this.getServername = function() {
             return that.peer.getServerName();
        };

        this.getCurrentCipher = function() {
            return that.peer.getCipherSuite();
        };

        this.start = function() {
            if (this.onhandshakestart) {
                this.onhandshakestart();
            }
            that.peer.start();
        };

        this.close = function() {
            that.peer.close();
        };

        this.clearOut = function(pool, offset, length) {
             return that.peer.clearOut(pool._impl, offset, length);
        };

        this.getPeerCertificate = function() {
            var out = {};
            var jcert = that.peer.getPeerCertificate();
            if (jcert !== null) {
                var alt = jcert.alternateNames;
                if (alt === null) {
                    alt = undefined;
                }
                out = {issuer: jcert.issuer,
                       subject: jcert.subject,
                       notBefore: jcert.notBefore,
                       notAfter: jcert.notAfter,
                       signature: new Buffer(jcert.signature),
                       version: jcert.version,
                       tbsCertificate: new Buffer(jcert.TBSCertificate),
                       subjectUniqueID: jcert.subjectUniqueID,
                       sigAlgParams: new Buffer(jcert.sigAlgParams),
                       sigAlgOID: jcert.sigAlgOID,
                       sigAlgName: jcert.sigAlgName,
                       serialNumber: jcert.serialNumber,
                       keyUsage: jcert.keyUsage,
                       issuerUniqueID: jcert.issuerUniqueID,
                       extendedKeyUsage: Java.from(jcert.extendedKeyUsage),
                       ext_key_usage: Java.from(jcert.extendedKeyUsage),
                       basicConstraints: jcert.basicConstraints,
                       subjectaltname: jcert.subjectaltname,
                       modulus: jcert.modulus,
                       exponent: jcert.exponent,
                       checkValidity: function() {
                           return jcert.checkValidity();
                       }
                };
            }
            return out;
        };

        this.verifyError = function() {
            var err;
            try {
                that.peer.verifyError();
            } catch (ex) {
                err = new Error(ex.getMessage());
            }
            return err;
        };

        Object.defineProperty(this, 'error', {
            enumerable: true,
            get: function() {
                return that.peer.getError();
            },
            set: function(value) { // only null
                return that.peer.resetError();
            }
        });

        this.isInitFinished = function() {
             var finished = that.peer.isHandshakeFinished();
             if (!this.hsDoneCalled && finished && this.onhandshakedone) {
                 this.hsDoneCalled = true;
                 this.onhandshakedone();
             }
             return finished;
        };

        this.setSession = function(session) {
             that.peer.setSession(session);
        };

        this.getSession = function() {
             return that.peer.getSession();
        };

        this.isSessionReused = function() {
            return that.peer.isSessionReused();
        };

        this.getNegotiatedProtocol = function() {
            print("getNegotiatedProtocol not yet implemented");
        };

        this.clearIn = function(pool, offset, length) {
             return that.peer.clearIn(pool._impl, offset, length);
        };

        this.clearPending = function() {
             return that.peer.clearPending();
        };

        this.shutdown = function() {
            that.peer.shutdown();
        };

        this.encOut = function(pool, offset, length) {
             return that.peer.encOut(pool._impl, offset, length);
        };

        this.encIn = function(pool, offset, length) {
             return that.peer.encIn(pool._impl, offset, length);
        };

        this.encPending = function() {
             return that.peer.encPending();
        };
    }

    exports.Connection = Connection;

});
