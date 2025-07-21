package org.apache.synapse.custom.mediation;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sequence of mediators that are applied to a message in order.
 * Sequences are the primary mechanism for orchestrating message flow in Synapse.
 */
public class MediationSequence {
    private static final Logger logger = LoggerFactory.getLogger(MediationSequence.class);
    
    private final String name;
    private final List<Mediator> mediators;
    private MediationSequence errorSequence;
    private boolean isInSequence;
    private boolean isOutSequence;
    private boolean isTemplate;
    private List<String> parameters;
    
    /**
     * Create a new mediation sequence with the specified name
     * 
     * @param name The sequence name
     */
    public MediationSequence(String name) {
        this.name = name;
        this.mediators = new ArrayList<>();
        this.isInSequence = false;
        this.isOutSequence = false;
        this.isTemplate = false;
        this.parameters = new ArrayList<>();
    }
    
    /**
     * Create a new mediation sequence with the specified name and mediators
     * 
     * @param name The sequence name
     * @param mediators The list of mediators
     */
    public MediationSequence(String name, List<Mediator> mediators) {
        this(name);
        if (mediators != null) {
            this.mediators.addAll(mediators);
        }
    }
    
    /**
     * Get the name of this sequence
     * 
     * @return The sequence name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Add a mediator to this sequence
     * 
     * @param mediator The mediator to add
     */
    public void addMediator(Mediator mediator) {
        if (mediator == null) {
            throw new IllegalArgumentException("Mediator cannot be null");
        }
        
        mediators.add(mediator);
        logger.debug("Added mediator to sequence {}: {}", name, mediator.getName());
    }
    
    /**
     * Get the list of mediators in this sequence
     * 
     * @return Unmodifiable list of mediators
     */
    public List<Mediator> getMediators() {
        return Collections.unmodifiableList(mediators);
    }
    
    /**
     * Apply this sequence to a message
     * 
     * @param message The message to apply the sequence to
     * @return The mediated message
     * @throws MediationEngine.MediationException if mediation fails
     */
    public Message apply(Message message) throws MediationEngine.MediationException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        logger.debug("Applying sequence: {}", name);
        
        try {
            Message mediatedMessage = message;
            for (Mediator mediator : mediators) {
                mediatedMessage = mediator.mediate(mediatedMessage);
                
                // Check if flow has been stopped
                if (Boolean.TRUE.equals(mediatedMessage.getProperty("STOP_FLOW"))) {
                    logger.debug("Flow stopped by mediator: {}", mediator.getName());
                    break;
                }
            }
            
            return mediatedMessage;
        } catch (Exception e) {
            logger.error("Error applying sequence: " + name, e);
            
            // Apply error sequence if available
            if (errorSequence != null) {
                try {
                    logger.debug("Applying error sequence: {}", errorSequence.getName());
                    message.setProperty("ERROR_MESSAGE", e.getMessage());
                    message.setProperty("ERROR_DETAIL", e);
                    return errorSequence.apply(message);
                } catch (Exception errorEx) {
                    logger.error("Error in error sequence: " + errorSequence.getName(), errorEx);
                }
            }
            
            throw new MediationEngine.MediationException("Error applying sequence: " + name, e);
        }
    }
    
    /**
     * Set the error sequence for this sequence
     * 
     * @param errorSequence The error sequence
     */
    public void setErrorSequence(MediationSequence errorSequence) {
        this.errorSequence = errorSequence;
        logger.debug("Set error sequence for {}: {}", name, errorSequence.getName());
    }
    
    /**
     * Get the error sequence for this sequence
     * 
     * @return The error sequence
     */
    public MediationSequence getErrorSequence() {
        return errorSequence;
    }
    
    /**
     * Check if this is an in sequence
     * 
     * @return true if this is an in sequence, false otherwise
     */
    public boolean isInSequence() {
        return isInSequence;
    }
    
    /**
     * Set whether this is an in sequence
     * 
     * @param inSequence true if this is an in sequence, false otherwise
     */
    public void setInSequence(boolean inSequence) {
        isInSequence = inSequence;
    }
    
    /**
     * Check if this is an out sequence
     * 
     * @return true if this is an out sequence, false otherwise
     */
    public boolean isOutSequence() {
        return isOutSequence;
    }
    
    /**
     * Set whether this is an out sequence
     * 
     * @param outSequence true if this is an out sequence, false otherwise
     */
    public void setOutSequence(boolean outSequence) {
        isOutSequence = outSequence;
    }
    
    /**
     * Check if this sequence is a template
     * 
     * @return true if this is a template, false otherwise
     */
    public boolean isTemplate() {
        return isTemplate;
    }
    
    /**
     * Set whether this sequence is a template
     * 
     * @param template true if this is a template, false otherwise
     */
    public void setTemplate(boolean template) {
        isTemplate = template;
    }
    
    /**
     * Get the template parameters
     * 
     * @return List of parameter names
     */
    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
    }
    
    /**
     * Set the template parameters
     * 
     * @param parameters List of parameter names
     */
    public void setParameters(List<String> parameters) {
        this.parameters = new ArrayList<>(parameters);
    }
    
    /**
     * Create a new instance of this template with the specified parameters
     * 
     * @param paramValues Map of parameter values
     * @return A new sequence instance
     * @throws IllegalStateException if this sequence is not a template
     */
    public MediationSequence instantiate(List<Object> paramValues) {
        if (!isTemplate) {
            throw new IllegalStateException("Cannot instantiate a non-template sequence");
        }
        
        if (paramValues.size() != parameters.size()) {
            throw new IllegalArgumentException("Expected " + parameters.size() + 
                                              " parameters, but got " + paramValues.size());
        }
        
        // Create a new sequence with the same mediators
        MediationSequence instance = new MediationSequence(name + "-instance");
        
        // Set parameter values as properties
        for (int i = 0; i < parameters.size(); i++) {
            instance.setProperty(parameters.get(i), paramValues.get(i));
        }
        
        // Copy mediators (this would need to be a deep copy for mutable mediators)
        instance.mediators.addAll(this.mediators);
        
        return instance;
    }
    
    /**
     * Set a property on this sequence
     * 
     * @param name The property name
     * @param value The property value
     */
    public void setProperty(String name, Object value) {
        // Properties would be stored here
    }
} 