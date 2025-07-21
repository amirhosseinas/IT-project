package org.apache.synapse.custom.qos.throttling;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Represents a throttled request that is queued for later processing.
 * Implements the Delayed interface for use with DelayQueue.
 */
public class ThrottledRequest implements Delayed {
    
    private final String policyId;
    private final String clientId;
    private final long expirationTime;
    
    /**
     * Create a new throttled request
     * 
     * @param policyId The policy ID that throttled the request
     * @param clientId The client ID that made the request
     * @param delayMs The delay in milliseconds before the request can be processed
     */
    public ThrottledRequest(String policyId, String clientId, long delayMs) {
        this.policyId = policyId;
        this.clientId = clientId;
        this.expirationTime = System.currentTimeMillis() + delayMs;
    }
    
    /**
     * Get the policy ID
     * 
     * @return The policy ID
     */
    public String getPolicyId() {
        return policyId;
    }
    
    /**
     * Get the client ID
     * 
     * @return The client ID
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Get the expiration time
     * 
     * @return The expiration time in milliseconds
     */
    public long getExpirationTime() {
        return expirationTime;
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long diff = expirationTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        
        if (other instanceof ThrottledRequest) {
            ThrottledRequest otherRequest = (ThrottledRequest) other;
            return Long.compare(expirationTime, otherRequest.expirationTime);
        }
        
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        ThrottledRequest other = (ThrottledRequest) obj;
        return expirationTime == other.expirationTime &&
               policyId.equals(other.policyId) &&
               clientId.equals(other.clientId);
    }
    
    @Override
    public int hashCode() {
        int result = policyId.hashCode();
        result = 31 * result + clientId.hashCode();
        result = 31 * result + (int) (expirationTime ^ (expirationTime >>> 32));
        return result;
    }
} 