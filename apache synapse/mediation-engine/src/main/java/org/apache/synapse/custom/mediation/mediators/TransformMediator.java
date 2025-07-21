package org.apache.synapse.custom.mediation.mediators;

import org.apache.synapse.custom.mediation.AbstractMediator;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;
import org.apache.synapse.custom.message.builder.XmlMessageBuilder;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Mediator for applying XSLT transformations to XML messages.
 */
public class TransformMediator extends AbstractMediator {
    
    private String xsltKey;
    private String xsltContent;
    private final Map<String, Object> parameters;
    private String sourceXPath;
    private String targetProperty;
    
    /**
     * Create a new transform mediator
     * 
     * @param name The mediator name
     */
    public TransformMediator(String name) {
        super(name);
        this.parameters = new HashMap<>();
    }
    
    /**
     * Create a new transform mediator with the specified XSLT key
     * 
     * @param name The mediator name
     * @param xsltKey The XSLT key in the registry
     */
    public TransformMediator(String name, String xsltKey) {
        super(name);
        this.xsltKey = xsltKey;
        this.parameters = new HashMap<>();
    }
    
    /**
     * Create a new transform mediator with the specified XSLT content
     * 
     * @param name The mediator name
     * @param xsltContent The XSLT content
     * @param isContent true to indicate this is XSLT content, not a key
     */
    public TransformMediator(String name, String xsltContent, boolean isContent) {
        super(name);
        if (isContent) {
            this.xsltContent = xsltContent;
        } else {
            this.xsltKey = xsltContent;
        }
        this.parameters = new HashMap<>();
    }
    
    @Override
    protected Message doMediate(Message message) throws Exception {
        if (message.getContentType() == null || !message.getContentType().contains("xml")) {
            logger.warn("Message is not XML, skipping XSLT transformation");
            return message;
        }
        
        String xslt = resolveXslt(message);
        if (xslt == null || xslt.isEmpty()) {
            throw new MediationEngine.MediationException("XSLT not found: " + xsltKey);
        }
        
        try {
            // Get the XML content from the message
            String xml;
            if (sourceXPath != null && !sourceXPath.isEmpty()) {
                // Extract XML using XPath (in a real implementation)
                // For now, we'll just use the whole message
                xml = MessageBuilderUtil.getStringFromMessage(message);
            } else {
                xml = MessageBuilderUtil.getStringFromMessage(message);
            }
            
            if (xml == null || xml.isEmpty()) {
                logger.warn("No XML content found in message");
                return message;
            }
            
            // Perform the transformation
            String transformedXml = transform(xml, xslt);
            
            if (targetProperty != null && !targetProperty.isEmpty()) {
                // Store the result in a property
                message.setProperty(targetProperty, transformedXml);
                return message;
            } else {
                // Replace the message payload
                message.setPayload(transformedXml.getBytes(StandardCharsets.UTF_8));
                message.setContentType("application/xml");
                return message;
            }
        } catch (Exception e) {
            logger.error("Error transforming message", e);
            throw new MediationEngine.MediationException("Error transforming message", e);
        }
    }
    
    /**
     * Resolve the XSLT to use
     * 
     * @param message The message
     * @return The XSLT content
     */
    private String resolveXslt(Message message) {
        if (xsltContent != null && !xsltContent.isEmpty()) {
            return xsltContent;
        }
        
        if (xsltKey != null && !xsltKey.isEmpty()) {
            // In a real implementation, we would get the XSLT from the registry
            // For now, we'll just check if it's in the message properties
            Object xsltObj = message.getProperty(xsltKey);
            if (xsltObj instanceof String) {
                return (String) xsltObj;
            }
        }
        
        return null;
    }
    
    /**
     * Apply an XSLT transformation to XML content
     * 
     * @param xml The XML content
     * @param xslt The XSLT content
     * @return The transformed XML
     * @throws Exception if transformation fails
     */
    private String transform(String xml, String xslt) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xsltSource = new StreamSource(new StringReader(xslt));
        Transformer transformer = factory.newTransformer(xsltSource);
        
        // Set parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            transformer.setParameter(entry.getKey(), entry.getValue());
        }
        
        Source xmlSource = new StreamSource(new StringReader(xml));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(outputStream);
        
        transformer.transform(xmlSource, result);
        
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
    
    /**
     * Get the XSLT key
     * 
     * @return The XSLT key
     */
    public String getXsltKey() {
        return xsltKey;
    }
    
    /**
     * Set the XSLT key
     * 
     * @param xsltKey The XSLT key
     */
    public void setXsltKey(String xsltKey) {
        this.xsltKey = xsltKey;
    }
    
    /**
     * Get the XSLT content
     * 
     * @return The XSLT content
     */
    public String getXsltContent() {
        return xsltContent;
    }
    
    /**
     * Set the XSLT content
     * 
     * @param xsltContent The XSLT content
     */
    public void setXsltContent(String xsltContent) {
        this.xsltContent = xsltContent;
    }
    
    /**
     * Add a parameter for the XSLT transformation
     * 
     * @param name The parameter name
     * @param value The parameter value
     */
    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }
    
    /**
     * Get the parameters for the XSLT transformation
     * 
     * @return Map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Get the source XPath
     * 
     * @return The source XPath
     */
    public String getSourceXPath() {
        return sourceXPath;
    }
    
    /**
     * Set the source XPath
     * 
     * @param sourceXPath The source XPath
     */
    public void setSourceXPath(String sourceXPath) {
        this.sourceXPath = sourceXPath;
    }
    
    /**
     * Get the target property
     * 
     * @return The target property
     */
    public String getTargetProperty() {
        return targetProperty;
    }
    
    /**
     * Set the target property
     * 
     * @param targetProperty The target property
     */
    public void setTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
    }
} 