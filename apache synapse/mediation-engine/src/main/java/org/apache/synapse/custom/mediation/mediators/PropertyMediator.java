package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.message.Message;

/**
 * Mediator for setting or removing properties on messages.
 * Properties can be set from literal values, XPath expressions, or other properties.
 */
public class PropertyMediator extends AbstractMediator {
    
    /**
     * Action to perform on the property
     */
    public enum Action {
        SET,    // Set a property value
        REMOVE  // Remove a property
    }
    
    /**
     * Scope of the property
     */
    public enum Scope {
        DEFAULT,  // Message scope (default)
        AXIS2,    // Axis2 message context scope
        TRANSPORT // Transport headers scope
    }
    
    private String propertyName;
    private Object propertyValue;
    private Action action;
    private Scope scope;
    private String expression;
    private String type;
    
    /**
     * Create a new property mediator
     * 
     * @param name The mediator name
     */
    public PropertyMediator(String name) {
        super(name);
        this.action = Action.SET;
        this.scope = Scope.DEFAULT;
    }
    
    /**
     * Create a new property mediator with the specified property name and value
     * 
     * @param name The mediator name
     * @param propertyName The property name
     * @param propertyValue The property value
     */
    public PropertyMediator(String name, String propertyName, Object propertyValue) {
        super(name);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.action = Action.SET;
        this.scope = Scope.DEFAULT;
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        if (propertyName == null || propertyName.isEmpty()) {
            logger.warn("Property name is null or empty, skipping property mediator");
            return message;
        }
        
        switch (action) {
            case SET:
                if (scope == Scope.DEFAULT) {
                    // Set property on message
                    Object value = resolveValue(message);
                    message.setProperty(propertyName, value);
                    logger.debug("Set property '{}' to '{}'", propertyName, value);
                } else if (scope == Scope.TRANSPORT) {
                    // Set as transport header
                    String value = resolveValue(message).toString();
                    message.setHeader(propertyName, value);
                    logger.debug("Set transport header '{}' to '{}'", propertyName, value);
                }
                break;
                
            case REMOVE:
                if (scope == Scope.DEFAULT) {
                    // Remove property from message
                    Object removed = message.getProperty(propertyName);
                    if (removed != null) {
                        // In a real implementation, we would need to actually remove the property
                        // For now, we'll just set it to null
                        message.setProperty(propertyName, null);
                        logger.debug("Removed property '{}'", propertyName);
                    }
                } else if (scope == Scope.TRANSPORT) {
                    // Remove transport header
                    String removed = message.getHeader(propertyName);
                    if (removed != null) {
                        // In a real implementation, we would need to actually remove the header
                        // For now, we'll just set it to null or empty string
                        message.setHeader(propertyName, "");
                        logger.debug("Removed transport header '{}'", propertyName);
                    }
                }
                break;
        }
        
        return message;
    }
    
    /**
     * Resolve the property value
     * 
     * @param message The message
     * @return The resolved value
     */
    private Object resolveValue(Message message) {
        if (expression != null && !expression.isEmpty()) {
            // In a real implementation, we would evaluate the XPath expression
            // For now, we'll just return the expression as a string
            return "Expression: " + expression;
        } else {
            return propertyValue;
        }
    }
    
    /**
     * Get the property name
     * 
     * @return The property name
     */
    public String getPropertyName() {
        return propertyName;
    }
    
    /**
     * Set the property name
     * 
     * @param propertyName The property name
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    
    /**
     * Get the property value
     * 
     * @return The property value
     */
    public Object getPropertyValue() {
        return propertyValue;
    }
    
    /**
     * Set the property value
     * 
     * @param propertyValue The property value
     */
    public void setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
    }
    
    /**
     * Get the action
     * 
     * @return The action
     */
    public Action getAction() {
        return action;
    }
    
    /**
     * Set the action
     * 
     * @param action The action
     */
    public void setAction(Action action) {
        this.action = action;
    }
    
    /**
     * Get the scope
     * 
     * @return The scope
     */
    public Scope getScope() {
        return scope;
    }
    
    /**
     * Set the scope
     * 
     * @param scope The scope
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }
    
    /**
     * Get the expression
     * 
     * @return The expression
     */
    public String getExpression() {
        return expression;
    }
    
    /**
     * Set the expression
     * 
     * @param expression The expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }
    
    /**
     * Get the type
     * 
     * @return The type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the type
     * 
     * @param type The type
     */
    public void setType(String type) {
        this.type = type;
    }
} 