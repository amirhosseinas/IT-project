package org.apache.synapse.custom.mediation.endpoint;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;
import org.apache.synapse.custom.message.formatter.MessageFormatterUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint implementation for HTTP endpoints.
 */
public class HttpEndpoint extends AbstractEndpoint {
    
    private int connectionTimeout;
    private int socketTimeout;
    private boolean followRedirects;
    
    /**
     * Create a new HTTP endpoint
     * 
     * @param name The endpoint name
     * @param url The endpoint URL
     */
    public HttpEndpoint(String name, String url) {
        super(name, url);
        this.connectionTimeout = 30000; // 30 seconds
        this.socketTimeout = 60000; // 60 seconds
        this.followRedirects = true;
    }
    
    @Override
    protected Message doSend(Message message) throws Exception {
        URL url = new URL(getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Configure connection
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(socketTimeout);
        connection.setInstanceFollowRedirects(followRedirects);
        connection.setRequestMethod(getRequestMethod(message));
        connection.setDoOutput(true);
        
        // Set headers
        for (Map.Entry<String, String> header : message.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        
        // Set content type if available
        if (message.getContentType() != null) {
            connection.setRequestProperty("Content-Type", message.getContentType());
        }
        
        // Write payload if available
        if (message.getPayload() != null && message.getPayload().length > 0) {
            connection.getOutputStream().write(message.getPayload());
        }
        
        // Send request and get response
        int responseCode = connection.getResponseCode();
        logger.debug("HTTP response code: {}", responseCode);
        
        // Create response message
        Message response = new Message(UUID.randomUUID().toString());
        response.setDirection(Message.Direction.RESPONSE);
        
        // Set response headers
        for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                response.setHeader(header.getKey(), String.join(", ", header.getValue()));
            }
        }
        
        // Set content type
        String contentType = connection.getContentType();
        if (contentType != null) {
            response.setContentType(contentType);
        }
        
        // Read response payload
        InputStream inputStream;
        if (responseCode >= 400) {
            inputStream = connection.getErrorStream();
        } else {
            inputStream = connection.getInputStream();
        }
        
        if (inputStream != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            response.setPayload(outputStream.toByteArray());
        }
        
        // Set response code as a property
        response.setProperty("HTTP_STATUS_CODE", responseCode);
        
        // Clean up
        connection.disconnect();
        
        return response;
    }
    
    /**
     * Get the request method from the message
     * 
     * @param message The message
     * @return The request method
     */
    private String getRequestMethod(Message message) {
        String method = (String) message.getProperty("HTTP_METHOD");
        if (method != null) {
            return method;
        }
        
        // Default to POST if payload is present, GET otherwise
        return (message.getPayload() != null && message.getPayload().length > 0) ? "POST" : "GET";
    }
    
    /**
     * Get the connection timeout
     * 
     * @return The connection timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    /**
     * Set the connection timeout
     * 
     * @param connectionTimeout The connection timeout in milliseconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    /**
     * Get the socket timeout
     * 
     * @return The socket timeout in milliseconds
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }
    
    /**
     * Set the socket timeout
     * 
     * @param socketTimeout The socket timeout in milliseconds
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
    
    /**
     * Check if redirects are followed
     * 
     * @return true if redirects are followed, false otherwise
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }
    
    /**
     * Set whether redirects are followed
     * 
     * @param followRedirects true to follow redirects, false otherwise
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }
} 