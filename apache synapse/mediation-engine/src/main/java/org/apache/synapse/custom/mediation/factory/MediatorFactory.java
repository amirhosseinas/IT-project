package org.apache.synapse.custom.mediation.factory;

import org.apache.synapse.custom.mediation.Mediator;
import org.apache.synapse.custom.mediation.registry.XMLConfigurationParser;
import org.w3c.dom.Element;

/**
 * Factory interface for creating mediators from XML elements.
 */
public interface MediatorFactory {
    
    /**
     * Create a mediator from an XML element
     * 
     * @param element The XML element
     * @return The created mediator
     * @throws XMLConfigurationParser.ConfigurationException if creation fails
     */
    Mediator createMediator(Element element) throws XMLConfigurationParser.ConfigurationException;
    
    /**
     * Get the tag QName that this factory handles
     * 
     * @return The tag QName
     */
    String getTagQName();
} 