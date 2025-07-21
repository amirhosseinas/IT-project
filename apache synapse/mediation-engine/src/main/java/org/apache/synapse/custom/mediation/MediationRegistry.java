package org.apache.synapse.custom.mediation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The MediationRegistry maintains references to all registered
 * mediation sequences and endpoints.
 */
public class MediationRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MediationRegistry.class);
    
    private final Map<String, MediationSequence> sequences;
    private final Map<String, Endpoint> endpoints;
    
    /**
     * Create a new mediation registry
     */
    public MediationRegistry() {
        this.sequences = new HashMap<>();
        this.endpoints = new HashMap<>();
        logger.info("Mediation Registry initialized");
    }
    
    /**
     * Register a mediation sequence
     * 
     * @param sequence The sequence to register
     */
    public void registerSequence(MediationSequence sequence) {
        sequences.put(sequence.getName(), sequence);
        logger.info("Registered sequence in registry: {}", sequence.getName());
    }
    
    /**
     * Register an endpoint
     * 
     * @param endpoint The endpoint to register
     */
    public void registerEndpoint(Endpoint endpoint) {
        endpoints.put(endpoint.getName(), endpoint);
        logger.info("Registered endpoint in registry: {}", endpoint.getName());
    }
    
    /**
     * Get a sequence by name
     * 
     * @param name The sequence name
     * @return The sequence or null if not found
     */
    public MediationSequence getSequence(String name) {
        MediationSequence sequence = sequences.get(name);
        if (sequence == null) {
            logger.warn("Sequence not found in registry: {}", name);
        }
        return sequence;
    }
    
    /**
     * Get an endpoint by name
     * 
     * @param name The endpoint name
     * @return The endpoint or null if not found
     */
    public Endpoint getEndpoint(String name) {
        Endpoint endpoint = endpoints.get(name);
        if (endpoint == null) {
            logger.warn("Endpoint not found in registry: {}", name);
        }
        return endpoint;
    }
    
    /**
     * Remove a sequence from the registry
     * 
     * @param name The sequence name
     * @return The removed sequence or null if not found
     */
    public MediationSequence removeSequence(String name) {
        MediationSequence removed = sequences.remove(name);
        if (removed != null) {
            logger.info("Removed sequence from registry: {}", name);
        }
        return removed;
    }
    
    /**
     * Remove an endpoint from the registry
     * 
     * @param name The endpoint name
     * @return The removed endpoint or null if not found
     */
    public Endpoint removeEndpoint(String name) {
        Endpoint removed = endpoints.remove(name);
        if (removed != null) {
            logger.info("Removed endpoint from registry: {}", name);
        }
        return removed;
    }
} 