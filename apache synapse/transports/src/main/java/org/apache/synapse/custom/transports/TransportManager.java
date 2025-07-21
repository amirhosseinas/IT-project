package org.apache.synapse.custom.transports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages transport listeners and senders for different protocols.
 * This is the main entry point for the transport layer.
 */
public class TransportManager {
    private static final Logger logger = LoggerFactory.getLogger(TransportManager.class);
    
    private final Map<String, TransportListener> listeners;
    private final Map<String, TransportSender> senders;
    
    /**
     * Create a new transport manager
     */
    public TransportManager() {
        this.listeners = new HashMap<>();
        this.senders = new HashMap<>();
        logger.info("Transport Manager initialized");
    }
    
    /**
     * Register a transport listener
     * 
     * @param name The unique name for this listener
     * @param listener The listener to register
     */
    public void registerListener(String name, TransportListener listener) {
        if (name == null || listener == null) {
            throw new IllegalArgumentException("Name and listener cannot be null");
        }
        
        listeners.put(name, listener);
        logger.info("Registered transport listener: {}", name);
    }
    
    /**
     * Register a transport sender
     * 
     * @param name The unique name for this sender
     * @param sender The sender to register
     */
    public void registerSender(String name, TransportSender sender) {
        if (name == null || sender == null) {
            throw new IllegalArgumentException("Name and sender cannot be null");
        }
        
        senders.put(name, sender);
        logger.info("Registered transport sender: {}", name);
    }
    
    /**
     * Initialize all registered listeners
     */
    public void initializeListeners() {
        for (Map.Entry<String, TransportListener> entry : listeners.entrySet()) {
            try {
                entry.getValue().init();
                logger.info("Initialized transport listener: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to initialize transport listener: {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * Start all registered listeners
     */
    public void startListeners() {
        for (Map.Entry<String, TransportListener> entry : listeners.entrySet()) {
            try {
                entry.getValue().start();
                logger.info("Started transport listener: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to start transport listener: {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * Stop all registered listeners
     */
    public void stopListeners() {
        for (Map.Entry<String, TransportListener> entry : listeners.entrySet()) {
            try {
                entry.getValue().stop();
                logger.info("Stopped transport listener: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to stop transport listener: {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * Get a transport sender by name
     * 
     * @param name The name of the sender
     * @return The transport sender or null if not found
     */
    public TransportSender getSender(String name) {
        return senders.get(name);
    }
    
    /**
     * Get a transport listener by name
     * 
     * @param name The name of the listener
     * @return The transport listener or null if not found
     */
    public TransportListener getListener(String name) {
        return listeners.get(name);
    }
} 