package org.apache.synapse.custom.transports.jms;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Converts between JMS messages and Synapse messages.
 */
public class JmsMessageConverter {
    private static final Logger logger = LoggerFactory.getLogger(JmsMessageConverter.class);

    /**
     * Convert a JMS message to a Synapse message
     * 
     * @param jmsMessage The JMS message to convert
     * @return The converted Synapse message
     * @throws JMSException if conversion fails
     */
    public Message toSynapseMessage(javax.jms.Message jmsMessage) throws JMSException {
        Message synapseMessage = new Message();
        
        // Set message ID
        String messageId = jmsMessage.getJMSMessageID();
        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
        synapseMessage.setMessageId(messageId);
        
        // Set direction
        synapseMessage.setDirection(Message.Direction.REQUEST);
        
        // Copy JMS properties to Synapse message properties
        Enumeration<?> propertyNames = jmsMessage.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            Object value = jmsMessage.getObjectProperty(name);
            synapseMessage.setProperty(name, value);
        }
        
        // Copy JMS headers to Synapse message properties
        synapseMessage.setProperty("JMS_CORRELATION_ID", jmsMessage.getJMSCorrelationID());
        synapseMessage.setProperty("JMS_DELIVERY_MODE", jmsMessage.getJMSDeliveryMode());
        synapseMessage.setProperty("JMS_DESTINATION", jmsMessage.getJMSDestination().toString());
        synapseMessage.setProperty("JMS_EXPIRATION", jmsMessage.getJMSExpiration());
        synapseMessage.setProperty("JMS_MESSAGE_ID", jmsMessage.getJMSMessageID());
        synapseMessage.setProperty("JMS_PRIORITY", jmsMessage.getJMSPriority());
        synapseMessage.setProperty("JMS_REDELIVERED", jmsMessage.getJMSRedelivered());
        synapseMessage.setProperty("JMS_REPLY_TO", jmsMessage.getJMSReplyTo() != null ? 
                jmsMessage.getJMSReplyTo().toString() : null);
        synapseMessage.setProperty("JMS_TIMESTAMP", jmsMessage.getJMSTimestamp());
        synapseMessage.setProperty("JMS_TYPE", jmsMessage.getJMSType());
        
        // Extract message content based on message type
        if (jmsMessage instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) jmsMessage;
            String text = textMessage.getText();
            synapseMessage.setPayload(text != null ? text.getBytes() : new byte[0]);
            synapseMessage.setContentType("text/plain");
            
        } else if (jmsMessage instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) jmsMessage;
            byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(bytes);
            synapseMessage.setPayload(bytes);
            synapseMessage.setContentType("application/octet-stream");
            
        } else if (jmsMessage instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) jmsMessage;
            StringBuilder builder = new StringBuilder();
            Enumeration<?> mapNames = mapMessage.getMapNames();
            
            while (mapNames.hasMoreElements()) {
                String name = (String) mapNames.nextElement();
                Object value = mapMessage.getObject(name);
                synapseMessage.setProperty("JMS_MAP_" + name, value);
                builder.append(name).append("=").append(value).append("\n");
            }
            
            synapseMessage.setPayload(builder.toString().getBytes());
            synapseMessage.setContentType("text/plain");
            
        } else if (jmsMessage instanceof StreamMessage) {
            // For stream messages, we can't easily convert them
            // So we store a placeholder and set a property
            synapseMessage.setPayload("JMS Stream Message".getBytes());
            synapseMessage.setContentType("text/plain");
            synapseMessage.setProperty("JMS_MESSAGE_TYPE", "STREAM");
            
        } else if (jmsMessage instanceof ObjectMessage) {
            ObjectMessage objectMessage = (ObjectMessage) jmsMessage;
            Object object = objectMessage.getObject();
            synapseMessage.setProperty("JMS_OBJECT", object);
            synapseMessage.setPayload("JMS Object Message".getBytes());
            synapseMessage.setContentType("text/plain");
            synapseMessage.setProperty("JMS_MESSAGE_TYPE", "OBJECT");
            
        } else {
            // Default case
            synapseMessage.setPayload("JMS Message".getBytes());
            synapseMessage.setContentType("text/plain");
            synapseMessage.setProperty("JMS_MESSAGE_TYPE", "UNKNOWN");
        }
        
        return synapseMessage;
    }
    
    /**
     * Convert a Synapse message to a JMS message
     * 
     * @param synapseMessage The Synapse message to convert
     * @param session The JMS session to use for creating the message
     * @return The converted JMS message
     * @throws JMSException if conversion fails
     */
    public javax.jms.Message toJmsMessage(Message synapseMessage, Session session) throws JMSException {
        javax.jms.Message jmsMessage;
        
        // Determine the JMS message type based on content type
        String contentType = synapseMessage.getContentType();
        byte[] payload = synapseMessage.getPayload();
        
        if (contentType != null && contentType.startsWith("text/")) {
            // Create a text message
            TextMessage textMessage = session.createTextMessage();
            if (payload != null) {
                textMessage.setText(new String(payload));
            }
            jmsMessage = textMessage;
            
        } else {
            // Create a bytes message for all other content types
            BytesMessage bytesMessage = session.createBytesMessage();
            if (payload != null) {
                bytesMessage.writeBytes(payload);
            }
            jmsMessage = bytesMessage;
        }
        
        // Set JMS headers from Synapse message properties
        Object correlationId = synapseMessage.getProperty("JMS_CORRELATION_ID");
        if (correlationId != null) {
            jmsMessage.setJMSCorrelationID(correlationId.toString());
        }
        
        Object deliveryMode = synapseMessage.getProperty("JMS_DELIVERY_MODE");
        if (deliveryMode != null && deliveryMode instanceof Integer) {
            jmsMessage.setJMSDeliveryMode((Integer) deliveryMode);
        }
        
        Object priority = synapseMessage.getProperty("JMS_PRIORITY");
        if (priority != null && priority instanceof Integer) {
            jmsMessage.setJMSPriority((Integer) priority);
        }
        
        Object type = synapseMessage.getProperty("JMS_TYPE");
        if (type != null) {
            jmsMessage.setJMSType(type.toString());
        }
        
        // Copy Synapse message properties to JMS message properties
        // Skip properties that are JMS headers or internal properties
        for (String name : synapseMessage.getProperties().keySet()) {
            if (!name.startsWith("JMS_")) {
                Object value = synapseMessage.getProperty(name);
                if (value != null) {
                    jmsMessage.setObjectProperty(name, value);
                }
            }
        }
        
        return jmsMessage;
    }
} 