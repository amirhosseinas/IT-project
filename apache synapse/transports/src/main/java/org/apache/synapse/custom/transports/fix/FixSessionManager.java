package org.apache.synapse.custom.transports.fix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages FIX sessions and provides session-related functionality.
 */
public class FixSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(FixSessionManager.class);
    
    private final Map<String, SessionID> sessionIdMap = new ConcurrentHashMap<>();
    private final Map<SessionID, SessionSettings> sessionSettingsMap = new HashMap<>();
    
    private Initiator initiator;
    private Acceptor acceptor;
    
    /**
     * Initialize the session manager with the given configuration
     * 
     * @param configFile Path to the QuickFIX/J configuration file
     * @param application The FIX application to use
     * @throws ConfigError if there is an error in the configuration
     * @throws FileNotFoundException if the config file is not found
     */
    public void initialize(String configFile, Application application) 
            throws ConfigError, FileNotFoundException {
        initialize(new FileInputStream(configFile), application);
    }
    
    /**
     * Initialize the session manager with the given configuration
     * 
     * @param configStream Input stream for the QuickFIX/J configuration
     * @param application The FIX application to use
     * @throws ConfigError if there is an error in the configuration
     */
    public void initialize(InputStream configStream, Application application) throws ConfigError {
        SessionSettings settings = new SessionSettings(configStream);
        
        // Store session settings for later use
        for (SessionID sessionId : settings.getSessions()) {
            sessionIdMap.put(sessionId.toString(), sessionId);
            sessionSettingsMap.put(sessionId, settings);
        }
        
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new SLF4JLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        // Create both initiator and acceptor if configured
        try {
            // Create initiator for client sessions
            initiator = new SocketInitiator(
                    application, 
                    messageStoreFactory, 
                    settings, 
                    logFactory, 
                    messageFactory);
            
            // Create acceptor for server sessions
            acceptor = new SocketAcceptor(
                    application, 
                    messageStoreFactory, 
                    settings, 
                    logFactory, 
                    messageFactory);
            
            logger.info("FIX session manager initialized with {} sessions", sessionIdMap.size());
        } catch (ConfigError e) {
            logger.error("Error initializing FIX session manager", e);
            throw e;
        }
    }
    
    /**
     * Start the session manager
     * 
     * @throws ConfigError if there is an error in the configuration
     */
    public void start() throws ConfigError {
        if (initiator != null) {
            initiator.start();
            logger.info("FIX initiator started");
        }
        
        if (acceptor != null) {
            acceptor.start();
            logger.info("FIX acceptor started");
        }
    }
    
    /**
     * Stop the session manager
     */
    public void stop() {
        if (initiator != null) {
            initiator.stop();
            logger.info("FIX initiator stopped");
        }
        
        if (acceptor != null) {
            acceptor.stop();
            logger.info("FIX acceptor stopped");
        }
    }
    
    /**
     * Get a session by its string ID
     * 
     * @param sessionIdStr The session ID string
     * @return The session or null if not found
     */
    public Session getSession(String sessionIdStr) {
        SessionID sessionId = sessionIdMap.get(sessionIdStr);
        if (sessionId != null) {
            return Session.lookupSession(sessionId);
        }
        return null;
    }
    
    /**
     * Get a session by its SessionID object
     * 
     * @param sessionId The session ID
     * @return The session or null if not found
     */
    public Session getSession(SessionID sessionId) {
        return Session.lookupSession(sessionId);
    }
    
    /**
     * Check if a session is logged on
     * 
     * @param sessionIdStr The session ID string
     * @return true if the session is logged on, false otherwise
     */
    public boolean isSessionLoggedOn(String sessionIdStr) {
        Session session = getSession(sessionIdStr);
        return session != null && session.isLoggedOn();
    }
    
    /**
     * Get the next expected incoming sequence number for a session
     * 
     * @param sessionIdStr The session ID string
     * @return The next expected sequence number or -1 if the session is not found
     */
    public int getNextExpectedSequenceNumber(String sessionIdStr) {
        Session session = getSession(sessionIdStr);
        if (session != null) {
            return session.getExpectedTargetNum();
        }
        return -1;
    }
    
    /**
     * Get the next outgoing sequence number for a session
     * 
     * @param sessionIdStr The session ID string
     * @return The next outgoing sequence number or -1 if the session is not found
     */
    public int getNextSenderSequenceNumber(String sessionIdStr) {
        Session session = getSession(sessionIdStr);
        if (session != null) {
            return session.getExpectedSenderNum();
        }
        return -1;
    }
    
    /**
     * Reset the sequence numbers for a session
     * 
     * @param sessionIdStr The session ID string
     * @throws IOException if there is an error resetting the sequence numbers
     */
    public void resetSequenceNumbers(String sessionIdStr) throws IOException {
        Session session = getSession(sessionIdStr);
        if (session != null) {
            session.reset();
            logger.info("Reset sequence numbers for session: {}", sessionIdStr);
        } else {
            logger.warn("Cannot reset sequence numbers - session not found: {}", sessionIdStr);
        }
    }
} 