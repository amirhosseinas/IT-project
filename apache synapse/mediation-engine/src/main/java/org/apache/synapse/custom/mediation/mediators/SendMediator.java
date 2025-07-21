package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.mediation.Endpoint;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.MediationRegistry;
import org.apache.synapse.custom.message.Message;

/**
 * Mediator for sending messages to endpoints.
 * This is a terminal mediator that ends the mediation flow.
 */
public class SendMediator extends AbstractMediator {
    
    private Endpoint endpoint;
    private String endpointRef;
    private MediationRegistry registry;
    
    /**
     * Create a new send mediator
     * 
     * @param name The mediator name
     * @param registry The mediation registry
     */
    public SendMediator(String name, MediationRegistry registry) {
        super(name);
        this.registry = registry;
    }
    
    /**
     * Create a new send mediator with the specified endpoint
     * 
     * @param name The mediator name
     * @param endpoint The endpoint to send to
     * @param registry The mediation registry
     */
    public SendMediator(String name, Endpoint endpoint, MediationRegistry registry) {
        super(name);
        this.endpoint = endpoint;
        this.registry = registry;
    }
    
    /**
     * Create a new send mediator with the specified endpoint reference
     * 
     * @param name The mediator name
     * @param endpointRef The endpoint reference
     * @param registry The mediation registry
     */
    public SendMediator(String name, String endpointRef, MediationRegistry registry) {
        super(name);
        this.endpointRef = endpointRef;
        this.registry = registry;
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        Endpoint targetEndpoint = resolveEndpoint(message);
        
        if (targetEndpoint == null) {
            throw new MediationEngine.MediationException("No endpoint specified for send mediator");
        }
        
        if (!targetEndpoint.isAvailable()) {
            logger.warn("Endpoint {} is not available", targetEndpoint.getName());
            throw new MediationEngine.MediationException("Endpoint " + targetEndpoint.getName() + " is not available");
        }
        
        logger.debug("Sending message to endpoint: {}", targetEndpoint.getName());
        
        try {
            Message response = targetEndpoint.send(message);
            
            // Mark the flow as stopped since this is a terminal mediator
            response.setProperty("STOP_FLOW", Boolean.TRUE);
            
            return response;
        } catch (Exception e) {
            logger.error("Error sending message to endpoint: " + targetEndpoint.getName(), e);
            throw new MediationEngine.MediationException("Error sending message to endpoint: " + targetEndpoint.getName(), e);
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
} 