package org.apache.synapse.custom.qos.reliable;

import java.io.Serializable;

/**
 * Represents the context of a message in the reliable messaging system.
 * Contains the message itself along with metadata for tracking delivery.
 */
public class MessageContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String messageId;
    private final Object message;
    private final String sequenceId;
    private final long creationTime;
    
    private int retryCount;
    private long lastAttemptTime;
    private boolean acknowledged;
    
    /**
     * Create a new message context
     * 
     * @param messageId Unique message identifier
     * @param message The message payload
     * @param sequenceId Optional sequence identifier for ordered delivery (can be null)
     * @param creationTime Message creation timestamp
     */
    public MessageContext(String messageId, Object message, String sequenceId, long creationTime) {
        this.messageId = messageId;
        this.message = message;
        this.sequenceId = sequenceId;
        this.creationTime = creationTime;
        this.retryCount = 0;
        this.lastAttemptTime = 0;
        this.acknowledged = false;
    }
    
    /**
     * Get the message ID
     * 
     * @return The message ID
     */
    public String getMessageId() {
        return messageId;
    }
    
    /**
     * Get the message payload
     * 
     * @return The message payload
     */
    public Object getMessage() {
        return message;
    }
    
    /**
     * Get the sequence ID
     * 
     * @return The sequence ID, or null if not part of a sequence
     */
    public String getSequenceId() {
        return sequenceId;
    }
    
    /**
     * Get the message creation time
     * 
     * @return The creation timestamp
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Get the current retry count
     * 
     * @return The number of delivery attempts
     */
    public int getRetryCount() {
        return retryCount;
    }
    
    /**
     * Increment the retry count
     * 
     * @return The new retry count
     */
    public int incrementRetryCount() {
        return ++retryCount;
    }
    
    /**
     * Reset the retry count to zero
     */
    public void resetRetryCount() {
        retryCount = 0;
    }
    
    /**
     * Get the timestamp of the last delivery attempt
     * 
     * @return The last attempt timestamp
     */
    public long getLastAttemptTime() {
        return lastAttemptTime;
    }
    
    /**
     * Set the timestamp of the last delivery attempt
     * 
     * @param lastAttemptTime The last attempt timestamp
     */
    public void setLastAttemptTime(long lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }
    
    /**
     * Check if the message has been acknowledged
     * 
     * @return true if acknowledged, false otherwise
     */
    public boolean isAcknowledged() {
        return acknowledged;
    }
    
    /**
     * Set the acknowledged status
     * 
     * @param acknowledged true if acknowledged, false otherwise
     */
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
    
    /**
     * Get the age of the message in milliseconds
     * 
     * @return The message age
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - creationTime;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        MessageContext other = (MessageContext) obj;
        return messageId.equals(other.messageId);
    }
    
    @Override
    public int hashCode() {
        return messageId.hashCode();
    }
    
    @Override
    public String toString() {
        return "MessageContext{" +
                "messageId='" + messageId + '\'' +
                ", sequenceId='" + sequenceId + '\'' +
                ", retryCount=" + retryCount +
                ", acknowledged=" + acknowledged +
                '}';
    }
} 