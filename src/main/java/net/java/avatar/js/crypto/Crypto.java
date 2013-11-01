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

package net.java.avatar.js.crypto;

import net.java.libuv.cb.Callback;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.buffer.Base64Decoder;
import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLContext;

/**
 *
 * Crypto Module java binding.
 */
public final class Crypto {

    private final EventLoop eventLoop;
    private final net.java.avatar.js.log.Logger LOG;
    private final SecureRandom sr = new SecureRandom();
    private final Random rand = new Random();

    private static final String OFB = "OFB";
    private static final String ECB = "ECB";
    private static final String CBC = "CBC";
    private static final String CFB = "CFB";
    private static final String CFB1 = "CFB1";
    private static final String CFB8 = "CFB8";
    private static final String BF = "Blowfish";
    private static final String DES = "DES";
    private static final String DES_EDE = "DESede";
    private static final String RC2 = "RC2";
    private static final String RC4 = "RC4";
    private static final String RC5 = "RC5";
    private static final String AES = "AES";
    private static final String PADDING = "PKCS5Padding";
    private static final String NO_PADDING = "NoPadding";
    private static final String MD5 = "MD5";
    private static final String MD2 = "MD2";
    private static final String SHA_1 = "SHA-1";
    private static final String SHA_224 = "SHA-224";// Since JDK 8
    private static final String SHA_256 = "SHA-256";
    private static final String SHA_384 = "SHA-384";
    private static final String SHA_512 = "SHA-512";
    private static final String SHA1 = "SHA1";
    private static final String SHA224 = "SHA224";// Since JDK 8
    private static final String SHA256 = "SHA256";
    private static final String SHA384 = "SHA384";
    private static final String SHA512 = "SHA512";
    private static final String HMAC = "Hmac";
    private static final String DSA = "DSA";
    private static final String RSA = "RSA";
    private static final String WITH = "with";

    private final Map<String, CipherJavaName> CIPHER_NAME_MAPPING = new HashMap<>();
    private final Map<String, String> DIGEST_NAME_MAPPING = new HashMap<>();
    private final Map<String, String> HMAC_NAME_MAPPING = new HashMap<>();
    private final Map<String, String> SIGN_NAME_MAPPING = new HashMap<>();

    public Crypto(final EventLoop eventLoop) {
        CIPHER_NAME_MAPPING.put("bf-cbc", new CipherJavaName(BF, CBC));
        CIPHER_NAME_MAPPING.put("bf", new CipherJavaName(BF, CBC));
        CIPHER_NAME_MAPPING.put("bf-cfb", new CipherJavaName(BF, CFB));
        CIPHER_NAME_MAPPING.put("bf-ecb", new CipherJavaName(BF, ECB));
        CIPHER_NAME_MAPPING.put("bf-ofb", new CipherJavaName(BF, OFB));

        CIPHER_NAME_MAPPING.put("des-cbc", new CipherJavaName(DES, CBC));
        CIPHER_NAME_MAPPING.put("des", new CipherJavaName(DES, CBC));
        CIPHER_NAME_MAPPING.put("des-cfb", new CipherJavaName(DES, CFB));
        CIPHER_NAME_MAPPING.put("des-ecb", new CipherJavaName(DES, ECB));
        CIPHER_NAME_MAPPING.put("des-ofb", new CipherJavaName(DES, OFB));

        CIPHER_NAME_MAPPING.put("des-ede-cbc", new CipherJavaName(DES_EDE, CBC));
        CIPHER_NAME_MAPPING.put("des-ede", new CipherJavaName(DES_EDE, ECB));
        CIPHER_NAME_MAPPING.put("des-ede-ecb", new CipherJavaName(DES_EDE, ECB));
        CIPHER_NAME_MAPPING.put("des-ede-cfb", new CipherJavaName(DES_EDE, CFB));
        CIPHER_NAME_MAPPING.put("des-ede-ofb", new CipherJavaName(DES_EDE, OFB));

        CIPHER_NAME_MAPPING.put("des-ede3-cbc", new CipherJavaName(DES_EDE, CBC));
        CIPHER_NAME_MAPPING.put("des-ede3", new CipherJavaName(DES_EDE, ECB));
        CIPHER_NAME_MAPPING.put("des-ede3-ecb", new CipherJavaName(DES_EDE, ECB));
        CIPHER_NAME_MAPPING.put("des-ede3-cfb", new CipherJavaName(DES_EDE, CFB));
        CIPHER_NAME_MAPPING.put("des-ede3-ofb", new CipherJavaName(DES_EDE, OFB));

        CIPHER_NAME_MAPPING.put("rc2-cbc", new CipherJavaName(RC2, CBC));
        CIPHER_NAME_MAPPING.put("rc2", new CipherJavaName(RC2, CBC));
        CIPHER_NAME_MAPPING.put("rc2-cfb", new CipherJavaName(RC2, CFB));
        CIPHER_NAME_MAPPING.put("rc2-ecb", new CipherJavaName(RC2, ECB));
        CIPHER_NAME_MAPPING.put("rc2-ofb", new CipherJavaName(RC2, OFB));

        CIPHER_NAME_MAPPING.put("rc2-64-cbc", new CipherJavaName(RC2, 64, CBC));
        CIPHER_NAME_MAPPING.put("rc2-40-cbc", new CipherJavaName(RC2, 40, CBC));

        CIPHER_NAME_MAPPING.put("rc4", new CipherJavaName(RC4));
        CIPHER_NAME_MAPPING.put("rc4-64", new CipherJavaName(RC4, 64));
        CIPHER_NAME_MAPPING.put("rc4-40", new CipherJavaName(RC4, 40));

        CIPHER_NAME_MAPPING.put("aes128", new CipherJavaName(AES, 128, CBC));
        CIPHER_NAME_MAPPING.put("aes192", new CipherJavaName(AES, 192, CBC));
        CIPHER_NAME_MAPPING.put("aes256", new CipherJavaName(AES, 256, CBC));

        CIPHER_NAME_MAPPING.put("aes-128", new CipherJavaName(AES, 128, CBC));
        CIPHER_NAME_MAPPING.put("aes-192", new CipherJavaName(AES, 192, CBC));
        CIPHER_NAME_MAPPING.put("aes-256", new CipherJavaName(AES, 256, CBC));

        CIPHER_NAME_MAPPING.put("aes-128-cbc", new CipherJavaName(AES, 128, CBC));
        CIPHER_NAME_MAPPING.put("aes-192-cbc", new CipherJavaName(AES, 192, CBC));
        CIPHER_NAME_MAPPING.put("aes-256-cbc", new CipherJavaName(AES, 256, CBC));

        CIPHER_NAME_MAPPING.put("aes-128-cfb", new CipherJavaName(AES, 128, CFB));
        CIPHER_NAME_MAPPING.put("aes-192-cfb", new CipherJavaName(AES, 192, CFB));
        CIPHER_NAME_MAPPING.put("aes-256-cfb", new CipherJavaName(AES, 256, CFB));

        CIPHER_NAME_MAPPING.put("aes-128-cfb8", new CipherJavaName(AES, 128, CFB8));
        CIPHER_NAME_MAPPING.put("aes-192-cfb8", new CipherJavaName(AES, 192, CFB8));
        CIPHER_NAME_MAPPING.put("aes-256-cfb8", new CipherJavaName(AES, 256, CFB8));

        CIPHER_NAME_MAPPING.put("aes-128-ecb", new CipherJavaName(AES, 128, ECB));
        CIPHER_NAME_MAPPING.put("aes-192-ecb", new CipherJavaName(AES, 192, ECB));
        CIPHER_NAME_MAPPING.put("aes-256-ecb", new CipherJavaName(AES, 256, ECB));

        CIPHER_NAME_MAPPING.put("aes-128-ofb", new CipherJavaName(AES, 128, OFB));
        CIPHER_NAME_MAPPING.put("aes-192-ofb", new CipherJavaName(AES, 192, OFB));
        CIPHER_NAME_MAPPING.put("aes-256-ofb", new CipherJavaName(AES, 256, OFB));

        /* NOT SUPPORTED, requires third party JCE
         desx               DESX algorithm.
         gost89             GOST 28147-89 in CFB mode (provided by ccgost engine)
         gost89-cnt        `GOST 28147-89 in CNT mode (provided by ccgost engine)
         idea-cbc           IDEA algorithm in CBC mode
         idea               same as idea-cbc
         idea-cfb           IDEA in CFB mode
         idea-ecb           IDEA in ECB mode
         idea-ofb           IDEA in OFB mode
         rc5 in all modes
         aes 128/192/256 in cfb1 mode
         */

        // START standard mapping but no implementation is Oracle JCE
        CIPHER_NAME_MAPPING.put("rc5-cbc", new CipherJavaName(RC5, CBC));
        CIPHER_NAME_MAPPING.put("rc5", new CipherJavaName(RC5, CBC));
        CIPHER_NAME_MAPPING.put("rc5-cfb", new CipherJavaName(RC5, CFB));
        CIPHER_NAME_MAPPING.put("rc5-ecb", new CipherJavaName(RC5, ECB));
        CIPHER_NAME_MAPPING.put("rc5-ofb", new CipherJavaName(RC5, OFB));

        CIPHER_NAME_MAPPING.put("aes-128-cfb1", new CipherJavaName(AES, 128, CFB1));
        CIPHER_NAME_MAPPING.put("aes-192-cfb1", new CipherJavaName(AES, 192, CFB1));
        CIPHER_NAME_MAPPING.put("aes-256-cfb1", new CipherJavaName(AES, 256, CFB1));
        // END standard mapping but no implementation is Oracle JCE

        DIGEST_NAME_MAPPING.put("md5", MD5);
        DIGEST_NAME_MAPPING.put("md2", MD2);
        DIGEST_NAME_MAPPING.put("sha1", SHA_1);
        DIGEST_NAME_MAPPING.put("sha224", SHA_224);
        DIGEST_NAME_MAPPING.put("sha256", SHA_256);
        DIGEST_NAME_MAPPING.put("sha384", SHA_384);
        DIGEST_NAME_MAPPING.put("sha512", SHA_512);

        HMAC_NAME_MAPPING.put("md5", HMAC + MD5);
        HMAC_NAME_MAPPING.put("sha1", HMAC + SHA1);
        HMAC_NAME_MAPPING.put("sha224", HMAC + SHA224);
        HMAC_NAME_MAPPING.put("sha256", HMAC + SHA256);
        HMAC_NAME_MAPPING.put("sha384", HMAC + SHA384);
        HMAC_NAME_MAPPING.put("sha512", HMAC + SHA512);

        /* NOT SUPPORTED
         -mdc2           to use the mdc2 message digest algorithm
         -ripemd160      to use the ripemd160 message digest algorithm
         -md4            to use the md4 message digest algorithm
         -sha            to use sha (sha-0) message digest algorithm
         */

        // dsa with SHA 1 is the legacy max 1024 bits key.
        // So key strength greater than 1024 can't be used.
        SIGN_NAME_MAPPING.put("dss1", SHA1 + WITH + DSA);
        SIGN_NAME_MAPPING.put("dsa-sha1", SHA1 + WITH + DSA);
        SIGN_NAME_MAPPING.put("dsa-sha224", SHA224 + WITH + DSA);
        SIGN_NAME_MAPPING.put("dsa-sha256", SHA256 + WITH + DSA);

        SIGN_NAME_MAPPING.put("rsa-md5", MD5 + WITH + RSA);
        SIGN_NAME_MAPPING.put("rsa-md2", MD2 + WITH + RSA);
        SIGN_NAME_MAPPING.put("rsa-sha1", SHA1 + WITH + RSA);
        SIGN_NAME_MAPPING.put("rsa-sha224", SHA224 + WITH + RSA);
        SIGN_NAME_MAPPING.put("rsa-sha256", SHA256 + WITH + RSA);
        SIGN_NAME_MAPPING.put("rsa-sha384", SHA256 + WITH + RSA);
        SIGN_NAME_MAPPING.put("rsa-sha512", SHA256 + WITH + RSA);

        this.eventLoop = eventLoop;
        this.LOG = eventLoop.logger("crypto");
    }

    private boolean isCertificate(final String pemContent) {
        assert pemContent != null && pemContent.length() > 0;
        final String[] lines = pemContent.split("\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("Invalid PEM content");
        }
        return lines[0].contains("BEGIN CERTIFICATE");
    }

    String removePEMHeaderAndFooter(final String pemContent) {
        assert pemContent != null && pemContent.length() > 0;
        final String[] lines = pemContent.split("\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("Invalid PEM content");
        }
        final StringBuilder filtered = new StringBuilder();
        for (int i = 1; i < lines.length - 1; i++) {
            filtered.append(lines[i]).append("\n");
        }
        return filtered.toString();
    }

    public Set<String> getCiphers() {
        return CIPHER_NAME_MAPPING.keySet();
    }

    public Set<String> getHashes() {
        HashSet<String> set = new HashSet<>();
        set.addAll(HMAC_NAME_MAPPING.keySet());
        set.addAll(DIGEST_NAME_MAPPING.keySet());
        return set;
    }

    public Set<String> getSSLCiphers() throws Exception {
        HashSet<String> set = new HashSet<>();
        set.addAll(Arrays.asList(SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites()));
        return set;
    }
    /**
     * A way to open the door to non supported name mapping.
     */
    public void addCipherNameMapping(final String openssl, final CipherJavaName jname) {
        CIPHER_NAME_MAPPING.put(openssl, jname);
    }

    public void addDigestNameMapping(final String openssl, final String jname) {
        DIGEST_NAME_MAPPING.put(openssl, jname);
    }

    public void addHmacNameMapping(final String openssl, final String jname) {
        HMAC_NAME_MAPPING.put(openssl, jname);
    }

    public void addSignNameMapping(final String openssl, final String jname) {
        SIGN_NAME_MAPPING.put(openssl, jname);
    }

    private boolean needsIV(final CipherJavaName jname) {
        return jname.getMode() != null && !jname.getMode().equals("ECB");
    }

    public SecureContext newSecureContext(final String name) throws Exception {
        return new SecureContext(this, name);
    }

    public final class CryptoSignature {

        private final List<String> updates = new ArrayList<>();
        private final String algo;

        private CryptoSignature(final String algo) {
            this.algo = algo;
        }

        public void update(final String content) {
            updates.add(content);
        }

        public Buffer sign(final String privKeyPEM) throws Exception {
            //First decode the base64 encoded key
            // It gets us the ASN.1 encoded key
            final byte[] encoded = Base64Decoder.decode(removePEMHeaderAndFooter(privKeyPEM));
            // Spec for private key

            final PKCS8EncodedKeySpec key = new PKCS8EncodedKeySpec(encoded);
            final KeyFactory kf = KeyFactory.getInstance(getSignAlgoPart(algo));
            final PrivateKey privKey = kf.generatePrivate(key);

            final Signature signature = Signature.getInstance(algo);
            signature.initSign(privKey);
            for (final String content : updates) {
                // There is no specified Data encoding. Using ascii.
                final Buffer b = new Buffer(content, "ascii");
                signature.update(b.array());
            }
            final byte[] signedContent = signature.sign();
            return new Buffer(signedContent);
        }

        /* publicKey is a PEM, X.509 encoded RSA public key, DSA public key,
         * or X.509 certificate.
         */
        public boolean verify(final String publicKey, final Buffer recSignature) throws Exception {
            final byte[] signatureBytes = recSignature.array();
            final boolean isCert = isCertificate(publicKey);

            //First decode the base64 encoded key
            // It gets us the ASN.1 encoded key
            final byte[] encoded = Base64Decoder.decode(removePEMHeaderAndFooter(publicKey));
            final Signature signature = Signature.getInstance(algo);
            if (isCert) {
                final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                final Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(encoded));
                signature.initVerify(cert);
            } else {
                // Spec for public key
                final X509EncodedKeySpec key = new X509EncodedKeySpec(encoded);
                final KeyFactory kf = KeyFactory.getInstance(getSignAlgoPart(algo));
                final PublicKey pubKey = kf.generatePublic(key);
                signature.initVerify(pubKey);
            }
            for (final String content : updates) {
                // There is no specified Data encoding. Using ascii.
                final Buffer b = new Buffer(content, "ascii");
                signature.update(b.array());
            }
            return signature.verify(signatureBytes);
        }
    }

    private String getSignAlgoPart(final String algo) {
        final int i = algo.indexOf(WITH);
        return algo.substring(i + WITH.length());
    }

    public abstract class CryptoCipher {

        private final CipherJavaName name;
        private boolean padding = true;
        private Cipher cipher;
        private final Buffer key;
        private final Buffer iv;
        private byte[] incomplete_base64;

        private CryptoCipher(final CipherJavaName name, final Buffer key, final Buffer iv) {
            this.name = name;
            this.key = key;
            this.iv = iv;
        }

        //https://github.com/joyent/node/issues/738
        private byte[] handleFinalBase64(final byte[] content) throws UnsupportedEncodingException {
            byte[] full = content;
            if (incomplete_base64 != null) {
                full = new byte[content.length + incomplete_base64.length];
                System.arraycopy(incomplete_base64, 0, full, 0, incomplete_base64.length);
                System.arraycopy(content, 0, full, incomplete_base64.length, content.length);
                incomplete_base64 = null;
            }
            return full;
        }

        //https://github.com/joyent/node/issues/738
        private byte[] handleUpdateBase64(final byte[] content) throws UnsupportedEncodingException {
            // Base64 encoding
            // Check to see if we need to add in previous base64 overhang
            byte[] full = content;
            if (incomplete_base64 != null) {
                full = new byte[content.length + incomplete_base64.length];
                System.arraycopy(incomplete_base64, 0, full, 0, incomplete_base64.length);
                System.arraycopy(content, 0, full, incomplete_base64.length, content.length);
                incomplete_base64 = null;
            }
            if ((full.length % 3) != 0) {
                incomplete_base64 = new byte[full.length % 3];
                System.arraycopy(content, full.length - incomplete_base64.length, incomplete_base64, 0, incomplete_base64.length);
                final byte[] trimedContent = new byte[full.length - incomplete_base64.length];
                System.arraycopy(full, 0, trimedContent, 0, trimedContent.length);
                full = trimedContent;
            }
            return full;
        }

        private Cipher getCipher() throws Exception {
            if (cipher == null) {
                cipher = createCipher();
            }
            return cipher;
        }

        protected abstract Cipher createCipher() throws Exception;

        public void setAutoPadding(final boolean padding) {
            this.padding = padding;
        }

        public CipherJavaName getName() {
            return name;
        }

        public boolean isAutoPadding() {
            return padding;
        }

        public void setCipher(final Cipher cipher) {
            this.cipher = cipher;
        }

        public Buffer getKey() {
            return key;
        }

        public Buffer getIV() {
            return iv;
        }
    }

    public final class Encrypt extends CryptoCipher {

        private Encrypt(final CipherJavaName name, final Buffer key, final Buffer iv) {
            super(name, key, iv);
        }

        @Override
        protected Cipher createCipher() throws Exception {
            return getIV() == null ?
                newEncryptCipher(getName(), getKey(), isAutoPadding()) :
                newEncryptCipher(getName(), getKey(), getIV(), isAutoPadding());
        }
    }

    public final class Decrypt extends CryptoCipher {

        private Decrypt(final CipherJavaName name, final Buffer key, final Buffer iv) {
            super(name, key, iv);
        }

        @Override
        protected Cipher createCipher() throws Exception {
            if (getKey().capacity() == 0) {
                throw new Exception("invalid public key");
            }
            return getIV() == null ?
                newDecryptCipher(getName(), getKey(), isAutoPadding()) :
                newDecryptCipher(getName(), getKey(), getIV(), isAutoPadding());
        }
    }

    public final class CipherJavaName {

        private final String name;
        private final String mode;
        private final int keySize;

        public CipherJavaName(final String name) {
            this(name, -1, null);
        }

        public CipherJavaName(final String name, final String mode) {
            this(name, -1, mode);
        }

        public CipherJavaName(final String name, final int keySize) {
            this(name, keySize, null);
        }

        public CipherJavaName(final String name, final int keySize, final String mode) {
            this.name = name;
            this.keySize = keySize;
            this.mode = mode;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the mode
         */
        public String getMode() {
            return mode;
        }

        /**
         * @return the keySize
         */
        public int getKeySize() {
            return keySize;
        }

        public String getTransformation(final boolean padding) {
            return getName() + (getMode() == null ? "" : "/" + getMode() + "/" + (padding ? PADDING : NO_PADDING));
        }
    }

    public final class DH {

        private final BigInteger g = BigInteger.valueOf(2);
        private final BigInteger p;
        private byte[] priv;
        private byte[] pub;

        private DH(final BigInteger p) {
            this.p = p;
        }

        private BigInteger getP() {
            return p;
        }

        private BigInteger getG() {
            return g;
        }

        private void setPrivKey(final byte[] priv) {
            this.priv = priv;
        }

        private void setPubKey(final byte[] pub) {
            this.pub = pub;
        }

        private byte[] getPubKey() {
            return pub;
        }

        private byte[] getPrivKey() {
            return priv;
        }
    }

    public void pbkdf2(final String password, final String salt,
            final int iteration, final int bytesLen, final Callback cb) {
        final Callable<Void> c = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final Buffer buff = pbkdf2(password, salt, iteration, bytesLen);
                eventLoop.post(new Event("crypto.pbkdf2", cb, null, buff));
                return null;
            }
        };
        submitToLoop(c, cb);
    }

    public Buffer pbkdf2(final String password, final String salt,
            final int iteration, final int bytesLen) throws Exception {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        final PBEKeySpec ks = new PBEKeySpec(password.toCharArray(),
                salt.getBytes(), iteration, bytesLen * 8);
        final SecretKey s = factory.generateSecret(ks);
        return new Buffer(s.getEncoded());
    }

    public DH getDHGroup(final String name) throws Exception {
        final BigInteger bi = DiffieHellman.getKnownGroup(name);
        return new DH(bi);
    }

    public DH createDH(final int numBits) throws Exception {
        final BigInteger bi = DiffieHellman.generateSafePrime(numBits);
        return new DH(bi);
    }

    public DH createDH(final Buffer b) throws Exception {
        final BigInteger bi = new BigInteger(1, b.array());
        DiffieHellman.checkPrime(bi);
        return new DH(bi);
    }

    public Buffer generateKeys(final DH dh) throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        final DHParameterSpec ps = new DHParameterSpec(dh.getP(), dh.getG());
        kpg.initialize(ps);
        final KeyPair keyPair = kpg.genKeyPair();
        final byte[] priv = keyPair.getPrivate().getEncoded();

        final byte[] pub = keyPair.getPublic().getEncoded();
        dh.setPrivKey(priv);
        dh.setPubKey(pub);
        return new Buffer(pub);
    }

    public Buffer getPrime(final DH dh) throws Exception {
        return new Buffer(dh.getP().toByteArray());
    }

    public Buffer getGenerator(final DH dh) throws Exception {
        return new Buffer(dh.getG().toByteArray());
    }

    public Buffer getPublicKey(final DH dh) throws Exception {
        if (dh.getPubKey() == null) {
            throw new IllegalArgumentException("No public Key");
        }
        return new Buffer(dh.getPubKey());
    }

    public Buffer getPrivateKey(final DH dh) throws Exception {
        if (dh.getPrivKey() == null) {
            throw new IllegalArgumentException("No private Key");
        }
        return new Buffer(dh.getPrivKey());
    }

    public void setPublicKey(final DH dh, final Buffer key) throws Exception {
        dh.setPubKey(key.array());
    }

    public void setPrivateKey(final DH dh, final Buffer key) throws Exception {
        dh.setPrivKey(key.array());
    }

    public Buffer computeSecret(final DH dh, final Buffer other_pubKey) throws Exception {
        final KeyAgreement ka = KeyAgreement.getInstance("DiffieHellman");
        final KeyFactory keyFactory = KeyFactory.getInstance("DH");
        final X509EncodedKeySpec ks = new X509EncodedKeySpec(other_pubKey.array());
        final PublicKey otherPubKey = keyFactory.generatePublic(ks);
        final PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(dh.getPrivKey());
        final PrivateKey privKey = keyFactory.generatePrivate(privSpec);
        ka.init(privKey);
        ka.doPhase(otherPubKey, true);
        final byte[] secret = ka.generateSecret();
        return new Buffer(secret);
    }

    public Buffer randomBytes(final int size) {
        checkSize(size);
        final byte[] buff = new byte[size];
        sr.nextBytes(buff);
        Buffer buffer = new Buffer(buff);
        return buffer;
    }

    public void randomBytes(final int size, final Callback cb) {
        checkSize(size);
        final Callable<Void> c = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final Buffer rand = randomBytes(size);
                eventLoop.post(new Event("crypto.randomBytes", cb, null, rand));
                return null;
            }
        };
        submitToLoop(c, cb);
    }

    public Buffer pseudoRandomBytes(final int size) {
        checkSize(size);
        final byte[] b = new byte[size];
        rand.nextBytes(b);
        final Buffer buffer = new Buffer(b);
        return buffer;
    }

    public void pseudoRandomBytes(final int size, final Callback cb) {
        checkSize(size);
        final Callable<Void> c = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final Buffer rand = pseudoRandomBytes(size);
                eventLoop.post(new Event("crypto.pseudoRandomBytes", cb, null, rand));
                return null;
            }
        };
        submitToLoop(c, cb);
    }

    public CryptoSignature newSignature(final String opensslName) {
        final String jname = getName(opensslName, SIGN_NAME_MAPPING);
        return new CryptoSignature(jname);
    }

    public void update(final CryptoSignature sign, final String data) {
        sign.update(data);
    }

    public Buffer sign(final CryptoSignature sign, final String privKey) throws Exception {
        return sign.sign(privKey);
    }

    public boolean verify(final CryptoSignature sign, final String pubKeyOrCert, final Buffer signature) throws Exception {
        return sign.verify(pubKeyOrCert, signature);
    }

    public Mac newHmac(final String opensslName, final Buffer key) throws Exception {
        final String jname = getName(opensslName, HMAC_NAME_MAPPING);
        if (jname == null) {
            throw new IllegalArgumentException("Unsupported algorithm " + opensslName);
        }
        final Mac mac = Mac.getInstance(jname);
        Buffer k = key;
        if (key.capacity() == 0) {
            k = new Buffer(1);
        }
        final SecretKeySpec keySpec = new SecretKeySpec(k.array(), jname);
        mac.init(keySpec);
        return mac;
    }

    public void update(final Mac digest, final Buffer content) throws Exception {
        digest.update(content.array());
    }

    public Buffer doFinal(final Mac digest) throws Exception {
        final byte[] ciphered = digest.doFinal();
        return new Buffer(ciphered);
    }

    public MessageDigest newMessageDigest(final String opensslName) throws NoSuchAlgorithmException {
        final String jname = getName(opensslName, DIGEST_NAME_MAPPING);
        if (jname == null) {
            throw new IllegalArgumentException("Unsupported algorithm " + opensslName);
        }
        final MessageDigest md = MessageDigest.getInstance(jname);
        return md;
    }

    public void update(final MessageDigest digest, final Buffer b) throws Exception {
        digest.update(b.array());
    }

    public Buffer digest(final MessageDigest digest) throws Exception {
        final byte[] ciphered = digest.digest();
        return new Buffer(ciphered);
    }

    public Buffer update(final CryptoCipher ccipher, final Buffer b) throws Exception {
        byte[] ciphered = ccipher.getCipher().update(b.array());
        return new Buffer(ciphered);
    }

    public Buffer doFinal(final CryptoCipher ccipher) throws Exception {
        byte[] ciphered = ccipher.getCipher().doFinal();
        return new Buffer(ciphered);
    }

    public CryptoCipher initEncrypt(final String opensslName, final Buffer key) {
        return new Encrypt(getName(opensslName, CIPHER_NAME_MAPPING), key, null);
    }

    public CryptoCipher initDecrypt(final String opensslName, final Buffer key) {
        return new Decrypt(getName(opensslName, CIPHER_NAME_MAPPING), key, null);
    }

    public CryptoCipher initivEncrypt(final String opensslName, final Buffer key, final Buffer iv) {
        return new Encrypt(getName(opensslName, CIPHER_NAME_MAPPING), key, iv);
    }

    public CryptoCipher initivDecrypt(final String opensslName, final Buffer key, final Buffer iv) {
        return new Decrypt(getName(opensslName, CIPHER_NAME_MAPPING), key, iv);
    }

    private Cipher newEncryptCipher(final CipherJavaName jname, final Buffer key, final Buffer iv, final boolean padding) throws Exception {
        return newCipher(Cipher.ENCRYPT_MODE, jname, key, iv, padding);
    }

    private Cipher newEncryptCipher(final CipherJavaName jname, final Buffer key, final boolean padding) throws Exception {
        return newCipher(Cipher.ENCRYPT_MODE, jname, key, padding);
    }

    private Cipher newDecryptCipher(final CipherJavaName jname, final Buffer key, final Buffer iv, final boolean padding) throws Exception {
        return newCipher(Cipher.DECRYPT_MODE, jname, key, iv, padding);
    }

    private Cipher newDecryptCipher(final CipherJavaName jname, final Buffer key, final boolean padding) throws Exception {
        return newCipher(Cipher.DECRYPT_MODE, jname, key, padding);
    }

    private <T> T getName(final String opensslName, final Map<String, T> map) {
        final T jname = map.get(opensslName.toLowerCase());
        if (jname == null) {
            throw new IllegalArgumentException("Unsupported algorithm " + opensslName);
        }
        return jname;
    }

    private Cipher newCipher(final int mode, final CipherJavaName jname, final Buffer password, final boolean padding) throws Exception {
        final Cipher cipher = Cipher.getInstance(jname.getTransformation(padding));
        final SecretKey key = passwordToKey(jname, password); // Compute deterministic key
        if (needsIV(jname)) {
            cipher.init(mode, key, passwordToIV(jname, password));
        } else {
            cipher.init(mode, key);
        }

        return cipher;
    }

    private SecretKey passwordToKey(final CipherJavaName jname, final Buffer password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        // min salt 8, 1000 iterations is safe.
        final PBEKeySpec ks = new PBEKeySpec(password.toStringContent().toCharArray(),
                new byte[8], 1000, getKeyLength(jname));
        final SecretKey s = factory.generateSecret(ks);
        final SecretKeySpec secretKeySpec = new SecretKeySpec(s.getEncoded(), jname.getName());
        return secretKeySpec;
    }

    private int getKeyLength(final CipherJavaName jname) throws NoSuchAlgorithmException {
        if (jname.getKeySize() != -1) {
            return jname.getKeySize();
        }
        final KeyGenerator keygenerator = KeyGenerator.getInstance(jname.getName());
        return keygenerator.generateKey().getEncoded().length * 8;
    }

    private IvParameterSpec passwordToIV(final CipherJavaName jname, final Buffer password) {
        final byte[] iv = new byte[getIVLength(jname)];
        final byte[] passArray = password.array();
        final int length = Math.min(passArray.length, iv.length);
        System.arraycopy(passArray, 0, iv, 0, length);
        final IvParameterSpec ivp = new IvParameterSpec(iv);
        return ivp;
    }

    private int getIVLength(final CipherJavaName jname) {
        int length = 8; //BF, DES/DES-EDE/RC2/RC5
        if (AES.equals(jname.getName())) {
            length = 16;
        }

        return length;
    }

    private Cipher newCipher(final int mode, final CipherJavaName jname, final Buffer key, final Buffer iv, final boolean padding) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        final Cipher cipher = Cipher.getInstance(jname.getTransformation(padding));
        final SecretKeySpec keySpec = new SecretKeySpec(key.array(), jname.getName());
        if (needsIV(jname) && // No IV for ECB
                iv != null && iv.capacity() > 0) {
            final IvParameterSpec ivp = new IvParameterSpec(iv.array());
            cipher.init(mode, keySpec, ivp);
        } else {
            cipher.init(mode, keySpec);
        }
        return cipher;
    }

    private void checkSize(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid size " + size);
        }
    }

    private void submitToLoop(final Callable<?> callable, final Callback cb) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (final Exception e) {
                    if (LOG.enabled()) {
                        LOG.log(e.getMessage());
                        //e.printStackTrace();
                    }
                    eventLoop.post(new Event("crypto.error", cb, e, null));
                } finally {
                    handle.close();
                }
            }
        });
    }
}
