package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.mediation.Endpoint;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.MediationRegistry;
import org.apache.synapse.custom.message.Message;

/**
 * Mediator for calling endpoints and continuing mediation.
 * Unlike SendMediator, this mediator does not end the mediation flow.
 */
public class CallMediator extends AbstractMediator {
    
    private Endpoint endpoint;
    private String endpointRef;
    private MediationRegistry registry;
    private boolean blocking;
    
    /**
     * Create a new call mediator
     * 
     * @param name The mediator name
     * @param registry The mediation registry
     */
    public CallMediator(String name, MediationRegistry registry) {
        super(name);
        this.registry = registry;
        this.blocking = true;
    }
    
    /**
     * Create a new call mediator with the specified endpoint
     * 
     * @param name The mediator name
     * @param endpoint The endpoint to call
     * @param registry The mediation registry
     */
    public CallMediator(String name, Endpoint endpoint, MediationRegistry registry) {
        super(name);
        this.endpoint = endpoint;
        this.registry = registry;
        this.blocking = true;
    }
    
    /**
     * Create a new call mediator with the specified endpoint reference
     * 
     * @param name The mediator name
     * @param endpointRef The endpoint reference
     * @param registry The mediation registry
     */
    public CallMediator(String name, String endpointRef, MediationRegistry registry) {
        super(name);
        this.endpointRef = endpointRef;
        this.registry = registry;
        this.blocking = true;
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        Endpoint targetEndpoint = resolveEndpoint(message);
        
        if (targetEndpoint == null) {
            throw new MediationEngine.MediationException("No endpoint specified for call mediator");
        }
        
        if (!targetEndpoint.isAvailable()) {
            logger.warn("Endpoint {} is not available", targetEndpoint.getName());
            throw new MediationEngine.MediationException("Endpoint " + targetEndpoint.getName() + " is not available");
        }
        
        logger.debug("Calling endpoint: {}", targetEndpoint.getName());
        
        try {
            if (blocking) {
                // Blocking call - wait for response
                Message response = targetEndpoint.send(message);
                
                // Store the original message as a property for reference
                response.setProperty("ORIGINAL_MESSAGE", message);
                
                return response;
            } else {
                // Non-blocking call - fire and forget
                // In a real implementation, this would be asynchronous
                // For now, we'll just make the call but return the original message
                targetEndpoint.send(message);
                
                return message;
            }
        } catch (Exception e) {
            logger.error("Error calling endpoint: " + targetEndpoint.getName(), e);
            throw new MediationEngine.MediationException("Error calling endpoint: " + targetEndpoint.getName(), e);
        }
    }
    
    /**
     * Resolve the endpoint to use
     * 
     * @param message The message
     * @return The resolved endpoint
     */
    private Endpoint resolveEndpoint(Message message) {
        if (endpoint != null) {
            return endpoint;
        }
        
        if (endpointRef != null && !endpointRef.isEmpty()) {
            // Try to get the endpoint from the registry
            Endpoint registryEndpoint = registry.getEndpoint(endpointRef);
            if (registryEndpoint != null) {
                return registryEndpoint;
            }
            
            // Try to get the endpoint from the message properties
            Object property = message.getProperty(endpointRef);
            if (property instanceof Endpoint) {
                return (Endpoint) property;
            }
        }
        
        // Try to get the default endpoint from the message
        Object defaultEndpoint = message.getProperty("DEFAULT_ENDPOINT");
        if (defaultEndpoint instanceof Endpoint) {
            return (Endpoint) defaultEndpoint;
        }
        
        return null;
    }
    
    /**
     * Get the endpoint
     * 
     * @return The endpoint
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }
    
    /**
     * Set the endpoint
     * 
     * @param endpoint The endpoint
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    /**
     * Get the endpoint reference
     * 
     * @return The endpoint reference
     */
    public String getEndpointRef() {
        return endpointRef;
    }
    
    /**
     * Set the endpoint reference
     * 
     * @param endpointRef The endpoint reference
     */
    public void setEndpointRef(String endpointRef) {
        this.endpointRef = endpointRef;
    }
    
    /**
     * Check if this mediator is blocking
     * 
     * @return true if blocking, false otherwise
     */
    public boolean isBlocking() {
        return blocking;
    }
    
    /**
     * Set whether this mediator is blocking
     * 
     * @param blocking true if blocking, false otherwise
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
} 