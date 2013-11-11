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

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import net.java.avatar.js.buffer.Base64Decoder;
import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.eventloop.Callback;
import net.java.avatar.js.log.Logger;
import java.util.Random;

/**
 * Instance of this class contains the information required to build an SSLContext.
 * The same instance can be shared.
 * On the server side, any connection share the same context.
 * On the client side, tls API allows to share the context between connection.
 */
public final class SecureContext {

    private static final String SSLV3 = "SSLv3";
    private static final String SSLV2 = "SSLv2";
    private static final String TLSV1 = "TLSv1";
    private static final String TLSV11 = "TLSv1.1";
    private static final String TLSV12 = "TLSv1.2";

    private static final Map<String, String[]> JAVA_PROTOCOLS = new HashMap<>();
    private static final Map<String, String> JAVA_CIPHER_SUITES = new HashMap<>();

    private Callback sniCallback;
    private Random random = new Random();
    private static String[] toJavaProtocols(final String secureProtocol) {
        if (secureProtocol == null) {
            return null;
        }
        String[] protocols = JAVA_PROTOCOLS.get(secureProtocol);
        if (protocols == null) {
            protocols = new String[]{secureProtocol};
        }
        return protocols;
    }

    private static String toJavaCipherSuite(final String name) {
        // XXX jfdenise, to be completed
        String jname = JAVA_CIPHER_SUITES.get(name);
        if (jname == null) {
            jname = name;
        }
        return jname;
    }

    static String toNodeCipherName(final String jname) {
        String ret = jname;
        for (final Entry<String, String> entry : JAVA_CIPHER_SUITES.entrySet()) {
            if (entry.getValue().equals(jname)) {
                ret = entry.getKey();
                break;
            }
        }
        return ret;
    }

    void setSNICallback(Callback sniCallback) {
        this.sniCallback = sniCallback;
    }

    /**
     * Wrapper required to handle SNI Certificate/hostname association
     */
    private class KeyManagerWrapper extends X509ExtendedKeyManager {

        private X509ExtendedKeyManager wrapped;
        private Map<String, PrivateKey> sniPrivateKeys = new HashMap<>();
        private Map<String, X509Certificate[]> sniCertificates = new HashMap<>();

        public KeyManagerWrapper(KeyManager[] keyManagers) {
            for (KeyManager km : keyManagers) {
                if (km instanceof X509ExtendedKeyManager) {
                    wrapped = (X509ExtendedKeyManager) km;
                    break;
                }
            }

            if (wrapped == null) {
                throw new IllegalArgumentException("Wrapped can't be null");
            }
        }

        @Override
        public String[] getClientAliases(String alias, Principal[] prncpls) {
            LOG.log("SNI getClientAliases " + alias);
            return wrapped.getClientAliases(alias, prncpls);
        }

        @Override
        public String chooseClientAlias(String[] strings, Principal[] prncpls, Socket socket) {
            LOG.log("SNI chooseClientAlias ");
            return wrapped.chooseClientAlias(strings, prncpls, socket);
        }

        @Override
        public String[] getServerAliases(String alias, Principal[] prncpls) {
            LOG.log("SNI getServerAliases " + alias);
            return wrapped.getServerAliases(alias, prncpls);
        }

        @Override
        public String chooseServerAlias(String alias, Principal[] prncpls, Socket socket) {
            LOG.log("SNI chooseServerAlias " + alias);
            return wrapped.chooseServerAlias(alias, prncpls, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            X509Certificate[] certs = sniCertificates.remove(alias);
            LOG.log("SNI getCertificateChain " + alias + " certs " +
                    (certs == null ? "0" : certs.length));
            if (certs != null) {
                return certs;
            } else {
                return wrapped.getCertificateChain(alias);
            }
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            PrivateKey pk = sniPrivateKeys.remove(alias);
            LOG.log("SNI getPrivateKey " + alias +
                    ", pkey " + pk == null ? "null" : "pkey");
            if (pk != null) {
                return pk;
            } else {
                return wrapped.getPrivateKey(alias);
            }
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType,
                Principal[] issuers,
                SSLEngine engine) {
            return wrapped.chooseEngineClientAlias(keyType, issuers, engine);
        }

        @Override
        public String chooseEngineServerAlias(String keyType,
                Principal[] issuers,
                SSLEngine engine) {
            LOG.log("SNI, chooseEngineServerAlias for " +  keyType);
            SSLSession session = engine.getHandshakeSession();
            if (session instanceof ExtendedSSLSession) {
                ExtendedSSLSession es = (ExtendedSSLSession) session;
                for (SNIServerName sni : es.getRequestedServerNames()) {
                    SNIHostName hn = new SNIHostName(sni.getEncoded());
                    LOG.log("SNI, SNI host name " + hn.getAsciiName());
                    SecureContext ctx = retrieveSNICallbackContext(hn);
                    if (ctx != null) {
                        String alias = retrieveInSNICallback(ctx, keyType, hn);
                        LOG.log("SNI, SNI configuration returned alias " + alias);
                        return alias;
                    }
                }
            }
            String alias = wrapped.chooseEngineServerAlias(keyType, issuers, engine);
            LOG.log("SNI, no SNI configuration, standard manager returned alias " + alias);
            return alias;

        }

        private SecureContext retrieveSNICallbackContext(SNIHostName hn) {
            if (sniCallback == null) {
                return null;
            }
            final List<SecureContext> retValue = new ArrayList<>(1);
            Callback ret = new Callback() {
                @Override
                public void call(String name, Object[] args) throws Exception {
                    retValue.add((SecureContext) args[0]);
                }
            };
            Object[] args = {hn.getAsciiName(), ret};
            try {
                sniCallback.call("", args);
            } catch (Exception ex) {
                LOG.log("SNI Exception when calling SNICallback " + ex);
            }

            SecureContext ctx = null;
            if (retValue.size() == 1 && retValue.get(0) != null) {
                ctx = retValue.get(0);
                LOG.log("SNI Custom context for SNI host " + hn.getAsciiName());
            }
            return ctx;
        }

        private String retrieveInSNICallback(SecureContext ctx, String keyType, SNIHostName hn) {
            String alias = null;
            try {
                alias = store(keyType, ctx.getKeyStore(), ctx.passPhrase);
                LOG.log("SNI computed alias " + alias);
            } catch (Exception ex) {
                LOG.log("SNI Exception when retrieving key/certificate, " + ex);
            }

            return alias;
        }

        // Returns non null if a PKey and a Certificate have been retrieved
        // for the given key type.
        private String store(String keyType, KeyStore ks, String passphrase) throws Exception {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    Key k = ks.getKey(alias, passphrase == null ? null : passphrase.toCharArray());
                    LOG.log("SNI Key alias " + alias + " key type " + k.getAlgorithm());
                    if (keyType.equals(k.getAlgorithm())) {
                        Certificate[] cert = ks.getCertificateChain(alias);
                        String computedAlias = "" + random.nextLong();
                        if (k instanceof PrivateKey) {
                            sniPrivateKeys.put(computedAlias, (PrivateKey) k);
                        }

                        X509Certificate[] x509Certs = Arrays.copyOf(cert,
                                cert.length, X509Certificate[].class);
                        sniCertificates.put(computedAlias, x509Certs);
                        return computedAlias;
                    }
                } else {
                   LOG.log("SNI Skiping alias " + alias + ", not a key");
                }
            }
            return null;
        }
    }

    /**
     *
     */
    private class TrustManagerWrapper implements X509TrustManager {

        private final TrustManager[] managers;

        private TrustManagerWrapper(final TrustManager[] managers) {
            this.managers = managers;
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String type) throws CertificateException {
            // certificate check is delegated post HS.
        }

        public void isClientTrusted(final X509Certificate[] chain, final String type) throws CertificateException {
            for (X509Certificate c : chain) {
                LOG.log("Client certificate subject " + c.getSubjectDN() + ", issuer " + c.getIssuerDN());
            }

            for (final TrustManager m : managers) {
                try {
                    if (m instanceof X509TrustManager) {
                        final X509TrustManager xtm = (X509TrustManager) m;
                        xtm.checkClientTrusted(chain, type);
                    }
                } catch (final CertificateException ex) {
                    LOG.log("Client Not trusted" + ex);
                    throw ex;
                }
            }
            LOG.log("OK, client certificate trusted");
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String type) throws CertificateException {
            // certificate check is delegated post HS.
        }

        public void isServerTrusted(final X509Certificate[] chain, final String type) throws CertificateException {
            for (X509Certificate c : chain) {
                LOG.log("Server certificate subject " + c.getSubjectDN() + ", issuer " + c.getIssuerDN());
            }
            for (final TrustManager m : managers) {
                try {
                    if (m instanceof X509TrustManager) {
                        final X509TrustManager xtm = (X509TrustManager) m;
                        xtm.checkServerTrusted(chain, type);
                    }
                } catch (final CertificateException ex) {
                    //addInvalid(chain, ex);
                    LOG.log("Server Not trusted " + ex);
                    throw ex;
                }
            }
            LOG.log("OK, server certificate trusted ");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            final List<X509Certificate> issuers = new ArrayList<>();
            for (final TrustManager m : managers) {
                if (m instanceof X509TrustManager) {
                    issuers.addAll(Arrays.asList(((X509TrustManager) m).getAcceptedIssuers()));
                }
            }
            final X509Certificate[] certs = new X509Certificate[issuers.size()];
            return issuers.toArray(certs);
        }
    }

    private final Crypto crypto;
    private final String[] enabledProtocols;
    private Certificate certificate;
    private final List<String> ciphers = new ArrayList<>();
    private Object sessionId;
    private final ArrayList<Certificate> trustedCAList = new ArrayList<>();
    private final List<X509CRL> crlList = new ArrayList<>();
    private KeyStore pkcs12;
    private String passPhrase;
    private EncryptedPrivateKeyInfo encryptedPK;
    private PKCS8EncodedKeySpec nonEncryptedPK;
    private SSLContext sslContext;
    private boolean sessionReused;
    private TrustManagerWrapper trustWrapper;
    private String host;
    private int port;

    private Logger LOG;

    SecureContext(final Crypto crypto, final String secureProtocol) throws Exception {
        JAVA_PROTOCOLS.put("SSLv3_method", new String[]{SSLV3});
        JAVA_PROTOCOLS.put("SSLv3_server_method", new String[]{SSLV3});
        JAVA_PROTOCOLS.put("SSLv3_client_method", new String[]{SSLV3});

        JAVA_PROTOCOLS.put("TLSv1_method", new String[]{TLSV1});
        JAVA_PROTOCOLS.put("TLSv1_server_method", new String[]{TLSV1});
        JAVA_PROTOCOLS.put("TLSv1_client_method", new String[]{TLSV1});

        JAVA_PROTOCOLS.put("TLSv1_1_method", new String[]{TLSV11});
        JAVA_PROTOCOLS.put("TLSv1_1_server_method", new String[]{TLSV11});
        JAVA_PROTOCOLS.put("TLSv1_1_client_method", new String[]{TLSV11});

        JAVA_PROTOCOLS.put("TLSv1_2_method", new String[]{TLSV12});
        JAVA_PROTOCOLS.put("TLSv1_2_server_method", new String[]{TLSV12});
        JAVA_PROTOCOLS.put("TLSv1_2_client_method", new String[]{TLSV12});

        JAVA_PROTOCOLS.put("SSLv23_method", new String[]{SSLV2, SSLV3, TLSV1});
        JAVA_PROTOCOLS.put("SSLv23_server_method", new String[]{SSLV2, SSLV3, TLSV1});
        JAVA_PROTOCOLS.put("SSLv23_client_method", new String[]{SSLV2, SSLV3, TLSV1});

        JAVA_CIPHER_SUITES.put("RC4-SHA", "SSL_RSA_WITH_RC4_128_SHA");
        JAVA_CIPHER_SUITES.put("RC4-MD5", "SSL_RSA_WITH_RC4_128_MD5");
        JAVA_CIPHER_SUITES.put("AES256-SHA", "TLS_RSA_WITH_AES_256_CBC_SHA");
        JAVA_CIPHER_SUITES.put("AES128-SHA", "TLS_RSA_WITH_AES_128_CBC_SHA");
        JAVA_CIPHER_SUITES.put("DES-CBC-SHA", "SSL_RSA_WITH_DES_CBC_SHA");
        JAVA_CIPHER_SUITES.put("NULL-MD5", "SSL_RSA_WITH_NULL_MD5");
        JAVA_CIPHER_SUITES.put("NULL-SHA", "SSL_RSA_WITH_NULL_SHA");
        this.crypto = crypto;
        this.enabledProtocols = toJavaProtocols(secureProtocol);
    }

    public void setCipherSuites(final String lst) {
        if (lst == null) {
            throw new IllegalArgumentException("Invalid null cipher suite");
        }
        final String[] split = lst.split(":");
        for (final String cs : split) {
            ciphers.add(toJavaCipherSuite(cs));
        }
    }

    String[] getCipherSuites() {
        final String[] arr = new String[ciphers.size()];
        return ciphers.toArray(arr);
    }

    public void setAddress(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    public void setPemCertificate(final String pemCertificate) throws Exception {
        final byte[] cert = Base64Decoder.decode(crypto.removePEMHeaderAndFooter(pemCertificate));
        final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        certificate = certFactory.generateCertificate(new ByteArrayInputStream(cert));
    }

    public void setKey(final String key, final String passPhrase) throws Exception {
        this.passPhrase = passPhrase;
        final byte[] bytes = Base64Decoder.decode(crypto.removePEMHeaderAndFooter(key));
        if (passPhrase == null) {
            nonEncryptedPK = new PKCS8EncodedKeySpec(bytes);
        } else {
            // Descrypt the encrypted private key. The encoded content contains both
            // algo+parametesr+encrypted key
            encryptedPK = new EncryptedPrivateKeyInfo(bytes);
        }
    }

    public void addTrustedPemCertificate(final String pemCertificate) throws Exception {
        final byte[] cert = Base64Decoder.decode(crypto.removePEMHeaderAndFooter(pemCertificate));
        final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        final Certificate certif = certFactory.generateCertificate(new ByteArrayInputStream(cert));
        trustedCAList.add(certif);
    }

    @SuppressWarnings("unchecked")
    private List<Certificate> getTrustedCertificates() {
        return (List<Certificate>) trustedCAList.clone();
    }

    public void addPemCRL(final String pemCRL) throws Exception {
        final byte[] bytes = Base64Decoder.decode(crypto.removePEMHeaderAndFooter(pemCRL));
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509CRL crl;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            crl = (X509CRL) cf.generateCRL(stream);
        }
        crlList.add(crl);
    }

    public void setSessionId(final Object sessionId) {
        this.sessionId = sessionId;
    }

    public void loadPKCS12(final Buffer pfx, final String passPhrase) throws Exception {
        this.passPhrase = passPhrase;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(pfx.array())) {
            pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(stream, passPhrase == null ? null : passPhrase.toCharArray());
            // Check authentication because no Exception is thrown
            // if the passphrase was wrong
            final Enumeration<String> it = pkcs12.aliases();
            while (it.hasMoreElements()) {
                String alias = it.nextElement();
                final Certificate cert = pkcs12.getCertificate(alias);
                if (cert == null) {
                    throw new Exception("Authentication failed");
                }
            }
        }
    }

    /**
     * Build with the key and certificate. These are local credentials, not used
     * for trust.
     */
    private KeyStore getPrivPubKeyStore() throws Exception {
        KeyStore ks = null;
        if (certificate != null) {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(null);
            final Certificate[] chain = new Certificate[1];
            chain[0] = certificate;
            final String alias = "priv-key";
            if (encryptedPK != null) {
                // The key is of format EncryptedPrivateKeyInfo
                ks.setKeyEntry(alias, encryptedPK.getEncoded(), chain);
            } else {
                if (nonEncryptedPK != null) {
                    final KeyFactory keyFactory = KeyFactory.getInstance(certificate.getPublicKey().getAlgorithm());
                    final Key pk = keyFactory.generatePrivate(nonEncryptedPK);
                    ks.setKeyEntry(alias, pk, null, chain);
                } else {
                    // no secure configuration, should not be called.
                    throw new IllegalArgumentException("No data to create certificate");
                }
            }
        }
        return ks;
    }

    private KeyStore getPKCS12() throws Exception {
        return pkcs12;
    }

    public void setOptions(final Object options) {
        // XXX jfdenise
        throw new UnsupportedOperationException("Setting options is not supported yet");
    }

    String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    public SSLContext getContext(final Logger log, final boolean isServer, final boolean rejectUnauthorized) throws Exception {
        this.LOG = log;
        if (sslContext != null) {
            LOG.log("Reusing SSLContext");
            return sslContext;
        }

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(getCustomKeyManagers(), getTrustManagers(), null);

        return sslContext;
    }

    private KeyManager[] getCustomKeyManagers() throws Exception {
        KeyManager[] managers = getKeyManagers();
        try {
            KeyManagerWrapper keyWrapper = new KeyManagerWrapper(managers);
            final KeyManager[] arr = {keyWrapper};
            return arr;
        } catch (Exception ex) {
            LOG.log("SNI Exception occured when creating KeyManager wrapper, "
                    + "fallback to standard one, NO SNI support. " + ex);
        }
        return managers;
    }

    private KeyStore ks;

    private KeyStore getKeyStore() throws Exception {
        if (ks == null) {
            ks = getPKCS12();
            if (ks == null) {
                ks = getPrivPubKeyStore();
            }
            if (ks == null) {
                ks = KeyStore.getInstance("PKCS12");
                ks.load(null);
            }
        }
        return ks;
    }

    private KeyManager[] getKeyManagers() throws Exception {
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(getKeyStore(), passPhrase == null ? null : passPhrase.toCharArray());
            return kmf.getKeyManagers();
    }

    private TrustManager[] getTrustManagers() throws Exception {
        // We want a null truststore in order to fallback on cacerts
        KeyStore ks = null;
        if (!getTrustedCertificates().isEmpty()) {
            ks = KeyStore.getInstance("JKS");
            ks.load(null);
        }

        // Client and server trusts the content of ca
        for (int i = 0; i < getTrustedCertificates().size(); i++) {
            final Certificate cert = getTrustedCertificates().get(i);
            LOG.log("Adding trusted certificate " + ((X509Certificate) cert).getIssuerDN());
            ks.setCertificateEntry("trusted-cert-" + i, cert);
        }

        // This TrustManagerFactory knows the default trusted certificates and will use it if the
        // init KeyStore is null.
        /*
        The default is PKIX, we are using SunX509. PKIX has more features but is less efficient. Furthermore,
        a compatibility issue has been found with the npm registry server certificate. The server certificate is not well
        formed.
        Using X509 type of Trustmanager increases compatibility with openssl.
        */
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        trustWrapper = new TrustManagerWrapper(tmf.getTrustManagers());
        LOG.log("Number of trusted issuers: " + trustWrapper.getAcceptedIssuers().length);
        final TrustManager[] arr = {trustWrapper};
        return arr;
    }

    public void isServerTrusted(X509Certificate[] chain, String authType) throws Exception {
        assert trustWrapper != null;
        trustWrapper.isServerTrusted(chain, authType);
    }

    public void isClientTrusted(X509Certificate[] chain, String authType) throws Exception {
        //first check in CRL list
        for (X509CRL crl : crlList) {
            for (X509Certificate c : chain) {
                if (crl.getRevokedCertificate(c) != null) {
                    throw new Exception("Certificate in CRL");
                }
            }
        }
        // Then if the chain is trusted.
        assert trustWrapper != null;
        trustWrapper.isClientTrusted(chain, authType);
    }

    public void setContext(final SSLContext sslContext) {
        if (sslContext == null) {
            throw new IllegalArgumentException("Invalid null session");
        }

        this.sessionReused = true;
        this.sslContext = sslContext;
    }

    public SSLContext getContext() {
        return sslContext;
    }

    public boolean isSessionReused() {
        return sessionReused;
    }
}
