package org.apache.synapse.custom.transports.jms;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JMS transport.
 * Note: These tests require an active JMS broker.
 */
@Disabled("Requires an active JMS broker")
public class JmsTransportTest {
    
    private TransportManager transportManager;
    private JmsTransportFactory transportFactory;
    private Properties properties;
    
    @BeforeEach
    public void setUp() {
        transportManager = new TransportManager();
        transportFactory = new JmsTransportFactory(transportManager);
        
        // Configure JMS properties for ActiveMQ
        properties = new Properties();
        properties.setProperty("jms.initialContextFactory", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty("jms.providerUrl", "tcp://localhost:61616");
        properties.setProperty("jms.connectionFactoryName", "ConnectionFactory");
        properties.setProperty("jms.username", "admin");
        properties.setProperty("jms.password", "admin");
        properties.setProperty("jms.queue.name", "test.queue");
        properties.setProperty("jms.topic.name", "test.topic");
    }
    
    @Test
    public void testJmsQueueSendReceive() throws Exception {
        // Create and register JMS transport
        transportFactory.createSender("jms");
        transportFactory.createListener("jms-queue", properties);
        
        // Initialize and start the transport
        transportManager.initializeListeners();
        transportManager.startListeners();
        
        // Create a latch to wait for the message
        CountDownLatch latch = new CountDownLatch(1);
        final Message[] receivedMessage = new Message[1];
        
        // Set message callback
        TransportListener listener = transportManager.getListener("jms-queue");
        listener.setMessageCallback(message -> {
            receivedMessage[0] = message;
            latch.countDown();
            return null;
        });
        
        // Create and send a test message
        Message message = new Message(UUID.randomUUID().toString());
        message.setPayload("Test message".getBytes());
        message.setContentType("text/plain");
        
        // Send the message
        String endpoint = "queue:test.queue?initialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory&providerUrl=tcp://localhost:61616";
        transportManager.getSender("jms").send(message, endpoint);
        
        // Wait for the message to be received
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Message not received within timeout");
        
        // Verify the message
        assertNotNull(receivedMessage[0], "Received message should not be null");
        assertEquals(new String(message.getPayload()), new String(receivedMessage[0].getPayload()), 
                "Message payload should match");
        
        // Stop the transport
        transportManager.stopListeners();
    }
    
    @Test
    public void testJmsTopicPublishSubscribe() throws Exception {
        // Create and register JMS transport
        transportFactory.createSender("jms");
        transportFactory.createListener("jms-topic", properties);
        
        // Initialize and start the transport
        transportManager.initializeListeners();
        transportManager.startListeners();
        
        // Create a latch to wait for the message
        CountDownLatch latch = new CountDownLatch(1);
        final Message[] receivedMessage = new Message[1];
        
        // Set message callback
        TransportListener listener = transportManager.getListener("jms-topic");
        listener.setMessageCallback(message -> {
            receivedMessage[0] = message;
            latch.countDown();
            return null;
        });
        
        // Create and send a test message
        Message message = new Message(UUID.randomUUID().toString());
        message.setPayload("Test topic message".getBytes());
        message.setContentType("text/plain");
        
        // Send the message
        String endpoint = "topic:test.topic?initialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory&providerUrl=tcp://localhost:61616";
        transportManager.getSender("jms").send(message, endpoint);
        
        // Wait for the message to be received
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Message not received within timeout");
        
        // Verify the message
        assertNotNull(receivedMessage[0], "Received message should not be null");
        assertEquals(new String(message.getPayload()), new String(receivedMessage[0].getPayload()), 
                "Message payload should match");
        
        // Stop the transport
        transportManager.stopListeners();
    }
    
    @Test
    public void testJmsRequestResponse() throws Exception {
        // Create and register JMS transport
        transportFactory.createSender("jms");
        transportFactory.createListener("jms-queue", properties);
        
        // Initialize and start the transport
        transportManager.initializeListeners();
        transportManager.startListeners();
        
        // Set message callback that returns a response
        TransportListener listener = transportManager.getListener("jms-queue");
        listener.setMessageCallback(message -> {
            // Create a response message
            Message response = new Message(UUID.randomUUID().toString());
            response.setPayload(("Response to: " + new String(message.getPayload())).getBytes());
            response.setContentType("text/plain");
            response.setDirection(Message.Direction.RESPONSE);
            return response;
        });
        
        // Create and send a test message
        Message message = new Message(UUID.randomUUID().toString());
        message.setPayload("Request message".getBytes());
        message.setContentType("text/plain");
        message.setProperty("JMS_WAIT_FOR_RESPONSE", true);
        message.setProperty("JMS_RESPONSE_TIMEOUT", 5000L);
        
        // Send the message and get the response
        String endpoint = "queue:test.queue?initialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory&providerUrl=tcp://localhost:61616";
        Message response = transportManager.getSender("jms").send(message, endpoint);
        
        // Verify the response
        assertNotNull(response, "Response should not be null");
        assertEquals("Response to: Request message", new String(response.getPayload()), 
                "Response payload should match");
        assertEquals(Message.Direction.RESPONSE, response.getDirection(), 
                "Response direction should be RESPONSE");
        
        // Stop the transport
        transportManager.stopListeners();
    }
} 