package org.apache.synapse.custom.qos;

/**
 * Defines a throttle policy with rate limits for a service.
 */
public class ThrottlePolicy {
    
    private final int maxRequestsPerMinute;
    private final String serviceId;
    
    /**
     * Create a new throttle policy
     * 
     * @param serviceId The service identifier
     * @param maxRequestsPerMinute The maximum requests allowed per minute
     */
    public ThrottlePolicy(String serviceId, int maxRequestsPerMinute) {
        this.serviceId = serviceId;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }
    
    /**
     * Get the service identifier
     * 
     * @return The service identifier
     */
    public String getServiceId() {
        return serviceId;
    }
    
    /**
     * Get the maximum requests allowed per minute
     * 
     * @return The maximum requests per minute
     */
    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }
} 