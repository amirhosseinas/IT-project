package org.apache.synapse.custom.transports;

/**
 * Exception thrown when a transport operation fails.
 */
public class TransportException extends Exception {
    
    /**
     * Create a new transport exception
     * 
     * @param message The exception message
     */
    public TransportException(String message) {
        super(message);
    }
    
    /**
     * Create a new transport exception
     * 
     * @param message The exception message
     * @param cause The cause of the exception
     */
    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
} 