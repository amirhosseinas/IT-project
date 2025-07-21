package org.apache.synapse.custom.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a message in the Synapse ESB system.
 * This can be a request, response, or an internal message.
 */
public class Message {
    
    public enum Direction {
        REQUEST,
        RESPONSE
    }
    
    private String messageId;
    private Direction direction;
    private String contentType;
    private byte[] payload;
    private Map<String, Object> properties;
    private Map<String, String> headers;
    
    /**
     * Create a new message
     */
    public Message() {
        this.properties = new HashMap<>();
        this.headers = new HashMap<>();
    }
    
    /**
     * Create a new message with the specified ID
     * 
     * @param messageId The unique ID for this message
     */
    public Message(String messageId) {
        this();
        this.messageId = messageId;
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
     * Set the message ID
     * 
     * @param messageId The message ID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    /**
     * Get the message direction
     * 
     * @return The message direction
     */
    public Direction getDirection() {
        return direction;
    }
    
    /**
     * Set the message direction
     * 
     * @param direction The message direction
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
    
    /**
     * Get the content type
     * 
     * @return The content type
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Set the content type
     * 
     * @param contentType The content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * Get the message payload
     * 
     * @return The message payload as a byte array
     */
    public byte[] getPayload() {
        return payload;
    }
    
    /**
     * Set the message payload
     * 
     * @param payload The message payload as a byte array
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
    
    /**
     * Get a message property
     * 
     * @param name The property name
     * @return The property value or null if not found
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * Set a message property
     * 
     * @param name The property name
     * @param value The property value
     */
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    /**
     * Get all message properties
     * 
     * @return Map of all properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * Get a message header
     * 
     * @param name The header name
     * @return The header value or null if not found
     */
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    /**
     * Set a message header
     * 
     * @param name The header name
     * @param value The header value
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }
    
    /**
     * Get all message headers
     * 
     * @return Map of all headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
} 