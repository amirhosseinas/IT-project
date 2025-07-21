package org.apache.synapse.custom.mediation.event;

import java.util.List;

/**
 * Interface for event sources in Apache Synapse.
 * Event sources generate events that can trigger mediation sequences.
 */
public interface EventSource {
    
    /**
     * Get the name of this event source
     * 
     * @return The event source name
     */
    String getName();
    
    /**
     * Start the event source
     * 
     * @throws EventException if starting fails
     */
    void start() throws EventException;
    
    /**
     * Stop the event source
     * 
     * @throws EventException if stopping fails
     */
    void stop() throws EventException;
    
    /**
     * Add an event listener
     * 
     * @param listener The listener to add
     */
    void addEventListener(EventListener listener);
    
    /**
     * Remove an event listener
     * 
     * @param listener The listener to remove
     */
    void removeEventListener(EventListener listener);
    
    /**
     * Get all event listeners
     * 
     * @return List of event listeners
     */
    List<EventListener> getEventListeners();
    
    /**
     * Check if this event source is started
     * 
     * @return true if started, false otherwise
     */
    boolean isStarted();
    
    /**
     * Exception thrown when an event source operation fails
     */
    class EventException extends Exception {
        public EventException(String message) {
            super(message);
        }
        
        public EventException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 