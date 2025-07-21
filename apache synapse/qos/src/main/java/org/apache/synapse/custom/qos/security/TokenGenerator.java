package org.apache.synapse.custom.qos.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for generating security tokens.
 */
public class TokenGenerator {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private TokenGenerator() {
        // Utility class, no instantiation
    }
    
    /**
     * Generate a random security token
     * 
     * @return A random security token
     */
    public static String generateToken() {
        // Generate a random UUID
        UUID uuid = UUID.randomUUID();
        
        // Add timestamp for uniqueness
        long timestamp = Instant.now().toEpochMilli();
        
        // Generate 8 random bytes
        byte[] randomBytes = new byte[8];
        SECURE_RANDOM.nextBytes(randomBytes);
        
        // Combine and encode
        String combined = uuid.toString() + "-" + timestamp + "-" + Base64.getEncoder().encodeToString(randomBytes);
        
        return combined;
    }
    
    /**
     * Generate a token with a specific expiration time
     * 
     * @param expirationSeconds Expiration time in seconds
     * @return A token with expiration information
     */
    public static String generateExpiringToken(int expirationSeconds) {
        // Generate base token
        String baseToken = generateToken();
        
        // Calculate expiration time
        long expirationTime = Instant.now().plusSeconds(expirationSeconds).toEpochMilli();
        
        // Combine token with expiration
        return baseToken + "-exp" + expirationTime;
    }
    
    /**
     * Extract the expiration time from an expiring token
     * 
     * @param token The token
     * @return Expiration time in milliseconds since epoch, or -1 if not an expiring token
     */
    public static long extractExpirationTime(String token) {
        if (token == null || !token.contains("-exp")) {
            return -1;
        }
        
        try {
            String expPart = token.substring(token.lastIndexOf("-exp") + 4);
            return Long.parseLong(expPart);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Check if a token has expired
     * 
     * @param token The token to check
     * @return true if the token has expired, false otherwise
     */
    public static boolean isTokenExpired(String token) {
        long expirationTime = extractExpirationTime(token);
        if (expirationTime == -1) {
            // Not an expiring token
            return false;
        }
        
        return Instant.now().toEpochMilli() > expirationTime;
    }
} 