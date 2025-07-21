package org.apache.synapse.custom.qos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements the Circuit Breaker pattern to prevent cascading failures.
 */
public class CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    public enum State {
        CLOSED,     // Normal operation, requests pass through
        OPEN,       // Circuit is open, fast-fail for all requests
        HALF_OPEN   // Testing if the system has recovered
    }
    
    private final String serviceId;
    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final int consecutiveSuccessThreshold;
    
    private volatile State state;
    private final AtomicInteger failureCount;
    private final AtomicInteger consecutiveSuccessCount;
    private final AtomicLong lastStateChangeTimestamp;
    
    /**
     * Create a new circuit breaker
     * 
     * @param serviceId The service identifier
     * @param failureThreshold Number of failures that will trip the circuit
     * @param resetTimeoutMs Time in milliseconds after which to try half-open state
     * @param consecutiveSuccessThreshold Number of consecutive successes to close circuit
     */
    public CircuitBreaker(String serviceId, int failureThreshold, long resetTimeoutMs, int consecutiveSuccessThreshold) {
        this.serviceId = serviceId;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.consecutiveSuccessThreshold = consecutiveSuccessThreshold;
        
        this.state = State.CLOSED;
        this.failureCount = new AtomicInteger(0);
        this.consecutiveSuccessCount = new AtomicInteger(0);
        this.lastStateChangeTimestamp = new AtomicLong(System.currentTimeMillis());
        
        logger.info("Circuit breaker initialized for service: {}", serviceId);
    }
    
    /**
     * Check if the circuit is currently open
     * 
     * @return true if the circuit is open, false otherwise
     */
    public boolean isOpen() {
        updateState();
        return state == State.OPEN;
    }
    
    /**
     * Record a successful request
     */
    public synchronized void recordSuccess() {
        updateState();
        
        switch (state) {
            case CLOSED:
                // Reset failure count on success
                failureCount.set(0);
                break;
                
            case HALF_OPEN:
                // Count consecutive successes to determine if circuit should close
                int successes = consecutiveSuccessCount.incrementAndGet();
                logger.debug("Service {} consecutive successes: {}/{}", serviceId, successes, consecutiveSuccessThreshold);
                
                if (successes >= consecutiveSuccessThreshold) {
                    transitionToState(State.CLOSED);
                }
                break;
                
            case OPEN:
                // Shouldn't happen, but just in case
                break;
        }
    }
    
    /**
     * Record a failed request
     */
    public synchronized void recordFailure() {
        updateState();
        
        switch (state) {
            case CLOSED:
                // Count failures until threshold
                int failures = failureCount.incrementAndGet();
                logger.debug("Service {} failures: {}/{}", serviceId, failures, failureThreshold);
                
                if (failures >= failureThreshold) {
                    transitionToState(State.OPEN);
                }
                break;
                
            case HALF_OPEN:
                // Any failure in half-open state opens the circuit again
                transitionToState(State.OPEN);
                break;
                
            case OPEN:
                // Already open, nothing to do
                break;
        }
    }
    
    private void updateState() {
        if (state == State.OPEN) {
            long elapsedMs = System.currentTimeMillis() - lastStateChangeTimestamp.get();
            if (elapsedMs >= resetTimeoutMs) {
                transitionToState(State.HALF_OPEN);
            }
        }
    }
    
    private void transitionToState(State newState) {
        if (state != newState) {
            logger.info("Circuit breaker for service {} transitioning from {} to {}", 
                    serviceId, state, newState);
            
            state = newState;
            lastStateChangeTimestamp.set(System.currentTimeMillis());
            
            if (newState == State.CLOSED) {
                failureCount.set(0);
            } else if (newState == State.HALF_OPEN) {
                consecutiveSuccessCount.set(0);
            }
        }
    }
    
    /**
     * Get the current state of the circuit breaker
     * 
     * @return The current state
     */
    public State getState() {
        updateState();
        return state;
    }
} 