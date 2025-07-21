package org.apache.synapse.custom.qos.security;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Handler for WS-Security processing.
 */
public class WSSHandler {
    private static final Logger logger = LoggerFactory.getLogger(WSSHandler.class);
    
    private final SecurityManager securityManager;
    private final WSSecurityEngine securityEngine;
    private final List<Integer> supportedActions;
    
    /**
     * Create a new WS-Security handler
     * 
     * @param securityManager Security manager
     */
    public WSSHandler(SecurityManager securityManager) {
        this.securityManager = securityManager;
        this.securityEngine = new WSSecurityEngine();
        this.supportedActions = Arrays.asList(
                WSConstants.SIGN,
                WSConstants.ENCR,
                WSConstants.UT,
                WSConstants.UT_SIGN,
                WSConstants.TIMESTAMP,
                WSConstants.ST_SIGNED,
                WSConstants.ST_UNSIGNED
        );
        logger.info("WS-Security handler initialized");
    }
    
    /**
     * Process an incoming message with WS-Security
     * 
     * @param messageBody XML message body
     * @return Processing result or null if processing failed
     * @throws SecurityException if security processing fails
     */
    public WSHandlerResult processIncomingMessage(String messageBody) throws SecurityException {
        try {
            // Parse XML document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(messageBody.getBytes()));
            
            // Setup request data
            RequestData requestData = new RequestData();
            requestData.setSigVerCrypto(securityManager.getCrypto());
            requestData.setDecCrypto(securityManager.getCrypto());
            
            // Process security
            return securityEngine.processSecurityHeader(doc, requestData);
        } catch (WSSecurityException e) {
            logger.error("WS-Security processing failed", e);
            throw new SecurityException("WS-Security processing failed: " + e.getMessage(), e);
        } catch (ParserConfigurationException | org.xml.sax.SAXException | IOException e) {
            logger.error("XML parsing failed", e);
            throw new SecurityException("XML parsing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Apply WS-Security to an outgoing message
     * 
     * @param document XML document to secure
     * @param action Security action to apply
     * @return Secured document
     * @throws SecurityException if security processing fails
     */
    public Document secureOutgoingMessage(Document document, int action) throws SecurityException {
        if (!supportedActions.contains(action)) {
            throw new SecurityException("Unsupported security action: " + action);
        }
        
        try {
            // Apply security based on action
            // This is a simplified example - in a real implementation, you would use WSSecHeader
            // and appropriate security classes based on the action
            
            logger.debug("Applying WS-Security action: {}", action);
            
            // Return the secured document
            return document;
        } catch (Exception e) {
            logger.error("Failed to secure outgoing message", e);
            throw new SecurityException("Failed to secure outgoing message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a security action is supported
     * 
     * @param action Security action
     * @return true if supported, false otherwise
     */
    public boolean isActionSupported(int action) {
        return supportedActions.contains(action);
    }
    
    /**
     * Get a list of supported security actions
     * 
     * @return List of supported actions
     */
    public List<Integer> getSupportedActions() {
        return supportedActions;
    }
} 