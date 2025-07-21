package org.apache.synapse.custom.message.builder;

import com.caucho.hessian.io.HessianInput;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Message builder implementation for Hessian serialized Java objects.
 * Uses Hessian for Java object serialization/deserialization.
 */
public class HessianMessageBuilder implements MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(HessianMessageBuilder.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/x-hessian",
            "application/hessian"
    ));
    
    @Override
    public Message buildMessage(InputStream inputStream, String contentType) throws MessageBuilderException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        
        try {
            // Read the input stream into a byte array
            byte[] content = IOUtils.toByteArray(inputStream);
            
            // Create a new message
            Message message = new Message(UUID.randomUUID().toString());
            message.setContentType(contentType);
            message.setPayload(content);
            
            // Try to deserialize the object to validate it's a valid Hessian object
            // and extract metadata
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            
            try {
                Object deserializedObject = hessianInput.readObject();
                
                // Add object metadata as properties
                if (deserializedObject != null) {
                    message.setProperty("hessian.objectClass", deserializedObject.getClass().getName());
                    message.setProperty("hessian.objectHashCode", deserializedObject.hashCode());
                    
                    // Store the deserialized object for direct access if needed
                    message.setProperty("hessian.object", deserializedObject);
                }
            } catch (Exception e) {
                // If deserialization fails, log a warning but still return the message
                // since we might just want to pass it through without processing
                logger.warn("Failed to deserialize Hessian object: {}", e.getMessage());
                message.setProperty("hessian.valid", false);
                message.setProperty("hessian.error", e.getMessage());
            }
            
            logger.debug("Successfully built Hessian message with ID: {}", message.getMessageId());
            return message;
        } catch (IOException e) {
            logger.error("Failed to build Hessian message", e);
            throw new MessageBuilderException("Failed to build Hessian message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canHandle(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Handle content types with parameters (e.g., application/x-hessian; charset=utf-8)
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
} 