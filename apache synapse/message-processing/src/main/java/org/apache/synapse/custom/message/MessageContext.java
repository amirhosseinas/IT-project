package org.apache.synapse.custom.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the context of a message being processed by Apache Synapse.
 * This context contains the message itself, as well as properties and state
 * information needed during message processing.
 */
public class MessageContext implements Serializable, AutoCloseable {
    private static final long serialVersionUID = 1L;
    
    // Scope constants
    public static final String SCOPE_DEFAULT = "default";
    public static final String SCOPE_TRANSPORT = "transport";
    public static final String SCOPE_AXIS2 = "axis2";
    public static final String SCOPE_OPERATION = "operation";
    
    // Property scopes
    private final Map<String, Object> defaultProperties;
    private final Map<String, Object> transportProperties;
    private final Map<String, Object> axis2Properties;
    private final Map<String, Object> operationProperties;
    
    // The message being processed
    private Message message;
    
    // Attachments
    private final Map<String, Attachment> attachments;
    
    // Parent context (for propagation)
    private MessageContext parentContext;
    
    // Child contexts (for propagation)
    private final List<MessageContext> childContexts;
    
    // Fault information
    private boolean faultMode = false;
    private Throwable faultException;
    private String faultCode;
    private String faultReason;
    
    // Context state
    private boolean processed = false;
    private long creationTime;
    private String contextId;
    
    /**
     * Create a new message context
     */
    public MessageContext() {
        this.defaultProperties = new ConcurrentHashMap<>();
        this.transportProperties = new ConcurrentHashMap<>();
        this.axis2Properties = new ConcurrentHashMap<>();
        this.operationProperties = new ConcurrentHashMap<>();
        this.attachments = new ConcurrentHashMap<>();
        this.childContexts = Collections.synchronizedList(new ArrayList<>());
        this.creationTime = System.currentTimeMillis();
        this.contextId = UUID.randomUUID().toString();
    }
    
    /**
     * Create a new message context with the specified message
     * 
     * @param message The message
     */
    public MessageContext(Message message) {
        this();
        this.message = message;
    }
    
    /**
     * Get the message
     * 
     * @return The message
     */
    public Message getMessage() {
        return message;
    }
    
    /**
     * Set the message
     * 
     * @param message The message
     */
    public void setMessage(Message message) {
        this.message = message;
    }
    
    /**
     * Get a property from the context
     * 
     * @param name The property name
     * @return The property value or null if not found
     */
    public Object getProperty(String name) {
        return getProperty(name, SCOPE_DEFAULT);
    }
    
    /**
     * Get a property from the context with the specified scope
     * 
     * @param name The property name
     * @param scope The property scope
     * @return The property value or null if not found
     */
    public Object getProperty(String name, String scope) {
        switch (scope) {
            case SCOPE_TRANSPORT:
                return transportProperties.get(name);
            case SCOPE_AXIS2:
                return axis2Properties.get(name);
            case SCOPE_OPERATION:
                return operationProperties.get(name);
            case SCOPE_DEFAULT:
            default:
                return defaultProperties.get(name);
        }
    }
    
    /**
     * Set a property in the context
     * 
     * @param name The property name
     * @param value The property value
     */
    public void setProperty(String name, Object value) {
        setProperty(name, value, SCOPE_DEFAULT);
    }
    
    /**
     * Set a property in the context with the specified scope
     * 
     * @param name The property name
     * @param value The property value
     * @param scope The property scope
     */
    public void setProperty(String name, Object value, String scope) {
        switch (scope) {
            case SCOPE_TRANSPORT:
                transportProperties.put(name, value);
                break;
            case SCOPE_AXIS2:
                axis2Properties.put(name, value);
                break;
            case SCOPE_OPERATION:
                operationProperties.put(name, value);
                break;
            case SCOPE_DEFAULT:
            default:
                defaultProperties.put(name, value);
                break;
        }
    }
    
    /**
     * Remove a property from the context
     * 
     * @param name The property name
     * @return The removed property value or null if not found
     */
    public Object removeProperty(String name) {
        return removeProperty(name, SCOPE_DEFAULT);
    }
    
    /**
     * Remove a property from the context with the specified scope
     * 
     * @param name The property name
     * @param scope The property scope
     * @return The removed property value or null if not found
     */
    public Object removeProperty(String name, String scope) {
        switch (scope) {
            case SCOPE_TRANSPORT:
                return transportProperties.remove(name);
            case SCOPE_AXIS2:
                return axis2Properties.remove(name);
            case SCOPE_OPERATION:
                return operationProperties.remove(name);
            case SCOPE_DEFAULT:
            default:
                return defaultProperties.remove(name);
        }
    }
    
    /**
     * Get all properties in the context with the specified scope
     * 
     * @param scope The property scope
     * @return Map of all properties in the scope
     */
    public Map<String, Object> getProperties(String scope) {
        switch (scope) {
            case SCOPE_TRANSPORT:
                return new HashMap<>(transportProperties);
            case SCOPE_AXIS2:
                return new HashMap<>(axis2Properties);
            case SCOPE_OPERATION:
                return new HashMap<>(operationProperties);
            case SCOPE_DEFAULT:
            default:
                return new HashMap<>(defaultProperties);
        }
    }
    
    /**
     * Add an attachment to the context
     * 
     * @param id The attachment ID
     * @param attachment The attachment
     */
    public void addAttachment(String id, Attachment attachment) {
        attachments.put(id, attachment);
    }
    
    /**
     * Get an attachment from the context
     * 
     * @param id The attachment ID
     * @return The attachment or null if not found
     */
    public Attachment getAttachment(String id) {
        return attachments.get(id);
    }
    
    /**
     * Remove an attachment from the context
     * 
     * @param id The attachment ID
     * @return The removed attachment or null if not found
     */
    public Attachment removeAttachment(String id) {
        return attachments.remove(id);
    }
    
    /**
     * Get all attachments
     * 
     * @return Map of all attachments
     */
    public Map<String, Attachment> getAttachments() {
        return Collections.unmodifiableMap(attachments);
    }
    
    /**
     * Get the parent context
     * 
     * @return The parent context or null if none
     */
    public MessageContext getParentContext() {
        return parentContext;
    }
    
    /**
     * Set the parent context
     * 
     * @param parentContext The parent context
     */
    public void setParentContext(MessageContext parentContext) {
        this.parentContext = parentContext;
        if (parentContext != null) {
            parentContext.addChildContext(this);
        }
    }
    
    /**
     * Add a child context
     * 
     * @param childContext The child context
     */
    public void addChildContext(MessageContext childContext) {
        if (!childContexts.contains(childContext)) {
            childContexts.add(childContext);
        }
    }
    
    /**
     * Get all child contexts
     * 
     * @return List of all child contexts
     */
    public List<MessageContext> getChildContexts() {
        return Collections.unmodifiableList(childContexts);
    }
    
    /**
     * Create a child context
     * 
     * @return The new child context
     */
    public MessageContext createChildContext() {
        MessageContext childContext = new MessageContext();
        childContext.setParentContext(this);
        return childContext;
    }
    
    /**
     * Check if the context is in fault mode
     * 
     * @return true if in fault mode, false otherwise
     */
    public boolean isFaultMode() {
        return faultMode;
    }
    
    /**
     * Set the fault mode
     * 
     * @param faultMode true to enable fault mode, false to disable
     */
    public void setFaultMode(boolean faultMode) {
        this.faultMode = faultMode;
    }
    
    /**
     * Get the fault exception
     * 
     * @return The fault exception or null if none
     */
    public Throwable getFaultException() {
        return faultException;
    }
    
    /**
     * Set the fault exception
     * 
     * @param faultException The fault exception
     */
    public void setFaultException(Throwable faultException) {
        this.faultException = faultException;
        if (faultException != null) {
            setFaultMode(true);
        }
    }
    
    /**
     * Get the fault code
     * 
     * @return The fault code or null if none
     */
    public String getFaultCode() {
        return faultCode;
    }
    
    /**
     * Set the fault code
     * 
     * @param faultCode The fault code
     */
    public void setFaultCode(String faultCode) {
        this.faultCode = faultCode;
    }
    
    /**
     * Get the fault reason
     * 
     * @return The fault reason or null if none
     */
    public String getFaultReason() {
        return faultReason;
    }
    
    /**
     * Set the fault reason
     * 
     * @param faultReason The fault reason
     */
    public void setFaultReason(String faultReason) {
        this.faultReason = faultReason;
    }
    
    /**
     * Check if the context has been processed
     * 
     * @return true if processed, false otherwise
     */
    public boolean isProcessed() {
        return processed;
    }
    
    /**
     * Set the processed state
     * 
     * @param processed true to mark as processed, false otherwise
     */
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    /**
     * Get the context creation time
     * 
     * @return The creation time in milliseconds
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Get the context ID
     * 
     * @return The context ID
     */
    public String getContextId() {
        return contextId;
    }
    
    /**
     * Create a deep copy of this context
     * 
     * @return A new context with the same properties and attachments
     * @throws IOException if serialization fails
     * @throws ClassNotFoundException if deserialization fails
     */
    public MessageContext clone() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (MessageContext) ois.readObject();
        }
    }
    
    /**
     * Release resources held by this context
     */
    @Override
    public void close() {
        // Close all attachments
        for (Attachment attachment : attachments.values()) {
            try {
                attachment.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Clear collections
        attachments.clear();
        defaultProperties.clear();
        transportProperties.clear();
        axis2Properties.clear();
        operationProperties.clear();
        
        // Close child contexts
        for (MessageContext childContext : childContexts) {
            try {
                childContext.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        childContexts.clear();
        
        // Clear references
        message = null;
        parentContext = null;
        faultException = null;
    }
    
    /**
     * Represents an attachment in a message context
     */
    public static class Attachment implements Serializable, AutoCloseable {
        private static final long serialVersionUID = 1L;
        
        private String contentId;
        private String contentType;
        private byte[] content;
        
        /**
         * Create a new attachment
         * 
         * @param contentId The content ID
         * @param contentType The content type
         * @param content The content
         */
        public Attachment(String contentId, String contentType, byte[] content) {
            this.contentId = contentId;
            this.contentType = contentType;
            this.content = content;
        }
        
        /**
         * Get the content ID
         * 
         * @return The content ID
         */
        public String getContentId() {
            return contentId;
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
         * Get the content
         * 
         * @return The content
         */
        public byte[] getContent() {
            return content;
        }
        
        /**
         * Release resources held by this attachment
         */
        @Override
        public void close() {
            content = null;
        }
    }
} 