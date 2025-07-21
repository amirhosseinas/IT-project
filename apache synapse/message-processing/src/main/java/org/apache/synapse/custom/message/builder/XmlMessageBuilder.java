package org.apache.synapse.custom.message.builder;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Message builder implementation for XML and SOAP content.
 * Uses Apache AXIOM for XML processing.
 */
public class XmlMessageBuilder implements MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(XmlMessageBuilder.class);
    
    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/xml",
            "text/xml",
            "application/soap+xml",
            "application/xhtml+xml"
    ));
    
    private static final Set<String> SOAP_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/soap+xml"
    ));
    
    @Override
    public Message buildMessage(InputStream inputStream, String contentType) throws MessageBuilderException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        
        try {
            // Read the input stream into a byte array
            byte[] content = IOUtils.toByteArray(inputStream);
            
            // Create a new message
            Message message = new Message(UUID.randomUUID().toString());
            message.setContentType(contentType);
            message.setPayload(content);
            
            // Determine if this is a SOAP message
            boolean isSoap = isSoapContentType(contentType);
            message.setProperty("xml.isSoap", isSoap);
            
            // Process XML content
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
            
            if (isSoap) {
                // Process as SOAP
                SOAPModelBuilder builder = OMXMLBuilderFactory.createSOAPModelBuilder(byteArrayInputStream, null);
                SOAPEnvelope envelope = builder.getSOAPEnvelope();
                
                // Extract SOAP information
                String soapVersion = envelope.getVersion().getNamespaceURI();
                message.setProperty("soap.version", soapVersion);
                
                // Check for SOAP Body and Header
                if (envelope.getBody() != null) {
                    message.setProperty("soap.hasBody", true);
                    
                    // Count payload elements in body
                    int payloadElementCount = 0;
                    for (Object child : envelope.getBody().getChildElements()) {
                        if (child instanceof OMElement) {
                            payloadElementCount++;
                        }
                    }
                    message.setProperty("soap.bodyElementCount", payloadElementCount);
                }
                
                if (envelope.getHeader() != null) {
                    message.setProperty("soap.hasHeader", true);
                    message.setProperty("soap.headerBlockCount", envelope.getHeader().getChildElements().hasNext() ? 1 : 0);
                }
            } else {
                // Process as plain XML
                OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(byteArrayInputStream);
                OMElement documentElement = builder.getDocumentElement();
                
                // Extract XML information
                QName rootQName = documentElement.getQName();
                message.setProperty("xml.rootElement", rootQName.getLocalPart());
                
                if (rootQName.getNamespaceURI() != null && !rootQName.getNamespaceURI().isEmpty()) {
                    message.setProperty("xml.namespace", rootQName.getNamespaceURI());
                }
                
                // Count child elements
                int childElementCount = 0;
                for (Object child : documentElement.getChildElements()) {
                    if (child instanceof OMElement) {
                        childElementCount++;
                    }
                }
                message.setProperty("xml.childElementCount", childElementCount);
            }
            
            logger.debug("Successfully built XML message with ID: {}", message.getMessageId());
            return message;
        } catch (IOException e) {
            logger.error("Failed to build XML message", e);
            throw new MessageBuilderException("Failed to build XML message: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to process XML content", e);
            throw new MessageBuilderException("Failed to process XML content: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canHandle(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Handle content types with parameters (e.g., text/xml; charset=utf-8)
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
    
    private boolean isSoapContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        
        // Check for explicit SOAP content types
        if (SOAP_CONTENT_TYPES.contains(baseContentType)) {
            return true;
        }
        
        // Check for action parameter indicating SOAP
        return contentType.toLowerCase().contains("action=") && 
               (baseContentType.equals("application/xml") || baseContentType.equals("text/xml"));
    }
} 