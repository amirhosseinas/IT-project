package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;

/**
 * Mediator for logging messages at different levels.
 * Can log different parts of the message (headers, body, etc.)
 */
public class LogMediator extends AbstractMediator {
    
    /**
     * Log levels supported by the mediator
     */
    public enum LogLevel {
        SIMPLE,   // Log minimal information
        HEADERS,  // Log message headers
        FULL,     // Log the entire message
        CUSTOM    // Log custom properties
    }
    
    /**
     * Categories for log output
     */
    public enum Category {
        INFO,
        DEBUG,
        WARN,
        ERROR,
        TRACE
    }
    
    private LogLevel level;
    private Category category;
    private String separator;
    private String[] properties;
    
    /**
     * Create a new log mediator
     * 
     * @param name The mediator name
     */
    public LogMediator(String name) {
        super(name);
        this.level = LogLevel.SIMPLE;
        this.category = Category.INFO;
        this.separator = ", ";
    }
    
    /**
     * Create a new log mediator with the specified level and category
     * 
     * @param name The mediator name
     * @param level The log level
     * @param category The log category
     */
    public LogMediator(String name, LogLevel level, Category category) {
        super(name);
        this.level = level;
        this.category = category;
        this.separator = ", ";
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        String logMessage = buildLogMessage(message);
        
        switch (category) {
            case INFO:
                logger.info(logMessage);
                break;
            case DEBUG:
                logger.debug(logMessage);
                break;
            case WARN:
                logger.warn(logMessage);
                break;
            case ERROR:
                logger.error(logMessage);
                break;
            case TRACE:
                logger.trace(logMessage);
                break;
        }
        
        // Log mediator doesn't modify the message
        return message;
    }
    
    /**
     * Build the log message based on the log level
     * 
     * @param message The message to log
     * @return The formatted log message
     */
    private String buildLogMessage(Message message) {
        StringBuilder builder = new StringBuilder();
        builder.append("Message ID: ").append(message.getMessageId());
        
        switch (level) {
            case SIMPLE:
                // Just log message ID and direction
                builder.append(", Direction: ").append(message.getDirection());
                break;
                
            case HEADERS:
                // Log headers
                builder.append(", Headers: [");
                message.getHeaders().forEach((key, value) -> 
                    builder.append(key).append("=").append(value).append(separator)
                );
                if (!message.getHeaders().isEmpty()) {
                    builder.delete(builder.length() - separator.length(), builder.length());
                }
                builder.append("]");
                break;
                
            case FULL:
                // Log everything
                builder.append(", Direction: ").append(message.getDirection());
                builder.append(", Content-Type: ").append(message.getContentType());
                
                // Headers
                builder.append(", Headers: [");
                message.getHeaders().forEach((key, value) -> 
                    builder.append(key).append("=").append(value).append(separator)
                );
                if (!message.getHeaders().isEmpty()) {
                    builder.delete(builder.length() - separator.length(), builder.length());
                }
                builder.append("]");
                
                // Properties
                builder.append(", Properties: [");
                message.getProperties().forEach((key, value) -> 
                    builder.append(key).append("=").append(value).append(separator)
                );
                if (!message.getProperties().isEmpty()) {
                    builder.delete(builder.length() - separator.length(), builder.length());
                }
                builder.append("]");
                
                // Payload (try to convert to string if possible)
                try {
                    String payload = MessageBuilderUtil.getStringFromMessage(message);
                    if (payload != null) {
                        builder.append(", Payload: ").append(payload);
                    }
                } catch (Exception e) {
                    builder.append(", Payload: [Binary content]");
                }
                break;
                
            case CUSTOM:
                // Log custom properties
                if (properties != null && properties.length > 0) {
                    builder.append(", Custom Properties: [");
                    for (String property : properties) {
                        Object value = message.getProperty(property);
                        if (value != null) {
                            builder.append(property).append("=").append(value).append(separator);
                        }
                    }
                    builder.delete(builder.length() - separator.length(), builder.length());
                    builder.append("]");
                }
                break;
        }
        
        return builder.toString();
    }
    
    /**
     * Get the log level
     * 
     * @return The log level
     */
    public LogLevel getLevel() {
        return level;
    }
    
    /**
     * Set the log level
     * 
     * @param level The log level
     */
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    /**
     * Get the log category
     * 
     * @return The log category
     */
    public Category getCategory() {
        return category;
    }
    
    /**
     * Set the log category
     * 
     * @param category The log category
     */
    public void setCategory(Category category) {
        this.category = category;
    }
    
    /**
     * Get the separator used for lists
     * 
     * @return The separator
     */
    public String getSeparator() {
        return separator;
    }
    
    /**
     * Set the separator used for lists
     * 
     * @param separator The separator
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }
    
    /**
     * Get the custom properties to log
     * 
     * @return Array of property names
     */
    public String[] getProperties() {
        return properties;
    }
    
    /**
     * Set the custom properties to log
     * 
     * @param properties Array of property names
     */
    public void setProperties(String[] properties) {
        this.properties = properties;
    }
} 