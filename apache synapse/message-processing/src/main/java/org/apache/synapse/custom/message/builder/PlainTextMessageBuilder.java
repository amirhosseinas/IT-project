package org.apache.synapse.custom.message.builder;

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
 * Message builder implementation for plain text content.
 */
public class PlainTextMessageBuilder implements MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PlainTextMessageBuilder.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "text/plain",
            "text/csv",
            "text/html",
            "text/css"
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
            
            // Extract text metadata
            String text = new String(content, StandardCharsets.UTF_8);
            int lineCount = text.split("\r\n|\r|\n").length;
            message.setProperty("text.lineCount", lineCount);
            message.setProperty("text.length", text.length());
            
            // Detect content subtype
            String baseContentType = contentType.split(";")[0].trim().toLowerCase();
            if ("text/csv".equals(baseContentType)) {
                // For CSV, count columns in the first row
                String firstLine = text.split("\r\n|\r|\n")[0];
                int columnCount = firstLine.split(",").length;
                message.setProperty("csv.columnCount", columnCount);
            } else if ("text/html".equals(baseContentType)) {
                // For HTML, set a flag
                message.setProperty("text.isHtml", true);
            }
            
            logger.debug("Successfully built text message with ID: {}", message.getMessageId());
            return message;
        } catch (IOException e) {
            logger.error("Failed to build text message", e);
            throw new MessageBuilderException("Failed to build text message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canHandle(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Handle content types with parameters (e.g., text/plain; charset=utf-8)
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
}