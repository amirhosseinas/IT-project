package org.apache.synapse.custom.mediation.endpoint;

import org.apache.synapse.custom.mediation.Endpoint;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for endpoints.
 * Provides common functionality and default implementations.
 */
public abstract class AbstractEndpoint implements Endpoint {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final String name;
    private final String url;
    private boolean available;
    private int failureCount;
    private long lastFailureTime;
    private int maxFailureCount;
    private long retryTimeout;
    
    /**
     * Create a new abstract endpoint
     * 
     * @param name The endpoint name
     * @param url The endpoint URL
     */
    public AbstractEndpoint(String name, String url) {
        this.name = name;
        this.url = url;
        this.available = true;
        this.failureCount = 0;
        this.lastFailureTime = 0;
        this.maxFailureCount = 3;
        this.retryTimeout = 30000; // 30 seconds
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getUrl() {
        return url;
    }
    
    @Override
    public boolean isAvailable() {
        // If we're not available, check if retry timeout has elapsed
        if (!available) {
            long now = System.currentTimeMillis();
            if (now - lastFailureTime > retryTimeout) {
                // Retry timeout has elapsed, reset failure count and mark as available
                logger.info("Retry timeout elapsed for endpoint {}, marking as available", name);
                available = true;
                failureCount = 0;
            }
        }
        
        return available;
    }
    
    @Override
    public Message send(Message message) throws MediationEngine.MediationException {
        if (!isAvailable()) {
            throw new MediationEngine.MediationException("Endpoint " + name + " is not available");
        }
        
        try {
            Message response = doSend(message);
            
            // Reset failure count on success
            failureCount = 0;
            
            return response;
        } catch (Exception e) {
            // Increment failure count
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            
            // Check if we've reached the max failure count
            if (failureCount >= maxFailureCount) {
                logger.warn("Endpoint {} has failed {} times, marking as unavailable", name, failureCount);
                available = false;
            }
            
            throw new MediationEngine.MediationException("Error sending message to endpoint " + name, e);
        }
    }
    
    /**
     * Send a message to this endpoint
     * 
     * @param message The message to send
     * @return The response message
     * @throws Exception if sending fails
     */
    protected abstract Message doSend(Message message) throws Exception;
    
    /**
     * Get the maximum failure count
     * 
     * @return The maximum failure count
     */
    public int getMaxFailureCount() {
        return maxFailureCount;
    }
    
    /**
     * Set the maximum failure count
     * 
     * @param maxFailureCount The maximum failure count
     */
    public void setMaxFailureCount(int maxFailureCount) {
        this.maxFailureCount = maxFailureCount;
    }
    
    /**
     * Get the retry timeout
     * 
     * @return The retry timeout in milliseconds
     */
    public long getRetryTimeout() {
        return retryTimeout;
    }
    
    /**
     * Set the retry timeout
     * 
     * @param retryTimeout The retry timeout in milliseconds
     */
    public void setRetryTimeout(long retryTimeout) {
        this.retryTimeout = retryTimeout;
    }
    
    /**
     * Get the current failure count
     * 
     * @return The current failure count
     */
    public int getFailureCount() {
        return failureCount;
    }
    
    /**
     * Reset the failure count and mark the endpoint as available
     */
    public void reset() {
        failureCount = 0;
        available = true;
        logger.info("Reset endpoint: {}", name);
    }
} 