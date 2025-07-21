package org.apache.synapse.custom.mediation.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory-based implementation of the Registry interface.
 * Stores all resources in memory using a ConcurrentHashMap.
 */
public class MemoryRegistry implements Registry {
    private static final Logger logger = LoggerFactory.getLogger(MemoryRegistry.class);
    
    private final Map<String, Object> resources;
    private final Map<String, Long> lastModifiedTimes;
    
    /**
     * Create a new memory registry
     */
    public MemoryRegistry() {
        this.resources = new ConcurrentHashMap<>();
        this.lastModifiedTimes = new ConcurrentHashMap<>();
        logger.info("Memory registry initialized");
    }
    
    @Override
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        
        Object value = resources.get(key);
        if (value == null) {
            logger.debug("Resource not found in registry: {}", key);
        } else {
            logger.debug("Retrieved resource from registry: {}", key);
        }
        
        return value;
    }
    
    @Override
    public void put(String key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
        
        resources.put(key, value);
        lastModifiedTimes.put(key, System.currentTimeMillis());
        logger.debug("Added resource to registry: {}", key);
    }
    
    @Override
    public Object remove(String key) {
        if (key == null) {
            return null;
        }
        
        Object removed = resources.remove(key);
        if (removed != null) {
            lastModifiedTimes.remove(key);
            logger.debug("Removed resource from registry: {}", key);
        }
        
        return removed;
    }
    
    @Override
    public boolean contains(String key) {
        if (key == null) {
            return false;
        }
        
        return resources.containsKey(key);
    }
    
    @Override
    public Collection<String> getKeys() {
        return Collections.unmodifiableCollection(resources.keySet());
    }
    
    @Override
    public Collection<Object> getAll() {
        return Collections.unmodifiableCollection(resources.values());
    }
    
    @Override
    public void clear() {
        resources.clear();
        lastModifiedTimes.clear();
        logger.info("Registry cleared");
    }
    
    @Override
    public void init() {
        // Nothing to do for memory-based registry
        logger.info("Memory registry initialized");
    }
    
    @Override
    public void destroy() {
        clear();
        logger.info("Memory registry destroyed");
    }
    
    /**
     * Get the last modified time for a resource
     * 
     * @param key The resource key
     * @return The last modified time in milliseconds, or 0 if not found
     */
    public long getLastModifiedTime(String key) {
        if (key == null) {
            return 0;
        }
        
        return lastModifiedTimes.getOrDefault(key, 0L);
    }
    
    /**
     * Get the number of resources in the registry
     * 
     * @return The number of resources
     */
    public int size() {
        return resources.size();
    }
} 