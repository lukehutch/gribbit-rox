package com.flat502.rox.processing;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// import sun.security.tools.keytool.CertAndKeyGen;
// import sun.security.x509.X500Name;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;

//@SuppressWarnings("restriction")
public class SSLConfiguration {
    public static enum ClientAuth {
        NONE, REQUEST, REQUIRE
    };

    private static Log log = LogFactory.getLog(SSLConfiguration.class);

    /**
     * A regular expression that matches only cipher suites that allow for anonymous key exchange.
     */
    public static final String ANON_CIPHER_SUITES = "_DH_anon_(?i)";

    /**
     * A regular expression that matches all cipher suites.
     */
    public static final String ALL_CIPHER_SUITES = ".*";

    /**
     * A regular expression that matches all protocols.
     */
    public static final String ALL_PROTOCOLS = ".*";

    /**
     * A regular expression that matches all TLS protocols.
     */
    public static final String TLS_PROTOCOLS = "^TLS";

    // The pattern used to select cipher suites
    private Pattern cipherSuitePattern;

    private Pattern protocolPattern;

    // Default to 10 seconds
    private int handshakeTimeout = 10000;

    private KeyStore keyStore;
    private KeyStore trustStore;
    private String keyStorePassphrase;

    private SecureRandom rng;

    private ClientAuth clientAuth = ClientAuth.NONE;

    private PrivateKey explicitPrivateKey;
    private X509Certificate[] explicitCertChain;

    private String keystoreName;
    private String truststoreName;

    private SSLContext explicitContext;

    public SSLConfiguration() {
        this.setCipherSuitePattern(ALL_CIPHER_SUITES);
        this.setProtocolPattern(ALL_PROTOCOLS);
    }

    public SSLConfiguration(SSLContext context) {
        this();
        this.explicitContext = context;
    }

    public SSLConfiguration(Properties props) throws GeneralSecurityException, IOException {
        this();
        String ks = getProperty(props, "javax.net.ssl.keyStore", null);
        String ksp = getProperty(props, "javax.net.ssl.keyStorePassword", null);
        String kst = getProperty(props, "javax.net.ssl.keyStoreType", "JKS");
        if (ks != null && ksp != null && kst != null) {
            this.setKeyStore(ks, ksp, ksp, kst);
        }
        String ts = getProperty(props, "javax.net.ssl.trustStore", null);
        String tsp = getProperty(props, "javax.net.ssl.trustStorePassword", null);
        String tst = getProperty(props, "javax.net.ssl.trustStoreType", "JKS");
        if (ts != null && tsp != null && tst != null) {
            this.setTrustStore(ts, tsp, tst);
        }
    }

    public SSLConfiguration(KeyStore keyStore, String keyStorePassphrase, KeyStore trustStore) {
        this();
        this.keyStore = keyStore;
        this.keyStorePassphrase = keyStorePassphrase;
        this.trustStore = trustStore;
    }

    public SSLConfiguration(String keyStorePath, String keyStorePassphrase, String keyStoreType,
            String trustStorePath, String trustStorePassphrase, String trustStoreType)
            throws GeneralSecurityException, IOException {
        this();
        this.setKeyStore(keyStorePath, keyStorePassphrase, keyStorePassphrase, keyStoreType);
        this.setTrustStore(keyStorePath, trustStorePassphrase, trustStoreType);
    }

    private static final Provider PROVIDER = new BouncyCastleProvider();

    // TODO: For letsencrypt cert generation using BouncyCastle, see:
    // https://www.mayrhofer.eu.org/create-x509-certs-in-java
    public static SSLConfiguration createSelfSignedCertificate() throws Exception {
        if (log.logInfo()) {
            log.info("Generating self-signed SSL key pair");
        }

        String domain = "localhost";
        SecureRandom random = new SecureRandom();
        final KeyPair keyPair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(/* bits = */1024, random);
            keyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            // Should not reach here, every Java implementation must have RSA key pair generator
            throw new RuntimeException(e);
        }

        //        // To encode the private key into KEY format:
        //        ByteBuffer keyByteBuf = ByteBuffer.allocate(1024);
        //        keyByteBuf.put("-----BEGIN PRIVATE KEY-----\n".getBytes("ASCII"));
        //        keyByteBuf.put(Base64.getEncoder().encode(key.getEncoded()));
        //        keyByteBuf.put("\n-----END PRIVATE KEY-----\n".getBytes("ASCII"));

        // Prepare the information required for generating an X.509 certificate
        X500Name owner = new X500Name("CN=" + domain);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(owner, new BigInteger(64, random),
                new Date(), new Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000), owner,
                keyPair.getPublic());

        // Sign a certificate using the private key
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);
        cert.verify(keyPair.getPublic());

        //        // To encode the certificate into CRT format:
        //        ByteBuffer crtByteBuf = ByteBuffer.allocate(1024);
        //        crtByteBuf.put("-----BEGIN CERTIFICATE-----\n".getBytes("ASCII"));
        //        crtByteBuf.put(Base64.getEncoder().encode(cert.getEncoded()));
        //        crtByteBuf.put("\n-----END CERTIFICATE-----\n".getBytes("ASCII"));
        //        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        //        // To load a CRT format cert back in:
        //        ByteArrayInputStream crtStream = new ByteArrayInputStream(Arrays.copyOf(crtByteBuf.array(),
        //                crtByteBuf.position()));
        //        X509Certificate caCert = (X509Certificate) cf.generateCertificate(crtStream);

        SSLConfiguration cfg = new SSLConfiguration();
        cfg.addTrustedEntity(cert);
        cfg.addIdentity(keyPair.getPrivate(), new X509Certificate[] { cert });
        return cfg;
    }

    public void setRandomNumberGenerator(SecureRandom rng) {
        this.rng = rng;
    }

    /**
     * Configure a timeout value for SSL handshaking.
     * <p>
     * If the remote server is not SSL enabled then it falls to some sort of timeout to determine this, since a
     * non-SSL server is waiting for a request from a client, which is in turn waiting for an SSL handshake to be
     * initiated by the server.
     * <p>
     * This method controls the length of that timeout.
     * <p>
     * This timeout defaults to 10 seconds.
     * <p>
     * The new timeout affects only connections initiated subsequent to the completion of this method call.
     *
     * @param timeout
     *            The timeout (in milliseconds). A value of 0 indicates no timeout should be enforced (not
     *            recommended).
     * @throws IllegalArgumentException
     *             If the timeout provided is negative.
     */
    public void setHandshakeTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout is negative");
        }

        this.handshakeTimeout = timeout;
    }

    public int getHandshakeTimeout() {
        return this.handshakeTimeout;
    }

    /**
     * Set the regular expression used to select the SSL cipher suites to use during SSL handshaking.
     *
     * @param cipherSuitePattern
     *            A regular expression for selecting the set of SSL cipher suites. A <code>null</code> value will
     *            treated as matching <i>all</i> cipher suites.
     * @see #ALL_CIPHER_SUITES
     * @see #ANON_CIPHER_SUITES
     */
    public void setCipherSuitePattern(String cipherSuitePattern) {
        if (cipherSuitePattern == null) {
            cipherSuitePattern = ALL_CIPHER_SUITES;
        }

        synchronized (this) {
            this.cipherSuitePattern = Pattern.compile(cipherSuitePattern);
        }
    }

    /**
     * Set the regular expression used to select the SSL protocol suites to use during SSL handshaking.
     *
     * @param protocolPattern
     *            A regular expression for selecting the set of SSL protocols. A <code>null</code> value will
     *            treated as matching <i>all</i> protocols.
     * @see #ALL_PROTOCOLS
     * @see #TLS_PROTOCOLS
     */
    public void setProtocolPattern(String protocolPattern) {
        if (protocolPattern == null) {
            protocolPattern = ALL_PROTOCOLS;
        }

        synchronized (this) {
            this.protocolPattern = Pattern.compile(protocolPattern);
        }
    }

    public void addTrustedEntities(Collection<X509Certificate> certs) throws GeneralSecurityException, IOException {
        for (X509Certificate certificate : certs) {
            this.addTrustedEntity(certificate);
        }
    }

    public void addTrustedEntity(X509Certificate cert) throws GeneralSecurityException, IOException {
        if (this.trustStore == null) {
            this.trustStore = SSLConfiguration.initKeyStore();
        }
        String alias = cert.getSubjectX500Principal().getName() + ":" + cert.getSerialNumber();
        this.trustStore.setCertificateEntry(alias, cert);
        this.setTrustStore(this.trustStore);
    }

    public void addIdentity(PrivateKey privateKey, X509Certificate[] chain) throws GeneralSecurityException,
            IOException {
        if (this.keyStore == null) {
            this.keyStore = SSLConfiguration.initKeyStore();
        }
        String alias = privateKey.getAlgorithm() + ":" + privateKey.hashCode();
        this.keyStore.setKeyEntry(alias, privateKey, "".toCharArray(), chain);
        this.setKeyStore(this.keyStore, "");

        this.explicitPrivateKey = privateKey;
        this.explicitCertChain = chain;
    }

    public void setClientAuthentication(ClientAuth auth) {
        this.clientAuth = auth;
    }

    public ClientAuth getClientAuthentication() {
        return this.clientAuth;
    }

    // Convenience method
    public void setKeyStore(String storeFile, String keyStorePassphrase, String entryPassphrase, String storeType)
            throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(storeType);
        ks.load(new FileInputStream(storeFile),
                keyStorePassphrase == null ? null : keyStorePassphrase.toCharArray());
        this.setKeyStore(ks, entryPassphrase);

        this.keystoreName = storeFile;
        this.keyStorePassphrase = keyStorePassphrase;
    }

    // Keystore.load must have been called.
    public void setKeyStore(KeyStore ks, String passphrase) throws GeneralSecurityException {
        this.keyStore = ks;
        this.keyStorePassphrase = passphrase;
    }

    // Convenience method
    public void setTrustStore(String storeFile, String passphrase, String storeType)
            throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(storeType);
        ks.load(new FileInputStream(storeFile), passphrase == null ? null : passphrase.toCharArray());
        this.setTrustStore(ks);

        this.truststoreName = storeFile;
    }

    public void setTrustStore(KeyStore ts) throws GeneralSecurityException {
        this.trustStore = ts;
    }

    public SSLContext createContext() throws GeneralSecurityException {
        if (this.explicitContext != null) {
            return this.explicitContext;
        }

        KeyManager[] km = null;
        if (this.keyStore != null) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(this.keyStore, this.keyStorePassphrase == null ? null : this.keyStorePassphrase.toCharArray());
            km = kmf.getKeyManagers();
        }

        TrustManager[] tm = null;
        if (this.trustStore != null) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(this.trustStore);
            tm = tmf.getTrustManagers();
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(km, tm, this.rng);
        return sslContext;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  client auth=" + this.clientAuth);
        sb.append("\n  handshake timeout=" + this.handshakeTimeout + "ms");
        if (this.explicitPrivateKey != null) {
            sb.append("\n  explicit identity(key)=" + this.explicitPrivateKey);
        }
        if (this.explicitCertChain != null) {
            sb.append("\n  explicit identity(certs)=" + Arrays.toString(this.explicitCertChain));
        }
        if (this.keystoreName != null) {
            sb.append("\n  keystore=" + keystoreName);
        }
        if (this.truststoreName != null) {
            sb.append("\n  truststore=" + truststoreName);
        }
        return sb.toString();
    }

    protected String[] selectCiphersuites(String[] supportedCipherSuites) {
        synchronized (this) {
            if (log.logTrace()) {
                log.trace("Selecting cipher suites using pattern [" + this.cipherSuitePattern + "]");
            }
            List<String> ciphers = new ArrayList<>(supportedCipherSuites.length);
            for (String supportedCipherSuite : supportedCipherSuites) {
                if (this.cipherSuitePattern.matcher(supportedCipherSuite).find()) {
                    if (log.logTrace()) {
                        log.trace("Matched " + supportedCipherSuite);
                    }
                    ciphers.add(supportedCipherSuite);
                }
            }
            return ciphers.toArray(new String[0]);
        }
    }

    protected String[] selectProtocols(String[] supportedProtocols) {
        synchronized (this) {
            if (log.logTrace()) {
                log.trace("Selecting protocols using pattern [" + this.protocolPattern + "]");
            }
            List<String> protocols = new ArrayList<>(supportedProtocols.length);
            for (String supportedProtocol : supportedProtocols) {
                if (this.protocolPattern.matcher(supportedProtocol).find()) {
                    if (log.logTrace()) {
                        log.trace("Matched " + supportedProtocol);
                    }
                    protocols.add(supportedProtocol);
                }
            }
            return protocols.toArray(new String[0]);
        }
    }

    //setProtocolPattern etc
    //

    private static KeyStore initKeyStore() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        return ks;
    }

    private static String getProperty(Properties props, String name, String defVal) throws SSLException {
        String v = props.getProperty(name, defVal);
        if (v == null) {
            log.warn("No value for property " + name);
        }
        return v;
    }
}
