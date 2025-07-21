package org.apache.synapse.custom.transports.jms;

import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportManager;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Factory for creating JMS transport listeners and senders.
 * Registers them with the transport manager.
 */
public class JmsTransportFactory {
    private static final Logger logger = LoggerFactory.getLogger(JmsTransportFactory.class);
    
    private final TransportManager transportManager;
    
    /**
     * Create a new JMS transport factory
     * 
     * @param transportManager The transport manager to register with
     */
    public JmsTransportFactory(TransportManager transportManager) {
        this.transportManager = transportManager;
    }
    
    /**
     * Create and register a JMS transport listener
     * 
     * @param name The name for the listener
     * @param properties The configuration properties
     * @throws TransportException if creation fails
     */
    public void createListener(String name, Properties properties) throws TransportException {
        JmsTransportConfiguration config = new JmsTransportConfiguration(properties);
        JmsTransportListener listener = new JmsTransportListener(config);
        
        transportManager.registerListener(name, listener);
        logger.info("Registered JMS transport listener: {}", name);
    }
    
    /**
     * Create and register a JMS transport sender
     * 
     * @param name The name for the sender
     * @throws TransportException if creation fails
     */
    public void createSender(String name) throws TransportException {
        JmsTransportSender sender = new JmsTransportSender();
        
        transportManager.registerSender(name, sender);
        logger.info("Registered JMS transport sender: {}", name);
    }
    
    /**
     * Create and register default JMS transport listeners and senders
     * 
     * @param properties The configuration properties
     * @throws TransportException if creation fails
     */
    public void createDefaultTransports(Properties properties) throws TransportException {
        // Create and register JMS transport sender
        createSender("jms");
        
        // Create and register JMS queue listener if configured
        String queueName = properties.getProperty("jms.queue.name");
        if (queueName != null && !queueName.isEmpty()) {
            Properties queueProps = new Properties(properties);
            queueProps.setProperty("jms.destinationName", queueName);
            queueProps.setProperty("jms.destinationType", "queue");
            
            createListener("jms-queue", queueProps);
        }
        
        // Create and register JMS topic listener if configured
        String topicName = properties.getProperty("jms.topic.name");
        if (topicName != null && !topicName.isEmpty()) {
            Properties topicProps = new Properties(properties);
            topicProps.setProperty("jms.destinationName", topicName);
            topicProps.setProperty("jms.destinationType", "topic");
            
            createListener("jms-topic", topicProps);
        }
    }
} 