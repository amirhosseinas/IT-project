package org.apache.synapse.custom.transports.jms;

import java.util.Properties;

/**
 * Configuration for JMS transport.
 */
public class JmsTransportConfiguration {
    // Connection properties
    private String initialContextFactory;
    private String providerUrl;
    private String connectionFactoryName;
    private String username;
    private String password;
    
    // Destination properties
    private String destinationName;
    private JmsDestinationType destinationType;
    
    // Session properties
    private boolean transacted = false;
    private int acknowledgeMode = javax.jms.Session.AUTO_ACKNOWLEDGE;
    
    // Connection pooling properties
    private int connectionPoolSize = 10;
    private int connectionIdleTimeout = 300000; // 5 minutes
    
    // Retry properties
    private int maxRetries = 3;
    private long retryInterval = 5000; // 5 seconds
    
    // Message properties
    private String messageSelector;
    private long receiveTimeout = 1000; // 1 second
    
    /**
     * Create a new JMS transport configuration
     */
    public JmsTransportConfiguration() {
    }
    
    /**
     * Create a new JMS transport configuration from properties
     * 
     * @param properties The properties to use
     */
    public JmsTransportConfiguration(Properties properties) {
        this.initialContextFactory = properties.getProperty("jms.initialContextFactory");
        this.providerUrl = properties.getProperty("jms.providerUrl");
        this.connectionFactoryName = properties.getProperty("jms.connectionFactoryName", "ConnectionFactory");
        this.username = properties.getProperty("jms.username");
        this.password = properties.getProperty("jms.password");
        this.destinationName = properties.getProperty("jms.destinationName");
        this.destinationType = JmsDestinationType.fromString(properties.getProperty("jms.destinationType"));
        
        String transactedStr = properties.getProperty("jms.transacted");
        if (transactedStr != null) {
            this.transacted = Boolean.parseBoolean(transactedStr);
        }
        
        String ackModeStr = properties.getProperty("jms.acknowledgeMode");
        if (ackModeStr != null) {
            this.acknowledgeMode = Integer.parseInt(ackModeStr);
        }
        
        String poolSizeStr = properties.getProperty("jms.connectionPoolSize");
        if (poolSizeStr != null) {
            this.connectionPoolSize = Integer.parseInt(poolSizeStr);
        }
        
        String idleTimeoutStr = properties.getProperty("jms.connectionIdleTimeout");
        if (idleTimeoutStr != null) {
            this.connectionIdleTimeout = Integer.parseInt(idleTimeoutStr);
        }
        
        String maxRetriesStr = properties.getProperty("jms.maxRetries");
        if (maxRetriesStr != null) {
            this.maxRetries = Integer.parseInt(maxRetriesStr);
        }
        
        String retryIntervalStr = properties.getProperty("jms.retryInterval");
        if (retryIntervalStr != null) {
            this.retryInterval = Long.parseLong(retryIntervalStr);
        }
        
        this.messageSelector = properties.getProperty("jms.messageSelector");
        
        String receiveTimeoutStr = properties.getProperty("jms.receiveTimeout");
        if (receiveTimeoutStr != null) {
            this.receiveTimeout = Long.parseLong(receiveTimeoutStr);
        }
    }
    
    /**
     * Convert this configuration to properties
     * 
     * @return Properties representing this configuration
     */
    public Properties toProperties() {
        Properties properties = new Properties();
        
        if (initialContextFactory != null) {
            properties.setProperty("jms.initialContextFactory", initialContextFactory);
        }
        
        if (providerUrl != null) {
            properties.setProperty("jms.providerUrl", providerUrl);
        }
        
        properties.setProperty("jms.connectionFactoryName", connectionFactoryName);
        
        if (username != null) {
            properties.setProperty("jms.username", username);
        }
        
        if (password != null) {
            properties.setProperty("jms.password", password);
        }
        
        if (destinationName != null) {
            properties.setProperty("jms.destinationName", destinationName);
        }
        
        properties.setProperty("jms.destinationType", destinationType.toString());
        properties.setProperty("jms.transacted", String.valueOf(transacted));
        properties.setProperty("jms.acknowledgeMode", String.valueOf(acknowledgeMode));
        properties.setProperty("jms.connectionPoolSize", String.valueOf(connectionPoolSize));
        properties.setProperty("jms.connectionIdleTimeout", String.valueOf(connectionIdleTimeout));
        properties.setProperty("jms.maxRetries", String.valueOf(maxRetries));
        properties.setProperty("jms.retryInterval", String.valueOf(retryInterval));
        
        if (messageSelector != null) {
            properties.setProperty("jms.messageSelector", messageSelector);
        }
        
        properties.setProperty("jms.receiveTimeout", String.valueOf(receiveTimeout));
        
        return properties;
    }
    
    // Getters and setters
    
    public String getInitialContextFactory() {
        return initialContextFactory;
    }
    
    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }
    
    public String getProviderUrl() {
        return providerUrl;
    }
    
    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }
    
    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }
    
    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDestinationName() {
        return destinationName;
    }
    
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
    
    public JmsDestinationType getDestinationType() {
        return destinationType;
    }
    
    public void setDestinationType(JmsDestinationType destinationType) {
        this.destinationType = destinationType;
    }
    
    public boolean isTransacted() {
        return transacted;
    }
    
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }
    
    public int getAcknowledgeMode() {
        return acknowledgeMode;
    }
    
    public void setAcknowledgeMode(int acknowledgeMode) {
        this.acknowledgeMode = acknowledgeMode;
    }
    
    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }
    
    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }
    
    public int getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }
    
    public void setConnectionIdleTimeout(int connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public long getRetryInterval() {
        return retryInterval;
    }
    
    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }
    
    public String getMessageSelector() {
        return messageSelector;
    }
    
    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }
    
    public long getReceiveTimeout() {
        return receiveTimeout;
    }
    
    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }
} 