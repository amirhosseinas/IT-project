package org.apache.synapse.custom.message.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating message builders based on content type.
 * Provides a central point for registering and retrieving message builders.
 */
public class MessageBuilderFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageBuilderFactory.class);
    
    private static final MessageBuilderFactory INSTANCE = new MessageBuilderFactory();
    
    private final List<MessageBuilder> builders;
    private MessageBuilder defaultBuilder;
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private MessageBuilderFactory() {
        this.builders = new ArrayList<>();
        
        // Register default builders
        registerDefaultBuilders();
    }
    
    /**
     * Get the singleton instance of the factory
     * 
     * @return The factory instance
     */
    public static MessageBuilderFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register default message builders
     */
    private void registerDefaultBuilders() {
        // Register priority builders first (order matters for fallback)
        registerBuilder(new JsonMessageBuilder());
        registerBuilder(new XmlMessageBuilder());
        registerBuilder(new PlainTextMessageBuilder());
        
        // Register optional builders
        registerBuilder(new BinaryMessageBuilder());
        registerBuilder(new HessianMessageBuilder());
        
        // Set the binary builder as the default (fallback)
        defaultBuilder = new BinaryMessageBuilder();
        
        logger.info("Registered default message builders");
    }
    
    /**
     * Register a message builder
     * 
     * @param builder The builder to register
     */
    public void registerBuilder(MessageBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }
        
        builders.add(builder);
        logger.debug("Registered message builder: {}", builder.getClass().getName());
    }
    
    /**
     * Get a message builder for the specified content type
     * 
     * @param contentType The content type
     * @return A message builder that can handle the content type, or the default builder if none found
     */
    public MessageBuilder getBuilder(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            logger.warn("No content type specified, using default builder");
            return defaultBuilder;
        }
        
        for (MessageBuilder builder : builders) {
            if (builder.canHandle(contentType)) {
                logger.debug("Found builder {} for content type: {}", 
                        builder.getClass().getName(), contentType);
                return builder;
            }
        }
        
        logger.warn("No builder found for content type: {}, using default builder", contentType);
        return defaultBuilder;
    }
    
    /**
     * Set the default builder to use when no suitable builder is found
     * 
     * @param builder The default builder
     */
    public void setDefaultBuilder(MessageBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Default builder cannot be null");
        }
        
        this.defaultBuilder = builder;
        logger.debug("Set default builder: {}", builder.getClass().getName());
    }
} 