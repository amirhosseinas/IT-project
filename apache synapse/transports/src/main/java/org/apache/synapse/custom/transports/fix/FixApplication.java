package org.apache.synapse.custom.transports.fix;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

/**
 * QuickFIX/J application implementation that handles FIX session events and messages.
 */
public class FixApplication implements Application {
    private static final Logger logger = LoggerFactory.getLogger(FixApplication.class);
    
    private final TransportListener.MessageCallback messageCallback;
    
    /**
     * Create a new FIX application
     * 
     * @param messageCallback The callback to invoke when a message is received
     */
    public FixApplication(TransportListener.MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }
    
    @Override
    public void onCreate(SessionID sessionID) {
        logger.info("FIX session created: {}", sessionID);
    }
    
    @Override
    public void onLogon(SessionID sessionID) {
        logger.info("FIX session logon: {}", sessionID);
    }
    
    @Override
    public void onLogout(SessionID sessionID) {
        logger.info("FIX session logout: {}", sessionID);
    }
    
    @Override
    public void toAdmin(quickfix.Message message, SessionID sessionID) {
        // This method is called before sending administrative messages
        logger.debug("Sending admin message: {} for session: {}", message, sessionID);
    }
    
    @Override
    public void fromAdmin(quickfix.Message message, SessionID sessionID) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // This method is called when administrative messages are received
        logger.debug("Received admin message: {} from session: {}", message, sessionID);
        
        // We don't process admin messages by default, but you could route them to the mediation engine if needed
    }
    
    @Override
    public void toApp(quickfix.Message message, SessionID sessionID) throws DoNotSend {
        // This method is called before sending application messages
        logger.debug("Sending application message: {} for session: {}", message, sessionID);
    }
    
    @Override
    public void fromApp(quickfix.Message message, SessionID sessionID) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // This method is called when application messages are received
        logger.info("Received application message: {} from session: {}", message, sessionID);
        
        try {
            // Convert FIX message to Synapse message
            Message synapseMessage = FixMessageConverter.toSynapseMessage(message, sessionID);
            
            // Process the message through the callback
            if (messageCallback != null) {
                Message response = messageCallback.onMessage(synapseMessage);
                
                // If there's a response, send it back through the same session
                if (response != null) {
                    handleResponse(response, sessionID);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing FIX message", e);
        }
    }
    
    /**
     * Handle a response message by sending it back through the appropriate FIX session
     * 
     * @param response The response message
     * @param originalSessionID The original session ID
     */
    private void handleResponse(Message response, SessionID originalSessionID) {
        try {
            // Convert Synapse message to FIX message
            quickfix.Message fixResponse = FixMessageConverter.toFixMessage(response);
            
            // Get the session ID from the message properties or use the original session ID
            String sessionIdStr = (String) response.getProperty(FixMessageConverter.FIX_SESSION_ID);
            SessionID sessionID = (sessionIdStr != null) ? new SessionID(sessionIdStr) : originalSessionID;
            
            // Send the response through the session
            Session session = Session.lookupSession(sessionID);
            if (session != null && session.isLoggedOn()) {
                session.send(fixResponse);
            } else {
                logger.warn("Cannot send response - session not found or not logged on: {}", sessionID);
            }
        } catch (Exception e) {
            logger.error("Error sending FIX response", e);
        }
    }
} 