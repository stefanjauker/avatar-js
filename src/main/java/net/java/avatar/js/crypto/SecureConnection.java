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

import net.java.libuv.Callback;

import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.ExtendedSSLSession;

import net.java.avatar.js.buffer.Buffer;
import net.java.avatar.js.buffer.HexUtils;
import net.java.avatar.js.eventloop.EventLoop;
import net.java.avatar.js.log.Logger;

/**
 * Secure Connection
 *
 */
public class SecureConnection {

    /**
     * Convey the session, no access to SSLContext outside current class.
     */
    public static final class SecureSession {

        private final SSLContext ctx;

        private SecureSession(final SSLContext ctx) {
            this.ctx = ctx;
        }

        private SSLContext getContext() {
            return ctx;
        }
    }
    private final Logger LOG;
    private final SecureContext context;
    private final boolean isServer;
    private final boolean requestCertificate;
    private final boolean rejectUnauthorized;
    private final String serverName;
    private SSLEngine sslEngine;
    private ByteBuffer localNetDataForPeer;
    private ByteBuffer localAppData;
    private ByteBuffer decryptedAppData;
    private ByteBuffer incomingFromPeer;
    private boolean shutingdown;
    private boolean started;
    private Exception exception;
    private final SSLContext ctx;

    public SecureConnection(final EventLoop eventLoop,
                            final SecureContext context,
                            final boolean requestCertificate,
                            final boolean rejectUnauthorized) throws Exception {
        this(eventLoop, true, context, requestCertificate, null, rejectUnauthorized);
    }

    public SecureConnection(final EventLoop eventLoop,
                            final SecureContext context,
                            final String serverName,
                            final boolean rejectUnauthorized) throws Exception {
        this(eventLoop, false, context, false, serverName, rejectUnauthorized);
    }

    private SecureConnection(final EventLoop eventLoop,
                             final boolean isServer,
                             final SecureContext context,
                             final boolean requestCertificate,
                             final String serverName,
                             final boolean rejectUnauthorized) throws Exception {
        this.isServer = isServer;
        this.context = context;
        this.requestCertificate = requestCertificate;
        this.serverName = serverName;
        this.rejectUnauthorized = rejectUnauthorized;
        LOG = eventLoop.logger("tls-" + (isServer ? "server" : "client"));
        ctx = context.getContext(LOG, isServer, rejectUnauthorized);
    }

    public static final class CipherSuite {

        private final String name;
        private final String jname;
        private final String version;

        private CipherSuite(final String name, final String jname, final String version) {
            this.name = name;
            this.jname = jname;
            this.version = version;
        }

        /**
         * @return the suite
         */
        public String getName() {
            return name;
        }

        public String getJName() {
            return jname;
        }

        /**
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return name + "(" + jname + ") " + version;
        }
    }

    public boolean getError() {
        return exception != null;
    }

    public void resetError() {
        exception = null;
    }

    public CipherSuite getCipherSuite() {
        CipherSuite suite = null;
        if (sslEngine != null && sslEngine.getSession() != null) {
            final String suiteName = sslEngine.getSession().getCipherSuite();
            final String version = sslEngine.getSession().getProtocol();
            suite = new CipherSuite(SecureContext.toNodeCipherName(suiteName), suiteName, version);
        }
        return suite;
    }

    public void setSession(final SecureSession session) {
        context.setContext(session.getContext());
    }

    public SecureSession getSession() {
        return new SecureSession(context.getContext());
    }

    public boolean isSessionReused() {
        return context.isSessionReused();
    }

    private SSLEngine createSSLEngine(final SecureContext context) throws Exception {
        LOG.log("New SSL Engine for host = " + context.getHost() + ", port = " + context.getPort());
        SSLEngine engine;
        SSLParameters params = new SSLParameters();

        if (context.getHost() != null) {
            engine = ctx.createSSLEngine(context.getHost(), context.getPort());
        } else {
            engine = ctx.createSSLEngine();
        }
        engine.setUseClientMode(!isServer);

        if (context.getCipherSuites().length != 0) {
            params.setCipherSuites(context.getCipherSuites());
        }

        if (context.getEnabledProtocols() != null) {
            params.setProtocols(context.getEnabledProtocols());
        }

        params.setNeedClientAuth(requestCertificate);

        LOG.log("Enabled protocols ");
        for (final String s : engine.getEnabledProtocols()) {
            LOG.log(s);
        }
        LOG.log("Enabled cipher suites ");
        for (final String s : engine.getEnabledCipherSuites()) {
            LOG.log(s);
        }

        // client SNI
        if (serverName != null) {
            LOG.log("SNI client extension " + serverName);
            try {
                SNIServerName sni = new SNIHostName(serverName);
                List<SNIServerName> lst = new ArrayList<>(1);
                lst.add(sni);
                params.setServerNames(lst);
            } catch(Exception ex) {
                // XXX OK, invalid SNI servername.
                LOG.log("SNI Exception " + ex + " with " + serverName);
            }
        }

        engine.setSSLParameters(params);
        return engine;
    }

    // Called by tls.js at the end of handshake.
    public String getServerName() {
        String ret = serverName;
        if (sslEngine != null && sslEngine.getSession() != null) {
            SSLSession session = sslEngine.getSession();
            if (session instanceof ExtendedSSLSession) {
                ExtendedSSLSession es = (ExtendedSSLSession) session;
                for (SNIServerName sni : es.getRequestedServerNames()) {
                    SNIHostName hn = new SNIHostName(sni.getEncoded());
                    ret = hn.getAsciiName();
                    break;
                }
            }
        }
        LOG.log("HS DONE, servername is  " + ret);
        return ret;
    }

    public void setSNICallback(Callback sniCallback) {
        context.setSNICallback(sniCallback);
    }

    public void start() throws Exception {
        LOG.log("--HS-- start");
        if (started) {
            return;
        }
        sslEngine = createSSLEngine(context);
        assert sslEngine != null;
        started = true;
        assert sslEngine != null;

        final SSLSession session = sslEngine.getSession();
        localNetDataForPeer = ByteBuffer.allocate(session.getPacketBufferSize());
        localAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        decryptedAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        incomingFromPeer = ByteBuffer.allocate(session.getPacketBufferSize());

        sslEngine.beginHandshake();
    }

    public static final class PeerCertificate {

        private final String issuer;
        private final String subject;
        private final String alternateNames;
        private final X509Certificate certificate;
        private final String modulus;
        private final String exponent;

        private PeerCertificate(final X509Certificate certificate, final String alternateNames) {
            this.certificate = certificate;
            this.issuer = formatDN(certificate.getIssuerX500Principal().getName(RFC2253, DN_MAPPING));
            this.subject = formatDN(certificate.getSubjectX500Principal().getName(RFC2253, DN_MAPPING));
            this.alternateNames = alternateNames;
            if (certificate.getPublicKey() instanceof RSAPublicKey) {
                modulus = ((RSAPublicKey) certificate.getPublicKey()).getModulus().toString(16).toUpperCase();
                exponent = ((RSAPublicKey) certificate.getPublicKey()).getPublicExponent().toString(16).toUpperCase();
            } else {
                modulus = exponent = null;
            }
        }

        public String getSubjectaltname() {
            return alternateNames;
        }

        public String getModulus() {
            return modulus;
        }

        public String getExponent() {
            return exponent;
        }

        /**
         * @return the issuer
         */
        public String getIssuer() {
            return issuer;
        }

        /**
         * @return the subject
         */
        public String getSubject() {
            return subject;
        }

        public String getNotBefore() {
            return certificate.getNotBefore().toString();
        }

        public String getNotAfter() {
            return certificate.getNotAfter().toString();
        }

        public Buffer getSignature() {
            return safeBuffer(certificate.getSignature());
        }

        public int getVersion() {
            return certificate.getVersion();
        }

        public Buffer getTBSCertificate() {
            try {
                return safeBuffer(certificate.getTBSCertificate());
            } catch (final Exception ex) {
                return new Buffer(0);
            }
        }

        public boolean[] getSubjectUniqueID() {
            return certificate.getSubjectUniqueID();
        }

        public Buffer getSigAlgParams() {
            return safeBuffer(certificate.getSigAlgParams());
        }

        public String getSigAlgOID() {
            return certificate.getSigAlgOID();
        }

        public String getSigAlgName() {
            return certificate.getSigAlgName();
        }

        public String getSerialNumber() {
            return certificate.getSerialNumber().toString();
        }

        public boolean[] getKeyUsage() {
            return certificate.getKeyUsage();
        }

        public boolean[] getIssuerUniqueID() {
            return certificate.getIssuerUniqueID();
        }

        public String[] getExtendedKeyUsage() {
            String[] ret = new String[0];
            try {
                List<String> lst = certificate.getExtendedKeyUsage();
                if (lst != null && !lst.isEmpty()) {
                    ret = new String[lst.size()];
                    lst.toArray(ret);
                }
            } catch (final CertificateParsingException ex) {
                // XXX OK
            }
            return ret;
        }

        public int getBasicConstraints() {
            return certificate.getBasicConstraints();
        }

        public boolean checkValidity() {
            try {
                certificate.checkValidity();
                return true;
            } catch (final Exception ex) {
                return false;
            }
        }
    }

    private static Buffer safeBuffer(final byte[] val) {
        if (val == null) {
            return new Buffer(0);
        } else {
            return new Buffer(val);
        }
    }
    private static final Map<String, String> DN_MAPPING = new HashMap<>();
    // This RFC grammar removes the optional character (such as space) in between each DN type.
    private static final String RFC2253 = "RFC2253";
    static {
        DN_MAPPING.put("2.5.29.17", "subjectAltName");
        DN_MAPPING.put("2.5.29.18", "issuerAltName");
    }
    private static String formatDN(final String quoted) {
        final String[] split = quoted.split(",");
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String item = split[i];
            // This shouldn't happen, just in case...
            while (item.charAt(0) == ' ') {
                item = item.substring(1);
            }
            final int eqindex = item.indexOf("=");
            final String value = item.substring(eqindex+1);
            builder.append(item.substring(0, eqindex+1)).
                    append(unquote(value)).append('\n');
        }
        return builder.toString();
    }

    private static String unquote(final String quoted) {
        // Value fully quoted
        if (quoted.charAt(0) == '"') {
            return quoted.substring(1, quoted.length()-1);
        }
        // Usage of \ character
        final StringBuilder builder = new StringBuilder();
        final boolean quote = false;
        int i = 0;
        while(i < quoted.length()) {
            final char c = quoted.charAt(i);
            if (c == '\\' && i < (quoted.length() - 1)) {
                i+=1;
            }
            builder.append(quoted.charAt(i));
            i+=1;
        }
        return builder.toString();

    }
    // If Kerberos in use, then no certificate
    // XXX jfdenise, need to check.

    public PeerCertificate getPeerCertificate() throws Exception {
        final SSLSession session = sslEngine.getSession();
        assert session != null;
        if (session == null) {
            throw new Exception("Secure Context not established");
        }
        Certificate[] certs = null;
        try {
            certs = session.getPeerCertificates();
        } catch (final Exception ex) {
            // special case, on the server side, no certificate received from peer
            // because we didn;t requested it.
            // Simply returns null.
            if (isServer && !requestCertificate) {
                return null;
            }
        }
        assert certs != null && certs.length != 0;
        if (certs == null || certs.length == 0) {
            throw new Exception("Secure Context not established");
        }
        final Certificate pc = certs[0];
        PeerCertificate peerC = null;
        if (pc instanceof X509Certificate) {
            final X509Certificate x509 = (X509Certificate) pc;

            final Collection<List<?>> alt = x509.getSubjectAlternativeNames();
            StringBuilder builder = new StringBuilder();
            if (alt != null && alt.size() > 0) {
                int i = 0;
                // [<Integer>, <String>]
                for (final List<?> name : alt) {
                    for (Object obj : name) {
                        if (obj instanceof String) {
                            builder.append((String) obj);
                        } else {
                            if (obj instanceof Integer) {
                                builder.append(typeToLabel((Integer)obj));
                            }
                        }
                    }
                    if (i < alt.size() - 1) {
                        builder.append(", ");
                    }
                    i++;
                }
            }

            peerC = new PeerCertificate(x509, builder.toString());
            LOG.log("Peer Issuer " + peerC.getIssuer()
                    + "Peer Subject " + peerC.getSubject());
        } else {
            throw new Exception("Unsupported Certificate " + pc.getType());
        }

        assert peerC != null;
        return peerC;
    }

    // DNS == 2, IP == 7, URI == 6
    private String typeToLabel(Integer type) {
        switch(type) {
            case 2:{
                return "DNS:";
            }
            case 7:{
                return "IP Address:";
            }
            case 6:{
                return "URI:";
            }
        };
        return ""+type+":";
    }
    /**
     * This state has to be handled carefully. It is checked at the end of each
     * cycle by the tls.js logic. When handshake is finished, a secure event is
     * emitted, then certifcate validation is operated if no error occured.
     *
     * A certificate validation failure is not an error. Errors are fatal. If a
     * certificate is not trusted handshake continues. A call to verifyError
     * will throw an exception.
     *
     * @return
     */
    public boolean isHandshakeFinished() {
        /*
         * shutingDown: the HS can ends with an unwrap containing both finished and closed.
         * getError(): A fatal error could have occured.
         * (started && !isHandshake()): nominal end of Handshake.
         */
        return shutingdown || getError() || (started && !isHandshake());
    }

    /**
     * When the connection is shutdown, the initiator doesn't wait for the peer
     * shutdown message.
     *
     */
    public void shutdown() {
        LOG.log("--CLOSE-- shutdown " + shutingdown);

        assert sslEngine != null;
        if (shutingdown) {
            return;
        }
        shutingdown = true;
        sslEngine.closeOutbound();
    }

    public void close() {
        LOG.log("explicit close");
        started = false;
    }

    public int clearPending() {
        if (!started) {
            return -1;
        }
        return decryptedAppData.position();
    }

    public int encPending() {
        if (!started) {
            return -1;
        }
        /**
         * When shutdown occured and there is no more pending values
         * tls.js will close the socket connection.
         * This means that we will not get the peer close_notify message if we
         * were the close initiator. This is a compliant with SSL spec and
         * obviously quicker.
         * It has also been observed that
         * To get the full notify_close messages,
         * do final int ret = shutdownInProgress() ? 1 : localNetDataForPeer.position();
         * until the remote close_notify is received. If it is never received,
         * the socket will at some point be closed and this connection will be cleared.
         */
        final int ret = hsNeedsForWrap() ? 1 : localNetDataForPeer.position();
        return ret;
    }

    /**
     * This is the way to check if the received certificate can be authentified.
     */
    public void verifyError() throws Exception {
        final SSLSession session = sslEngine.getSession();
        final Certificate[] chain = session.getPeerCertificates();
        final X509Certificate[] certificates = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            if (chain[i] instanceof X509Certificate) {
                certificates[i] = (X509Certificate) chain[i];
            }
        }

        if (isServer && requestCertificate) {
            final String authType = retrieveAuthType(certificates);
            context.isClientTrusted(certificates, authType);
        } else {
            final String authType = retrieveAuthType(session.getCipherSuite());
            context.isServerTrusted(certificates, authType);
        }
    }

    private static String retrieveAuthType(final X509Certificate[] certificates) {
        String type = null;
        if (certificates.length > 0) {
            type = certificates[0].getPublicKey().getAlgorithm();
        }
        return type;
    }

    private static String retrieveAuthType(String suite) {
        String ret = null;
        if (suite != null) {
            //starts by TLS_ or SSL_
            suite = suite.substring(4);
            final int index = suite.indexOf("_WITH_");
            ret = suite.substring(0, index);
        }
        return ret;
    }

    private boolean isHandshake() {
        if (sslEngine == null) {
            return false;
        }
        final HandshakeStatus status = sslEngine.getHandshakeStatus();
        final boolean ret = started && (status != HandshakeStatus.FINISHED && status != HandshakeStatus.NOT_HANDSHAKING);
        return ret;
    }

    /**
     * This side has not yet generated some hs data (eg: finish msg) but should do.
     * When this is true, encOut will be called to send hs message to peer.
     * @return
     */
    private boolean hsNeedsForWrap() {
        return isHandshake() && sslEngine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;
    }
    /**
     * Data received from the peer. In v10, encIn is not followed by an encOut in the
     * same cycle. encOut is called if there is some pending content (encPending).
     * If an exception occurs shutdown is initiated but the close
     * packet is sent in the next encOut
     */
    public int encIn(final Buffer data, final int offset, final int length) throws SSLException {
        if (!started) {
            return -1;
        }
        LOG.log("encIn, " + data + ", offset : " + offset + ", length : " + length);
        try {
            return unwrap(data, offset, length);
        } catch (final Exception ex) {
            LOG.log("WARNING, encIn, Unwrap Exception " + ex);
            incomingFromPeer.compact();
            exception = ex;
            if (LOG.enabled()) {
                ex.printStackTrace();
            }
            // The next call to encOut will generate a close message
            // and this side of the connection is closed.
            shutdown();
        }
        return 0;
    }

    /* Encrypted Data (handshake, app) for peer
     * There is 3 different cases.
     * - handshake, data to encrypt comes from the engine.
     * - application data, data to encrypt comes from the application in clearIn.
     * - close, data to encrypt comes from the engine.
     * If an exception occurs, the close message is set in the provided Buffer.
     */
    public int encOut(final Buffer pool, final int offset, final int length) throws SSLException {
        if (!started) {
            return -1;
        }
        int wrote = 0;
        if (localNetDataForPeer.position() != 0) {
            LOG.log("encOut, encrypted data length " + localNetDataForPeer.position());
            wrote = fillBuffer(localNetDataForPeer, pool);
            localNetDataForPeer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        } else {
            try {
                if (shutingdown) {
                    if (!sslEngine.isOutboundDone() && !sslEngine.isInboundDone()) {
                        wrote = wrapClose(pool, offset, length);
                    }
                } else {
                    if (isHandshake()) {
                        wrote = wrapHS(pool, offset, length);
                    }
                }
            } catch (final Exception ex) {
                LOG.log("WARNING, encOut, Wrap Exception " + ex);
                exception = ex;
                if (LOG.enabled()) {
                    ex.printStackTrace();
                }
                shutdown();
            }
        }
        return wrote;
    }

    // Encrypted data from application
    public int clearIn(final Buffer pool, final int offset, final int length) throws SSLException {
        if (shutingdown) {
            LOG.log("clearIn, already shutdown");
            exception = new Exception("Allready shutdown");
            return -1;
        }

        // Do not mix application data with handshake
        if (!started || isHandshake()) {
            LOG.log("clearIn, asked to encrypt content but HS in progress " + pool);
            //No mix with buffer of size 0.
            return -1;
        }
        LOG.log("clearIn offset " + offset + " length " + length);
        try {
            final int ret = wrap(pool, offset, length);
            return ret;
        } catch (final Exception ex) {
            LOG.log("WARNING, clearIn, Wrap Exception " + ex);
            exception = ex;
            if (LOG.enabled()) {
                ex.printStackTrace();
            }
            shutdown();
        }
        return -1;

    }

    // Decrypted Data that is to go to application
    public int clearOut(final Buffer pool, final int offset, final int length) throws SSLException {
        if (!started) {
            return -1;
        }
        int ret = 0;
        if (decryptedAppData.position() != 0) {
            ret = fillBuffer(decryptedAppData, pool);
            decryptedAppData =
                    ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
            LOG.log("clearOut produced " + ret + " bytes for application");
        }
        return ret;

    }

    private int wrapClose(final Buffer pool, final int offset, final int length) throws SSLException {
        int encLength = 0;
        initWrap();
        SSLEngineResult res;
        do {
            res = sslEngine.wrap(localAppData, localNetDataForPeer);
            LOG.log("--CLOSE-- wrap " + res);
            switch (res.getStatus()) {
                case CLOSED:
                case OK: {
                    encLength += fillBuffer(localNetDataForPeer, pool);
                    localNetDataForPeer.compact();
                    break;
                }
                case BUFFER_OVERFLOW: {
                    localNetDataForPeer = handleBufferOverFlow(sslEngine.getSession().getPacketBufferSize(), localNetDataForPeer);
                    break;
                }
                case BUFFER_UNDERFLOW: {
                    throw new RuntimeException("NOT EXPECTING THIS BUFFER_UNDERFLOW");
                }
            }
        } while (res.getStatus() == Status.BUFFER_OVERFLOW || !sslEngine.isOutboundDone());
        return encLength;
    }

    private int wrapHS(final Buffer pool, final int offset, final int length) throws Exception {
        int encLength = 0;
        if (sslEngine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
            initWrap();
            do {
                // Generate handshaking data
                localNetDataForPeer.clear();
                final SSLEngineResult res = sslEngine.wrap(localAppData, localNetDataForPeer);
                LOG.log("--HS-- wrap " + res);
                switch (res.getStatus()) {
                    case OK:
                        encLength += fillBuffer(localNetDataForPeer, pool);
                        localNetDataForPeer.compact();
                        break;
                    case CLOSED: {
                        shutdown();
                        break;
                    }
                    case BUFFER_OVERFLOW: {
                        localNetDataForPeer = handleBufferOverFlow(sslEngine.getSession().getPacketBufferSize(), localNetDataForPeer);
                        break;
                    }
                    case BUFFER_UNDERFLOW: {
                        throw new RuntimeException("NOT EXPECTING THIS BUFFER_UNDERFLOW");
                    }
                }
                if (res.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                    LOG.log("--HS-- FINISHED, Cipher suite [ " + getCipherSuite() + "] Session [" + HexUtils.encode(sslEngine.getSession().getId()) + "]");
                }
            } while (sslEngine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP);
        }
        otherStates();
        return encLength;
    }

    private int wrap(final Buffer pool, final int offset, final int length) throws SSLException {
        if (pool != null) {
            localAppData.clear();
            localAppData = safeAllocation(localAppData, pool, offset, length);
            // Allocating more than the packet size will avoid some useless Buffer Overflow
            // If some content, means that socket has reached its limit and content is accumulating
            // at some point, user will stop write content (write returning false) and
            // the accumulated content will be read and piped to socket.
            // User will then receive drain event to push more data (if needed)
            if (localNetDataForPeer.position() == 0) {
                localNetDataForPeer = ByteBuffer.allocate(Math.max(sslEngine.getSession().getPacketBufferSize(), length));
            }
        } else {
            // default buffer allocation
            initWrap();
        }
        int consumed = 0;
        SSLEngineResult res;
        do {
            res = sslEngine.wrap(localAppData, localNetDataForPeer);

            LOG.log("wrap " + res + " bytes consumed " + res.bytesConsumed() + ", enc length " + localNetDataForPeer.position());
            consumed += res.bytesConsumed();
            switch (res.getStatus()) {
                case OK: {
                    break;
                }
                case CLOSED: {
                    shutdown();
                    return length;
                }
                case BUFFER_OVERFLOW: {
                    localNetDataForPeer = handleBufferOverFlow(sslEngine.getSession().getPacketBufferSize(), localNetDataForPeer);
                    break;
                }
                case BUFFER_UNDERFLOW: {
                    // Waiting for more data from the Application, strange....
                    throw new RuntimeException("NOT EXPECTING THIS BUFFER_UNDERFLOW");
                }
            }
        } while (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW || consumed < length);

        return length;
    }

    private void initWrap() {
        localNetDataForPeer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        localAppData = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
    }

    private int unwrap(final Buffer data, final int offset, final int length) throws Exception {
        int readLength;
        SSLEngineResult res = null;
        initUnwrap(data, offset, length);
        do {
            do {
                // The received packet can contain multiple HS unwrap: ChangeCipherSpec and Finished
                res = sslEngine.unwrap(incomingFromPeer, decryptedAppData);
                LOG.log("unwrap " + res + ", consumed " + res.bytesConsumed() + ", produced " + res.bytesProduced());
            } while (res.getStatus() == SSLEngineResult.Status.OK
                    && res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP
                    && res.bytesProduced() == 0);

            LOG.log("unwrap, main loop done, has remaining in received from peer " + incomingFromPeer.hasRemaining());

            // Reallocate some space for clear content.
            if (res.getStatus() == Status.BUFFER_OVERFLOW) {
                decryptedAppData = handleBufferOverFlow(sslEngine.getSession().getApplicationBufferSize(), decryptedAppData);
            } else {
                // Check for other states in between multiple unwrap.
                // Handshake only.
                otherStates();
            }
            if (res.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                LOG.log("--HS-- FINISHED, Session [" + HexUtils.encode(sslEngine.getSession().getId()) + "]");
            }
            // If there is some data in the incoming buffer, then loop to decrypt it.
            // Termination occurs when there is not enough in incoming to decrypt
            // or if Closed has been received embedded in the current packet.
        } while (res.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW && res.getStatus() != SSLEngineResult.Status.CLOSED
                && incomingFromPeer.hasRemaining());
        readLength = endUnwrap(res, length);

        return readLength;

    }

    private void initUnwrap(final Buffer data, final int offset, final int length) {
        LOG.log("start unwrap, incomingFromPeer.position == " + incomingFromPeer.position());
        incomingFromPeer = safeAllocation(incomingFromPeer, data, offset, length);
    }

    private int endUnwrap(final SSLEngineResult res, final int length) throws SSLException {
        final int readLength = length;
        incomingFromPeer.compact();
        assert res.getStatus() != Status.BUFFER_OVERFLOW;
        LOG.log("end unwrap incomingFromPeer.position == " + incomingFromPeer.position() + " res " + res + " / bytes " + res.bytesProduced() + "decrypted data position " + decryptedAppData.position());

        switch (res.getStatus()) {
            case OK: {
                break;
            }
            case CLOSED: {
                // We could have received the close from peer.
                // If this is during handshake, means that an error occured during HS
                if (sslEngine.isInboundDone() && !isHandshake()) {
                    LOG.log("Inbound closed, received peer close");
                    sslEngine.closeInbound();
                }
                shutdown();
                break;
            }
            case BUFFER_UNDERFLOW: {
                incomingFromPeer = handleBufferUnderFlow(sslEngine.getSession().getPacketBufferSize(), incomingFromPeer);
                break;
            }
        }
        return readLength;
    }

    private static ByteBuffer handleBufferUnderFlow(final int size, final ByteBuffer underflowed) {
        ByteBuffer b = underflowed;
        if (size > underflowed.capacity()) {
            b = ByteBuffer.allocate(size);
            underflowed.flip();
            b.put(underflowed);
        }
        return b;
    }

    private static ByteBuffer handleBufferOverFlow(final int size, final ByteBuffer overflowed) {
        final ByteBuffer b = ByteBuffer.allocate(size + overflowed.position());
        overflowed.flip();
        b.put(overflowed);
        return b;
    }

    private static int fillBuffer(final ByteBuffer byteBuffer, final Buffer buffer) {
        byteBuffer.flip();
        final byte[] content = new byte[Math.min(byteBuffer.limit(), buffer.remaining())];
        byteBuffer.get(content);
        buffer.put(content);
        return content.length;
    }

    private ByteBuffer safeAllocation(ByteBuffer buffer, final Buffer data, final int offset, final int length) {
        final int total = buffer.position() + length;
        if (total > buffer.capacity()) {
            buffer = handleBufferOverFlow(buffer.remaining() + length, buffer);
            LOG.log("Overflow, length " + length + ", new Buffer capacity " + buffer.capacity() +", buffer position " + buffer.position());
        }
        buffer.put(data.array(), offset, length);
        buffer.flip();
        return buffer;
    }

    private void otherStates() throws Exception {
        if (sslEngine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            LOG.log("--HS-- NEED_TASK");
            doTasks();
        }

    }

    private void doTasks() throws Exception {
        Runnable dtask;
        while ((dtask = sslEngine.getDelegatedTask()) != null) {
            // Certificate are checked during this phase
            dtask.run();
        }
        LOG.log("--HS-- tasks done, status " + sslEngine.getHandshakeStatus());

        /*
         * Workaround of JDK-8005859
         * - if wrap is required, then nomial case.
         * - if wrap is not required, then nothing happens
         * - if there is an error (eg: server used an invalid private key), an exception is
         * thrown.
         */
        wrap(null, 0, 0);
    }
}
