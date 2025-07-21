package org.apache.synapse.custom.mediation;

import org.apache.synapse.custom.message.Message;

/**
 * Interface for all mediators in the system.
 * Mediators are the building blocks of the mediation engine, each
 * applying specific logic to messages as they flow through the system.
 */
public interface Mediator {
    
    /**
     * Get the name of this mediator
     * 
     * @return The mediator name
     */
    String getName();
    
    /**
     * Apply the mediation logic to a message
     * 
     * @param message The message to mediate
     * @return The mediated message
     * @throws MediationEngine.MediationException if mediation fails
     */
    Message mediate(Message message) throws MediationEngine.MediationException;
} 