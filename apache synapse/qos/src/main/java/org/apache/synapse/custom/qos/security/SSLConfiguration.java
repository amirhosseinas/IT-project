package org.apache.synapse.custom.qos.security;

/**
 * Configuration for SSL/TLS settings.
 */
public class SSLConfiguration {
    
    private String keyStorePath;
    private String keyStorePassword;
    private String keyStoreType;
    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType;
    private String[] enabledProtocols;
    private String[] enabledCipherSuites;
    private boolean clientAuthRequired;
    
    /**
     * Create a new SSL configuration
     */
    public SSLConfiguration() {
        this.keyStoreType = "JKS";
        this.trustStoreType = "JKS";
        this.clientAuthRequired = false;
    }
    
    /**
     * Get the keystore path
     * 
     * @return Keystore path
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }
    
    /**
     * Set the keystore path
     * 
     * @param keyStorePath Keystore path
     */
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }
    
    /**
     * Get the keystore password
     * 
     * @return Keystore password
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }
    
    /**
     * Set the keystore password
     * 
     * @param keyStorePassword Keystore password
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }
    
    /**
     * Get the keystore type
     * 
     * @return Keystore type
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }
    
    /**
     * Set the keystore type
     * 
     * @param keyStoreType Keystore type
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }
    
    /**
     * Get the truststore path
     * 
     * @return Truststore path
     */
    public String getTrustStorePath() {
        return trustStorePath;
    }
    
    /**
     * Set the truststore path
     * 
     * @param trustStorePath Truststore path
     */
    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }
    
    /**
     * Get the truststore password
     * 
     * @return Truststore password
     */
    public String getTrustStorePassword() {
        return trustStorePassword;
    }
    
    /**
     * Set the truststore password
     * 
     * @param trustStorePassword Truststore password
     */
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
    
    /**
     * Get the truststore type
     * 
     * @return Truststore type
     */
    public String getTrustStoreType() {
        return trustStoreType;
    }
    
    /**
     * Set the truststore type
     * 
     * @param trustStoreType Truststore type
     */
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }
    
    /**
     * Get the enabled SSL/TLS protocols
     * 
     * @return Enabled protocols
     */
    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }
    
    /**
     * Set the enabled SSL/TLS protocols
     * 
     * @param enabledProtocols Enabled protocols
     */
    public void setEnabledProtocols(String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }
    
    /**
     * Get the enabled cipher suites
     * 
     * @return Enabled cipher suites
     */
    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }
    
    /**
     * Set the enabled cipher suites
     * 
     * @param enabledCipherSuites Enabled cipher suites
     */
    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }
    
    /**
     * Check if client authentication is required
     * 
     * @return true if client auth is required, false otherwise
     */
    public boolean isClientAuthRequired() {
        return clientAuthRequired;
    }
    
    /**
     * Set whether client authentication is required
     * 
     * @param clientAuthRequired true if client auth is required, false otherwise
     */
    public void setClientAuthRequired(boolean clientAuthRequired) {
        this.clientAuthRequired = clientAuthRequired;
    }
} 