package org.apache.synapse.custom.mediation;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a proxy service in Apache Synapse.
 * A proxy service virtualizes a backend service and provides mediation capabilities.
 */
public class ProxyService {
    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);
    
    private final String name;
    private String wsdlUrl;
    private String targetEndpoint;
    private MediationSequence inSequence;
    private MediationSequence outSequence;
    private MediationSequence faultSequence;
    private final List<String> transports;
    private final Map<String, Object> parameters;
    private boolean started;
    private final MediationRegistry registry;
    
    /**
     * Create a new proxy service
     * 
     * @param name The service name
     * @param registry The mediation registry
     */
    public ProxyService(String name, MediationRegistry registry) {
        this.name = name;
        this.registry = registry;
        this.transports = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.started = false;
    }
    
    /**
     * Start the proxy service
     */
    public void start() {
        if (started) {
            logger.warn("Proxy service {} is already started", name);
            return;
        }
        
        logger.info("Starting proxy service: {}", name);
        started = true;
    }
    
    /**
     * Stop the proxy service
     */
    public void stop() {
        if (!started) {
            logger.warn("Proxy service {} is not started", name);
            return;
        }
        
        logger.info("Stopping proxy service: {}", name);
        started = false;
    }
    
    /**
     * Process a request message
     * 
     * @param message The request message
     * @return The response message
     * @throws MediationEngine.MediationException if processing fails
     */
    public Message processRequest(Message message) throws MediationEngine.MediationException {
        if (!started) {
            throw new MediationEngine.MediationException("Proxy service " + name + " is not started");
        }
        
        logger.debug("Processing request for proxy service: {}", name);
        
        try {
            // Apply in sequence if available
            if (inSequence != null) {
                message = inSequence.apply(message);
            }
            
            // Check if flow has been stopped
            if (Boolean.TRUE.equals(message.getProperty("STOP_FLOW"))) {
                logger.debug("Flow stopped by in sequence");
                return message;
            }
            
            // Send to target endpoint if specified
            Message response = message;
            if (targetEndpoint != null && !targetEndpoint.isEmpty()) {
                Endpoint endpoint = registry.getEndpoint(targetEndpoint);
                if (endpoint == null) {
                    throw new MediationEngine.MediationException("Target endpoint not found: " + targetEndpoint);
                }
                
                response = endpoint.send(message);
            }
            
            // Apply out sequence if available
            if (outSequence != null) {
                response = outSequence.apply(response);
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error processing request for proxy service: " + name, e);
            
            // Apply fault sequence if available
            if (faultSequence != null) {
                try {
                    message.setProperty("ERROR_MESSAGE", e.getMessage());
                    message.setProperty("ERROR_DETAIL", e);
                    return faultSequence.apply(message);
                } catch (Exception faultEx) {
                    logger.error("Error in fault sequence", faultEx);
                }
            }
            
            throw new MediationEngine.MediationException("Error processing request for proxy service: " + name, e);
        }
    }
    
    /**
     * Get the service name
     * 
     * @return The service name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the WSDL URL
     * 
     * @return The WSDL URL
     */
    public String getWsdlUrl() {
        return wsdlUrl;
    }
    
    /**
     * Set the WSDL URL
     * 
     * @param wsdlUrl The WSDL URL
     */
    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }
    
    /**
     * Get the target endpoint
     * 
     * @return The target endpoint
     */
    public String getTargetEndpoint() {
        return targetEndpoint;
    }
    
    /**
     * Set the target endpoint
     * 
     * @param targetEndpoint The target endpoint
     */
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }
    
    /**
     * Get the in sequence
     * 
     * @return The in sequence
     */
    public MediationSequence getInSequence() {
        return inSequence;
    }
    
    /**
     * Set the in sequence
     * 
     * @param inSequence The in sequence
     */
    public void setInSequence(MediationSequence inSequence) {
        this.inSequence = inSequence;
    }
    
    /**
     * Get the out sequence
     * 
     * @return The out sequence
     */
    public MediationSequence getOutSequence() {
        return outSequence;
    }
    
    /**
     * Set the out sequence
     * 
     * @param outSequence The out sequence
     */
    public void setOutSequence(MediationSequence outSequence) {
        this.outSequence = outSequence;
    }
    
    /**
     * Get the fault sequence
     * 
     * @return The fault sequence
     */
    public MediationSequence getFaultSequence() {
        return faultSequence;
    }
    
    /**
     * Set the fault sequence
     * 
     * @param faultSequence The fault sequence
     */
    public void setFaultSequence(MediationSequence faultSequence) {
        this.faultSequence = faultSequence;
    }
    
    /**
     * Add a transport
     * 
     * @param transport The transport name
     */
    public void addTransport(String transport) {
        transports.add(transport);
    }
    
    /**
     * Get the transports
     * 
     * @return List of transports
     */
    public List<String> getTransports() {
        return transports;
    }
    
    /**
     * Set a parameter
     * 
     * @param name The parameter name
     * @param value The parameter value
     */
    public void setParameter(String name, Object value) {
        parameters.put(name, value);
    }
    
    /**
     * Get a parameter
     * 
     * @param name The parameter name
     * @return The parameter value
     */
    public Object getParameter(String name) {
        return parameters.get(name);
    }
    
    /**
     * Get all parameters
     * 
     * @return Map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Check if the service is started
     * 
     * @return true if started, false otherwise
     */
    public boolean isStarted() {
        return started;
    }
} 