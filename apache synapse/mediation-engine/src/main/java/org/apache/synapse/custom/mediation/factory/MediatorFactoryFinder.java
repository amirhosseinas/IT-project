package org.apache.synapse.custom.mediation.factory;

import org.apache.synapse.custom.mediation.Mediator;
import org.apache.synapse.custom.mediation.registry.XMLConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry for mediator factories.
 * This class is responsible for finding and registering mediator factories.
 */
public class MediatorFactoryFinder {
    private static final Logger logger = LoggerFactory.getLogger(MediatorFactoryFinder.class);
    
    private final Map<String, MediatorFactory> factories;
    
    /**
     * Create a new mediator factory finder
     */
    public MediatorFactoryFinder() {
        this.factories = new HashMap<>();
        registerDefaultFactories();
    }
    
    /**
     * Register the default mediator factories
     */
    private void registerDefaultFactories() {
        // Register built-in factories
        registerFactory(new LogMediatorFactory());
        
        // Load factories using ServiceLoader
        ServiceLoader<MediatorFactory> serviceLoader = ServiceLoader.load(MediatorFactory.class);
        for (MediatorFactory factory : serviceLoader) {
            registerFactory(factory);
        }
    }
    
    /**
     * Register a mediator factory
     * 
     * @param factory The factory to register
     */
    public void registerFactory(MediatorFactory factory) {
        if (factory == null) {
            return;
        }
        
        String tagName = factory.getTagQName();
        if (tagName == null || tagName.isEmpty()) {
            logger.warn("Factory {} has no tag name, ignoring", factory.getClass().getName());
            return;
        }
        
        factories.put(tagName, factory);
        logger.debug("Registered mediator factory for tag: {}", tagName);
    }
    
    /**
     * Get a factory for the specified tag name
     * 
     * @param tagName The tag name
     * @return The factory, or null if not found
     */
    public MediatorFactory getFactory(String tagName) {
        return factories.get(tagName);
    }
    
    /**
     * Create a mediator from an XML element
     * 
     * @param element The XML element
     * @return The created mediator
     * @throws XMLConfigurationParser.ConfigurationException if creation fails
     */
    public Mediator createMediator(Element element) throws XMLConfigurationParser.ConfigurationException {
        String tagName = element.getTagName();
        MediatorFactory factory = getFactory(tagName);
        
        if (factory == null) {
            throw new XMLConfigurationParser.ConfigurationException("No factory found for tag: " + tagName);
        }
        
        return factory.createMediator(element);
    }
    
    /**
     * Get all registered factories
     * 
     * @return Map of tag names to factories
     */
    public Map<String, MediatorFactory> getFactories() {
        return factories;
    }
} 