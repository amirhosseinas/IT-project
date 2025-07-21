package org.apache.synapse.custom.qos.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines a security policy for a service, specifying which roles are allowed
 * to perform which actions.
 */
public class SecurityPolicy {
    
    private final String policyId;
    private final String serviceId;
    private final Map<String, Set<String>> actionRoleMap;
    
    /**
     * Create a new security policy
     * 
     * @param policyId Unique policy identifier
     * @param serviceId Service identifier
     */
    public SecurityPolicy(String policyId, String serviceId) {
        this.policyId = policyId;
        this.serviceId = serviceId;
        this.actionRoleMap = new HashMap<>();
    }
    
    /**
     * Add a role that is allowed to perform a specific action
     * 
     * @param action Action name
     * @param role Role name
     */
    public void addAllowedRole(String action, String role) {
        actionRoleMap.computeIfAbsent(action, k -> new HashSet<>()).add(role);
    }
    
    /**
     * Remove a role from those allowed to perform a specific action
     * 
     * @param action Action name
     * @param role Role name
     */
    public void removeAllowedRole(String action, String role) {
        Set<String> roles = actionRoleMap.get(action);
        if (roles != null) {
            roles.remove(role);
        }
    }
    
    /**
     * Check if a set of roles is authorized to perform an action
     * 
     * @param roles Set of roles
     * @param action Action name
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorized(Set<String> roles, String action) {
        if (roles == null || roles.isEmpty() || action == null) {
            return false;
        }
        
        Set<String> allowedRoles = actionRoleMap.get(action);
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return false;
        }
        
        // Check if any of the user's roles are in the allowed roles set
        for (String role : roles) {
            if (allowedRoles.contains(role)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the policy identifier
     * 
     * @return Policy identifier
     */
    public String getPolicyId() {
        return policyId;
    }
    
    /**
     * Get the service identifier
     * 
     * @return Service identifier
     */
    public String getServiceId() {
        return serviceId;
    }
    
    /**
     * Get all allowed roles for an action
     * 
     * @param action Action name
     * @return Set of allowed roles, or empty set if none
     */
    public Set<String> getAllowedRoles(String action) {
        Set<String> roles = actionRoleMap.get(action);
        return roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
    }
    
    /**
     * Get all actions defined in this policy
     * 
     * @return Set of action names
     */
    public Set<String> getActions() {
        return Collections.unmodifiableSet(actionRoleMap.keySet());
    }
} 