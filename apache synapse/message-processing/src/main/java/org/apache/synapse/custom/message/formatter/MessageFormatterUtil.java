package org.apache.synapse.custom.message.formatter;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for working with message formatters.
 * Provides convenience methods for formatting messages and optimizing responses.
 */
public class MessageFormatterUtil {
    private static final Logger logger = LoggerFactory.getLogger(MessageFormatterUtil.class);
    
    // Compression threshold in bytes (only compress responses larger than this)
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB
    
    /**
     * Format a message using the appropriate formatter
     * 
     * @param message The message to format
     * @return The formatted message as a byte array
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     */
    public static byte[] formatMessage(Message message) 
            throws MessageFormatter.MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(message);
        return formatter.formatMessage(message);
    }
    
    /**
     * Format a message using content negotiation
     * 
     * @param message The message to format
     * @param acceptHeader The Accept header from the client request
     * @return The formatted message as a byte array
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     */
    public static byte[] formatMessageWithContentNegotiation(Message message, String acceptHeader) 
            throws MessageFormatter.MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        MessageFormatter formatter = MessageFormatterFactory.getInstance()
                .getFormatterForContentNegotiation(message, acceptHeader);
        return formatter.formatMessage(message);
    }
    
    /**
     * Format a message and write it to an output stream
     * 
     * @param message The message to format
     * @param outputStream The output stream to write to
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     * @throws IOException if writing to the output stream fails
     */
    public static void formatMessage(Message message, OutputStream outputStream) 
            throws MessageFormatter.MessageFormatterException, IOException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        
        MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(message);
        formatter.formatMessage(message, outputStream);
    }
    
    /**
     * Optimize a response message based on client capabilities and message size
     * 
     * @param message The message to optimize
     * @param acceptEncoding The Accept-Encoding header from the client request
     * @return An optimized message with appropriate headers
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     */
    public static Message optimizeResponse(Message message, String acceptEncoding) 
            throws MessageFormatter.MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        // Format the message first
        byte[] formattedContent = formatMessage(message);
        
        // Check if compression is supported and beneficial
        if (shouldCompress(formattedContent, acceptEncoding)) {
            try {
                // Compress the content
                byte[] compressedContent = compressGzip(formattedContent);
                
                // Create a new message with the compressed content
                Message compressedMessage = new Message(message.getMessageId());
                compressedMessage.setContentType(message.getContentType());
                compressedMessage.setDirection(message.getDirection());
                
                // Copy all properties and headers
                for (Map.Entry<String, Object> entry : message.getProperties().entrySet()) {
                    compressedMessage.setProperty(entry.getKey(), entry.getValue());
                }
                
                for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                    compressedMessage.setHeader(entry.getKey(), entry.getValue());
                }
                
                // Set the compressed content and add compression headers
                compressedMessage.setPayload(compressedContent);
                compressedMessage.setHeader("Content-Encoding", "gzip");
                compressedMessage.setHeader("Vary", "Accept-Encoding");
                
                // Add compression ratio as a property for monitoring
                double ratio = (double) compressedContent.length / formattedContent.length;
                compressedMessage.setProperty("compression.ratio", ratio);
                
                logger.debug("Compressed response from {} bytes to {} bytes (ratio: {})",
                        formattedContent.length, compressedContent.length, ratio);
                
                return compressedMessage;
            } catch (IOException e) {
                logger.warn("Failed to compress response, returning uncompressed", e);
            }
        }
        
        // If we get here, either compression failed or wasn't needed
        message.setPayload(formattedContent);
        return message;
    }
    
    /**
     * Format a JSON message
     * 
     * @param message The message to format as JSON
     * @return The formatted JSON as a byte array
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     */
    public static byte[] formatJsonMessage(Message message) 
            throws MessageFormatter.MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        // Ensure the content type is set to JSON
        String originalContentType = message.getContentType();
        message.setContentType("application/json");
        
        try {
            return formatMessage(message);
        } finally {
            // Restore the original content type
            message.setContentType(originalContentType);
        }
    }
    
    /**
     * Format an XML message
     * 
     * @param message The message to format as XML
     * @return The formatted XML as a byte array
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     */
    public static byte[] formatXmlMessage(Message message) 
            throws MessageFormatter.MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        // Ensure the content type is set to XML
        String originalContentType = message.getContentType();
        message.setContentType("application/xml");
        
        try {
            return formatMessage(message);
        } finally {
            // Restore the original content type
            message.setContentType(originalContentType);
        }
    }
    
    /**
     * Format a plain text message
     * 
     * @param message The message to format as plain text
     * @return The formatted text as a byte array
     * @throws MessageFormatter.MessageFormatterException if formatting fails
     */
    public static byte[] formatTextMessage(Message message) 
            throws MessageFormatter.MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        // Ensure the content type is set to plain text
        String originalContentType = message.getContentType();
        message.setContentType("text/plain");
        
        try {
            return formatMessage(message);
        } finally {
            // Restore the original content type
            message.setContentType(originalContentType);
        }
    }
    
    /**
     * Determine if a response should be compressed based on size and client capabilities
     * 
     * @param content The content to potentially compress
     * @param acceptEncoding The Accept-Encoding header from the client request
     * @return true if the content should be compressed, false otherwise
     */
    private static boolean shouldCompress(byte[] content, String acceptEncoding) {
        // Don't compress if content is too small
        if (content == null || content.length < COMPRESSION_THRESHOLD) {
            return false;
        }
        
        // Don't compress if client doesn't support it
        if (acceptEncoding == null || acceptEncoding.trim().isEmpty()) {
            return false;
        }
        
        // Check if gzip is supported
        return acceptEncoding.toLowerCase().contains("gzip");
    }
    
    /**
     * Compress content using GZIP
     * 
     * @param content The content to compress
     * @return The compressed content
     * @throws IOException if compression fails
     */
    private static byte[] compressGzip(byte[] content) throws IOException {
        if (content == null) {
            return new byte[0];
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(content.length);
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos)) {
            gzipOutputStream.write(content);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Create response headers based on the formatter and optimization
     * 
     * @param message The message being formatted
     * @param formatter The formatter being used
     * @return A map of response headers
     */
    public static Map<String, String> createResponseHeaders(Message message, MessageFormatter formatter) {
        Map<String, String> headers = new HashMap<>();
        
        // Set content type
        headers.put("Content-Type", formatter.getContentType(message));
        
        // Copy any existing headers from the message
        headers.putAll(message.getHeaders());
        
        return headers;
    }
}
