package org.apache.synapse.custom.qos.security;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user's credentials including password hash and assigned roles.
 */
public class UserCredential {
    
    private final String username;
    private String passwordHash;
    private final Set<String> roles;
    
    /**
     * Create a new user credential
     * 
     * @param username Username
     * @param passwordHash Hashed password
     */
    public UserCredential(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>();
    }
    
    /**
     * Create a new user credential with plain text password (will be hashed)
     * 
     * @param username Username
     * @param plainPassword Plain text password
     * @return New UserCredential instance
     */
    public static UserCredential createWithPlainPassword(String username, String plainPassword) {
        String passwordHash = hashPassword(plainPassword);
        return new UserCredential(username, passwordHash);
    }
    
    /**
     * Add a role to this user
     * 
     * @param role Role name
     */
    public void addRole(String role) {
        roles.add(role);
    }
    
    /**
     * Remove a role from this user
     * 
     * @param role Role name
     */
    public void removeRole(String role) {
        roles.remove(role);
    }
    
    /**
     * Check if the user has a specific role
     * 
     * @param role Role name
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Get all roles assigned to this user
     * 
     * @return Unmodifiable set of roles
     */
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
    
    /**
     * Get the username
     * 
     * @return Username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Update the password with a new plain text password
     * 
     * @param newPassword New plain text password
     */
    public void updatePassword(String newPassword) {
        this.passwordHash = hashPassword(newPassword);
    }
    
    /**
     * Authenticate with a plain text password
     * 
     * @param plainPassword Plain text password
     * @return true if authentication succeeds, false otherwise
     */
    public boolean authenticate(String plainPassword) {
        String hashedInput = hashPassword(plainPassword);
        return passwordHash.equals(hashedInput);
    }
    
    /**
     * Hash a plain text password
     * 
     * @param plainPassword Plain text password
     * @return Hashed password
     */
    private static String hashPassword(String plainPassword) {
        return DigestUtils.sha256Hex(plainPassword);
    }
} 