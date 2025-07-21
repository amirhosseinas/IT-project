package org.apache.synapse.custom.qos.throttling;

/**
 * Defines a throttling policy with rate limits and other throttling parameters.
 */
public class ThrottlingPolicy {
    
    /**
     * Types of throttling policies
     */
    public enum ThrottlingType {
        /**
         * Global throttling applies to all requests regardless of client
         */
        GLOBAL,
        
        /**
         * Client-based throttling applies limits per client
         */
        CLIENT_BASED,
        
        /**
         * Time-based throttling varies limits based on time of day
         */
        TIME_BASED
    }
    
    private final String id;
    private final ThrottlingType type;
    private final int limit;
    private final long timeWindowMs;
    private final boolean queueThrottledRequests;
    private final long retryAfterMs;
    private final int concurrencyLimit;
    
    /**
     * Create a new throttling policy
     * 
     * @param builder The policy builder
     */
    private ThrottlingPolicy(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.limit = builder.limit;
        this.timeWindowMs = builder.timeWindowMs;
        this.queueThrottledRequests = builder.queueThrottledRequests;
        this.retryAfterMs = builder.retryAfterMs;
        this.concurrencyLimit = builder.concurrencyLimit;
    }
    
    /**
     * Get the policy ID
     * 
     * @return The policy ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the throttling type
     * 
     * @return The throttling type
     */
    public ThrottlingType getType() {
        return type;
    }
    
    /**
     * Get the request limit
     * 
     * @return The request limit
     */
    public int getLimit() {
        return limit;
    }
    
    /**
     * Get the time window in milliseconds
     * 
     * @return The time window
     */
    public long getTimeWindowMs() {
        return timeWindowMs;
    }
    
    /**
     * Check if throttled requests should be queued
     * 
     * @return true if requests should be queued, false otherwise
     */
    public boolean isQueueThrottledRequests() {
        return queueThrottledRequests;
    }
    
    /**
     * Get the retry delay in milliseconds
     * 
     * @return The retry delay
     */
    public long getRetryAfterMs() {
        return retryAfterMs;
    }
    
    /**
     * Get the concurrency limit
     * 
     * @return The concurrency limit
     */
    public int getConcurrencyLimit() {
        return concurrencyLimit;
    }
    
    /**
     * Builder for throttling policies
     */
    public static class Builder {
        private String id;
        private ThrottlingType type = ThrottlingType.GLOBAL;
        private int limit = 100;
        private long timeWindowMs = 60000; // 1 minute
        private boolean queueThrottledRequests = false;
        private long retryAfterMs = 1000;
        private int concurrencyLimit = 10;
        
        /**
         * Create a new builder with the specified ID
         * 
         * @param id The policy ID
         */
        public Builder(String id) {
            this.id = id;
        }
        
        /**
         * Set the throttling type
         * 
         * @param type The throttling type
         * @return This builder
         */
        public Builder type(ThrottlingType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Set the request limit
         * 
         * @param limit The request limit
         * @return This builder
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        /**
         * Set the time window in milliseconds
         * 
         * @param timeWindowMs The time window
         * @return This builder
         */
        public Builder timeWindowMs(long timeWindowMs) {
            this.timeWindowMs = timeWindowMs;
            return this;
        }
        
        /**
         * Set whether to queue throttled requests
         * 
         * @param queueThrottledRequests true to queue requests, false otherwise
         * @return This builder
         */
        public Builder queueThrottledRequests(boolean queueThrottledRequests) {
            this.queueThrottledRequests = queueThrottledRequests;
            return this;
        }
        
        /**
         * Set the retry delay in milliseconds
         * 
         * @param retryAfterMs The retry delay
         * @return This builder
         */
        public Builder retryAfterMs(long retryAfterMs) {
            this.retryAfterMs = retryAfterMs;
            return this;
        }
        
        /**
         * Set the concurrency limit
         * 
         * @param concurrencyLimit The concurrency limit
         * @return This builder
         */
        public Builder concurrencyLimit(int concurrencyLimit) {
            this.concurrencyLimit = concurrencyLimit;
            return this;
        }
        
        /**
         * Build the throttling policy
         * 
         * @return The built policy
         */
        public ThrottlingPolicy build() {
            return new ThrottlingPolicy(this);
        }
    }
} 