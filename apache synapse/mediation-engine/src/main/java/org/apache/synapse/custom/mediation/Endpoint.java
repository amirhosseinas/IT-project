package org.apache.synapse.custom.mediation;

import org.apache.synapse.custom.message.Message;

/**
 * Interface for all endpoints in the system.
 * An endpoint represents a destination for messages.
 */
public interface Endpoint {
    
    /**
     * Get the name of this endpoint
     * 
     * @return The endpoint name
     */
    String getName();
    
    /**
     * Get the URL of this endpoint
     * 
     * @return The endpoint URL
     */
    String getUrl();
    
    /**
     * Send a message to this endpoint
     * 
     * @param message The message to send
     * @return The response message
     * @throws MediationEngine.MediationException if sending fails
     */
    Message send(Message message) throws MediationEngine.MediationException;
    
    /**
     * Check if this endpoint is available
     * 
     * @return true if the endpoint is available, false otherwise
     */
    boolean isAvailable();
} 