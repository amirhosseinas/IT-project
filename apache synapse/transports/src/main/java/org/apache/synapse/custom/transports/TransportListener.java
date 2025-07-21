package org.apache.synapse.custom.transports;

import org.apache.synapse.custom.message.Message;

/**
 * Interface for transport listeners that receive incoming messages.
 */
public interface TransportListener {
    
    /**
     * Initialize the transport listener
     * 
     * @throws TransportException if initialization fails
     */
    void init() throws TransportException;
    
    /**
     * Start the transport listener
     * 
     * @throws TransportException if starting fails
     */
    void start() throws TransportException;
    
    /**
     * Stop the transport listener
     * 
     * @throws TransportException if stopping fails
     */
    void stop() throws TransportException;
    
    /**
     * Check if the listener is running
     * 
     * @return true if the listener is running, false otherwise
     */
    boolean isRunning();
    
    /**
     * Set the message callback for this listener
     * 
     * @param callback The callback to invoke when a message is received
     */
    void setMessageCallback(MessageCallback callback);
    
    /**
     * Interface for message callbacks
     */
    interface MessageCallback {
        /**
         * Called when a message is received
         * 
         * @param message The received message
         * @return The response message or null if no response is required
         */
        Message onMessage(Message message);
    }
} 