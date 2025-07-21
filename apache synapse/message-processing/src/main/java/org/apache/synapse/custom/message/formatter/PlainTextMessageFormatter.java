package org.apache.synapse.custom.message.formatter;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Message formatter implementation for plain text content.
 */
public class PlainTextMessageFormatter implements MessageFormatter {
    private static final Logger logger = LoggerFactory.getLogger(PlainTextMessageFormatter.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "text/plain",
            "text/html",
            "text/css"
    ));
    
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
            logger.error("Failed to write formatted text to output stream", e);
            throw new MessageFormatterException("Failed to write formatted text to output stream: " + e.getMessage(), e);
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
                logger.warn("Message payload is empty, returning empty string");
                return new byte[0];
            }
            
            // Plain text doesn't need any special formatting
            // Just ensure it's properly encoded as UTF-8
            String textContent = new String(payload, StandardCharsets.UTF_8);
            return textContent.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to format text message", e);
            throw new MessageFormatterException("Failed to format text message: " + e.getMessage(), e);
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
            return "text/plain";
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        
        if (SUPPORTED_CONTENT_TYPES.contains(baseContentType)) {
            return contentType;
        }
        
        return "text/plain";
    }
}
