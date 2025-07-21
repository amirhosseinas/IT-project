package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.MediationRegistry;
import org.apache.synapse.custom.mediation.MediationSequence;
import org.apache.synapse.custom.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Mediator for referencing and executing named sequences.
 * This allows for reuse of mediation logic across multiple flows.
 */
public class SequenceMediator extends AbstractMediator {
    
    private String sequenceRef;
    private MediationSequence sequence;
    private final MediationRegistry registry;
    private final List<Object> parameters;
    
    /**
     * Create a new sequence mediator
     * 
     * @param name The mediator name
     * @param registry The mediation registry
     */
    public SequenceMediator(String name, MediationRegistry registry) {
        super(name);
        this.registry = registry;
        this.parameters = new ArrayList<>();
    }
    
    /**
     * Create a new sequence mediator with the specified sequence reference
     * 
     * @param name The mediator name
     * @param sequenceRef The sequence reference
     * @param registry The mediation registry
     */
    public SequenceMediator(String name, String sequenceRef, MediationRegistry registry) {
        super(name);
        this.sequenceRef = sequenceRef;
        this.registry = registry;
        this.parameters = new ArrayList<>();
    }
    
    /**
     * Create a new sequence mediator with the specified sequence
     * 
     * @param name The mediator name
     * @param sequence The sequence
     * @param registry The mediation registry
     */
    public SequenceMediator(String name, MediationSequence sequence, MediationRegistry registry) {
        super(name);
        this.sequence = sequence;
        this.registry = registry;
        this.parameters = new ArrayList<>();
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        MediationSequence targetSequence = resolveSequence();
        
        if (targetSequence == null) {
            throw new MediationEngine.MediationException("Sequence not found: " + sequenceRef);
        }
        
        logger.debug("Applying sequence: {}", targetSequence.getName());
        
        if (targetSequence.isTemplate() && !parameters.isEmpty()) {
            // Instantiate the template with parameters
            MediationSequence instance = targetSequence.instantiate(parameters);
            return instance.apply(message);
        } else {
            // Apply the sequence directly
            return targetSequence.apply(message);
        }
    }
    
    /**
     * Resolve the sequence to use
     * 
     * @return The resolved sequence
     */
    private MediationSequence resolveSequence() {
        if (sequence != null) {
            return sequence;
        }
        
        if (sequenceRef != null && !sequenceRef.isEmpty() && registry != null) {
            return registry.getSequence(sequenceRef);
        }
        
        return null;
    }
    
    /**
     * Get the sequence reference
     * 
     * @return The sequence reference
     */
    public String getSequenceRef() {
        return sequenceRef;
    }
    
    /**
     * Set the sequence reference
     * 
     * @param sequenceRef The sequence reference
     */
    public void setSequenceRef(String sequenceRef) {
        this.sequenceRef = sequenceRef;
    }
    
    /**
     * Get the sequence
     * 
     * @return The sequence
     */
    public MediationSequence getSequence() {
        return sequence;
    }
    
    /**
     * Set the sequence
     * 
     * @param sequence The sequence
     */
    public void setSequence(MediationSequence sequence) {
        this.sequence = sequence;
    }
    
    /**
     * Add a parameter for template instantiation
     * 
     * @param parameter The parameter value
     */
    public void addParameter(Object parameter) {
        parameters.add(parameter);
    }
    
    /**
     * Get the parameters for template instantiation
     * 
     * @return List of parameters
     */
    public List<Object> getParameters() {
        return parameters;
    }
} 