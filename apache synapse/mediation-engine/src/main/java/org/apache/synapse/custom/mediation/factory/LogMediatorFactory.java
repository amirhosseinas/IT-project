package org.apache.synapse.custom.mediation.factory;

import org.apache.synapse.custom.mediation.Mediator;
import org.apache.synapse.custom.mediation.mediators.LogMediator;
import org.apache.synapse.custom.mediation.registry.XMLConfigurationParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Factory for creating LogMediator instances from XML configuration.
 */
public class LogMediatorFactory implements MediatorFactory {
    
    private static final String TAG_NAME = "log";
    
    @Override
    public Mediator createMediator(Element element) throws XMLConfigurationParser.ConfigurationException {
        String name = element.getAttribute("name");
        if (name == null || name.isEmpty()) {
            name = "log-" + System.currentTimeMillis();
        }
        
        // Create the mediator
        LogMediator logMediator = new LogMediator(name);
        
        // Parse level
        String level = element.getAttribute("level");
        if (level != null && !level.isEmpty()) {
            try {
                LogMediator.LogLevel logLevel = LogMediator.LogLevel.valueOf(level.toUpperCase());
                logMediator.setLevel(logLevel);
            } catch (IllegalArgumentException e) {
                throw new XMLConfigurationParser.ConfigurationException("Invalid log level: " + level);
            }
        }
        
        // Parse category
        String category = element.getAttribute("category");
        if (category != null && !category.isEmpty()) {
            try {
                LogMediator.Category logCategory = LogMediator.Category.valueOf(category.toUpperCase());
                logMediator.setCategory(logCategory);
            } catch (IllegalArgumentException e) {
                throw new XMLConfigurationParser.ConfigurationException("Invalid log category: " + category);
            }
        }
        
        // Parse separator
        String separator = element.getAttribute("separator");
        if (separator != null && !separator.isEmpty()) {
            logMediator.setSeparator(separator);
        }
        
        // Parse properties
        NodeList propertyNodes = element.getElementsByTagName("property");
        if (propertyNodes.getLength() > 0) {
            String[] properties = new String[propertyNodes.getLength()];
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Node propertyNode = propertyNodes.item(i);
                if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element propertyElement = (Element) propertyNode;
                    properties[i] = propertyElement.getAttribute("name");
                }
            }
            logMediator.setProperties(properties);
        }
        
        return logMediator;
    }
    
    @Override
    public String getTagQName() {
        return TAG_NAME;
    }
} 