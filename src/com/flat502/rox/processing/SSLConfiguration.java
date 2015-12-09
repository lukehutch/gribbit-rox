package com.flat502.rox.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
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
import java.util.Base64;
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

    // The following is taken from:
    // http://stackoverflow.com/questions/925377/generate-certificates-public-and-private-keys-with-java
    // N.B. if this fails in a future JDK, replace with:
    // github.com/netty/netty/blob/master/handler/src/main/java/io/netty/handler/ssl/util/SelfSignedCertificate.java
    // For complete cert generation using BouncyCastle, see:
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
        PrivateKey key = keyPair.getPrivate();

        // Prepare the information required for generating an X.509 certificate
        X500Name owner = new X500Name("CN=" + domain);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(owner, new BigInteger(64, random),
                new Date(), new Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000), owner,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(key);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);
        cert.verify(keyPair.getPublic());

        // Encode the private key into a file
        File keyFile = File.createTempFile("selfsigned_" + domain + '_', ".key");
        keyFile.deleteOnExit();
        OutputStream keyOut = new FileOutputStream(keyFile);
        try {
            ByteBuffer byteBuf = ByteBuffer.allocate(1024);
            byteBuf.put("-----BEGIN PRIVATE KEY-----\n".getBytes("ASCII"));
            byteBuf.put(Base64.getEncoder().encode(key.getEncoded()));
            byteBuf.put("\n-----END PRIVATE KEY-----\n".getBytes("ASCII"));
            keyOut.write(byteBuf.array(), 0, byteBuf.position());
            keyOut.close();
            keyOut = null;
        } finally {
            if (keyOut != null) {
                keyOut.close();
            }
        }

        // Encode the certificate into a CRT file
        File certFile = File.createTempFile("selfsigned_" + domain + '_', ".crt");
        certFile.deleteOnExit();
        OutputStream crtOut = new FileOutputStream(certFile);
        try {
            ByteBuffer byteBuf = ByteBuffer.allocate(1024);
            byteBuf.put("-----BEGIN CERTIFICATE-----\n".getBytes("ASCII"));
            byteBuf.put(Base64.getEncoder().encode(cert.getEncoded()));
            byteBuf.put("\n-----END CERTIFICATE-----\n".getBytes("ASCII"));
            crtOut.write(byteBuf.array(), 0, byteBuf.position());
            crtOut.close();
            crtOut = null;
        } finally {
            if (crtOut != null) {
                crtOut.close();
            }
        }

        return new SSLConfiguration(keyFile.getPath(), null, "pkcs12", certFile.getPath(), null, "pkcs12");

        //        KeyStore keyStore = KeyStore.getInstance("JKS");
        //        keyStore.load(null, null);
        //
        //        CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        //        keypair.generate(/* keysize = */1024);
        //        PrivateKey privKey = keypair.getPrivateKey();
        //
        //        X509Certificate[] chain = new X509Certificate[1];
        //        X500Name x500Name = new X500Name(/* commonName = */"localhost", /* organizationalUnit = */
        //        "None", /* organization = */"None", /* city = */"Nowhere", /* state = */"Nowhere", /* country = */
        //        "Nowhere");
        //        chain[0] = keypair.getSelfCertificate(x500Name, /* validity = */(long) 3 * 365 * 24 * 60 * 60);
        //
        //        String keyStorePassphrase = "changeit";
        //        char[] keyPass = keyStorePassphrase.toCharArray();
        //        keyStore.setKeyEntry(/* alias = */"testkey", privKey, keyPass, chain);
        //
        //        keyStore.store(new FileOutputStream("/tmp/.keystore"), keyPass); // TODO
        //
        //        return new SSLConfiguration(keyStore, keyStorePassphrase, keyStore);
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
            this.trustStore = this.initKeyStore();
        }
        String alias = cert.getSubjectDN().getName() + ":" + cert.getSerialNumber();
        this.trustStore.setCertificateEntry(alias, cert);
        this.setTrustStore(this.trustStore);
    }

    public void addIdentity(PrivateKey privateKey, X509Certificate[] chain) throws GeneralSecurityException,
            IOException {
        if (this.keyStore == null) {
            this.keyStore = this.initKeyStore();
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
            for (int i = 0; i < supportedCipherSuites.length; i++) {
                if (this.cipherSuitePattern.matcher(supportedCipherSuites[i]).find()) {
                    if (log.logTrace()) {
                        log.trace("Matched " + supportedCipherSuites[i]);
                    }
                    ciphers.add(supportedCipherSuites[i]);
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
            for (int i = 0; i < supportedProtocols.length; i++) {
                if (this.protocolPattern.matcher(supportedProtocols[i]).find()) {
                    if (log.logTrace()) {
                        log.trace("Matched " + supportedProtocols[i]);
                    }
                    protocols.add(supportedProtocols[i]);
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
