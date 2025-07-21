package org.apache.synapse.custom.qos.cache;

/**
 * Configuration for a cache.
 */
public class CacheConfig {
    
    /**
     * Default cache configuration with 1000 max entries and 5 minutes TTL
     */
    public static final CacheConfig DEFAULT = new CacheConfig(1000, 300);
    
    private final int maxSize;
    private final int defaultTtlSeconds;
    private final EvictionPolicy evictionPolicy;
    
    /**
     * Create a new cache configuration
     * 
     * @param maxSize Maximum number of entries
     * @param defaultTtlSeconds Default time to live in seconds
     */
    public CacheConfig(int maxSize, int defaultTtlSeconds) {
        this(maxSize, defaultTtlSeconds, EvictionPolicy.LRU);
    }
    
    /**
     * Create a new cache configuration with specified eviction policy
     * 
     * @param maxSize Maximum number of entries
     * @param defaultTtlSeconds Default time to live in seconds
     * @param evictionPolicy Eviction policy
     */
    public CacheConfig(int maxSize, int defaultTtlSeconds, EvictionPolicy evictionPolicy) {
        this.maxSize = maxSize;
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.evictionPolicy = evictionPolicy;
    }
    
    /**
     * Get the maximum size
     * 
     * @return Maximum number of entries
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Get the default TTL
     * 
     * @return Default time to live in seconds
     */
    public int getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }
    
    /**
     * Get the eviction policy
     * 
     * @return Eviction policy
     */
    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }
    
    /**
     * Eviction policies for cache
     */
    public enum EvictionPolicy {
        /**
         * Least Recently Used - evicts entries that haven't been accessed for the longest time
         */
        LRU,
        
        /**
         * Least Frequently Used - evicts entries that are used least often
         */
        LFU,
        
        /**
         * First In First Out - evicts oldest entries first
         */
        FIFO
    }
} 