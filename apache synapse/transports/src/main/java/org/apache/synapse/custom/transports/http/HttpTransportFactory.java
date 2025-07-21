package org.apache.synapse.custom.transports.http;

import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating HTTP and HTTPS transport components.
 */
public class HttpTransportFactory {
    private static final Logger logger = LoggerFactory.getLogger(HttpTransportFactory.class);
    
    /**
     * Create a transport listener for the specified protocol
     * 
     * @param protocol The transport protocol ("http" or "https")
     * @param host The host to bind to
     * @param port The port to listen on
     * @return The transport listener
     * @throws TransportException if the protocol is not supported
     */
    public static TransportListener createListener(String protocol, String host, int port) throws TransportException {
        if ("http".equalsIgnoreCase(protocol)) {
            return new HttpTransportListener(host, port, false);
        } else if ("https".equalsIgnoreCase(protocol)) {
            throw new TransportException("HTTPS listener requires keystore configuration");
        } else {
            throw new TransportException("Unsupported protocol: " + protocol);
        }
    }
    
    /**
     * Create an HTTPS transport listener
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     * @return The HTTPS transport listener
     */
    public static TransportListener createHttpsListener(String host, int port,
                                                      String keystorePath, String keystorePassword, String keystoreType) {
        return new HttpsTransportListener(host, port, keystorePath, keystorePassword, keystoreType);
    }
    
    /**
     * Create an HTTPS transport listener with full configuration
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
     * @return The HTTPS transport listener
     */
    public static TransportListener createHttpsListener(String host, int port,
                                                      int ioThreadCount, int workerThreadCount,
                                                      int socketTimeout, int connectionTimeout,
                                                      String keystorePath, String keystorePassword, String keystoreType,
                                                      String truststorePath, String truststorePassword, String truststoreType,
                                                      String[] enabledProtocols, String[] enabledCipherSuites) {
        return new HttpsTransportListener(host, port, ioThreadCount, workerThreadCount, socketTimeout, connectionTimeout,
                                        keystorePath, keystorePassword, keystoreType,
                                        truststorePath, truststorePassword, truststoreType,
                                        enabledProtocols, enabledCipherSuites);
    }
    
    /**
     * Create a transport sender for the specified protocol
     * 
     * @param protocol The transport protocol ("http" or "https")
     * @return The transport sender
     * @throws TransportException if the protocol is not supported
     */
    public static TransportSender createSender(String protocol) throws TransportException {
        if ("http".equalsIgnoreCase(protocol)) {
            return new HttpTransportSender();
        } else if ("https".equalsIgnoreCase(protocol)) {
            throw new TransportException("HTTPS sender requires keystore configuration");
        } else {
            throw new TransportException("Unsupported protocol: " + protocol);
        }
    }
    
    /**
     * Create an HTTPS transport sender
     * 
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (e.g., "JKS", "PKCS12")
     * @return The HTTPS transport sender
     */
    public static TransportSender createHttpsSender(String keystorePath, String keystorePassword, String keystoreType) {
        return new HttpsTransportSender(keystorePath, keystorePassword, keystoreType);
    }
    
    /**
     * Create an HTTPS transport sender with full configuration
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
     * @return The HTTPS transport sender
     */
    public static TransportSender createHttpsSender(int maxTotalConnections, int maxConnectionsPerRoute,
                                                  int connectionTimeout, int socketTimeout, int connectionRequestTimeout,
                                                  String keystorePath, String keystorePassword, String keystoreType,
                                                  String truststorePath, String truststorePassword, String truststoreType,
                                                  String[] enabledProtocols, String[] enabledCipherSuites,
                                                  boolean hostnameVerification) {
        return new HttpsTransportSender(maxTotalConnections, maxConnectionsPerRoute,
                                      connectionTimeout, socketTimeout, connectionRequestTimeout,
                                      keystorePath, keystorePassword, keystoreType,
                                      truststorePath, truststorePassword, truststoreType,
                                      enabledProtocols, enabledCipherSuites,
                                      hostnameVerification);
    }
} 