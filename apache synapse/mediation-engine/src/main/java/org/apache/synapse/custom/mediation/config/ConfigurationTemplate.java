package org.apache.synapse.custom.mediation.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a configuration template that can be used to generate configuration
 * files with variable substitution.
 */
public class ConfigurationTemplate {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    private final String templateName;
    private final String templateContent;
    private final Map<String, String> defaultValues;
    
    /**
     * Create a new configuration template
     * 
     * @param templateName The name of the template
     * @param templateContent The content of the template
     */
    public ConfigurationTemplate(String templateName, String templateContent) {
        this.templateName = templateName;
        this.templateContent = templateContent;
        this.defaultValues = new HashMap<>();
    }
    
    /**
     * Create a new configuration template from an input stream
     * 
     * @param templateName The name of the template
     * @param inputStream The input stream containing the template
     * @throws IOException if reading fails
     */
    public ConfigurationTemplate(String templateName, InputStream inputStream) throws IOException {
        this.templateName = templateName;
        this.templateContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        this.defaultValues = new HashMap<>();
    }
    
    /**
     * Get the template name
     * 
     * @return The template name
     */
    public String getTemplateName() {
        return templateName;
    }
    
    /**
     * Get the template content
     * 
     * @return The template content
     */
    public String getTemplateContent() {
        return templateContent;
    }
    
    /**
     * Set a default value for a variable
     * 
     * @param name The variable name
     * @param value The default value
     * @return This template for chaining
     */
    public ConfigurationTemplate setDefaultValue(String name, String value) {
        defaultValues.put(name, value);
        return this;
    }
    
    /**
     * Set default values for variables
     * 
     * @param values Map of variable names to default values
     * @return This template for chaining
     */
    public ConfigurationTemplate setDefaultValues(Map<String, String> values) {
        defaultValues.putAll(values);
        return this;
    }
    
    /**
     * Get all default values
     * 
     * @return Map of variable names to default values
     */
    public Map<String, String> getDefaultValues() {
        return new HashMap<>(defaultValues);
    }
    
    /**
     * Generate a configuration from this template with the provided values
     * 
     * @param values Map of variable names to values
     * @return The generated configuration
     */
    public String generate(Map<String, String> values) {
        String result = templateContent;
        
        // Create a combined map of values with defaults
        Map<String, String> allValues = new HashMap<>(defaultValues);
        if (values != null) {
            allValues.putAll(values);
        }
        
        // Replace all variables
        Matcher matcher = VARIABLE_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = allValues.getOrDefault(variableName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Extract all variable names from the template
     * 
     * @return Array of variable names
     */
    public String[] extractVariableNames() {
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        StringWriter writer = new StringWriter();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            writer.write(variableName);
            writer.write('\n');
        }
        
        return writer.toString().split("\n");
    }
    
    /**
     * Check if all required variables have values
     * 
     * @param values Map of variable names to values
     * @return true if all required variables have values, false otherwise
     */
    public boolean validateRequiredVariables(Map<String, String> values) {
        // Create a combined map of values with defaults
        Map<String, String> allValues = new HashMap<>(defaultValues);
        if (values != null) {
            allValues.putAll(values);
        }
        
        // Check if all variables have values
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!allValues.containsKey(variableName)) {
                return false;
            }
        }
        
        return true;
    }
} 