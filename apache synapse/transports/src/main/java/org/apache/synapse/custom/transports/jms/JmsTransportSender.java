package org.apache.synapse.custom.transports.jms;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * JMS transport sender implementation.
 * Sends messages to JMS destinations.
 */
public class JmsTransportSender implements TransportSender {
    private static final Logger logger = LoggerFactory.getLogger(JmsTransportSender.class);
    
    private JmsConnectionFactory connectionFactory;
    private JmsMessageConverter messageConverter;
    private boolean initialized = false;
    
    /**
     * Create a new JMS transport sender
     */
    public JmsTransportSender() {
        this.messageConverter = new JmsMessageConverter();
    }
    
    @Override
    public void init() throws TransportException {
        if (initialized) {
            return;
        }
        
        // We'll initialize the connection factory when sending a message
        // since we need the endpoint information
        
        initialized = true;
        logger.info("JMS transport sender initialized");
    }
    
    @Override
    public Message send(Message message, String endpoint) throws TransportException {
        if (!initialized) {
            init();
        }
        
        // Parse the endpoint URI
        JmsEndpointParser endpointParser = new JmsEndpointParser();
        JmsTransportConfiguration config = endpointParser.parse(endpoint);
        
        if (config == null) {
            throw new TransportException("Invalid JMS endpoint: " + endpoint);
        }
        
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        Message response = null;
        
        try {
            // Initialize connection factory if needed
            if (connectionFactory == null) {
                initConnectionFactory(config);
            }
            
            // Get a connection from the pool
            connection = connectionFactory.getConnection();
            connection.start();
            
            // Create a session
            session = connectionFactory.createSession(
                    connection, 
                    config.isTransacted(), 
                    config.getAcknowledgeMode());
            
            // Create the destination
            Destination destination;
            if (config.getDestinationType() == JmsDestinationType.TOPIC) {
                destination = session.createTopic(config.getDestinationName());
            } else {
                destination = session.createQueue(config.getDestinationName());
            }
            
            // Create a producer
            producer = session.createProducer(destination);
            
            // Set producer properties
            producer.setDeliveryMode(
                    message.getProperty("JMS_DELIVERY_MODE") != null ? 
                    (Integer) message.getProperty("JMS_DELIVERY_MODE") : 
                    DeliveryMode.PERSISTENT);
            
            producer.setPriority(
                    message.getProperty("JMS_PRIORITY") != null ? 
                    (Integer) message.getProperty("JMS_PRIORITY") : 
                    Message.DEFAULT_PRIORITY);
            
            producer.setTimeToLive(
                    message.getProperty("JMS_TTL") != null ? 
                    (Long) message.getProperty("JMS_TTL") : 
                    Message.DEFAULT_TIME_TO_LIVE);
            
            // Convert Synapse message to JMS message
            javax.jms.Message jmsMessage = messageConverter.toJmsMessage(message, session);
            
            // Create temporary queue for response if needed
            TemporaryQueue replyQueue = null;
            MessageConsumer replyConsumer = null;
            
            boolean waitForResponse = Boolean.TRUE.equals(message.getProperty("JMS_WAIT_FOR_RESPONSE"));
            long timeout = message.getProperty("JMS_RESPONSE_TIMEOUT") != null ? 
                    (Long) message.getProperty("JMS_RESPONSE_TIMEOUT") : 
                    config.getReceiveTimeout();
            
            if (waitForResponse) {
                replyQueue = session.createTemporaryQueue();
                replyConsumer = session.createConsumer(replyQueue);
                jmsMessage.setJMSReplyTo(replyQueue);
            }
            
            // Send the message
            producer.send(jmsMessage);
            
            if (config.isTransacted()) {
                session.commit();
            }
            
            logger.debug("Sent JMS message to destination: {}", config.getDestinationName());
            
            // Wait for response if needed
            if (waitForResponse && replyConsumer != null) {
                javax.jms.Message replyMessage = replyConsumer.receive(timeout);
                
                if (replyMessage != null) {
                    response = messageConverter.toSynapseMessage(replyMessage);
                    response.setDirection(Message.Direction.RESPONSE);
                }
            }
            
            return response;
            
        } catch (JMSException e) {
            throw new TransportException("Failed to send JMS message: " + e.getMessage(), e);
        } finally {
            try {
                if (producer != null) {
                    producer.close();
                }
                
                if (session != null) {
                    session.close();
                }
                
                if (connection != null) {
                    connectionFactory.returnConnection(connection);
                }
            } catch (JMSException e) {
                logger.warn("Error closing JMS resources", e);
            }
        }
    }
    
    @Override
    public boolean canHandle(String endpoint) {
        return endpoint != null && 
               (endpoint.startsWith("jms:") || 
                endpoint.startsWith("queue:") || 
                endpoint.startsWith("topic:"));
    }
    
    @Override
    public void close() {
        if (connectionFactory != null) {
            connectionFactory.close();
        }
        
        logger.info("JMS transport sender closed");
    }
    
    /**
     * Initialize the connection factory
     * 
     * @param config The JMS transport configuration
     * @throws TransportException if initialization fails
     */
    private void initConnectionFactory(JmsTransportConfiguration config) throws TransportException {
        try {
            // Create JNDI context
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory());
            env.put(Context.PROVIDER_URL, config.getProviderUrl());
            
            if (config.getUsername() != null && config.getPassword() != null) {
                env.put(Context.SECURITY_PRINCIPAL, config.getUsername());
                env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
            }
            
            Context context = new InitialContext(env);
            
            // Look up connection factory
            javax.jms.ConnectionFactory jmsConnectionFactory = 
                    (javax.jms.ConnectionFactory) context.lookup(config.getConnectionFactoryName());
            
            // Create our connection factory wrapper
            this.connectionFactory = new JmsConnectionFactory(jmsConnectionFactory, config.toProperties());
            
        } catch (NamingException e) {
            throw new TransportException("Failed to initialize JMS connection factory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper class to parse JMS endpoints.
     */
    private static class JmsEndpointParser {
        /**
         * Parse a JMS endpoint URI
         * 
         * @param endpoint The endpoint URI to parse
         * @return The JMS transport configuration
         */
        public JmsTransportConfiguration parse(String endpoint) {
            if (endpoint == null) {
                return null;
            }
            
            JmsTransportConfiguration config = new JmsTransportConfiguration();
            Properties properties = new Properties();
            
            // Parse the URI
            String uri = endpoint.trim();
            
            // Determine protocol and destination type
            if (uri.startsWith("jms:")) {
                uri = uri.substring(4);
            } else if (uri.startsWith("queue:")) {
                uri = uri.substring(6);
                config.setDestinationType(JmsDestinationType.QUEUE);
            } else if (uri.startsWith("topic:")) {
                uri = uri.substring(6);
                config.setDestinationType(JmsDestinationType.TOPIC);
            } else {
                return null;
            }
            
            // Parse destination name
            int paramIndex = uri.indexOf('?');
            String destinationName;
            
            if (paramIndex > 0) {
                destinationName = uri.substring(0, paramIndex);
                String params = uri.substring(paramIndex + 1);
                
                // Parse parameters
                String[] paramPairs = params.split("&");
                for (String pair : paramPairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        properties.setProperty("jms." + keyValue[0], keyValue[1]);
                    }
                }
            } else {
                destinationName = uri;
            }
            
            config.setDestinationName(destinationName);
            
            // Apply properties
            if (!properties.isEmpty()) {
                config = new JmsTransportConfiguration(properties);
                config.setDestinationName(destinationName);
            }
            
            return config;
        }
    }
} 