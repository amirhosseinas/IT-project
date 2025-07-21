package org.apache.synapse.custom.transports;

import org.apache.synapse.custom.message.Message;

/**
 * Interface for transport senders that send outgoing messages.
 */
public interface TransportSender {
    
    /**
     * Initialize the transport sender
     * 
     * @throws TransportException if initialization fails
     */
    void init() throws TransportException;
    
    /**
     * Send a message
     * 
     * @param message The message to send
     * @param endpoint The endpoint to send the message to
     * @return The response message or null if no response is expected
     * @throws TransportException if sending fails
     */
    Message send(Message message, String endpoint) throws TransportException;
    
    /**
     * Check if this sender can handle the given endpoint
     * 
     * @param endpoint The endpoint to check
     * @return true if this sender can handle the endpoint, false otherwise
     */
    boolean canHandle(String endpoint);
    
    /**
     * Close the transport sender and release resources
     */
    void close();
} 