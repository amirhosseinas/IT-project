package org.apache.synapse.custom.message.formatter;

import org.apache.synapse.custom.message.Message;

import java.io.OutputStream;

/**
 * Interface for message formatters that convert messages to specific formats.
 */
public interface MessageFormatter {
    
    /**
     * Format a message and write the formatted content to the output stream
     * 
     * @param message The message to format
     * @param outputStream The output stream to write the formatted content to
     * @throws MessageFormatterException if formatting fails
     */
    void formatMessage(Message message, OutputStream outputStream) throws MessageFormatterException;
    
    /**
     * Format a message and return the formatted content as a byte array
     * 
     * @param message The message to format
     * @return The formatted content as a byte array
     * @throws MessageFormatterException if formatting fails
     */
    byte[] formatMessage(Message message) throws MessageFormatterException;
    
    /**
     * Check if this formatter can handle the given message
     * 
     * @param message The message to check
     * @return true if this formatter can handle the message, false otherwise
     */
    boolean canFormat(Message message);
    
    /**
     * Get the content type of the formatted message
     * 
     * @param message The message to get the content type for
     * @return The content type of the formatted message
     */
    String getContentType(Message message);
    
    /**
     * Exception thrown when message formatting fails
     */
    class MessageFormatterException extends Exception {
        public MessageFormatterException(String message) {
            super(message);
        }
        
        public MessageFormatterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 