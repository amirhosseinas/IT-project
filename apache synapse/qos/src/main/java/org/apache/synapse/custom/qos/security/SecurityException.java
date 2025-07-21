package org.apache.synapse.custom.qos.security;

/**
 * Exception thrown for security-related errors.
 */
public class SecurityException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Create a new security exception with a message
     * 
     * @param message Error message
     */
    public SecurityException(String message) {
        super(message);
    }
    
    /**
     * Create a new security exception with a message and cause
     * 
     * @param message Error message
     * @param cause Cause of the exception
     */
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
} 