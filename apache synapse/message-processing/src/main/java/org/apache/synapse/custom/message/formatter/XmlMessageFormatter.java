package org.apache.synapse.custom.message.formatter;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Message formatter implementation for XML and SOAP content.
 * Uses Apache AXIOM for XML processing.
 */
public class XmlMessageFormatter implements MessageFormatter {
    private static final Logger logger = LoggerFactory.getLogger(XmlMessageFormatter.class);
    
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
    public void formatMessage(Message message, OutputStream outputStream) throws MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        
        try {
            byte[] formattedContent = formatMessage(message);
            outputStream.write(formattedContent);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Failed to write formatted XML to output stream", e);
            throw new MessageFormatterException("Failed to write formatted XML to output stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] formatMessage(Message message) throws MessageFormatterException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            byte[] payload = message.getPayload();
            if (payload == null || payload.length == 0) {
                logger.warn("Message payload is empty, returning empty XML document");
                return "<root/>".getBytes(StandardCharsets.UTF_8);
            }
            
            // Determine if this is a SOAP message
            boolean isSoap = isSoapContentType(message.getContentType());
            
            // Format the XML with proper indentation
            return formatXml(payload, isSoap);
        } catch (Exception e) {
            logger.error("Failed to format XML message", e);
            throw new MessageFormatterException("Failed to format XML message: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canFormat(Message message) {
        if (message == null || message.getContentType() == null) {
            return false;
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_CONTENT_TYPES.contains(baseContentType);
    }
    
    @Override
    public String getContentType(Message message) {
        if (message == null || message.getContentType() == null) {
            return "application/xml";
        }
        
        String contentType = message.getContentType();
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();
        
        if (SUPPORTED_CONTENT_TYPES.contains(baseContentType)) {
            return contentType;
        }
        
        return "application/xml";
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
    
    private byte[] formatXml(byte[] xmlContent, boolean isSoap) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlContent);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        if (isSoap) {
            // Process as SOAP
            SOAPModelBuilder builder = OMXMLBuilderFactory.createSOAPModelBuilder(inputStream, null);
            SOAPEnvelope envelope = builder.getSOAPEnvelope();
            
            // Format the SOAP envelope
            envelope.serialize(outputStream);
            byte[] formattedSoap = outputStream.toByteArray();
            
            // Apply additional formatting using transformer
            return prettyPrintXml(formattedSoap);
        } else {
            // Process as plain XML
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(inputStream);
            OMElement documentElement = builder.getDocumentElement();
            
            // Format the XML
            documentElement.serialize(outputStream);
            byte[] formattedXml = outputStream.toByteArray();
            
            // Apply additional formatting using transformer
            return prettyPrintXml(formattedXml);
        }
    }
    
    private byte[] prettyPrintXml(byte[] xmlContent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        
        // Configure the transformer
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        // Create source and result
        StreamSource source = new StreamSource(new ByteArrayInputStream(xmlContent));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(outputStream);
        
        // Transform the XML
        transformer.transform(source, result);
        
        return outputStream.toByteArray();
    }
} 