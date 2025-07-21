package org.apache.synapse.custom.message.formatter;

import com.caucho.hessian.io.HessianOutput;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Message formatter implementation for Hessian binary serialization format.
 * Uses Caucho Hessian for serialization.
 */
public class HessianMessageFormatter implements MessageFormatter {
    private static final Logger logger = LoggerFactory.getLogger(HessianMessageFormatter.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/x-hessian"
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
            logger.error("Failed to write Hessian content to output stream", e);
            throw new MessageFormatterException("Failed to write Hessian content to output stream: " + e.getMessage(), e);
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
            
            // For Hessian, we'll just pass through the binary content
            // since it's already in Hessian format
            return payload;
        } catch (Exception e) {
            logger.error("Failed to format Hessian message", e);
            throw new MessageFormatterException("Failed to format Hessian message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Serialize an object to Hessian binary format
     * 
     * @param obj The object to serialize
     * @return The serialized object as a byte array
     * @throws IOException if serialization fails
     */
    public byte[] serializeToHessian(Object obj) throws IOException {
        if (obj == null) {
            return new byte[0];
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HessianOutput hessianOutput = new HessianOutput(baos);
        hessianOutput.writeObject(obj);
        hessianOutput.flush();
        
        return baos.toByteArray();
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
            return "application/x-hessian";
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        
        if (SUPPORTED_CONTENT_TYPES.contains(baseContentType)) {
            return contentType;
        }
        
        return "application/x-hessian";
    }
}
