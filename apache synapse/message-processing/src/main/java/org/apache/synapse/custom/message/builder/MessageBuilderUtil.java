package org.apache.synapse.custom.message.builder;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for working with message builders.
 * Provides convenience methods for building messages from various sources.
 */
public class MessageBuilderUtil {
    private static final Logger logger = LoggerFactory.getLogger(MessageBuilderUtil.class);
    
    /**
     * Build a message from an input stream
     * 
     * @param inputStream The input stream containing the message content
     * @param contentType The content type of the message
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildMessage(InputStream inputStream, String contentType) 
            throws MessageBuilder.MessageBuilderException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        
        MessageBuilder builder = MessageBuilderFactory.getInstance().getBuilder(contentType);
        return builder.buildMessage(inputStream, contentType);
    }
    
    /**
     * Build a message from a byte array
     * 
     * @param content The byte array containing the message content
     * @param contentType The content type of the message
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildMessage(byte[] content, String contentType) 
            throws MessageBuilder.MessageBuilderException {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        return buildMessage(inputStream, contentType);
    }
    
    /**
     * Build a message from a string
     * 
     * @param content The string containing the message content
     * @param contentType The content type of the message
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildMessage(String content, String contentType) 
            throws MessageBuilder.MessageBuilderException {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return buildMessage(bytes, contentType);
    }
    
    /**
     * Build a JSON message from a string
     * 
     * @param jsonContent The JSON string
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildJsonMessage(String jsonContent) 
            throws MessageBuilder.MessageBuilderException {
        return buildMessage(jsonContent, "application/json");
    }
    
    /**
     * Build an XML message from a string
     * 
     * @param xmlContent The XML string
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildXmlMessage(String xmlContent) 
            throws MessageBuilder.MessageBuilderException {
        return buildMessage(xmlContent, "application/xml");
    }
    
    /**
     * Build a SOAP message from a string
     * 
     * @param soapContent The SOAP XML string
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildSoapMessage(String soapContent) 
            throws MessageBuilder.MessageBuilderException {
        return buildMessage(soapContent, "application/soap+xml");
    }
    
    /**
     * Build a plain text message from a string
     * 
     * @param textContent The text string
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildTextMessage(String textContent) 
            throws MessageBuilder.MessageBuilderException {
        return buildMessage(textContent, "text/plain");
    }
    
    /**
     * Build a Hessian message from a serialized object
     * 
     * @param serializedContent The serialized Hessian object as byte array
     * @return A new Message object
     * @throws MessageBuilder.MessageBuilderException if building fails
     */
    public static Message buildHessianMessage(byte[] serializedContent) 
            throws MessageBuilder.MessageBuilderException {
        return buildMessage(serializedContent, "application/x-hessian");
    }
} 