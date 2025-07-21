package org.apache.synapse.custom.qos.cache;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents an entry in the cache with metadata.
 */
public class CacheEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final Object value;
    private final long creationTime;
    private final long expirationTime;
    private long lastAccessTime;
    private int accessCount;
    
    /**
     * Create a new cache entry
     * 
     * @param value The value to cache
     * @param ttlSeconds Time to live in seconds
     */
    public CacheEntry(Object value, int ttlSeconds) {
        this.value = value;
        this.creationTime = Instant.now().toEpochMilli();
        this.lastAccessTime = this.creationTime;
        this.expirationTime = this.creationTime + (ttlSeconds * 1000L);
        this.accessCount = 0;
    }
    
    /**
     * Get the cached value
     * 
     * @return Cached value
     */
    public Object getValue() {
        this.lastAccessTime = Instant.now().toEpochMilli();
        this.accessCount++;
        return value;
    }
    
    /**
     * Get the creation time
     * 
     * @return Creation time in milliseconds since epoch
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Get the expiration time
     * 
     * @return Expiration time in milliseconds since epoch
     */
    public long getExpirationTime() {
        return expirationTime;
    }
    
    /**
     * Get the last access time
     * 
     * @return Last access time in milliseconds since epoch
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Get the number of times this entry has been accessed
     * 
     * @return Access count
     */
    public int getAccessCount() {
        return accessCount;
    }
    
    /**
     * Check if this entry has expired
     * 
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().toEpochMilli() > expirationTime;
    }
    
    /**
     * Get the remaining time to live
     * 
     * @return Remaining TTL in seconds, or 0 if expired
     */
    public long getRemainingTtlSeconds() {
        long now = Instant.now().toEpochMilli();
        if (now >= expirationTime) {
            return 0;
        }
        return (expirationTime - now) / 1000;
    }
    
    /**
     * Calculate the age of this entry
     * 
     * @return Age in seconds
     */
    public long getAgeSeconds() {
        return (Instant.now().toEpochMilli() - creationTime) / 1000;
    }
    
    /**
     * Calculate the idle time (time since last access)
     * 
     * @return Idle time in seconds
     */
    public long getIdleTimeSeconds() {
        return (Instant.now().toEpochMilli() - lastAccessTime) / 1000;
    }
} 