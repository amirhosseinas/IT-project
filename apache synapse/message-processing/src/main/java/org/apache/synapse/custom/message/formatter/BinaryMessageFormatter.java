package org.apache.synapse.custom.message.formatter;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Message formatter implementation for binary content.
 * Passes through binary content without any transformation.
 */
public class BinaryMessageFormatter implements MessageFormatter {
    private static final Logger logger = LoggerFactory.getLogger(BinaryMessageFormatter.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/octet-stream",
            "application/binary",
            "image/jpeg",
            "image/png",
            "image/gif",
            "audio/mpeg",
            "video/mp4"
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
            logger.error("Failed to write binary content to output stream", e);
            throw new MessageFormatterException("Failed to write binary content to output stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] formatMessage(Message message) throws MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            byte[] payload = message.getPayload();
            if (payload == null) {
                logger.warn("Message payload is null, returning empty byte array");
                return new byte[0];
            }
            
            // Binary content doesn't need any transformation
            return payload;
        } catch (Exception e) {
            logger.error("Failed to format binary message", e);
            throw new MessageFormatterException("Failed to format binary message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canFormat(Message message) {
        if (message == null || message.getContentType() == null) {
            return false;
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        
        // Binary formatter can handle any content type as a fallback
        // but we'll prioritize specific binary content types
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
    
    @Override
    public String getContentType(Message message) {
        if (message == null || message.getContentType() == null) {
            return "application/octet-stream";
        }
        
        return message.getContentType();
    }
}
