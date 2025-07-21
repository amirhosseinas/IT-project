package org.apache.synapse.custom.transports.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for creating and caching mail sessions.
 * This helps in reusing mail sessions for better performance.
 */
public class MailSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MailSessionManager.class);
    
    private static final MailSessionManager INSTANCE = new MailSessionManager();
    
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private MailSessionManager() {
    }
    
    /**
     * Get the singleton instance of the mail session manager
     * 
     * @return The mail session manager instance
     */
    public static MailSessionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Get or create a mail session for POP3
     * 
     * @param host The POP3 server host
     * @param port The POP3 server port
     * @param username The username for authentication
     * @param password The password for authentication
     * @param ssl Whether to use SSL/TLS
     * @return The mail session
     */
    public Session getPop3Session(String host, int port, String username, String password, boolean ssl) {
        String key = "pop3:" + host + ":" + port + ":" + username + ":" + ssl;
        
        return sessionCache.computeIfAbsent(key, k -> {
            logger.debug("Creating new POP3 session for {}:{}", host, port);
            
            Properties properties = new Properties();
            properties.setProperty("mail.store.protocol", ssl ? "pop3s" : "pop3");
            properties.setProperty("mail.pop3.host", host);
            properties.setProperty("mail.pop3.port", String.valueOf(port));
            properties.setProperty("mail.pop3.connectiontimeout", "10000");
            properties.setProperty("mail.pop3.timeout", "10000");
            
            if (ssl) {
                properties.setProperty("mail.pop3.ssl.enable", "true");
                properties.setProperty("mail.pop3.ssl.trust", "*");
            }
            
            return createSession(properties, username, password);
        });
    }
    
    /**
     * Get or create a mail session for IMAP
     * 
     * @param host The IMAP server host
     * @param port The IMAP server port
     * @param username The username for authentication
     * @param password The password for authentication
     * @param ssl Whether to use SSL/TLS
     * @return The mail session
     */
    public Session getImapSession(String host, int port, String username, String password, boolean ssl) {
        String key = "imap:" + host + ":" + port + ":" + username + ":" + ssl;
        
        return sessionCache.computeIfAbsent(key, k -> {
            logger.debug("Creating new IMAP session for {}:{}", host, port);
            
            Properties properties = new Properties();
            properties.setProperty("mail.store.protocol", ssl ? "imaps" : "imap");
            properties.setProperty("mail.imap.host", host);
            properties.setProperty("mail.imap.port", String.valueOf(port));
            properties.setProperty("mail.imap.connectiontimeout", "10000");
            properties.setProperty("mail.imap.timeout", "10000");
            
            if (ssl) {
                properties.setProperty("mail.imap.ssl.enable", "true");
                properties.setProperty("mail.imap.ssl.trust", "*");
            }
            
            return createSession(properties, username, password);
        });
    }
    
    /**
     * Get or create a mail session for SMTP
     * 
     * @param host The SMTP server host
     * @param port The SMTP server port
     * @param username The username for authentication (optional)
     * @param password The password for authentication (optional)
     * @param ssl Whether to use SSL/TLS
     * @param connectionTimeout Connection timeout in milliseconds
     * @param socketTimeout Socket timeout in milliseconds
     * @return The mail session
     */
    public Session getSmtpSession(String host, int port, String username, String password, 
                              boolean ssl, int connectionTimeout, int socketTimeout) {
        String key = "smtp:" + host + ":" + port + ":" + username + ":" + ssl;
        
        return sessionCache.computeIfAbsent(key, k -> {
            logger.debug("Creating new SMTP session for {}:{}", host, port);
            
            Properties properties = new Properties();
            properties.setProperty("mail.transport.protocol", "smtp");
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.port", String.valueOf(port));
            properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
            properties.setProperty("mail.smtp.timeout", String.valueOf(socketTimeout));
            properties.setProperty("mail.smtp.writetimeout", String.valueOf(socketTimeout));
            
            if (username != null && !username.isEmpty()) {
                properties.setProperty("mail.smtp.auth", "true");
            }
            
            if (ssl) {
                properties.setProperty("mail.smtp.ssl.enable", "true");
                properties.setProperty("mail.smtp.ssl.trust", "*");
                properties.setProperty("mail.smtp.starttls.enable", "true");
            }
            
            return createSession(properties, username, password);
        });
    }
    
    /**
     * Create a mail session with the specified properties and authentication
     * 
     * @param properties The mail properties
     * @param username The username for authentication (optional)
     * @param password The password for authentication (optional)
     * @return The mail session
     */
    private Session createSession(Properties properties, String username, String password) {
        if (username != null && !username.isEmpty() && password != null) {
            return Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            return Session.getInstance(properties);
        }
    }
    
    /**
     * Clear the session cache
     */
    public void clearCache() {
        logger.info("Clearing mail session cache");
        sessionCache.clear();
    }
} 