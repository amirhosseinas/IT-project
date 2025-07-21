package org.apache.synapse.custom.qos.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages security aspects of the Apache Synapse ESB including WS-Security,
 * authentication, authorization, token-based security, and SSL/TLS configuration.
 */
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    private final Map<String, SecurityPolicy> securityPolicies;
    private final Map<String, UserCredential> userCredentials;
    private final Map<String, String> tokenUserMap;
    private Crypto crypto;
    private SSLConfiguration sslConfiguration;
    
    public SecurityManager() {
        this.securityPolicies = new HashMap<>();
        this.userCredentials = new ConcurrentHashMap<>();
        this.tokenUserMap = new ConcurrentHashMap<>();
        logger.info("Security Manager initialized");
    }
    
    /**
     * Initialize WS-Security crypto with properties
     * 
     * @param cryptoProperties Properties for crypto initialization
     * @throws SecurityException if crypto initialization fails
     */
    public void initializeCrypto(Properties cryptoProperties) throws SecurityException {
        try {
            this.crypto = CryptoFactory.getInstance(cryptoProperties);
            logger.info("WS-Security crypto initialized");
        } catch (WSSecurityException e) {
            logger.error("Failed to initialize WS-Security crypto", e);
            throw new SecurityException("Failed to initialize WS-Security crypto", e);
        }
    }
    
    /**
     * Configure SSL/TLS settings
     * 
     * @param sslConfiguration SSL configuration
     */
    public void configureSSL(SSLConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
        logger.info("SSL/TLS configuration updated");
    }
    
    /**
     * Register a security policy
     * 
     * @param serviceId Service identifier
     * @param policy Security policy to apply
     */
    public void registerSecurityPolicy(String serviceId, SecurityPolicy policy) {
        if (StringUtils.isBlank(serviceId)) {
            throw new IllegalArgumentException("Service ID cannot be null or empty");
        }
        securityPolicies.put(serviceId, policy);
        logger.info("Registered security policy for service: {}", serviceId);
    }
    
    /**
     * Add a user credential
     * 
     * @param username Username
     * @param credential User credential
     */
    public void addUserCredential(String username, UserCredential credential) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        userCredentials.put(username, credential);
        logger.info("Added user credential for: {}", username);
    }
    
    /**
     * Remove a user credential
     * 
     * @param username Username
     */
    public void removeUserCredential(String username) {
        if (userCredentials.remove(username) != null) {
            logger.info("Removed user credential for: {}", username);
        }
    }
    
    /**
     * Authenticate a user with username and password
     * 
     * @param username Username
     * @param password Password
     * @return true if authentication succeeds, false otherwise
     */
    public boolean authenticate(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return false;
        }
        
        UserCredential credential = userCredentials.get(username);
        if (credential == null) {
            return false;
        }
        
        boolean authenticated = credential.authenticate(password);
        if (authenticated) {
            logger.debug("User {} authenticated successfully", username);
        } else {
            logger.debug("Authentication failed for user {}", username);
        }
        
        return authenticated;
    }
    
    /**
     * Generate a security token for a user
     * 
     * @param username Username
     * @return Security token or null if user doesn't exist
     */
    public String generateToken(String username) {
        UserCredential credential = userCredentials.get(username);
        if (credential == null) {
            return null;
        }
        
        String token = TokenGenerator.generateToken();
        tokenUserMap.put(token, username);
        logger.debug("Generated token for user: {}", username);
        
        return token;
    }
    
    /**
     * Validate a security token
     * 
     * @param token Security token
     * @return Username associated with the token, or null if invalid
     */
    public String validateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        
        String username = tokenUserMap.get(token);
        if (username != null) {
            logger.debug("Token validated for user: {}", username);
        } else {
            logger.debug("Invalid token provided");
        }
        
        return username;
    }
    
    /**
     * Revoke a security token
     * 
     * @param token Security token
     */
    public void revokeToken(String token) {
        if (tokenUserMap.remove(token) != null) {
            logger.debug("Token revoked");
        }
    }
    
    /**
     * Check if a user is authorized for a specific action on a service
     * 
     * @param username Username
     * @param serviceId Service identifier
     * @param action Action to perform
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorized(String username, String serviceId, String action) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(serviceId) || StringUtils.isBlank(action)) {
            return false;
        }
        
        SecurityPolicy policy = securityPolicies.get(serviceId);
        if (policy == null) {
            // No policy means no restrictions
            return true;
        }
        
        UserCredential credential = userCredentials.get(username);
        if (credential == null) {
            return false;
        }
        
        boolean authorized = policy.isAuthorized(credential.getRoles(), action);
        if (authorized) {
            logger.debug("User {} authorized for action {} on service {}", username, action, serviceId);
        } else {
            logger.debug("User {} not authorized for action {} on service {}", username, action, serviceId);
        }
        
        return authorized;
    }
    
    /**
     * Get the WS-Security crypto instance
     * 
     * @return Crypto instance
     */
    public Crypto getCrypto() {
        return crypto;
    }
    
    /**
     * Get the SSL configuration
     * 
     * @return SSL configuration
     */
    public SSLConfiguration getSSLConfiguration() {
        return sslConfiguration;
    }
} 