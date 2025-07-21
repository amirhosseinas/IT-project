package org.apache.synapse.custom.transports.jms;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JMS transport listener implementation.
 * Listens for JMS messages and forwards them to the message processor.
 */
public class JmsTransportListener implements TransportListener, ExceptionListener {
    private static final Logger logger = LoggerFactory.getLogger(JmsTransportListener.class);
    
    private JmsTransportConfiguration config;
    private JmsConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private Destination destination;
    private MessageCallback callback;
    private JmsMessageConverter messageConverter;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private volatile boolean initialized = false;
    
    /**
     * Create a new JMS transport listener
     * 
     * @param config The JMS transport configuration
     */
    public JmsTransportListener(JmsTransportConfiguration config) {
        this.config = config;
        this.messageConverter = new JmsMessageConverter();
    }
    
    @Override
    public void init() throws TransportException {
        if (initialized) {
            return;
        }
        
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
            
            // Create thread pool for message processing
            this.executorService = Executors.newFixedThreadPool(
                    config.getConnectionPoolSize(),
                    new JmsThreadFactory("JmsListener"));
            
            initialized = true;
            logger.info("JMS transport listener initialized");
            
        } catch (NamingException e) {
            throw new TransportException("Failed to initialize JMS transport listener: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void start() throws TransportException {
        if (!initialized) {
            init();
        }
        
        if (running) {
            return;
        }
        
        try {
            // Get a connection from the pool
            connection = connectionFactory.getConnection();
            connection.setExceptionListener(this);
            connection.start();
            
            // Create a session
            session = connectionFactory.createSession(
                    connection, 
                    config.isTransacted(), 
                    config.getAcknowledgeMode());
            
            // Look up or create the destination
            if (config.getDestinationType() == JmsDestinationType.TOPIC) {
                destination = session.createTopic(config.getDestinationName());
            } else {
                destination = session.createQueue(config.getDestinationName());
            }
            
            // Create a consumer with optional message selector
            if (config.getMessageSelector() != null && !config.getMessageSelector().isEmpty()) {
                consumer = session.createConsumer(destination, config.getMessageSelector());
            } else {
                consumer = session.createConsumer(destination);
            }
            
            // Set message listener
            consumer.setMessageListener(new JmsMessageListener());
            
            running = true;
            logger.info("JMS transport listener started for destination: {}", config.getDestinationName());
            
        } catch (JMSException e) {
            throw new TransportException("Failed to start JMS transport listener: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void stop() throws TransportException {
        if (!running) {
            return;
        }
        
        try {
            if (consumer != null) {
                consumer.close();
            }
            
            if (session != null) {
                session.close();
            }
            
            if (connection != null) {
                connection.stop();
                connectionFactory.returnConnection(connection);
            }
            
            executorService.shutdown();
            
            running = false;
            logger.info("JMS transport listener stopped");
            
        } catch (JMSException e) {
            throw new TransportException("Failed to stop JMS transport listener: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void setMessageCallback(MessageCallback callback) {
        this.callback = callback;
    }
    
    @Override
    public void onException(JMSException e) {
        logger.error("JMS exception occurred: {}", e.getMessage(), e);
        
        // Try to reconnect
        try {
            stop();
            start();
        } catch (TransportException te) {
            logger.error("Failed to reconnect after JMS exception", te);
        }
    }
    
    /**
     * JMS message listener implementation.
     */
    private class JmsMessageListener implements MessageListener {
        @Override
        public void onMessage(javax.jms.Message jmsMessage) {
            executorService.submit(() -> processMessage(jmsMessage));
        }
        
        private void processMessage(javax.jms.Message jmsMessage) {
            try {
                // Convert JMS message to Synapse message
                Message synapseMessage = messageConverter.toSynapseMessage(jmsMessage);
                
                // Process message if callback is set
                if (callback != null) {
                    Message response = callback.onMessage(synapseMessage);
                    
                    // Send response if required
                    if (response != null && jmsMessage.getJMSReplyTo() != null) {
                        sendResponse(response, jmsMessage.getJMSReplyTo(), jmsMessage.getJMSCorrelationID());
                    }
                }
                
                // Acknowledge message if necessary
                if (config.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE) {
                    jmsMessage.acknowledge();
                }
                
            } catch (Exception e) {
                logger.error("Error processing JMS message: {}", e.getMessage(), e);
                
                // Implement retry logic if configured
                if (config.getMaxRetries() > 0) {
                    retryMessage(jmsMessage, 1);
                }
            }
        }
        
        private void retryMessage(javax.jms.Message jmsMessage, int attempt) {
            if (attempt > config.getMaxRetries()) {
                logger.error("Max retries ({}) exceeded for message", config.getMaxRetries());
                return;
            }
            
            try {
                Thread.sleep(config.getRetryInterval());
                logger.info("Retrying message, attempt {}/{}", attempt, config.getMaxRetries());
                processMessage(jmsMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Retry interrupted", e);
            }
        }
        
        private void sendResponse(Message response, Destination replyTo, String correlationId) {
            Connection conn = null;
            Session session = null;
            MessageProducer producer = null;
            
            try {
                conn = connectionFactory.getConnection();
                session = connectionFactory.createSession(conn, false, Session.AUTO_ACKNOWLEDGE);
                producer = session.createProducer(replyTo);
                
                // Convert Synapse message to JMS message
                javax.jms.Message jmsResponse = messageConverter.toJmsMessage(response, session);
                
                // Set correlation ID if available
                if (correlationId != null) {
                    jmsResponse.setJMSCorrelationID(correlationId);
                }
                
                // Send the response
                producer.send(jmsResponse);
                
            } catch (JMSException e) {
                logger.error("Error sending JMS response: {}", e.getMessage(), e);
            } finally {
                try {
                    if (producer != null) {
                        producer.close();
                    }
                    if (session != null) {
                        session.close();
                    }
                    if (conn != null) {
                        connectionFactory.returnConnection(conn);
                    }
                } catch (JMSException e) {
                    logger.warn("Error closing JMS resources", e);
                }
            }
        }
    }
    
    /**
     * Thread factory for JMS listener threads.
     */
    private static class JmsThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        public JmsThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
} 