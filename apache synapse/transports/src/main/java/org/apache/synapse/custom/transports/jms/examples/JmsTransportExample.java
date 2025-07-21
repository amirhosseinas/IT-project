package org.apache.synapse.custom.transports.jms.examples;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportManager;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.jms.JmsTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

/**
 * Example demonstrating how to use the JMS transport.
 */
public class JmsTransportExample {
    private static final Logger logger = LoggerFactory.getLogger(JmsTransportExample.class);
    
    public static void main(String[] args) {
        try {
            // Create transport manager
            TransportManager transportManager = new TransportManager();
            
            // Create JMS transport factory
            JmsTransportFactory transportFactory = new JmsTransportFactory(transportManager);
            
            // Configure JMS properties for ActiveMQ
            Properties properties = new Properties();
            properties.setProperty("jms.initialContextFactory", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            properties.setProperty("jms.providerUrl", "tcp://localhost:61616");
            properties.setProperty("jms.connectionFactoryName", "ConnectionFactory");
            properties.setProperty("jms.username", "admin");
            properties.setProperty("jms.password", "admin");
            properties.setProperty("jms.queue.name", "example.queue");
            properties.setProperty("jms.topic.name", "example.topic");
            
            // Create JMS transports
            transportFactory.createDefaultTransports(properties);
            
            // Initialize and start listeners
            transportManager.initializeListeners();
            transportManager.startListeners();
            
            // Set up message callbacks
            setupQueueListener(transportManager);
            setupTopicListener(transportManager);
            
            // Example menu
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                System.out.println("\nJMS Transport Example");
                System.out.println("1. Send message to queue");
                System.out.println("2. Send message to topic");
                System.out.println("3. Send request and wait for response");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");
                
                String option = scanner.nextLine();
                
                switch (option) {
                    case "1":
                        sendQueueMessage(transportManager);
                        break;
                    case "2":
                        sendTopicMessage(transportManager);
                        break;
                    case "3":
                        sendRequestResponse(transportManager);
                        break;
                    case "4":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option");
                }
            }
            
            // Stop listeners
            transportManager.stopListeners();
            
            System.out.println("Example completed");
            
        } catch (Exception e) {
            logger.error("Error in JMS transport example", e);
        }
    }
    
    private static void setupQueueListener(TransportManager transportManager) {
        TransportListener queueListener = transportManager.getListener("jms-queue");
        
        if (queueListener != null) {
            queueListener.setMessageCallback(message -> {
                logger.info("Received queue message: {}", new String(message.getPayload()));
                
                // If this is a request-response pattern, return a response
                if (message.getProperty("JMS_REPLY_TO") != null) {
                    Message response = new Message(UUID.randomUUID().toString());
                    response.setPayload(("Response to: " + new String(message.getPayload())).getBytes());
                    response.setContentType("text/plain");
                    response.setDirection(Message.Direction.RESPONSE);
                    return response;
                }
                
                return null;
            });
        }
    }
    
    private static void setupTopicListener(TransportManager transportManager) {
        TransportListener topicListener = transportManager.getListener("jms-topic");
        
        if (topicListener != null) {
            topicListener.setMessageCallback(message -> {
                logger.info("Received topic message: {}", new String(message.getPayload()));
                return null;
            });
        }
    }
    
    private static void sendQueueMessage(TransportManager transportManager) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter message to send to queue: ");
            String messageText = scanner.nextLine();
            
            Message message = new Message(UUID.randomUUID().toString());
            message.setPayload(messageText.getBytes());
            message.setContentType("text/plain");
            
            String endpoint = "queue:example.queue";
            transportManager.getSender("jms").send(message, endpoint);
            
            logger.info("Sent message to queue");
            
        } catch (Exception e) {
            logger.error("Error sending queue message", e);
        }
    }
    
    private static void sendTopicMessage(TransportManager transportManager) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter message to publish to topic: ");
            String messageText = scanner.nextLine();
            
            Message message = new Message(UUID.randomUUID().toString());
            message.setPayload(messageText.getBytes());
            message.setContentType("text/plain");
            
            String endpoint = "topic:example.topic";
            transportManager.getSender("jms").send(message, endpoint);
            
            logger.info("Published message to topic");
            
        } catch (Exception e) {
            logger.error("Error publishing topic message", e);
        }
    }
    
    private static void sendRequestResponse(TransportManager transportManager) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter request message: ");
            String messageText = scanner.nextLine();
            
            Message message = new Message(UUID.randomUUID().toString());
            message.setPayload(messageText.getBytes());
            message.setContentType("text/plain");
            message.setProperty("JMS_WAIT_FOR_RESPONSE", true);
            message.setProperty("JMS_RESPONSE_TIMEOUT", 10000L);
            
            String endpoint = "queue:example.queue";
            Message response = transportManager.getSender("jms").send(message, endpoint);
            
            if (response != null) {
                logger.info("Received response: {}", new String(response.getPayload()));
            } else {
                logger.info("No response received within timeout");
            }
            
        } catch (Exception e) {
            logger.error("Error in request-response", e);
        }
    }
} 