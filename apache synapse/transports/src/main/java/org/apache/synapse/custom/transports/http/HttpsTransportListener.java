package org.apache.synapse.custom.transports.http;

import org.apache.http.impl.nio.reactor.SSLIOSessionStrategy;
import org.apache.http.impl.nio.reactor.SSLSetupHandler;
import org.apache.synapse.custom.transports.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

/**
 * HTTPS Transport listener implementation that extends the HTTP transport
 * with SSL/TLS support.
 */
public class HttpsTransportListener extends HttpTransportListener {
    private static final Logger logger = LoggerFactory.getLogger(HttpsTransportListener.class);
    
    private final String keystorePath;
    private final String keystorePassword;
    private final String keystoreType;
    private final String truststorePath;
    private final String truststorePassword;
    private final String truststoreType;
    private final String[] enabledProtocols;
    private final String[] enabledCipherSuites;
    
    /**
     * Create a new HTTPS transport listener
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     */
    public HttpsTransportListener(String host, int port, 
                                String keystorePath, String keystorePassword, String keystoreType) {
        this(host, port, keystorePath, keystorePassword, keystoreType,
             null, null, null, 
             new String[]{"TLSv1.2", "TLSv1.3"}, null);
    }
    
    /**
     * Create a new HTTPS transport listener with full configuration
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     * @param truststorePath Path to the truststore file (optional)
     * @param truststorePassword Password for the truststore (optional)
     * @param truststoreType Type of truststore (e.g., "JKS", "PKCS12") (optional)
     * @param enabledProtocols Array of enabled SSL/TLS protocols (optional)
     * @param enabledCipherSuites Array of enabled cipher suites (optional)
     */
    public HttpsTransportListener(String host, int port, 
                                String keystorePath, String keystorePassword, String keystoreType,
                                String truststorePath, String truststorePassword, String truststoreType,
                                String[] enabledProtocols, String[] enabledCipherSuites) {
        super(host, port, true);
        
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.enabledProtocols = enabledProtocols;
        this.enabledCipherSuites = enabledCipherSuites;
    }
    
    /**
     * Create a new HTTPS transport listener with custom configuration
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @param ioThreadCount Number of I/O threads
     * @param workerThreadCount Number of worker threads
     * @param socketTimeout Socket timeout in milliseconds
     * @param connectionTimeout Connection timeout in milliseconds
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     * @param truststorePath Path to the truststore file (optional)
     * @param truststorePassword Password for the truststore (optional)
     * @param truststoreType Type of truststore (e.g., "JKS", "PKCS12") (optional)
     * @param enabledProtocols Array of enabled SSL/TLS protocols (optional)
     * @param enabledCipherSuites Array of enabled cipher suites (optional)
     */
    public HttpsTransportListener(String host, int port, 
                                int ioThreadCount, int workerThreadCount, 
                                int socketTimeout, int connectionTimeout,
                                String keystorePath, String keystorePassword, String keystoreType,
                                String truststorePath, String truststorePassword, String truststoreType,
                                String[] enabledProtocols, String[] enabledCipherSuites) {
        super(host, port, true, ioThreadCount, workerThreadCount, socketTimeout, connectionTimeout);
        
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.enabledProtocols = enabledProtocols;
        this.enabledCipherSuites = enabledCipherSuites;
    }
    
    @Override
    public void init() throws TransportException {
        try {
            // Create SSL context
            SSLContext sslContext = createSSLContext();
            
            // Create SSL setup handler
            SSLSetupHandler sslSetupHandler = new SSLSetupHandler() {
                @Override
                public void initalize(SSLEngine sslEngine) throws SSLException {
                    SSLParameters sslParams = sslEngine.getSSLParameters();
                    
                    // Set enabled protocols if specified
                    if (enabledProtocols != null && enabledProtocols.length > 0) {
                        sslParams.setProtocols(enabledProtocols);
                    }
                    
                    // Set enabled cipher suites if specified
                    if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
                        sslParams.setCipherSuites(enabledCipherSuites);
                    }
                    
                    sslEngine.setSSLParameters(sslParams);
                    sslEngine.setUseClientMode(false);
                    
                    logger.debug("SSL Engine initialized with protocols: {}, cipher suites: {}",
                              Arrays.toString(sslEngine.getEnabledProtocols()),
                              Arrays.toString(sslEngine.getEnabledCipherSuites()));
                }
                
                @Override
                public void verify(SSLEngine sslEngine, SSLSession sslSession) throws SSLException {
                    // Client certificate verification can be done here if needed
                }
            };
            
            // Initialize the HTTP transport listener
            super.init();
            
            logger.info("HTTPS transport listener initialized with SSL/TLS support");
        } catch (Exception e) {
            throw new TransportException("Failed to initialize HTTPS transport listener", e);
        }
    }
    
    /**
     * Create an SSL context with the configured keystore and truststore
     * 
     * @return The SSL context
     * @throws TransportException if creating the SSL context fails
     */
    private SSLContext createSSLContext() throws TransportException {
        try {
            // Initialize SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            
            // Initialize key managers
            KeyManager[] keyManagers = null;
            if (keystorePath != null && keystorePassword != null) {
                KeyStore keyStore = KeyStore.getInstance(keystoreType != null ? keystoreType : KeyStore.getDefaultType());
                try (InputStream is = new FileInputStream(keystorePath)) {
                    keyStore.load(is, keystorePassword.toCharArray());
                }
                
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, keystorePassword.toCharArray());
                keyManagers = kmf.getKeyManagers();
            }
            
            // Initialize trust managers
            TrustManager[] trustManagers = null;
            if (truststorePath != null && truststorePassword != null) {
                KeyStore trustStore = KeyStore.getInstance(truststoreType != null ? truststoreType : KeyStore.getDefaultType());
                try (InputStream is = new FileInputStream(truststorePath)) {
                    trustStore.load(is, truststorePassword.toCharArray());
                }
                
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                trustManagers = tmf.getTrustManagers();
            }
            
            // Initialize SSL context with key managers and trust managers
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            
            return sslContext;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                 UnrecoverableKeyException | KeyManagementException e) {
            throw new TransportException("Failed to create SSL context", e);
        }
    }
} 