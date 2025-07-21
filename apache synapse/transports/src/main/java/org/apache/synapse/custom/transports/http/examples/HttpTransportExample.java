package org.apache.synapse.custom.transports.http.examples;

import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.MediationSequence;
import org.apache.synapse.custom.mediation.Mediator;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.MessageProcessor;
import org.apache.synapse.custom.qos.QosManager;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportManager;
import org.apache.synapse.custom.transports.TransportSender;
import org.apache.synapse.custom.transports.http.HttpRequestRouter;
import org.apache.synapse.custom.transports.http.HttpTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of how to set up and use the HTTP/HTTPS transport in a real application.
 * This class demonstrates:
 * 1. Setting up HTTP and HTTPS listeners
 * 2. Configuring request routing
 * 3. Connecting the transport layer to the mediation engine
 * 4. Sending HTTP/HTTPS requests
 */
public class HttpTransportExample {
    private static final Logger logger = LoggerFactory.getLogger(HttpTransportExample.class);
    
    private TransportManager transportManager;
    private MediationEngine mediationEngine;
    
    /**
     * Initialize the example
     */
    public void init() throws Exception {
        // Create transport manager
        transportManager = new TransportManager();
        
        // Create message processor
        MessageProcessor messageProcessor = new MessageProcessor();
        
        // Create QoS manager
        QosManager qosManager = new QosManager();
        
        // Create mediation engine
        mediationEngine = new MediationEngine(transportManager, messageProcessor, qosManager);
        
        // Create and register mediation sequences
        setupMediationSequences();
        
        // Set up HTTP transport
        setupHttpTransport();
        
        // Set up HTTPS transport
        setupHttpsTransport();
        
        // Start the mediation engine (which starts the transports)
        mediationEngine.start();
        
        logger.info("HTTP transport example initialized");
    }
    
    /**
     * Set up mediation sequences
     */
    private void setupMediationSequences() {
        // Create a main sequence for HTTP requests
        MediationSequence mainSequence = new MediationSequence("main");
        mainSequence.addMediator(new LoggingMediator());
        mainSequence.addMediator(new EchoMediator());
        mediationEngine.registerSequence(mainSequence);
        
        // Create a secure sequence for HTTPS requests
        MediationSequence secureSequence = new MediationSequence("secure");
        secureSequence.addMediator(new LoggingMediator());
        secureSequence.addMediator(new SecureEchoMediator());
        mediationEngine.registerSequence(secureSequence);
        
        // Create API sequences
        MediationSequence apiSequence = new MediationSequence("api");
        apiSequence.addMediator(new LoggingMediator());
        apiSequence.addMediator(new ApiMediator());
        mediationEngine.registerSequence(apiSequence);
        
        logger.info("Mediation sequences set up");
    }
    
    /**
     * Set up HTTP transport
     */
    private void setupHttpTransport() throws Exception {
        // Create an HTTP transport listener
        TransportListener httpListener = HttpTransportFactory.createListener("http", "localhost", 8080);
        
        // Create a router to route HTTP requests to the mediation engine
        HttpRequestRouter router = new HttpRequestRouter(mediationEngine, "main");
        
        // Add routing rules
        router.addRoutingRule("/api/v1/.*", "api");
        router.addRoutingRule("/echo/.*", "main");
        
        // Set the router as the message callback
        httpListener.setMessageCallback(router);
        
        // Register the listener with the transport manager
        transportManager.registerListener("http", httpListener);
        
        // Create an HTTP transport sender
        TransportSender httpSender = HttpTransportFactory.createSender("http");
        
        // Register the sender with the transport manager
        transportManager.registerSender("http", httpSender);
        
        // Initialize the transport components
        httpListener.init();
        httpSender.init();
        
        logger.info("HTTP transport set up on port 8080");
    }
    
    /**
     * Set up HTTPS transport
     */
    private void setupHttpsTransport() throws Exception {
        // In a real application, you would provide actual keystore/truststore files
        String keystorePath = "/path/to/keystore.jks";
        String keystorePassword = "password";
        String keystoreType = "JKS";
        
        // Create an HTTPS transport listener with SSL/TLS support
        // Note: This is commented out since the keystore doesn't exist in this example
        // TransportListener httpsListener = HttpTransportFactory.createHttpsListener(
        //     "localhost", 8443, keystorePath, keystorePassword, keystoreType);
        
        // Create a router to route HTTPS requests to the mediation engine
        // HttpRequestRouter secureRouter = new HttpRequestRouter(mediationEngine, "secure");
        
        // Add routing rules
        // secureRouter.addRoutingRule("/secure/.*", "secure");
        // secureRouter.addRoutingRule("/api/v1/secure/.*", "api");
        
        // Set the router as the message callback
        // httpsListener.setMessageCallback(secureRouter);
        
        // Register the listener with the transport manager
        // transportManager.registerListener("https", httpsListener);
        
        // Create an HTTPS transport sender with SSL/TLS support
        String truststorePath = "/path/to/truststore.jks";
        String truststorePassword = "password";
        String truststoreType = "JKS";
        
        // Create an HTTPS transport sender
        // Note: This is commented out since the keystore doesn't exist in this example
        // TransportSender httpsSender = HttpTransportFactory.createHttpsSender(
        //     keystorePath, keystorePassword, keystoreType);
        
        // Register the sender with the transport manager
        // transportManager.registerSender("https", httpsSender);
        
        // Initialize the transport components
        // httpsListener.init();
        // httpsSender.init();
        
        logger.info("HTTPS transport setup would be on port 8443 (commented out in this example)");
    }
    
    /**
     * Send an HTTP request
     * 
     * @param endpoint The endpoint to send the request to
     * @param payload The payload to send
     * @return The response message
     */
    public Message sendHttpRequest(String endpoint, String payload) throws Exception {
        // Create a request message
        Message requestMessage = new Message();
        requestMessage.setDirection(Message.Direction.REQUEST);
        requestMessage.setContentType("text/plain");
        requestMessage.setPayload(payload.getBytes());
        requestMessage.setProperty("http.method", "POST");
        
        // Get the HTTP sender
        TransportSender httpSender = transportManager.getSender("http");
        
        // Send the request
        logger.info("Sending HTTP request to: {}", endpoint);
        Message responseMessage = httpSender.send(requestMessage, endpoint);
        
        return responseMessage;
    }
    
    /**
     * Stop the example
     */
    public void stop() {
        if (mediationEngine != null) {
            mediationEngine.stop();
        }
        
        logger.info("HTTP transport example stopped");
    }
    
    /**
     * Example mediator that logs messages
     */
    private static class LoggingMediator implements Mediator {
        private static final Logger logger = LoggerFactory.getLogger(LoggingMediator.class);
        
        @Override
        public Message mediate(Message message) {
            logger.info("Processing message: {}", message.getMessageId());
            return message;
        }
        
        @Override
        public String getName() {
            return "LoggingMediator";
        }
    }
    
    /**
     * Example mediator that echoes messages
     */
    private static class EchoMediator implements Mediator {
        @Override
        public Message mediate(Message message) {
            // For requests, create a response
            if (message.getDirection() == Message.Direction.REQUEST) {
                Message response = new Message();
                response.setDirection(Message.Direction.RESPONSE);
                response.setContentType(message.getContentType());
                response.setPayload(message.getPayload());
                response.setProperty("http.status.code", 200);
                return response;
            }
            
            return message;
        }
        
        @Override
        public String getName() {
            return "EchoMediator";
        }
    }
    
    /**
     * Example mediator for secure endpoints
     */
    private static class SecureEchoMediator implements Mediator {
        @Override
        public Message mediate(Message message) {
            // For requests, create a response
            if (message.getDirection() == Message.Direction.REQUEST) {
                Message response = new Message();
                response.setDirection(Message.Direction.RESPONSE);
                response.setContentType(message.getContentType());
                response.setPayload(("SECURE: " + new String(message.getPayload())).getBytes());
                response.setProperty("http.status.code", 200);
                return response;
            }
            
            return message;
        }
        
        @Override
        public String getName() {
            return "SecureEchoMediator";
        }
    }
    
    /**
     * Example mediator for API endpoints
     */
    private static class ApiMediator implements Mediator {
        @Override
        public Message mediate(Message message) {
            // For requests, create a response
            if (message.getDirection() == Message.Direction.REQUEST) {
                String uri = (String) message.getProperty("http.uri");
                
                Message response = new Message();
                response.setDirection(Message.Direction.RESPONSE);
                response.setContentType("application/json");
                response.setPayload(("{\"status\":\"success\",\"path\":\"" + uri + "\"}").getBytes());
                response.setProperty("http.status.code", 200);
                return response;
            }
            
            return message;
        }
        
        @Override
        public String getName() {
            return "ApiMediator";
        }
    }
    
    /**
     * Main method to run the example
     */
    public static void main(String[] args) {
        HttpTransportExample example = new HttpTransportExample();
        
        try {
            // Initialize the example
            example.init();
            
            // Keep the example running for a while
            logger.info("HTTP transport example running. Press Ctrl+C to stop.");
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            logger.error("Error running HTTP transport example", e);
        } finally {
            // Stop the example
            example.stop();
        }
    }
}