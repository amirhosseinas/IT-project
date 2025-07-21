package org.apache.synapse.custom.transports.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;

/**
 * HTTP Transport sender implementation based on Apache HttpClient.
 * Provides connection pooling and HTTP client functionality.
 */
public class HttpTransportSender implements TransportSender {
    private static final Logger logger = LoggerFactory.getLogger(HttpTransportSender.class);
    
    private final int maxTotalConnections;
    private final int maxConnectionsPerRoute;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final int connectionRequestTimeout;
    
    private PoolingHttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;
    
    /**
     * Create a new HTTP transport sender with default settings
     */
    public HttpTransportSender() {
        this(100, 20, 30000, 30000, 30000);
    }
    
    /**
     * Create a new HTTP transport sender with custom settings
     * 
     * @param maxTotalConnections Maximum total connections in the pool
     * @param maxConnectionsPerRoute Maximum connections per route
     * @param connectionTimeout Connection timeout in milliseconds
     * @param socketTimeout Socket timeout in milliseconds
     * @param connectionRequestTimeout Connection request timeout in milliseconds
     */
    public HttpTransportSender(int maxTotalConnections, int maxConnectionsPerRoute,
                             int connectionTimeout, int socketTimeout, int connectionRequestTimeout) {
        this.maxTotalConnections = maxTotalConnections;
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
    }
    
    @Override
    public void init() throws TransportException {
        try {
            // Create connection manager
            connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(maxTotalConnections);
            connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            
            // Create request configuration
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(socketTimeout)
                    .setConnectionRequestTimeout(connectionRequestTimeout)
                    .build();
            
            // Create HTTP client
            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
            
            logger.info("HTTP transport sender initialized with connection pool: maxTotal={}, maxPerRoute={}",
                      maxTotalConnections, maxConnectionsPerRoute);
        } catch (Exception e) {
            throw new TransportException("Failed to initialize HTTP transport sender", e);
        }
    }
    
    @Override
    public Message send(Message message, String endpoint) throws TransportException {
        if (httpClient == null) {
            throw new TransportException("HTTP transport sender not initialized");
        }
        
        try {
            // Parse endpoint URI
            URI uri = new URI(endpoint);
            
            // Create HTTP host for connection pool management
            HttpHost target = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            
            // Create HTTP request
            HttpRequestBase request = createHttpRequest(message, uri);
            
            // Set headers
            for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
            
            // Execute request
            logger.debug("Sending HTTP request to: {}", endpoint);
            HttpResponse response = httpClient.execute(target, request);
            
            // Convert response to Synapse message
            Message responseMessage = convertHttpResponseToSynapseMessage(response);
            
            return responseMessage;
        } catch (URISyntaxException | IOException e) {
            throw new TransportException("Failed to send HTTP request", e);
        }
    }
    
    @Override
    public boolean canHandle(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        
        return endpoint.startsWith("http://") || endpoint.startsWith("https://");
    }
    
    @Override
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            
            if (connectionManager != null) {
                connectionManager.close();
            }
            
            logger.info("HTTP transport sender closed");
        } catch (IOException e) {
            logger.error("Error closing HTTP transport sender", e);
        }
    }
    
    /**
     * Create an HTTP request from a Synapse message
     * 
     * @param message The Synapse message
     * @param uri The endpoint URI
     * @return The HTTP request
     * @throws TransportException if creating the request fails
     */
    private HttpRequestBase createHttpRequest(Message message, URI uri) throws TransportException {
        // Get HTTP method
        String method = (String) message.getProperty("http.method");
        if (method == null) {
            method = "POST"; // Default to POST if not specified
        }
        
        HttpRequestBase request;
        
        // Create request based on method
        switch (method.toUpperCase()) {
            case "GET":
                request = new HttpGet(uri);
                break;
            case "POST":
                request = new HttpPost(uri);
                break;
            case "PUT":
                request = new HttpPut(uri);
                break;
            case "DELETE":
                request = new HttpDelete(uri);
                break;
            case "HEAD":
                request = new HttpHead(uri);
                break;
            case "OPTIONS":
                request = new HttpOptions(uri);
                break;
            case "TRACE":
                request = new HttpTrace(uri);
                break;
            case "PATCH":
                request = new HttpPatch(uri);
                break;
            default:
                throw new TransportException("Unsupported HTTP method: " + method);
        }
        
        // Set payload for methods that support it
        if (request instanceof HttpEntityEnclosingRequestBase && message.getPayload() != null) {
            ByteArrayEntity entity = new ByteArrayEntity(message.getPayload());
            if (message.getContentType() != null) {
                entity.setContentType(message.getContentType());
            }
            ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
        }
        
        return request;
    }
    
    /**
     * Convert an HTTP response to a Synapse message
     * 
     * @param response The HTTP response
     * @return The Synapse message
     * @throws IOException if reading the response fails
     */
    private Message convertHttpResponseToSynapseMessage(HttpResponse response) throws IOException {
        Message message = new Message(UUID.randomUUID().toString());
        message.setDirection(Message.Direction.RESPONSE);
        
        // Set status code
        message.setProperty("http.status.code", response.getStatusLine().getStatusCode());
        message.setProperty("http.status.line", response.getStatusLine().toString());
        
        // Copy headers
        for (Header header : response.getAllHeaders()) {
            message.setHeader(header.getName(), header.getValue());
        }
        
        // Set content type
        if (response.getEntity() != null && response.getEntity().getContentType() != null) {
            message.setContentType(response.getEntity().getContentType().getValue());
        }
        
        // Copy payload if present
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            message.setPayload(IOUtils.toByteArray(entity.getContent()));
        }
        
        return message;
    }
    
    /**
     * Set the maximum connections for a specific route
     * 
     * @param host The target host
     * @param port The target port
     * @param scheme The target scheme (http/https)
     * @param maxConnections The maximum connections
     */
    public void setMaxConnectionsForRoute(String host, int port, String scheme, int maxConnections) {
        if (connectionManager != null) {
            HttpHost target = new HttpHost(host, port, scheme);
            connectionManager.setMaxPerRoute(new HttpRoute(target), maxConnections);
            logger.info("Set max connections for route {}: {}", target, maxConnections);
        }
    }
} 