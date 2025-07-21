package org.apache.synapse.custom.mediation.event;

/**
 * Interface for event listeners in Apache Synapse.
 * Event listeners are notified when events occur in event sources.
 */
public interface EventListener {
    
    /**
     * Handle an event
     * 
     * @param event The event to handle
     */
    void handleEvent(Event event);
    
    /**
     * Get the name of this event listener
     * 
     * @return The event listener name
     */
    String getName();
} 