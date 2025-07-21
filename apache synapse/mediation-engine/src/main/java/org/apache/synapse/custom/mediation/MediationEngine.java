package org.apache.synapse.custom.mediation;

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.MessageProcessor;
import org.apache.synapse.custom.qos.QosManager;
import org.apache.synapse.custom.transports.TransportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The MediationEngine is the central component of the Synapse ESB.
 * It coordinates the message flow through the system, applying mediators
 * as required by the mediation sequence.
 */
public class MediationEngine {
    private static final Logger logger = LoggerFactory.getLogger(MediationEngine.class);
    
    private final List<Mediator> globalMediators;
    private final MediationRegistry registry;
    private final TransportManager transportManager;
    private final MessageProcessor messageProcessor;
    private final QosManager qosManager;
    
    /**
     * Create a new mediation engine with all required components
     * 
     * @param transportManager Transport manager for handling I/O
     * @param messageProcessor Message processor for handling message transformation
     * @param qosManager QoS manager for handling quality of service aspects
     */
    public MediationEngine(TransportManager transportManager, 
                         MessageProcessor messageProcessor,
                         QosManager qosManager) {
        this.transportManager = transportManager;
        this.messageProcessor = messageProcessor;
        this.qosManager = qosManager;
        this.globalMediators = new ArrayList<>();
        this.registry = new MediationRegistry();
        
        logger.info("Mediation Engine initialized");
    }
    
    /**
     * Register a global mediator that will be applied to all messages
     * 
     * @param mediator The mediator to register
     */
    public void registerGlobalMediator(Mediator mediator) {
        if (mediator == null) {
            throw new IllegalArgumentException("Mediator cannot be null");
        }
        
        globalMediators.add(mediator);
        logger.info("Registered global mediator: {}", mediator.getName());
    }
    
    /**
     * Register a mediation sequence
     * 
     * @param sequence The mediation sequence to register
     */
    public void registerSequence(MediationSequence sequence) {
        if (sequence == null || StringUtils.isBlank(sequence.getName())) {
            throw new IllegalArgumentException("Sequence or sequence name cannot be null or empty");
        }
        
        registry.registerSequence(sequence);
        logger.info("Registered mediation sequence: {}", sequence.getName());
    }
    
    /**
     * Mediate a message through the system
     * 
     * @param message The message to mediate
     * @param sequenceName The name of the sequence to apply (optional)
     * @return The mediated message
     * @throws MediationException if mediation fails
     */
    public Message mediate(Message message, String sequenceName) throws MediationException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            // Apply global mediators first
            Message mediatedMessage = message;
            for (Mediator mediator : globalMediators) {
                mediatedMessage = mediator.mediate(mediatedMessage);
            }
            
            // Apply sequence if specified
            if (StringUtils.isNotBlank(sequenceName)) {
                MediationSequence sequence = registry.getSequence(sequenceName);
                if (sequence == null) {
                    throw new MediationException("Sequence not found: " + sequenceName);
                }
                
                mediatedMessage = sequence.apply(mediatedMessage);
            }
            
            return mediatedMessage;
        } catch (Exception e) {
            logger.error("Mediation failed", e);
            throw new MediationException("Mediation failed", e);
        }
    }
    
    /**
     * Start the mediation engine and its components
     */
    public void start() {
        logger.info("Starting Mediation Engine");
        // Start components in order
        transportManager.initializeListeners();
        transportManager.startListeners();
    }
    
    /**
     * Stop the mediation engine and its components
     */
    public void stop() {
        logger.info("Stopping Mediation Engine");
        transportManager.stopListeners();
        qosManager.shutdown();
    }
    
    /**
     * Exception thrown when mediation fails
     */
    public static class MediationException extends Exception {
        public MediationException(String message) {
            super(message);
        }
        
        public MediationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 