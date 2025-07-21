package org.apache.synapse.custom.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes messages by applying registered handlers in sequence.
 */
public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    
    private final List<MessageHandler> handlers;
    
    /**
     * Create a new message processor
     */
    public MessageProcessor() {
        this.handlers = new ArrayList<>();
    }
    
    /**
     * Register a message handler
     * 
     * @param handler The handler to register
     */
    public void registerHandler(MessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        
        handlers.add(handler);
        logger.info("Registered message handler: {}", handler.getClass().getName());
    }
    
    /**
     * Process a message through all applicable handlers
     * 
     * @param message The message to process
     * @return The processed message
     * @throws MessageProcessingException if processing fails
     */
    public Message process(Message message) throws MessageProcessingException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        Message processedMessage = message;
        boolean handled = false;
        
        try {
            for (MessageHandler handler : handlers) {
                if (handler.canHandle(processedMessage)) {
                    processedMessage = handler.handle(processedMessage);
                    handled = true;
                }
            }
            
            if (!handled) {
                logger.warn("No handler found for message: {}", message.getMessageId());
            }
            
            return processedMessage;
        } catch (Exception e) {
            logger.error("Message processing failed", e);
            throw new MessageProcessingException("Message processing failed", e);
        }
    }
    
    /**
     * Exception thrown when message processing fails
     */
    public static class MessageProcessingException extends Exception {
        public MessageProcessingException(String message) {
            super(message);
        }
        
        public MessageProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 