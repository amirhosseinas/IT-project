package org.apache.synapse.custom.mediation.registry;

import java.util.Collection;

/**
 * Registry interface for Apache Synapse.
 * The registry is responsible for storing and managing configuration resources,
 * such as sequences, endpoints, and proxy services.
 */
public interface Registry {
    
    /**
     * Get a resource from the registry
     * 
     * @param key The resource key
     * @return The resource value, or null if not found
     */
    Object get(String key);
    
    /**
     * Put a resource in the registry
     * 
     * @param key The resource key
     * @param value The resource value
     */
    void put(String key, Object value);
    
    /**
     * Remove a resource from the registry
     * 
     * @param key The resource key
     * @return The removed resource, or null if not found
     */
    Object remove(String key);
    
    /**
     * Check if a resource exists in the registry
     * 
     * @param key The resource key
     * @return true if the resource exists, false otherwise
     */
    boolean contains(String key);
    
    /**
     * Get all keys in the registry
     * 
     * @return Collection of all keys
     */
    Collection<String> getKeys();
    
    /**
     * Get all resources in the registry
     * 
     * @return Collection of all resources
     */
    Collection<Object> getAll();
    
    /**
     * Clear the registry
     */
    void clear();
    
    /**
     * Initialize the registry
     */
    void init();
    
    /**
     * Destroy the registry and release resources
     */
    void destroy();
} 