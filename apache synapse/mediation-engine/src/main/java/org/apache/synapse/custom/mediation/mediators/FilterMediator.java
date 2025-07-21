package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.Mediator;
import org.apache.synapse.custom.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Mediator for conditional message routing.
 * Applies a list of child mediators only if a condition is met.
 */
public class FilterMediator extends AbstractMediator {
    
    private Predicate<Message> condition;
    private String source;
    private String pattern;
    private String xpath;
    private boolean regex;
    private final List<Mediator> thenMediators;
    private final List<Mediator> elseMediators;
    
    /**
     * Create a new filter mediator
     * 
     * @param name The mediator name
     */
    public FilterMediator(String name) {
        super(name);
        this.thenMediators = new ArrayList<>();
        this.elseMediators = new ArrayList<>();
        this.regex = false;
    }
    
    /**
     * Create a new filter mediator with the specified condition
     * 
     * @param name The mediator name
     * @param condition The condition predicate
     */
    public FilterMediator(String name, Predicate<Message> condition) {
        super(name);
        this.condition = condition;
        this.thenMediators = new ArrayList<>();
        this.elseMediators = new ArrayList<>();
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        boolean conditionResult = evaluateCondition(message);
        
        if (conditionResult) {
            logger.debug("Filter condition evaluated to true, executing 'then' branch");
            return applyMediators(thenMediators, message);
        } else {
            logger.debug("Filter condition evaluated to false, executing 'else' branch");
            return applyMediators(elseMediators, message);
        }
    }
    
    /**
     * Evaluate the filter condition
     * 
     * @param message The message
     * @return true if the condition is met, false otherwise
     */
    private boolean evaluateCondition(Message message) {
        if (condition != null) {
            return condition.test(message);
        }
        
        if (source != null && pattern != null) {
            // Get the source value from the message
            Object sourceValue = null;
            
            if (source.startsWith("$header.")) {
                // Header value
                String headerName = source.substring("$header.".length());
                sourceValue = message.getHeader(headerName);
            } else if (source.startsWith("$property.")) {
                // Property value
                String propertyName = source.substring("$property.".length());
                sourceValue = message.getProperty(propertyName);
            }
            
            if (sourceValue != null) {
                String sourceStr = sourceValue.toString();
                
                if (regex) {
                    // Regex matching
                    return sourceStr.matches(pattern);
                } else {
                    // Simple string comparison
                    return sourceStr.equals(pattern);
                }
            }
        }
        
        if (xpath != null) {
            // In a real implementation, we would evaluate the XPath expression
            // For now, we'll just return false
            return false;
        }
        
        // Default to false if no condition is specified
        return false;
    }
    
    /**
     * Apply a list of mediators to a message
     * 
     * @param mediators The mediators to apply
     * @param message The message
     * @return The mediated message
     * @throws MediationEngine.MediationException if mediation fails
     */
    private Message applyMediators(List<Mediator> mediators, Message message) throws MediationEngine.MediationException {
        Message result = message;
        
        for (Mediator mediator : mediators) {
            result = mediator.mediate(result);
            
            // Check if flow has been stopped
            if (Boolean.TRUE.equals(result.getProperty("STOP_FLOW"))) {
                logger.debug("Flow stopped by mediator: {}", mediator.getName());
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Add a mediator to the 'then' branch
     * 
     * @param mediator The mediator to add
     */
    public void addThenMediator(Mediator mediator) {
        if (mediator == null) {
            throw new IllegalArgumentException("Mediator cannot be null");
        }
        
        thenMediators.add(mediator);
    }
    
    /**
     * Add a mediator to the 'else' branch
     * 
     * @param mediator The mediator to add
     */
    public void addElseMediator(Mediator mediator) {
        if (mediator == null) {
            throw new IllegalArgumentException("Mediator cannot be null");
        }
        
        elseMediators.add(mediator);
    }
    
    /**
     * Get the 'then' mediators
     * 
     * @return List of mediators
     */
    public List<Mediator> getThenMediators() {
        return thenMediators;
    }
    
    /**
     * Get the 'else' mediators
     * 
     * @return List of mediators
     */
    public List<Mediator> getElseMediators() {
        return elseMediators;
    }
    
    /**
     * Set the condition predicate
     * 
     * @param condition The condition predicate
     */
    public void setCondition(Predicate<Message> condition) {
        this.condition = condition;
    }
    
    /**
     * Get the source
     * 
     * @return The source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Set the source
     * 
     * @param source The source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Get the pattern
     * 
     * @return The pattern
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * Set the pattern
     * 
     * @param pattern The pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    /**
     * Get the XPath expression
     * 
     * @return The XPath expression
     */
    public String getXpath() {
        return xpath;
    }
    
    /**
     * Set the XPath expression
     * 
     * @param xpath The XPath expression
     */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }
    
    /**
     * Check if regex matching is enabled
     * 
     * @return true if regex matching is enabled, false otherwise
     */
    public boolean isRegex() {
        return regex;
    }
    
    /**
     * Set whether regex matching is enabled
     * 
     * @param regex true to enable regex matching, false otherwise
     */
    public void setRegex(boolean regex) {
        this.regex = regex;
    }
} 