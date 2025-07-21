package org.apache.synapse.custom.qos;

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.custom.qos.cache.Cache;
import org.apache.synapse.custom.qos.cache.CacheManager;
import org.apache.synapse.custom.qos.reliable.MessageDeliveryCallback;
import org.apache.synapse.custom.qos.reliable.ReliableMessagingModule;
import org.apache.synapse.custom.qos.security.SecurityManager;
import org.apache.synapse.custom.qos.throttling.ThrottlingModule;
import org.apache.synapse.custom.qos.throttling.ThrottlingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The QosManager is responsible for managing Quality of Service aspects
 * such as throttling, rate limiting, circuit breaking, caching, security,
 * and reliable messaging.
 */
public class QosManager {
    private static final Logger logger = LoggerFactory.getLogger(QosManager.class);
    
    // Legacy throttling components (for backward compatibility)
    private final Map<String, ThrottlePolicy> throttlePolicies;
    private final Map<String, AtomicInteger> requestCounters;
    private final Map<String, CircuitBreaker> circuitBreakers;
    private final ScheduledExecutorService scheduler;
    
    // New QoS modules
    private final ThrottlingModule throttlingModule;
    private final CacheManager cacheManager;
    private final SecurityManager securityManager;
    private final ReliableMessagingModule reliableMessagingModule;
    
    /**
     * Create a new QoS manager with all modules
     */
    public QosManager() {
        // Initialize legacy components
        this.throttlePolicies = new HashMap<>();
        this.requestCounters = new ConcurrentHashMap<>();
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule the reset of counters every minute
        scheduler.scheduleAtFixedRate(this::resetCounters, 1, 1, TimeUnit.MINUTES);
        
        // Initialize new modules
        this.throttlingModule = new ThrottlingModule();
        this.cacheManager = new CacheManager();
        this.securityManager = new SecurityManager();
        this.reliableMessagingModule = new ReliableMessagingModule();
        
        logger.info("QoS Manager initialized with all modules");
    }
    
    // Legacy methods for backward compatibility
    
    /**
     * Register a throttle policy for a service
     * 
     * @param serviceId The service identifier
     * @param policy The throttle policy to apply
     * @deprecated Use the ThrottlingModule directly instead
     */
    @Deprecated
    public void registerThrottlePolicy(String serviceId, ThrottlePolicy policy) {
        if (StringUtils.isBlank(serviceId)) {
            throw new IllegalArgumentException("Service ID cannot be null or empty");
        }
        throttlePolicies.put(serviceId, policy);
        requestCounters.putIfAbsent(serviceId, new AtomicInteger(0));
        logger.info("Registered throttle policy for service: {}", serviceId);
    }
    
    /**
     * Register a circuit breaker for a service
     * 
     * @param serviceId The service identifier
     * @param circuitBreaker The circuit breaker to apply
     */
    public void registerCircuitBreaker(String serviceId, CircuitBreaker circuitBreaker) {
        if (StringUtils.isBlank(serviceId)) {
            throw new IllegalArgumentException("Service ID cannot be null or empty");
        }
        circuitBreakers.put(serviceId, circuitBreaker);
        logger.info("Registered circuit breaker for service: {}", serviceId);
    }
    
    /**
     * Check if a request should be throttled
     * 
     * @param serviceId The service identifier
     * @return true if the request should be throttled, false otherwise
     * @deprecated Use the ThrottlingModule directly instead
     */
    @Deprecated
    public boolean shouldThrottle(String serviceId) {
        ThrottlePolicy policy = throttlePolicies.get(serviceId);
        if (policy == null) {
            return false; // No policy means no throttling
        }
        
        AtomicInteger counter = requestCounters.get(serviceId);
        if (counter == null) {
            counter = new AtomicInteger(0);
            requestCounters.put(serviceId, counter);
        }
        
        int currentCount = counter.incrementAndGet();
        boolean shouldThrottle = currentCount > policy.getMaxRequestsPerMinute();
        
        if (shouldThrottle) {
            logger.warn("Request throttled for service: {} (count: {})", serviceId, currentCount);
        }
        
        return shouldThrottle;
    }
    
    /**
     * Check if a circuit is open (service unavailable)
     * 
     * @param serviceId The service identifier
     * @return true if the circuit is open, false otherwise
     */
    public boolean isCircuitOpen(String serviceId) {
        CircuitBreaker breaker = circuitBreakers.get(serviceId);
        if (breaker == null) {
            return false; // No circuit breaker means always closed circuit
        }
        
        boolean isOpen = breaker.isOpen();
        if (isOpen) {
            logger.warn("Circuit is open for service: {}", serviceId);
        }
        
        return isOpen;
    }
    
    /**
     * Record a successful request for circuit breaker statistics
     * 
     * @param serviceId The service identifier
     */
    public void recordSuccess(String serviceId) {
        CircuitBreaker breaker = circuitBreakers.get(serviceId);
        if (breaker != null) {
            breaker.recordSuccess();
        }
    }
    
    /**
     * Record a failed request for circuit breaker statistics
     * 
     * @param serviceId The service identifier
     */
    public void recordFailure(String serviceId) {
        CircuitBreaker breaker = circuitBreakers.get(serviceId);
        if (breaker != null) {
            breaker.recordFailure();
        }
    }
    
    private void resetCounters() {
        logger.debug("Resetting request counters");
        for (AtomicInteger counter : requestCounters.values()) {
            counter.set(0);
        }
    }
    
    // New methods for accessing the QoS modules
    
    /**
     * Get the throttling module
     * 
     * @return The throttling module
     */
    public ThrottlingModule getThrottlingModule() {
        return throttlingModule;
    }
    
    /**
     * Get the cache manager
     * 
     * @return The cache manager
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Get the security manager
     * 
     * @return The security manager
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    /**
     * Get the reliable messaging module
     * 
     * @return The reliable messaging module
     */
    public ReliableMessagingModule getReliableMessagingModule() {
        return reliableMessagingModule;
    }
    
    /**
     * Create a new cache with the specified ID
     * 
     * @param cacheId The cache identifier
     * @return The created cache
     */
    public Cache createCache(String cacheId) {
        return cacheManager.createCache(cacheId);
    }
    
    /**
     * Set the message delivery callback for reliable messaging
     * 
     * @param callback The callback to invoke for message delivery
     */
    public void setMessageDeliveryCallback(MessageDeliveryCallback callback) {
        reliableMessagingModule.setDeliveryCallback(callback);
    }
    
    /**
     * Register a throttling policy with the throttling module
     * 
     * @param policy The throttling policy to register
     */
    public void registerThrottlingPolicy(ThrottlingPolicy policy) {
        throttlingModule.registerPolicy(policy);
    }
    
    /**
     * Shutdown the QoS manager and all its modules
     */
    public void shutdown() {
        // Shutdown legacy components
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Shutdown new modules
        throttlingModule.shutdown();
        reliableMessagingModule.shutdown();
        
        logger.info("QoS Manager shutdown complete");
    }
} 