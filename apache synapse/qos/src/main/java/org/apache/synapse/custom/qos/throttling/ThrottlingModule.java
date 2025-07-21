package org.apache.synapse.custom.qos.throttling;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Advanced throttling module that provides rate limiting capabilities
 * for API requests based on various criteria.
 */
public class ThrottlingModule {
    private static final Logger logger = LoggerFactory.getLogger(ThrottlingModule.class);
    
    // Maps to store throttling policies and counters
    private final Map<String, ThrottlingPolicy> policies;
    private final Map<String, Map<String, AtomicInteger>> counters;
    private final Map<String, ReadWriteLock> locks;
    
    // Queue for throttled requests
    private final DelayQueue<ThrottledRequest> throttledRequests;
    
    // Scheduler for periodic tasks
    private final ScheduledExecutorService scheduler;
    
    /**
     * Create a new throttling module
     */
    public ThrottlingModule() {
        this.policies = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
        this.locks = new ConcurrentHashMap<>();
        this.throttledRequests = new DelayQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Schedule counter reset based on time windows
        scheduler.scheduleAtFixedRate(this::resetCounters, 1, 1, TimeUnit.MINUTES);
        
        // Start the throttled request processor
        scheduler.submit(this::processThrottledRequests);
        
        logger.info("Throttling module initialized");
    }
    
    /**
     * Register a throttling policy
     * 
     * @param policy The throttling policy to register
     */
    public void registerPolicy(ThrottlingPolicy policy) {
        if (policy == null || StringUtils.isBlank(policy.getId())) {
            throw new IllegalArgumentException("Policy or policy ID cannot be null");
        }
        
        policies.put(policy.getId(), policy);
        counters.put(policy.getId(), new ConcurrentHashMap<>());
        locks.put(policy.getId(), new ReentrantReadWriteLock());
        
        logger.info("Registered throttling policy: {}", policy.getId());
    }
    
    /**
     * Check if a request should be throttled
     * 
     * @param policyId The policy ID to check against
     * @param clientId The client ID making the request
     * @return true if the request should be throttled, false otherwise
     */
    public boolean shouldThrottle(String policyId, String clientId) {
        if (StringUtils.isBlank(policyId) || StringUtils.isBlank(clientId)) {
            return false;
        }
        
        ThrottlingPolicy policy = policies.get(policyId);
        if (policy == null) {
            return false;
        }
        
        Map<String, AtomicInteger> policyCounters = counters.get(policyId);
        ReadWriteLock lock = locks.get(policyId);
        
        // Use read lock for checking
        lock.readLock().lock();
        try {
            // Get or create counter for this client
            AtomicInteger counter = policyCounters.computeIfAbsent(clientId, k -> new AtomicInteger(0));
            
            // Check if limit is exceeded
            int currentCount = counter.incrementAndGet();
            boolean shouldThrottle = false;
            
            switch (policy.getType()) {
                case GLOBAL:
                    shouldThrottle = currentCount > policy.getLimit();
                    break;
                case CLIENT_BASED:
                    shouldThrottle = currentCount > policy.getLimit();
                    break;
                case TIME_BASED:
                    shouldThrottle = currentCount > calculateTimeBasedLimit(policy);
                    break;
            }
            
            if (shouldThrottle) {
                logger.debug("Request throttled: policy={}, client={}, count={}", 
                        policyId, clientId, currentCount);
                
                // If queue management is enabled, add to throttled requests queue
                if (policy.isQueueThrottledRequests()) {
                    long delayMs = calculateBackoffDelay(policy, currentCount - policy.getLimit());
                    ThrottledRequest request = new ThrottledRequest(policyId, clientId, delayMs);
                    throttledRequests.add(request);
                    logger.debug("Added to throttled queue with delay: {}ms", delayMs);
                }
            }
            
            return shouldThrottle;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Calculate the limit based on time of day for time-based policies
     */
    private int calculateTimeBasedLimit(ThrottlingPolicy policy) {
        // Simple implementation - could be extended to support different rates
        // based on time of day, day of week, etc.
        int hour = java.time.LocalTime.now().getHour();
        
        // Reduce limit during peak hours (9am-5pm)
        if (hour >= 9 && hour <= 17) {
            return policy.getLimit() / 2;
        }
        
        return policy.getLimit();
    }
    
    /**
     * Calculate exponential backoff delay for throttled requests
     */
    private long calculateBackoffDelay(ThrottlingPolicy policy, int overLimit) {
        // Simple exponential backoff with a cap
        long baseDelay = policy.getRetryAfterMs();
        int factor = Math.min(overLimit, 10); // Cap the factor at 10
        
        return Math.min(baseDelay * (1L << factor), 60000); // Cap at 1 minute
    }
    
    /**
     * Reset the request counters based on policy time windows
     */
    private void resetCounters() {
        for (String policyId : policies.keySet()) {
            ReadWriteLock lock = locks.get(policyId);
            
            // Use write lock for resetting
            lock.writeLock().lock();
            try {
                counters.get(policyId).clear();
                logger.debug("Reset counters for policy: {}", policyId);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Process throttled requests that have completed their delay period
     */
    private void processThrottledRequests() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ThrottledRequest request = throttledRequests.take();
                logger.debug("Processing throttled request: policy={}, client={}", 
                        request.getPolicyId(), request.getClientId());
                
                // Here we would notify listeners that the request can now proceed
                // For now, just log it
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Throttled request processor interrupted", e);
                break;
            }
        }
    }
    
    /**
     * Get the current request count for a client under a specific policy
     * 
     * @param policyId The policy ID
     * @param clientId The client ID
     * @return The current request count
     */
    public int getCurrentCount(String policyId, String clientId) {
        Map<String, AtomicInteger> policyCounters = counters.get(policyId);
        if (policyCounters == null) {
            return 0;
        }
        
        AtomicInteger counter = policyCounters.get(clientId);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Shutdown the throttling module
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Throttling module shutdown complete");
    }
} 