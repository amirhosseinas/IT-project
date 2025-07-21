package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.Mediator;
import org.apache.synapse.custom.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Mediator for multi-branch conditional routing.
 * Similar to a switch statement in programming languages.
 */
public class SwitchMediator extends AbstractMediator {
    
    private String source;
    private String xpath;
    private final List<Case> cases;
    private final List<Mediator> defaultMediators;
    
    /**
     * Create a new switch mediator
     * 
     * @param name The mediator name
     */
    public SwitchMediator(String name) {
        super(name);
        this.cases = new ArrayList<>();
        this.defaultMediators = new ArrayList<>();
    }
    
    /**
     * Create a new switch mediator with the specified source
     * 
     * @param name The mediator name
     * @param source The source to switch on
     */
    public SwitchMediator(String name, String source) {
        super(name);
        this.source = source;
        this.cases = new ArrayList<>();
        this.defaultMediators = new ArrayList<>();
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        Object sourceValue = resolveSource(message);
        
        if (sourceValue != null) {
            // Try to match a case
            for (Case caseObj : cases) {
                if (caseObj.matches(sourceValue)) {
                    logger.debug("Switch case matched: {}", caseObj.getValue());
                    return applyMediators(caseObj.getMediators(), message);
                }
            }
        }
        
        // No case matched, use default
        logger.debug("No switch case matched, using default");
        return applyMediators(defaultMediators, message);
    }
    
    /**
     * Resolve the source value from the message
     * 
     * @param message The message
     * @return The source value
     */
    private Object resolveSource(Message message) {
        if (source != null) {
            if (source.startsWith("$header.")) {
                // Header value
                String headerName = source.substring("$header.".length());
                return message.getHeader(headerName);
            } else if (source.startsWith("$property.")) {
                // Property value
                String propertyName = source.substring("$property.".length());
                return message.getProperty(propertyName);
            }
        }
        
        if (xpath != null) {
            // In a real implementation, we would evaluate the XPath expression
            // For now, we'll just return null
            return null;
        }
        
        return null;
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
     * Add a case to the switch
     * 
     * @param caseObj The case to add
     */
    public void addCase(Case caseObj) {
        if (caseObj == null) {
            throw new IllegalArgumentException("Case cannot be null");
        }
        
        cases.add(caseObj);
    }
    
    /**
     * Add a mediator to the default case
     * 
     * @param mediator The mediator to add
     */
    public void addDefaultMediator(Mediator mediator) {
        if (mediator == null) {
            throw new IllegalArgumentException("Mediator cannot be null");
        }
        
        defaultMediators.add(mediator);
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
     * Get the cases
     * 
     * @return List of cases
     */
    public List<Case> getCases() {
        return cases;
    }
    
    /**
     * Get the default mediators
     * 
     * @return List of mediators
     */
    public List<Mediator> getDefaultMediators() {
        return defaultMediators;
    }
    
    /**
     * Case class for the switch mediator
     */
    public static class Case {
        private final Object value;
        private final boolean regex;
        private final Predicate<Object> predicate;
        private final List<Mediator> mediators;
        
        /**
         * Create a new case with the specified value
         * 
         * @param value The case value
         */
        public Case(Object value) {
            this.value = value;
            this.regex = false;
            this.predicate = null;
            this.mediators = new ArrayList<>();
        }
        
        /**
         * Create a new case with the specified regex pattern
         * 
         * @param pattern The regex pattern
         * @param regex true to indicate this is a regex pattern
         */
        public Case(String pattern, boolean regex) {
            this.value = pattern;
            this.regex = regex;
            this.predicate = null;
            this.mediators = new ArrayList<>();
        }
        
        /**
         * Create a new case with the specified predicate
         * 
         * @param predicate The predicate to match
         */
        public Case(Predicate<Object> predicate) {
            this.value = null;
            this.regex = false;
            this.predicate = predicate;
            this.mediators = new ArrayList<>();
        }
        
        /**
         * Check if this case matches the specified value
         * 
         * @param testValue The value to test
         * @return true if the case matches, false otherwise
         */
        public boolean matches(Object testValue) {
            if (predicate != null) {
                return predicate.test(testValue);
            }
            
            if (testValue == null) {
                return value == null;
            }
            
            if (regex && value instanceof String && testValue instanceof String) {
                return ((String) testValue).matches((String) value);
            }
            
            return testValue.equals(value);
        }
        
        /**
         * Add a mediator to this case
         * 
         * @param mediator The mediator to add
         */
        public void addMediator(Mediator mediator) {
            if (mediator == null) {
                throw new IllegalArgumentException("Mediator cannot be null");
            }
            
            mediators.add(mediator);
        }
        
        /**
         * Get the case value
         * 
         * @return The case value
         */
        public Object getValue() {
            return value;
        }
        
        /**
         * Check if this is a regex case
         * 
         * @return true if this is a regex case, false otherwise
         */
        public boolean isRegex() {
            return regex;
        }
        
        /**
         * Get the mediators for this case
         * 
         * @return List of mediators
         */
        public List<Mediator> getMediators() {
            return mediators;
        }
    }
} 