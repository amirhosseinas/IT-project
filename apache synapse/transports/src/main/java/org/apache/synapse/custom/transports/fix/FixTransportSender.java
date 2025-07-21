package org.apache.synapse.custom.transports.fix;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.io.FileNotFoundException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * FIX transport sender that sends outgoing FIX messages.
 */
public class FixTransportSender implements TransportSender {
    private static final Logger logger = LoggerFactory.getLogger(FixTransportSender.class);
    
    private static final String FIX_ENDPOINT_PREFIX = "fix://";
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    private String configFile;
    private FixSessionManager sessionManager;
    private FixApplication application;
    private final ConcurrentHashMap<String, ResponseHandler> responseHandlers = new ConcurrentHashMap<>();
    
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
            logger.info("Initializing FIX transport sender with config file: {}", configFile);
            
            // Create the FIX application with a response handler
            application = new FixApplication(this::handleResponse);
            
            // Create and initialize the session manager
            sessionManager = new FixSessionManager();
            sessionManager.initialize(configFile, application);
            
            // Start the session manager
            sessionManager.start();
            
            logger.info("FIX transport sender initialized");
        } catch (ConfigError | FileNotFoundException e) {
            logger.error("Error initializing FIX transport sender", e);
            throw new TransportException("Error initializing FIX transport sender", e);
        }
    }
    
    @Override
    public Message send(Message message, String endpoint) throws TransportException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint cannot be null");
        }
        
        try {
            // Extract session ID from endpoint
            String sessionIdStr = extractSessionId(endpoint);
            if (sessionIdStr == null) {
                throw new TransportException("Invalid FIX endpoint format: " + endpoint);
            }
            
            // Get the session
            Session session = sessionManager.getSession(sessionIdStr);
            if (session == null) {
                throw new TransportException("FIX session not found: " + sessionIdStr);
            }
            
            // Check if session is logged on
            if (!session.isLoggedOn()) {
                throw new TransportException("FIX session not logged on: " + sessionIdStr);
            }
            
            // Convert Synapse message to FIX message
            quickfix.Message fixMessage = FixMessageConverter.toFixMessage(message);
            
            // Validate the message
            FixMessageValidator.ValidationResult validationResult = FixMessageValidator.validate(fixMessage);
            if (!validationResult.isValid()) {
                throw new TransportException("Invalid FIX message: " + validationResult.getMessage());
            }
            
            // Create a response handler if we expect a response
            String correlationId = UUID.randomUUID().toString();
            CountDownLatch latch = new CountDownLatch(1);
            ResponseHandler handler = new ResponseHandler(latch);
            responseHandlers.put(correlationId, handler);
            
            // Store correlation ID in message properties
            message.setProperty("fix.correlation.id", correlationId);
            
            // Send the message
            boolean sent = session.send(fixMessage);
            if (!sent) {
                responseHandlers.remove(correlationId);
                throw new TransportException("Failed to send FIX message");
            }
            
            // Wait for response if synchronous
            if (message.getProperty("fix.async") == null) {
                if (latch.await(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    Message response = handler.getResponse();
                    responseHandlers.remove(correlationId);
                    return response;
                } else {
                    responseHandlers.remove(correlationId);
                    throw new TransportException("Timeout waiting for FIX response");
                }
            }
            
            return null;
        } catch (Exception e) {
            if (!(e instanceof TransportException)) {
                logger.error("Error sending FIX message", e);
                throw new TransportException("Error sending FIX message", e);
            } else {
                throw (TransportException) e;
            }
        }
    }
    
    @Override
    public boolean canHandle(String endpoint) {
        return endpoint != null && endpoint.startsWith(FIX_ENDPOINT_PREFIX);
    }
    
    @Override
    public void close() {
        if (sessionManager != null) {
            sessionManager.stop();
        }
        
        responseHandlers.clear();
        logger.info("FIX transport sender closed");
    }
    
    /**
     * Extract session ID from endpoint URL
     * 
     * @param endpoint The endpoint URL (format: fix://sessionID)
     * @return The session ID or null if invalid format
     */
    private String extractSessionId(String endpoint) {
        if (endpoint == null || !endpoint.startsWith(FIX_ENDPOINT_PREFIX)) {
            return null;
        }
        
        return endpoint.substring(FIX_ENDPOINT_PREFIX.length());
    }
    
    /**
     * Handle a response message
     * 
     * @param response The response message
     * @return null (no further response needed)
     */
    private Message handleResponse(Message response) {
        try {
            // Extract correlation ID from message properties
            String correlationId = (String) response.getProperty("fix.correlation.id");
            if (correlationId != null) {
                ResponseHandler handler = responseHandlers.get(correlationId);
                if (handler != null) {
                    handler.setResponse(response);
                    handler.getLatch().countDown();
                }
            }
        } catch (Exception e) {
            logger.error("Error handling FIX response", e);
        }
        
        return null;
    }
    
    /**
     * Handler for asynchronous responses
     */
    private static class ResponseHandler {
        private final CountDownLatch latch;
        private Message response;
        
        public ResponseHandler(CountDownLatch latch) {
            this.latch = latch;
        }
        
        public CountDownLatch getLatch() {
            return latch;
        }
        
        public Message getResponse() {
            return response;
        }
        
        public void setResponse(Message response) {
            this.response = response;
        }
    }
} 