package org.apache.synapse.custom.qos.reliable;

/**
 * Callback interface for message delivery in the reliable messaging system.
 */
@FunctionalInterface
public interface MessageDeliveryCallback {
    
    /**
     * Called when a message is ready for delivery
     * 
     * @param messageId The message identifier
     * @param message The message payload
     * @throws Exception if message delivery fails
     */
    void onMessageDelivery(String messageId, Object message) throws Exception;
} 