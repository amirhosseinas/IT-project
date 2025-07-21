package org.apache.synapse.custom.message;

/**
 * Interface for message handlers that process specific types of messages.
 */
public interface MessageHandler {
    
    /**
     * Handle and process a message
     * 
     * @param message The message to process
     * @return The processed message
     * @throws MessageProcessor.MessageProcessingException if processing fails
     */
    Message handle(Message message) throws MessageProcessor.MessageProcessingException;
    
    /**
     * Validates if this handler can process the given message
     * 
     * @param message The message to validate
     * @return true if this handler can process the message, false otherwise
     */
    boolean canHandle(Message message);
} 