package org.apache.synapse.custom.qos.cache;

/**
 * Interface for cache implementations.
 */
public interface Cache {
    
    /**
     * Get the cache identifier
     * 
     * @return Cache identifier
     */
    String getCacheId();
    
    /**
     * Get the cache configuration
     * 
     * @return Cache configuration
     */
    CacheConfig getConfig();
    
    /**
     * Put an item in the cache with default TTL
     * 
     * @param key Cache key
     * @param value Value to cache
     * @return Previous value, or null if none
     */
    Object put(String key, Object value);
    
    /**
     * Put an item in the cache with custom TTL
     * 
     * @param key Cache key
     * @param value Value to cache
     * @param ttlSeconds Time to live in seconds
     * @return Previous value, or null if none
     */
    Object put(String key, Object value, int ttlSeconds);
    
    /**
     * Get an item from the cache
     * 
     * @param key Cache key
     * @return Cached value, or null if not found or expired
     */
    Object get(String key);
    
    /**
     * Check if the cache contains a key
     * 
     * @param key Cache key
     * @return true if the key exists and is not expired, false otherwise
     */
    boolean containsKey(String key);
    
    /**
     * Remove an item from the cache
     * 
     * @param key Cache key
     * @return Removed value, or null if not found
     */
    Object remove(String key);
    
    /**
     * Clear the cache
     */
    void clear();
    
    /**
     * Get the number of items in the cache
     * 
     * @return Number of items
     */
    int size();
    
    /**
     * Remove expired entries from the cache
     * 
     * @return Number of entries removed
     */
    int removeExpiredEntries();
    
    /**
     * Get the number of cache hits
     * 
     * @return Number of cache hits
     */
    long getHitCount();
    
    /**
     * Get the number of cache misses
     * 
     * @return Number of cache misses
     */
    long getMissCount();
    
    /**
     * Reset the hit and miss counters
     */
    void resetStatistics();
} 