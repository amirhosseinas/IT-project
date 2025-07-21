package org.apache.synapse.custom.transports.jms;

/**
 * Enum representing the type of JMS destination.
 */
public enum JmsDestinationType {
    /**
     * JMS Queue - point-to-point messaging model
     */
    QUEUE,
    
    /**
     * JMS Topic - publish-subscribe messaging model
     */
    TOPIC;
    
    /**
     * Parse a string into a JmsDestinationType
     * 
     * @param type The string to parse
     * @return The corresponding JmsDestinationType
     */
    public static JmsDestinationType fromString(String type) {
        if (type == null) {
            return QUEUE; // Default to queue
        }
        
        if (type.equalsIgnoreCase("topic")) {
            return TOPIC;
        } else {
            return QUEUE;
        }
    }
} 