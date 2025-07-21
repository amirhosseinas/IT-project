package org.apache.synapse.custom.transports.fix;

import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;

import java.io.FileNotFoundException;

/**
 * FIX transport listener that receives incoming FIX messages.
 */
public class FixTransportListener implements TransportListener {
    private static final Logger logger = LoggerFactory.getLogger(FixTransportListener.class);
    
    private String configFile;
    private FixSessionManager sessionManager;
    private FixApplication application;
    private MessageCallback messageCallback;
    private boolean running = false;
    
    /**
     * Set the QuickFIX/J configuration file path
     * 
     * @param configFile Path to the configuration file
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }
    
    @Override
    public void init() throws TransportException {
        try {
            logger.info("Initializing FIX transport listener with config file: {}", configFile);
            
            // Create the FIX application
            application = new FixApplication(messageCallback);
            
            // Create and initialize the session manager
            sessionManager = new FixSessionManager();
            sessionManager.initialize(configFile, application);
            
            logger.info("FIX transport listener initialized");
        } catch (ConfigError | FileNotFoundException e) {
            logger.error("Error initializing FIX transport listener", e);
            throw new TransportException("Error initializing FIX transport listener", e);
        }
    }
    
    @Override
    public void start() throws TransportException {
        try {
            logger.info("Starting FIX transport listener");
            
            // Start the session manager
            sessionManager.start();
            running = true;
            
            logger.info("FIX transport listener started");
        } catch (ConfigError e) {
            logger.error("Error starting FIX transport listener", e);
            throw new TransportException("Error starting FIX transport listener", e);
        }
    }
    
    @Override
    public void stop() throws TransportException {
        logger.info("Stopping FIX transport listener");
        
        if (sessionManager != null) {
            sessionManager.stop();
        }
        
        running = false;
        logger.info("FIX transport listener stopped");
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
        
        // If application already exists, update its callback
        if (application != null) {
            application = new FixApplication(callback);
            
            // Reinitialize if session manager exists
            if (sessionManager != null && configFile != null) {
                try {
                    sessionManager.initialize(configFile, application);
                } catch (Exception e) {
                    logger.error("Error updating FIX application callback", e);
                }
            }
        }
    }
    
    /**
     * Get the FIX session manager
     * 
     * @return The session manager
     */
    public FixSessionManager getSessionManager() {
        return sessionManager;
    }
} 