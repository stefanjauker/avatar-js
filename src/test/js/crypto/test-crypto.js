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

var crypto = require("crypto");
var assert = require('assert');
var process = require('process');
var fs = require('fs');
var rand = new java.util.Random();
var hexutils = Packages.net.java.avatar.js.buffer.HexUtils;

// DH START
var alice = crypto.getDiffieHellman('modp5');
var bob = crypto.getDiffieHellman('modp5');

alice.generateKeys();
bob.generateKeys();

var alice_secret = alice.computeSecret(bob.getPublicKey(), 'binary', 'hex');
var bob_secret = bob.computeSecret(alice.getPublicKey(), 'binary', 'hex');

/* alice_secret and bob_secret should be the same */
assert.equal(alice_secret, bob_secret);

// DH END

// CIPHER
function testCipher(key, algo) {
    // Test encryption and decryption
    var plaintext = 'Keep this a secret? No! Tell everyone about node.js!';
    var cipher = crypto.createCipher(algo, key);

    // encrypt plaintext which is in utf8 format
    // to a ciphertext which will be in hex
    var ciph = cipher.update(plaintext, 'utf8', 'hex');
    // Only use binary or hex, not base64.
    ciph += cipher.final('hex');

    var decipher = crypto.createDecipher(algo, key);
    var txt = decipher.update(ciph, 'hex', 'utf8');
    txt += decipher.final('utf8');

    assert.equal(txt, plaintext, 'encryption and decryption');
}

function testCipherIV(key, iv, algo) {
    // Test encryption and decryption
    var plaintext = 'Keep this a secret? No! Tell everyone about node.js!';
    var cipher = crypto.createCipheriv(algo, key, iv);

    // encrypt plaintext which is in utf8 format
    // to a ciphertext which will be in hex
    var ciph = cipher.update(plaintext, 'utf8', 'hex');
    // Only use binary or hex, not base64.
    ciph += cipher.final('hex');

    var decipher = crypto.createDecipheriv(algo, key, iv);
    var txt = decipher.update(ciph, 'hex', 'utf8');
    txt += decipher.final('utf8');

    assert.equal(txt, plaintext, 'encryption and decryption');
}

var algos = [
["bf-cbc",64],
["bf",64],
["bf-cfb",64],
["bf-ecb",64],
["bf-ofb",64],

["des-cbc",64],
["des",64],
["des-cfb",64],
["des-ecb",64],
["des-ofb",64],

// This is 2 sub keys should be 128, but
// JCE provider requires 192
["des-ede-cbc",192],
["des-ede",192],
["des-ede-ecb",192],
["des-ede-cfb",192],
["des-ede-ofb",192],

["des-ede3-cbc",192],
["des-ede3",192],
["des-ede3-ecb",192],
["des-ede3-cfb",192],
["des-ede3-ofb",192],

["rc2-cbc",128],
["rc2",128],
["rc2-cfb",128],
["rc2-ecb",128],
["rc2-ofb",128],

["rc2-64-cbc",64],
["rc2-40-cbc",40],

["rc4",128],
["rc4-64",64],
["rc4-40",40],

["aes128",128,16],
["aes192",192,16],
["aes256",256,16],

["aes-128",128,16],
["aes-192",192,16],
["aes-256",256,16],

["aes-128-cbc",128,16],
["aes-192-cbc",192,16],
["aes-256-cbc",256,16],

["aes-128-cfb",128,16],
["aes-192-cfb",192,16],
["aes-256-cfb",256,16],

["aes-128-cfb8",128,16],
["aes-192-cfb8",192,16],
["aes-256-cfb8",256,16],

["aes-128-ecb",128,16],
["aes-192-ecb",192,16],
["aes-256-ecb",256,16],

["aes-128-ofb",128,16],
["aes-192-ofb",192,16],
["aes-256-ofb",256,16]];

for(var i = 0; i < algos.length; i++){
    doTest(algos[i]);
}

function log(txt) {
    console.log(txt);
}

function doTest(algo){
    log("Algo " + algo[0] + ", key length " + (algo[1] / 8));

    //password based
    var buf = new Buffer('01234567890abcdefghijklmnopqrstuvwxyz');
    testCipher(buf.toString('hex'), algo[0]);

    //key based
    var key = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, (algo[1] / 8));
    rand.nextBytes(key);
    var str = hexutils.encode(key);
    var iv = '12345678';
    if(algo[2]){ // special initialization vector
        var bytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, (algo[2]));
        rand.nextBytes(key);
        iv = hexutils.encode(bytes);
        iv = new Buffer(iv, 'hex');
    }
    testCipherIV(new Buffer(str, 'hex'), iv, algo[0]);
}

//Hash
function testHashDigest(algo){
    var data = "some data to hash";
    var hasher = crypto.createHash(algo);
    hasher.update(data);
    var digest = hasher.digest('hex');
    log("Algo " + algo + " " + digest);
}

//HashMac
function testHashMacDigest(algo){
    var data = "some data to hash";
    var hasher = crypto.createHmac(algo, "key");
    hasher.update(data);
    var digest = hasher.digest('hex');
    log("Algo " + algo + " " + digest);
}

var hashAlgos = ["md5","md2","sha1","sha224","sha256","sha384","sha512"];
var hmacAlgos = ["md5","sha1","sha224","sha256","sha384","sha512"];

for(var i = 0; i < hashAlgos.length; i++){
    testHashDigest(hashAlgos[i]);
}

for(var i = 0; i < hmacAlgos.length; i++){
    testHashMacDigest(hmacAlgos[i]);
}


// Sign / verify
var dsaprivateKey = fs.readFileSync('crypto/fixtures/test_dsa_privkey.pem');
var dsapublicKey = fs.readFileSync('crypto/fixtures/test_dsa_pubkey.pem');
var dsa_sign_algos = ["dss1","dsa-sha1","dsa-sha224","dsa-sha256"];
//"dsa-md5","dsa-md2","dsa-sha384","dsa-sha512"
for(var i = 0; i < dsa_sign_algos.length; i++){
    testSignVerify(dsa_sign_algos[i], dsaprivateKey, dsapublicKey);
}

var rsaprivateKey = fs.readFileSync('crypto/fixtures/test_rsa_privkey_2.pem');
var rsapublicKey = fs.readFileSync('crypto/fixtures/test_rsa_pubkey_2.pem');
var rsa_sign_algos = ["rsa-md5","rsa-md2","rsa-sha1","rsa-sha224","rsa-sha256",
    "rsa-sha384", "rsa-sha512"];

for(var i = 0; i < rsa_sign_algos.length; i++){
    testSignVerify(rsa_sign_algos[i], rsaprivateKey, rsapublicKey);
}

function testSignVerify(algo, privateKey, publicKey){
var rsaSign = crypto.createSign(algo);
var rsaVerify = crypto.createVerify(algo);
var data = "some content";
rsaSign.update(data);
var rsaSignature = rsaSign.sign(privateKey, 'hex');
rsaVerify.update(data);
assert.strictEqual(rsaVerify.verify(publicKey, rsaSignature, 'hex'), true);
log(algo);
}

var argv = process.argv;
if(argv[2] === '-unsupported'){
    log("WARNING, running tests for unsupported algorithms");
    // For some Algorithms we don't have a Java standard name.
    // Use the pluggability
    var CryptoBinding = Packages.com.oracle.node.crypto.Crypto;
    var CipherName = CryptoBinding.CipherJavaName;
    var GOST89 = "GOST28147";
    var IDEA = "IDEA";
    var CBC = "CBC";
    var CFB = "CFB";
    var CNT = "CNT";
    var ECB = "ECB";
    var OFB = "OFB";
    // Didn't find a provider to test this desx one.
    // seems very exotic
    //CryptoBinding.addCipherNameMapping("desx", new CipherName("DESX", CBC));
    CryptoBinding.addCipherNameMapping("gost89", new CipherName(GOST89, CFB));
    CryptoBinding.addCipherNameMapping("gost89-cnt", new CipherName(GOST89, CNT));
    CryptoBinding.addCipherNameMapping("idea-cbc", new CipherName(IDEA, CBC));
    CryptoBinding.addCipherNameMapping("idea", new CipherName(IDEA, CBC));
    CryptoBinding.addCipherNameMapping("idea-cfb", new CipherName(IDEA, CFB));
    CryptoBinding.addCipherNameMapping("idea-ecb", new CipherName(IDEA, ECB));
    CryptoBinding.addCipherNameMapping("idea-ofb", new CipherName(IDEA, OFB));

    // requires third party Provider(s) installed
    var unsupported_algos = [
    ["rc5-cbc",128],
    ["rc5",128],
    ["rc5-cfb",128],
    ["rc5-ecb",128],
    ["rc5-ofb",128],

    ["gost89",256],

    ["idea-cbc",128],
    ["idea",128],
    ["idea-cfb",128],
    ["idea-ecb",128],
    ["idea-ofb",128],

    // These ones are failing with the third party provider I used
    //["aes-128-cfb1",128,16],
    //["aes-192-cfb1",192,16],
    //["aes-256-cfb1",256,16],
    //["desx",192],
    //["gost89-cnt",256]
    ];

    for(var i = 0; i < unsupported_algos.length; i++){
        doTest(unsupported_algos[i]);
    }

    CryptoBinding.addDigestNameMapping("mdc2", "mdc2");
    CryptoBinding.addDigestNameMapping("md4", "md4");
    CryptoBinding.addDigestNameMapping("ripemd160", "ripemd160");
    var unsupported_hash = [
        "md4",
        "ripemd160",
        // These ones are failing with the third party provider I used
        //"mdc2"
    ];

    for(var i = 0; i < unsupported_hash.length; i++){
        testHashDigest(unsupported_hash[i]);
    }
    CryptoBinding.addHmacNameMapping("md2", "HmacMD2");
    CryptoBinding.addHmacNameMapping("md4", "HmacMD4");
    CryptoBinding.addHmacNameMapping("ripemd160", "Hmacripemd160");
    CryptoBinding.addHmacNameMapping("mdc2", "Hmacmdc2");
    var unsupported_hashmac = [
        "md2",
         "md4",
        "ripemd160",
        // These ones are failing with the third party provider I used
        //"mdc2"
    ];
     for(var i = 0; i < unsupported_hashmac.length; i++){
        testHashMacDigest(unsupported_hashmac[i]);
    }

    //DSA
    CryptoBinding.addSignNameMapping("dsa-md5", "md5withdsa");
    CryptoBinding.addSignNameMapping("dsa-md2", "md2withdsa");
    CryptoBinding.addSignNameMapping("dsa-md4", "md4withdsa");
    CryptoBinding.addSignNameMapping("dsa-ripemd160", "ripemd160withdsa");
    CryptoBinding.addSignNameMapping("dsa-sha384", "sha384withdsa");
    CryptoBinding.addSignNameMapping("dsa-sha512", "sha512withdsa");
    var unsupported_dsa_sign_algos = [
        "dsa-sha384",
        "dsa-sha512",

        // These ones are failing with the third party provider I used
        //"dsa-ripemd160",
        //"dsa-md4",
        //"dsa-md5",
        //"dsa-md2"
        ];
    for(var i = 0; i < unsupported_dsa_sign_algos.length; i++){
        testSignVerify(unsupported_dsa_sign_algos[i], dsaprivateKey, dsapublicKey);
    }

    //RSA
    CryptoBinding.addSignNameMapping("rsa-md4", "md4withrsa");
    CryptoBinding.addSignNameMapping("rsa-ripemd160", "ripemd160withrsa");
    var unsupported_rsa_sign_algos = [
        "rsa-md4",
        "rsa-ripemd160"
    ];
    for(var i = 0; i < unsupported_rsa_sign_algos.length; i++){
        testSignVerify(unsupported_rsa_sign_algos[i], rsaprivateKey, rsapublicKey);
    }
}

