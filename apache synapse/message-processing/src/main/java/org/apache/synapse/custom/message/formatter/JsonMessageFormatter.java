package org.apache.synapse.custom.message.formatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Message formatter implementation for JSON content.
 * Uses Jackson for JSON processing.
 */
public class JsonMessageFormatter implements MessageFormatter {
    private static final Logger logger = LoggerFactory.getLogger(JsonMessageFormatter.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/json",
            "text/json",
            "application/javascript",
            "text/javascript"
    ));
    
    private final ObjectMapper objectMapper;
    
    public JsonMessageFormatter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @Override
    public void formatMessage(Message message, OutputStream outputStream) throws MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        
        try {
            byte[] formattedContent = formatMessage(message);
            outputStream.write(formattedContent);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Failed to write formatted JSON to output stream", e);
            throw new MessageFormatterException("Failed to write formatted JSON to output stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] formatMessage(Message message) throws MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            byte[] payload = message.getPayload();
            if (payload == null || payload.length == 0) {
                logger.warn("Message payload is empty, returning empty JSON object");
                return "{}".getBytes();
            }
            
            // Parse the payload as JSON to validate and pretty-print
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            // Write the JSON with proper formatting
            return objectMapper.writeValueAsBytes(jsonNode);
        } catch (IOException e) {
            logger.error("Failed to format JSON message", e);
            throw new MessageFormatterException("Failed to format JSON message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canFormat(Message message) {
        if (message == null || message.getContentType() == null) {
            return false;
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
    
    @Override
    public String getContentType(Message message) {
        if (message == null || message.getContentType() == null) {
            return "application/json";
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        
        if (SUPPORTED_CONTENT_TYPES.contains(baseContentType)) {
            return contentType;
        }
        
        return "application/json";
    }
} 