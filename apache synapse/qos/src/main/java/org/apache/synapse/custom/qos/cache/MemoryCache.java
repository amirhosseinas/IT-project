package org.apache.synapse.custom.qos.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory-based implementation of the Cache interface.
 * Stores cache entries in memory using a ConcurrentHashMap.
 */
public class MemoryCache implements Cache {
    private static final Logger logger = LoggerFactory.getLogger(MemoryCache.class);
    
    private final String cacheId;
    private final CacheConfig config;
    private final Map<String, CacheEntry> entries;
    
    /**
     * Create a new memory cache with the specified ID and configuration
     * 
     * @param cacheId Unique cache identifier
     * @param config Cache configuration
     */
    public MemoryCache(String cacheId, CacheConfig config) {
        this.cacheId = cacheId;
        this.config = config;
        this.entries = new ConcurrentHashMap<>();
        
        logger.info("Memory cache created: {}", cacheId);
    }
    
    @Override
    public String getCacheId() {
        return cacheId;
    }
    
    @Override
    public void put(String key, Object value) {
        put(key, value, config.getDefaultTtlSeconds());
    }
    
    @Override
    public void put(String key, Object value, int ttlSeconds) {
        // Check if cache is at capacity
        if (entries.size() >= config.getMaxSize() && !entries.containsKey(key)) {
            evictEntries();
        }
        
        // Create and store the cache entry
        long expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000L);
        CacheEntry entry = new CacheEntry(key, value, expiryTime);
        entries.put(key, entry);
        
        logger.debug("Added entry to cache {}: key={}, ttl={}s", cacheId, key, ttlSeconds);
    }
    
    @Override
    public Object get(String key) {
        CacheEntry entry = entries.get(key);
        
        // Return null if entry doesn't exist
        if (entry == null) {
            logger.debug("Cache miss for key: {}", key);
            return null;
        }
        
        // Check if entry has expired
        if (entry.isExpired()) {
            entries.remove(key);
            logger.debug("Cache entry expired: {}", key);
            return null;
        }
        
        // Update access time and return value
        entry.updateAccessTime();
        logger.debug("Cache hit for key: {}", key);
        return entry.getValue();
    }
    
    @Override
    public boolean contains(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            entries.remove(key);
            return false;
        }
        
        return true;
    }
    
    @Override
    public void remove(String key) {
        entries.remove(key);
        logger.debug("Removed entry from cache {}: key={}", cacheId, key);
    }
    
    @Override
    public void clear() {
        entries.clear();
        logger.info("Cleared cache: {}", cacheId);
    }
    
    @Override
    public int size() {
        // Remove expired entries before returning size
        removeExpiredEntries();
        return entries.size();
    }
    
    /**
     * Remove all expired entries from the cache
     * 
     * @return The number of entries removed
     */
    public int removeExpiredEntries() {
        int count = 0;
        long now = System.currentTimeMillis();
        
        for (Map.Entry<String, CacheEntry> entry : entries.entrySet()) {
            if (entry.getValue().getExpiryTime() <= now) {
                entries.remove(entry.getKey());
                count++;
            }
        }
        
        if (count > 0) {
            logger.debug("Removed {} expired entries from cache: {}", count, cacheId);
        }
        
        return count;
    }
    
    /**
     * Evict entries based on the configured eviction policy
     */
    private void evictEntries() {
        // For now, just remove the oldest accessed entry
        // This can be extended to support other eviction policies
        String oldestKey = null;
        long oldestAccessTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : entries.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestAccessTime) {
                oldestAccessTime = accessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            entries.remove(oldestKey);
            logger.debug("Evicted oldest entry from cache {}: key={}", cacheId, oldestKey);
        }
    }
} 