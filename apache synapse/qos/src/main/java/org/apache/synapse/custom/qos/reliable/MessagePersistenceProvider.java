package org.apache.synapse.custom.qos.reliable;

/**
 * Interface for message persistence providers in the reliable messaging system.
 * Implementations of this interface handle storing and retrieving messages
 * to ensure they are not lost in case of system failures.
 */
public interface MessagePersistenceProvider {
    
    /**
     * Persist a message to storage
     * 
     * @param context The message context to persist
     */
    void persistMessage(MessageContext context);
    
    /**
     * Remove a message from storage
     * 
     * @param messageId The message ID to remove
     */
    void removeMessage(String messageId);
    
    /**
     * Move a message to the dead letter queue in storage
     * 
     * @param messageId The message ID to move
     */
    void moveToDeadLetter(String messageId);
    
    /**
     * Load all pending messages from storage
     * 
     * @return Array of message contexts
     */
    MessageContext[] loadPendingMessages();
    
    /**
     * Load all dead letter messages from storage
     * 
     * @return Array of message contexts
     */
    MessageContext[] loadDeadLetterMessages();
    
    /**
     * Initialize the persistence provider
     */
    void initialize();
    
    /**
     * Close the persistence provider and release resources
     */
    void close();
} 