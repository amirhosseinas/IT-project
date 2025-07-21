package org.apache.synapse.custom.message.formatter;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating message formatters based on content type and accept headers.
 * Provides a central point for registering and retrieving message formatters.
 * Handles content negotiation and formatter selection.
 */
public class MessageFormatterFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageFormatterFactory.class);
    
    private static final MessageFormatterFactory INSTANCE = new MessageFormatterFactory();
    
    private final List<MessageFormatter> formatters;
    private MessageFormatter defaultFormatter;
    
    // Cache for content type to formatter mapping
    private final Map<String, MessageFormatter> formatterCache;
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private MessageFormatterFactory() {
        this.formatters = new ArrayList<>();
        this.formatterCache = new HashMap<>();
        
        // Register default formatters
        registerDefaultFormatters();
    }
    
    /**
     * Get the singleton instance of the factory
     * 
     * @return The factory instance
     */
    public static MessageFormatterFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register default message formatters
     */
    private void registerDefaultFormatters() {
        // Register priority formatters first (order matters for content negotiation)
        registerFormatter(new JsonMessageFormatter());
        registerFormatter(new XmlMessageFormatter());
        registerFormatter(new PlainTextMessageFormatter());
        
        // Register optional formatters
        registerFormatter(new BinaryMessageFormatter());
        registerFormatter(new HessianMessageFormatter());
        
        // Set the binary formatter as the default (fallback)
        defaultFormatter = new BinaryMessageFormatter();
        
        logger.info("Registered default message formatters");
    }
    
    /**
     * Register a message formatter
     * 
     * @param formatter The formatter to register
     */
    public void registerFormatter(MessageFormatter formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter cannot be null");
        }
        
        formatters.add(formatter);
        logger.debug("Registered message formatter: {}", formatter.getClass().getName());
    }
    
    /**
     * Get a message formatter for the specified message
     * 
     * @param message The message to format
     * @return A message formatter that can handle the message, or the default formatter if none found
     */
    public MessageFormatter getFormatter(Message message) {
        if (message == null) {
            logger.warn("No message specified, using default formatter");
            return defaultFormatter;
        }
        
        String contentType = message.getContentType();
        if (contentType == null || contentType.trim().isEmpty()) {
            logger.warn("No content type specified, using default formatter");
            return defaultFormatter;
        }
        
        // Check cache first
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        MessageFormatter cachedFormatter = formatterCache.get(baseContentType);
        if (cachedFormatter != null) {
            return cachedFormatter;
        }
        
        // Find a formatter that can handle this message
        for (MessageFormatter formatter : formatters) {
            if (formatter.canFormat(message)) {
                logger.debug("Found formatter {} for content type: {}", 
                        formatter.getClass().getName(), contentType);
                
                // Cache the formatter for this content type
                formatterCache.put(baseContentType, formatter);
                return formatter;
            }
        }
        
        logger.warn("No formatter found for content type: {}, using default formatter", contentType);
        return defaultFormatter;
    }
    
    /**
     * Get a message formatter based on content negotiation
     * 
     * @param message The message to format
     * @param acceptHeader The Accept header from the client request
     * @return The best matching formatter based on content negotiation
     */
    public MessageFormatter getFormatterForContentNegotiation(Message message, String acceptHeader) {
        if (message == null) {
            logger.warn("No message specified, using default formatter");
            return defaultFormatter;
        }
        
        // If no Accept header is provided, use the message's content type
        if (acceptHeader == null || acceptHeader.trim().isEmpty()) {
            return getFormatter(message);
        }
        
        // Parse the Accept header
        List<AcceptType> acceptTypes = parseAcceptHeader(acceptHeader);
        
        // If no valid Accept types, use the message's content type
        if (acceptTypes.isEmpty()) {
            return getFormatter(message);
        }
        
        // Try to find a formatter that matches one of the accepted types
        for (AcceptType acceptType : acceptTypes) {
            // Special case for wildcard
            if (acceptType.getType().equals("*/*")) {
                return getFormatter(message);
            }
            
            // Check if we have a formatter for this accept type
            for (MessageFormatter formatter : formatters) {
                // Create a temporary message with the accept type as content type
                Message tempMessage = new Message();
                tempMessage.setContentType(acceptType.getType());
                
                if (formatter.canFormat(tempMessage)) {
                    logger.debug("Found formatter {} for accept type: {}", 
                            formatter.getClass().getName(), acceptType.getType());
                    return formatter;
                }
            }
        }
        
        // If no match found, fall back to the message's content type
        logger.warn("No formatter found for Accept header: {}, using message content type", acceptHeader);
        return getFormatter(message);
    }
    
    /**
     * Set the default formatter to use when no suitable formatter is found
     * 
     * @param formatter The default formatter
     */
    public void setDefaultFormatter(MessageFormatter formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("Default formatter cannot be null");
        }
        
        this.defaultFormatter = formatter;
        logger.debug("Set default formatter: {}", formatter.getClass().getName());
    }
    
    /**
     * Parse the Accept header into a list of AcceptType objects
     * 
     * @param acceptHeader The Accept header string
     * @return A list of AcceptType objects sorted by quality factor
     */
    private List<AcceptType> parseAcceptHeader(String acceptHeader) {
        List<AcceptType> acceptTypes = new ArrayList<>();
        
        if (acceptHeader == null || acceptHeader.trim().isEmpty()) {
            return acceptTypes;
        }
        
        String[] types = acceptHeader.split(",");
        for (String type : types) {
            String[] parts = type.split(";");
            String mediaType = parts[0].trim();
            float quality = 1.0f;
            
            // Check for q parameter
            for (int i = 1; i < parts.length; i++) {
                String param = parts[i].trim();
                if (param.startsWith("q=")) {
                    try {
                        quality = Float.parseFloat(param.substring(2));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid quality value in Accept header: {}", param);
                    }
                    break;
                }
            }
            
            acceptTypes.add(new AcceptType(mediaType, quality));
        }
        
        // Sort by quality factor (highest first)
        acceptTypes.sort((a, b) -> Float.compare(b.getQuality(), a.getQuality()));
        
        return acceptTypes;
    }
    
    /**
     * Helper class to represent an Accept type with its quality factor
     */
    private static class AcceptType {
        private final String type;
        private final float quality;
        
        public AcceptType(String type, float quality) {
            this.type = type;
            this.quality = quality;
        }
        
        public String getType() {
            return type;
        }
        
        public float getQuality() {
            return quality;
        }
    }
}
