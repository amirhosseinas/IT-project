package org.apache.synapse.custom.transports.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.synapse.custom.transports.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
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

/**
 * HTTPS Transport sender implementation that extends the HTTP transport
 * with SSL/TLS support.
 */
public class HttpsTransportSender extends HttpTransportSender {
    private static final Logger logger = LoggerFactory.getLogger(HttpsTransportSender.class);
    
    private final String keystorePath;
    private final String keystorePassword;
    private final String keystoreType;
    private final String truststorePath;
    private final String truststorePassword;
    private final String truststoreType;
    private final String[] enabledProtocols;
    private final String[] enabledCipherSuites;
    private final boolean hostnameVerification;
    
    /**
     * Create a new HTTPS transport sender with default settings
     * 
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     */
    public HttpsTransportSender(String keystorePath, String keystorePassword, String keystoreType) {
        this(100, 20, 30000, 30000, 30000,
             keystorePath, keystorePassword, keystoreType,
             null, null, null,
             new String[]{"TLSv1.2", "TLSv1.3"}, null, true);
    }
    
    /**
     * Create a new HTTPS transport sender with custom settings
     * 
     * @param maxTotalConnections Maximum total connections in the pool
     * @param maxConnectionsPerRoute Maximum connections per route
     * @param connectionTimeout Connection timeout in milliseconds
     * @param socketTimeout Socket timeout in milliseconds
     * @param connectionRequestTimeout Connection request timeout in milliseconds
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     * @param truststorePath Path to the truststore file (optional)
     * @param truststorePassword Password for the truststore (optional)
     * @param truststoreType Type of truststore (e.g., "JKS", "PKCS12") (optional)
     * @param enabledProtocols Array of enabled SSL/TLS protocols (optional)
     * @param enabledCipherSuites Array of enabled cipher suites (optional)
     * @param hostnameVerification Whether to verify hostnames
     */
    public HttpsTransportSender(int maxTotalConnections, int maxConnectionsPerRoute,
                              int connectionTimeout, int socketTimeout, int connectionRequestTimeout,
                              String keystorePath, String keystorePassword, String keystoreType,
                              String truststorePath, String truststorePassword, String truststoreType,
                              String[] enabledProtocols, String[] enabledCipherSuites,
                              boolean hostnameVerification) {
        super(maxTotalConnections, maxConnectionsPerRoute, connectionTimeout, socketTimeout, connectionRequestTimeout);
        
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.enabledProtocols = enabledProtocols;
        this.enabledCipherSuites = enabledCipherSuites;
        this.hostnameVerification = hostnameVerification;
    }
    
    @Override
    public void init() throws TransportException {
        try {
            // Create SSL context
            SSLContext sslContext = createSSLContext();
            
            // Create SSL socket factory
            SSLConnectionSocketFactory sslSocketFactory;
            if (hostnameVerification) {
                sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        enabledProtocols,
                        enabledCipherSuites,
                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            } else {
                sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        enabledProtocols,
                        enabledCipherSuites,
                        NoopHostnameVerifier.INSTANCE);
            }
            
            // Create socket factory registry
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();
            
            // Create connection manager
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connectionManager.setMaxTotal(getMaxTotalConnections());
            connectionManager.setDefaultMaxPerRoute(getMaxConnectionsPerRoute());
            
            // Create request configuration
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(getConnectionTimeout())
                    .setSocketTimeout(getSocketTimeout())
                    .setConnectionRequestTimeout(getConnectionRequestTimeout())
                    .build();
            
            // Create HTTP client
            setHttpClient(HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .build());
            
            setConnectionManager(connectionManager);
            
            logger.info("HTTPS transport sender initialized with SSL/TLS support");
        } catch (Exception e) {
            throw new TransportException("Failed to initialize HTTPS transport sender", e);
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
    
    @Override
    public boolean canHandle(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        
        return endpoint.startsWith("https://");
    }
    
    // Getter methods for HttpTransportSender fields (needed for initialization)
    
    protected int getMaxTotalConnections() {
        return super.maxTotalConnections;
    }
    
    protected int getMaxConnectionsPerRoute() {
        return super.maxConnectionsPerRoute;
    }
    
    protected int getConnectionTimeout() {
        return super.connectionTimeout;
    }
    
    protected int getSocketTimeout() {
        return super.socketTimeout;
    }
    
    protected int getConnectionRequestTimeout() {
        return super.connectionRequestTimeout;
    }
    
    // Setter methods for HttpTransportSender fields
    
    protected void setConnectionManager(PoolingHttpClientConnectionManager connectionManager) {
        super.connectionManager = connectionManager;
    }
    
    protected void setHttpClient(org.apache.http.impl.client.CloseableHttpClient httpClient) {
        super.httpClient = httpClient;
    }
} 