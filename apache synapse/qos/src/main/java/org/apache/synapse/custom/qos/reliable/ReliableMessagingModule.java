package org.apache.synapse.custom.qos.reliable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides reliable messaging capabilities including message acknowledgment,
 * retry, dead letter handling, and ordered delivery.
 */
public class ReliableMessagingModule {
    private static final Logger logger = LoggerFactory.getLogger(ReliableMessagingModule.class);
    
    // Default settings
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long DEFAULT_RETRY_INTERVAL_MS = 5000;
    private static final long DEFAULT_ACK_TIMEOUT_MS = 30000;
    
    // Message storage
    private final Map<String, MessageContext> pendingMessages;
    private final Map<String, MessageContext> processedMessages;
    private final Map<String, Queue<MessageContext>> sequenceGroups;
    private final Queue<MessageContext> deadLetterQueue;
    
    // Configuration
    private final int maxRetries;
    private final long retryIntervalMs;
    private final long ackTimeoutMs;
    private final boolean persistMessages;
    
    // Scheduler for retry and cleanup tasks
    private final ScheduledExecutorService scheduler;
    
    // Message persistence provider (if enabled)
    private final MessagePersistenceProvider persistenceProvider;
    
    // Message delivery callback
    private MessageDeliveryCallback deliveryCallback;
    
    /**
     * Create a new reliable messaging module with default settings
     */
    public ReliableMessagingModule() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_RETRY_INTERVAL_MS, DEFAULT_ACK_TIMEOUT_MS, false, null);
    }
    
    /**
     * Create a new reliable messaging module with custom settings
     * 
     * @param maxRetries Maximum number of retry attempts
     * @param retryIntervalMs Interval between retries in milliseconds
     * @param ackTimeoutMs Timeout for acknowledgments in milliseconds
     * @param persistMessages Whether to persist messages
     * @param persistenceProvider Provider for message persistence (required if persistMessages is true)
     */
    public ReliableMessagingModule(int maxRetries, long retryIntervalMs, long ackTimeoutMs, 
                                 boolean persistMessages, MessagePersistenceProvider persistenceProvider) {
        this.maxRetries = maxRetries;
        this.retryIntervalMs = retryIntervalMs;
        this.ackTimeoutMs = ackTimeoutMs;
        this.persistMessages = persistMessages;
        this.persistenceProvider = persistenceProvider;
        
        if (persistMessages && persistenceProvider == null) {
            throw new IllegalArgumentException("Persistence provider is required when message persistence is enabled");
        }
        
        this.pendingMessages = new ConcurrentHashMap<>();
        this.processedMessages = new ConcurrentHashMap<>();
        this.sequenceGroups = new ConcurrentHashMap<>();
        this.deadLetterQueue = new ConcurrentLinkedQueue<>();
        
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Schedule retry task
        scheduler.scheduleAtFixedRate(this::checkAndRetryMessages, 
                retryIntervalMs, retryIntervalMs, TimeUnit.MILLISECONDS);
        
        // Schedule cleanup task
        scheduler.scheduleAtFixedRate(this::cleanupProcessedMessages, 
                ackTimeoutMs * 2, ackTimeoutMs * 2, TimeUnit.MILLISECONDS);
        
        logger.info("Reliable messaging module initialized with maxRetries={}, retryInterval={}ms, ackTimeout={}ms",
                maxRetries, retryIntervalMs, ackTimeoutMs);
    }
    
    /**
     * Set the message delivery callback
     * 
     * @param callback The callback to invoke when delivering messages
     */
    public void setDeliveryCallback(MessageDeliveryCallback callback) {
        this.deliveryCallback = callback;
    }
    
    /**
     * Send a message reliably
     * 
     * @param message The message to send
     * @return The message ID
     */
    public String sendMessage(Object message) {
        return sendMessage(message, null);
    }
    
    /**
     * Send a message reliably with a sequence ID for ordered delivery
     * 
     * @param message The message to send
     * @param sequenceId The sequence ID for ordered delivery (or null for unordered)
     * @return The message ID
     */
    public String sendMessage(Object message, String sequenceId) {
        String messageId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        MessageContext context = new MessageContext(messageId, message, sequenceId, timestamp);
        
        // Store the message
        pendingMessages.put(messageId, context);
        
        // Persist if enabled
        if (persistMessages) {
            persistenceProvider.persistMessage(context);
        }
        
        // If part of a sequence, add to sequence group
        if (sequenceId != null) {
            Queue<MessageContext> sequenceQueue = sequenceGroups.computeIfAbsent(
                    sequenceId, k -> new ConcurrentLinkedQueue<>());
            sequenceQueue.add(context);
        }
        
        // Attempt delivery
        deliverMessage(context);
        
        logger.debug("Message queued: id={}, sequence={}", messageId, sequenceId);
        return messageId;
    }
    
    /**
     * Acknowledge receipt of a message
     * 
     * @param messageId The message ID to acknowledge
     * @return true if the message was acknowledged, false if not found
     */
    public boolean acknowledgeMessage(String messageId) {
        MessageContext context = pendingMessages.remove(messageId);
        if (context == null) {
            logger.warn("Attempted to acknowledge unknown message: {}", messageId);
            return false;
        }
        
        // Mark as processed
        context.setAcknowledged(true);
        processedMessages.put(messageId, context);
        
        // If part of a sequence, try to deliver next messages
        if (context.getSequenceId() != null) {
            deliverNextInSequence(context.getSequenceId());
        }
        
        // Remove from persistence if enabled
        if (persistMessages) {
            persistenceProvider.removeMessage(messageId);
        }
        
        logger.debug("Message acknowledged: {}", messageId);
        return true;
    }
    
    /**
     * Get a message from the dead letter queue
     * 
     * @return The next message from the dead letter queue, or null if empty
     */
    public MessageContext getNextDeadLetter() {
        return deadLetterQueue.poll();
    }
    
    /**
     * Get the number of messages in the dead letter queue
     * 
     * @return The dead letter queue size
     */
    public int getDeadLetterQueueSize() {
        return deadLetterQueue.size();
    }
    
    /**
     * Reprocess a message from the dead letter queue
     * 
     * @param messageId The message ID to reprocess
     * @return true if the message was found and reprocessed, false otherwise
     */
    public boolean reprocessDeadLetter(String messageId) {
        for (MessageContext context : deadLetterQueue) {
            if (context.getMessageId().equals(messageId)) {
                deadLetterQueue.remove(context);
                context.resetRetryCount();
                pendingMessages.put(messageId, context);
                deliverMessage(context);
                logger.info("Reprocessing message from dead letter queue: {}", messageId);
                return true;
            }
        }
        
        logger.warn("Message not found in dead letter queue: {}", messageId);
        return false;
    }
    
    /**
     * Check for pending messages that need to be retried
     */
    private void checkAndRetryMessages() {
        long now = System.currentTimeMillis();
        
        for (MessageContext context : pendingMessages.values()) {
            // Skip messages that are not yet due for retry
            if (now - context.getLastAttemptTime() < retryIntervalMs) {
                continue;
            }
            
            // Check if max retries exceeded
            if (context.getRetryCount() >= maxRetries) {
                logger.warn("Max retries exceeded for message: {}", context.getMessageId());
                pendingMessages.remove(context.getMessageId());
                deadLetterQueue.add(context);
                
                // Update persistence if enabled
                if (persistMessages) {
                    persistenceProvider.moveToDeadLetter(context.getMessageId());
                }
                
                continue;
            }
            
            // Retry the message
            deliverMessage(context);
        }
    }
    
    /**
     * Deliver a message
     * 
     * @param context The message context
     */
    private void deliverMessage(MessageContext context) {
        // Skip if no callback is registered
        if (deliveryCallback == null) {
            logger.warn("No delivery callback registered, message delivery skipped");
            return;
        }
        
        // Skip if part of a sequence and not the next in line
        if (context.getSequenceId() != null && !isNextInSequence(context)) {
            return;
        }
        
        try {
            // Update attempt information
            context.incrementRetryCount();
            context.setLastAttemptTime(System.currentTimeMillis());
            
            // Deliver the message
            deliveryCallback.onMessageDelivery(context.getMessageId(), context.getMessage());
            
            logger.debug("Message delivered: id={}, retry={}/{}", 
                    context.getMessageId(), context.getRetryCount(), maxRetries);
            
        } catch (Exception e) {
            logger.error("Error delivering message {}: {}", context.getMessageId(), e.getMessage(), e);
        }
    }
    
    /**
     * Check if a message is the next one to be delivered in its sequence
     * 
     * @param context The message context
     * @return true if the message is next in sequence, false otherwise
     */
    private boolean isNextInSequence(MessageContext context) {
        String sequenceId = context.getSequenceId();
        if (sequenceId == null) {
            return true; // Not part of a sequence
        }
        
        Queue<MessageContext> sequenceQueue = sequenceGroups.get(sequenceId);
        return sequenceQueue != null && sequenceQueue.peek() == context;
    }
    
    /**
     * Deliver the next message in a sequence
     * 
     * @param sequenceId The sequence ID
     */
    private void deliverNextInSequence(String sequenceId) {
        Queue<MessageContext> sequenceQueue = sequenceGroups.get(sequenceId);
        if (sequenceQueue == null || sequenceQueue.isEmpty()) {
            return;
        }
        
        MessageContext nextContext = sequenceQueue.peek();
        if (nextContext != null && pendingMessages.containsKey(nextContext.getMessageId())) {
            deliverMessage(nextContext);
        }
    }
    
    /**
     * Clean up old processed messages to prevent memory leaks
     */
    private void cleanupProcessedMessages() {
        long cutoffTime = System.currentTimeMillis() - (ackTimeoutMs * 2);
        
        processedMessages.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttemptTime() < cutoffTime);
    }
    
    /**
     * Shutdown the reliable messaging module
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Reliable messaging module shutdown complete");
    }
} 