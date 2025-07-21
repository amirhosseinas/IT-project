package org.apache.synapse.custom.transports.http;

import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.MessageProcessor;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportSender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test class for HTTP transport components.
 * This demonstrates how to set up and use the HTTP transport.
 */
public class HttpTransportTest {
    private static final Logger logger = LoggerFactory.getLogger(HttpTransportTest.class);
    
    private static TransportListener httpListener;
    private static TransportSender httpSender;
    private static HttpRequestRouter router;
    
    @BeforeAll
    public static void setUp() throws Exception {
        // Create HTTP transport components
        httpListener = HttpTransportFactory.createListener("http", "localhost", 8080);
        httpSender = HttpTransportFactory.createSender("http");
        
        // Initialize components
        httpListener.init();
        httpSender.init();
        
        // Create a simple mediation engine (mock for testing)
        MediationEngine mediationEngine = new MockMediationEngine();
        
        // Create router and set it as the message callback
        router = new HttpRequestRouter(mediationEngine, "default");
        router.addRoutingRule("/api/.*", "api");
        router.addRoutingRule("/services/.*", "services");
        httpListener.setMessageCallback(router);
        
        // Start the listener
        httpListener.start();
        
        logger.info("HTTP transport test setup complete");
    }
    
    @AfterAll
    public static void tearDown() throws Exception {
        // Stop and clean up
        if (httpListener != null && httpListener.isRunning()) {
            httpListener.stop();
        }
        
        if (httpSender != null) {
            ((HttpTransportSender) httpSender).close();
        }
        
        logger.info("HTTP transport test teardown complete");
    }
    
    @Test
    public void testHttpRequest() throws Exception {
        // This is a simple test to demonstrate how to use the HTTP transport
        // In a real test, you would send an actual HTTP request and verify the response
        
        logger.info("HTTP transport is running: {}", httpListener.isRunning());
        
        // In a real test, you would do something like:
        // Message requestMessage = new Message();
        // requestMessage.setPayload("test payload".getBytes());
        // Message responseMessage = httpSender.send(requestMessage, "http://localhost:8080/api/test");
        // Assert.assertNotNull(responseMessage);
        
        // For this example, we'll just wait a bit to keep the server running
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(1, TimeUnit.SECONDS);
    }
    
    /**
     * Mock mediation engine for testing
     */
    private static class MockMediationEngine extends MediationEngine {
        public MockMediationEngine() {
            super(null, null, null);
        }
        
        @Override
        public Message mediate(Message message, String sequenceName) {
            // Create a simple response
            Message response = new Message();
            response.setDirection(Message.Direction.RESPONSE);
            response.setContentType("text/plain");
            response.setPayload(("Response from sequence: " + sequenceName).getBytes());
            response.setProperty("http.status.code", 200);
            
            return response;
        }
    }
} 
 