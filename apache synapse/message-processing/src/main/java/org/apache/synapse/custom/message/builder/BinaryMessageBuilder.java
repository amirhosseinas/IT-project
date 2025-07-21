package org.apache.synapse.custom.message.builder;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Message builder implementation for binary content.
 * Handles file attachments and other binary data.
 */
public class BinaryMessageBuilder implements MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(BinaryMessageBuilder.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/octet-stream",
            "application/pdf",
            "application/zip",
            "application/gzip",
            "application/x-tar",
            "image/jpeg",
            "image/png",
            "image/gif",
            "audio/mpeg",
            "video/mp4"
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
            
            // Add binary metadata
            message.setProperty("binary.size", content.length);
            
            // Determine binary content category
            String baseContentType = contentType.split(";")[0].trim().toLowerCase();
            String category = getBinaryCategory(baseContentType);
            message.setProperty("binary.category", category);
            
            // Add file extension if applicable
            String extension = getFileExtension(baseContentType);
            if (extension != null) {
                message.setProperty("binary.fileExtension", extension);
            }
            
            logger.debug("Successfully built binary message with ID: {}", message.getMessageId());
            return message;
        } catch (IOException e) {
            logger.error("Failed to build binary message", e);
            throw new MessageBuilderException("Failed to build binary message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canHandle(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Handle content types with parameters (e.g., application/octet-stream; name=file.bin)
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType) || 
               baseContentType.startsWith("image/") || 
               baseContentType.startsWith("audio/") || 
               baseContentType.startsWith("video/");
    }
    
    private String getBinaryCategory(String contentType) {
        if (contentType.startsWith("image/")) {
            return "image";
        } else if (contentType.startsWith("audio/")) {
            return "audio";
        } else if (contentType.startsWith("video/")) {
            return "video";
        } else if (contentType.contains("zip") || contentType.contains("tar") || contentType.contains("gzip")) {
            return "archive";
        } else if (contentType.equals("application/pdf")) {
            return "document";
        } else {
            return "binary";
        }
    }
    
    private String getFileExtension(String contentType) {
        switch (contentType) {
            case "application/pdf":
                return "pdf";
            case "application/zip":
                return "zip";
            case "application/gzip":
                return "gz";
            case "application/x-tar":
                return "tar";
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "audio/mpeg":
                return "mp3";
            case "video/mp4":
                return "mp4";
            default:
                return null;
        }
    }
}