package org.apache.synapse.custom.message.builder.examples;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.builder.MessageBuilder;
import org.apache.synapse.custom.message.builder.MessageBuilderFactory;
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Example demonstrating how to use the message builders.
 */
public class MessageBuilderExample {
    private static final Logger logger = LoggerFactory.getLogger(MessageBuilderExample.class);
    
    public static void main(String[] args) {
        try {
            // Example 1: Build a JSON message
            String jsonContent = "{ \"name\": \"John Doe\", \"age\": 30, \"email\": \"john@example.com\" }";
            Message jsonMessage = MessageBuilderUtil.buildJsonMessage(jsonContent);
            printMessageInfo(jsonMessage);
            
            // Example 2: Build an XML message
            String xmlContent = "<person><name>John Doe</name><age>30</age><email>john@example.com</email></person>";
            Message xmlMessage = MessageBuilderUtil.buildXmlMessage(xmlContent);
            printMessageInfo(xmlMessage);
            
            // Example 3: Build a SOAP message
            String soapContent = 
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "  <soapenv:Header/>" +
                    "  <soapenv:Body>" +
                    "    <person><name>John Doe</name><age>30</age><email>john@example.com</email></person>" +
                    "  </soapenv:Body>" +
                    "</soapenv:Envelope>";
            Message soapMessage = MessageBuilderUtil.buildSoapMessage(soapContent);
            printMessageInfo(soapMessage);
            
            // Example 4: Build a plain text message
            String textContent = "Hello, world! This is a plain text message.";
            Message textMessage = MessageBuilderUtil.buildTextMessage(textContent);
            printMessageInfo(textMessage);
            
            // Example 5: Using the factory directly
            String csvContent = "name,age,email\nJohn Doe,30,john@example.com";
            ByteArrayInputStream csvInputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
            
            MessageBuilder builder = MessageBuilderFactory.getInstance().getBuilder("text/csv");
            Message csvMessage = builder.buildMessage(csvInputStream, "text/csv");
            printMessageInfo(csvMessage);
            
            logger.info("All examples completed successfully");
        } catch (Exception e) {
            logger.error("Error in message builder example", e);
        }
    }
    
    private static void printMessageInfo(Message message) {
        logger.info("Message ID: {}", message.getMessageId());
        logger.info("Content Type: {}", message.getContentType());
        logger.info("Payload Size: {} bytes", message.getPayload().length);
        logger.info("Properties: {}", message.getProperties());
        logger.info("---");
    }
} 