package org.apache.synapse.custom.qos.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages caching for responses to improve performance.
 */
public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    private final Map<String, Cache> caches;
    private final ScheduledExecutorService cleanupScheduler;
    
    /**
     * Create a new cache manager
     */
    public CacheManager() {
        this.caches = new HashMap<>();
        this.cleanupScheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule cache cleanup every minute
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
        
        logger.info("Cache Manager initialized");
    }
    
    /**
     * Create a new cache with default settings
     * 
     * @param cacheId Unique cache identifier
     * @return The created cache
     */
    public Cache createCache(String cacheId) {
        return createCache(cacheId, CacheConfig.DEFAULT);
    }
    
    /**
     * Create a new cache with custom configuration
     * 
     * @param cacheId Unique cache identifier
     * @param config Cache configuration
     * @return The created cache
     */
    public Cache createCache(String cacheId, CacheConfig config) {
        Cache cache = new MemoryCache(cacheId, config);
        caches.put(cacheId, cache);
        logger.info("Created cache: {} with max size: {}, TTL: {}s", 
                cacheId, config.getMaxSize(), config.getDefaultTtlSeconds());
        return cache;
    }
    
    /**
     * Get an existing cache by ID
     * 
     * @param cacheId Cache identifier
     * @return The cache, or null if not found
     */
    public Cache getCache(String cacheId) {
        return caches.get(cacheId);
    }
    
    /**
     * Remove a cache
     * 
     * @param cacheId Cache identifier
     */
    public void removeCache(String cacheId) {
        Cache cache = caches.remove(cacheId);
        if (cache != null) {
            cache.clear();
            logger.info("Removed cache: {}", cacheId);
        }
    }
    
    /**
     * Get the number of caches
     * 
     * @return Number of caches
     */
    public int getCacheCount() {
        return caches.size();
    }
    
    /**
     * Get the total number of cached entries across all caches
     * 
     * @return Total number of cached entries
     */
    public int getTotalEntryCount() {
        int count = 0;
        for (Cache cache : caches.values()) {
            count += cache.size();
        }
        return count;
    }
    
    /**
     * Clean up expired entries in all caches
     */
    private void cleanupExpiredEntries() {
        int removedCount = 0;
        
        for (Cache cache : caches.values()) {
            removedCount += cache.removeExpiredEntries();
        }
        
        if (removedCount > 0) {
            logger.debug("Removed {} expired cache entries", removedCount);
        }
    }
    
    /**
     * Shutdown the cache manager
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear all caches
        for (Cache cache : caches.values()) {
            cache.clear();
        }
        caches.clear();
        
        logger.info("Cache Manager shutdown complete");
    }
} 