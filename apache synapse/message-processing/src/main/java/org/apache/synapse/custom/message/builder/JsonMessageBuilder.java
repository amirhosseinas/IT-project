package org.apache.synapse.custom.message.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Message builder implementation for JSON content.
 * Uses Jackson for JSON processing.
 */
public class JsonMessageBuilder implements MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(JsonMessageBuilder.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/json",
            "text/json",
            "application/javascript",
            "text/javascript"
    ));
    
    private final ObjectMapper objectMapper;
    
    public JsonMessageBuilder() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public Message buildMessage(InputStream inputStream, String contentType) throws MessageBuilderException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        
        try {
            // Read the input stream into a byte array
            byte[] content = IOUtils.toByteArray(inputStream);
            
            // Parse and validate JSON
            JsonNode jsonNode = objectMapper.readTree(content);
            
            // Create a new message
            Message message = new Message(UUID.randomUUID().toString());
            message.setContentType(contentType);
            message.setPayload(content);
            
            // Add JSON metadata as properties
            if (jsonNode.isObject()) {
                message.setProperty("json.rootType", "object");
                message.setProperty("json.fieldCount", jsonNode.size());
            } else if (jsonNode.isArray()) {
                message.setProperty("json.rootType", "array");
                message.setProperty("json.arraySize", jsonNode.size());
            }
            
            logger.debug("Successfully built JSON message with ID: {}", message.getMessageId());
            return message;
        } catch (IOException e) {
            logger.error("Failed to build JSON message", e);
            throw new MessageBuilderException("Failed to build JSON message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canHandle(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Handle content types with parameters (e.g., application/json; charset=utf-8)
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
} 