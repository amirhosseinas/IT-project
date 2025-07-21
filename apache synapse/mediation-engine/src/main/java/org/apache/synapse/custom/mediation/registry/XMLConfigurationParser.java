package org.apache.synapse.custom.mediation.registry;

import org.apache.synapse.custom.mediation.Endpoint;
import org.apache.synapse.custom.mediation.MediationSequence;
import org.apache.synapse.custom.mediation.Mediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for XML configuration files used in Apache Synapse.
 * This class handles parsing of sequences, endpoints, proxies, and other configuration elements.
 */
public class XMLConfigurationParser {
    private static final Logger logger = LoggerFactory.getLogger(XMLConfigurationParser.class);
    
    private final DocumentBuilderFactory documentBuilderFactory;
    private final Map<String, MediatorFactory> mediatorFactories;
    private final Map<String, EndpointFactory> endpointFactories;
    
    /**
     * Create a new XML configuration parser
     */
    public XMLConfigurationParser() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
        this.mediatorFactories = new HashMap<>();
        this.endpointFactories = new HashMap<>();
        
        // Initialize factories
        registerDefaultFactories();
    }
    
    /**
     * Register the default mediator and endpoint factories
     */
    private void registerDefaultFactories() {
        // This will be implemented when the mediator factories are created
        logger.info("Registered default factories");
    }
    
    /**
     * Register a mediator factory
     * 
     * @param elementName The XML element name this factory handles
     * @param factory The mediator factory
     */
    public void registerMediatorFactory(String elementName, MediatorFactory factory) {
        mediatorFactories.put(elementName, factory);
        logger.debug("Registered mediator factory for element: {}", elementName);
    }
    
    /**
     * Register an endpoint factory
     * 
     * @param elementName The XML element name this factory handles
     * @param factory The endpoint factory
     */
    public void registerEndpointFactory(String elementName, EndpointFactory factory) {
        endpointFactories.put(elementName, factory);
        logger.debug("Registered endpoint factory for element: {}", elementName);
    }
    
    /**
     * Parse a configuration from an XML string
     * 
     * @param xml The XML string
     * @return Map of parsed configuration objects
     * @throws ConfigurationException if parsing fails
     */
    public Map<String, Object> parseConfiguration(String xml) throws ConfigurationException {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            return parseDocument(document);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConfigurationException("Failed to parse XML configuration", e);
        }
    }
    
    /**
     * Parse a configuration from an input stream
     * 
     * @param inputStream The input stream containing XML
     * @return Map of parsed configuration objects
     * @throws ConfigurationException if parsing fails
     */
    public Map<String, Object> parseConfiguration(InputStream inputStream) throws ConfigurationException {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            return parseDocument(document);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConfigurationException("Failed to parse XML configuration", e);
        }
    }
    
    /**
     * Parse a DOM document into configuration objects
     * 
     * @param document The DOM document
     * @return Map of parsed configuration objects
     * @throws ConfigurationException if parsing fails
     */
    private Map<String, Object> parseDocument(Document document) throws ConfigurationException {
        Map<String, Object> configuration = new HashMap<>();
        Element rootElement = document.getDocumentElement();
        
        // Parse sequences
        NodeList sequenceNodes = rootElement.getElementsByTagName("sequence");
        for (int i = 0; i < sequenceNodes.getLength(); i++) {
            Element sequenceElement = (Element) sequenceNodes.item(i);
            MediationSequence sequence = parseSequence(sequenceElement);
            configuration.put("sequence:" + sequence.getName(), sequence);
        }
        
        // Parse endpoints
        NodeList endpointNodes = rootElement.getElementsByTagName("endpoint");
        for (int i = 0; i < endpointNodes.getLength(); i++) {
            Element endpointElement = (Element) endpointNodes.item(i);
            Endpoint endpoint = parseEndpoint(endpointElement);
            configuration.put("endpoint:" + endpoint.getName(), endpoint);
        }
        
        // Parse proxy services (will be implemented later)
        
        return configuration;
    }
    
    /**
     * Parse a sequence element
     * 
     * @param element The sequence element
     * @return The parsed sequence
     * @throws ConfigurationException if parsing fails
     */
    private MediationSequence parseSequence(Element element) throws ConfigurationException {
        String name = element.getAttribute("name");
        if (name == null || name.isEmpty()) {
            throw new ConfigurationException("Sequence must have a name");
        }
        
        List<Mediator> mediators = new ArrayList<>();
        NodeList childNodes = element.getChildNodes();
        
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element mediatorElement = (Element) node;
                String mediatorType = mediatorElement.getTagName();
                
                MediatorFactory factory = mediatorFactories.get(mediatorType);
                if (factory == null) {
                    throw new ConfigurationException("Unknown mediator type: " + mediatorType);
                }
                
                Mediator mediator = factory.createMediator(mediatorElement);
                mediators.add(mediator);
            }
        }
        
        return new MediationSequence(name, mediators);
    }
    
    /**
     * Parse an endpoint element
     * 
     * @param element The endpoint element
     * @return The parsed endpoint
     * @throws ConfigurationException if parsing fails
     */
    private Endpoint parseEndpoint(Element element) throws ConfigurationException {
        String name = element.getAttribute("name");
        if (name == null || name.isEmpty()) {
            throw new ConfigurationException("Endpoint must have a name");
        }
        
        String type = element.getAttribute("type");
        if (type == null || type.isEmpty()) {
            // Default to address endpoint
            type = "address";
        }
        
        EndpointFactory factory = endpointFactories.get(type);
        if (factory == null) {
            throw new ConfigurationException("Unknown endpoint type: " + type);
        }
        
        return factory.createEndpoint(element);
    }
    
    /**
     * Validate configuration against XML schema
     * 
     * @param xml The XML string to validate
     * @param schemaUrl The URL of the schema
     * @throws ConfigurationException if validation fails
     */
    public void validateConfiguration(String xml, String schemaUrl) throws ConfigurationException {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(getClass().getResource(schemaUrl));
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setSchema(schema);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new ConfigurationException("XML validation failed", e);
        }
    }
    
    /**
     * Factory interface for creating mediators from XML elements
     */
    public interface MediatorFactory {
        /**
         * Create a mediator from an XML element
         * 
         * @param element The XML element
         * @return The created mediator
         * @throws ConfigurationException if creation fails
         */
        Mediator createMediator(Element element) throws ConfigurationException;
    }
    
    /**
     * Factory interface for creating endpoints from XML elements
     */
    public interface EndpointFactory {
        /**
         * Create an endpoint from an XML element
         * 
         * @param element The XML element
         * @return The created endpoint
         * @throws ConfigurationException if creation fails
         */
        Endpoint createEndpoint(Element element) throws ConfigurationException;
    }
    
    /**
     * Exception thrown when configuration parsing fails
     */
    public static class ConfigurationException extends Exception {
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 