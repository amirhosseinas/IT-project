package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 * Transport listener for receiving emails via POP3 protocol.
 */
public class Pop3TransportListener extends AbstractMailTransportListener {
    
    private static final Logger logger = LoggerFactory.getLogger(Pop3TransportListener.class);
    
    private Session session;
    private Store store;
    private Folder folder;
    private Date lastChecked;
    
    /**
     * Create a new POP3 transport listener
     * 
     * @param host The POP3 server host
     * @param port The POP3 server port
     * @param username The username for authentication
     * @param password The password for authentication
     * @param pollingInterval The interval in milliseconds to check for new mail
     * @param ssl Whether to use SSL/TLS
     */
    public Pop3TransportListener(String host, int port, String username, String password, 
                               long pollingInterval, boolean ssl) {
        super(host, port, username, password, pollingInterval, ssl);
        this.lastChecked = new Date();
    }
    
    @Override
    public void init() throws TransportException {
        super.init();
        
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
        
        try {
            session = Session.getInstance(properties);
            store = session.getStore(ssl ? "pop3s" : "pop3");
            logger.info("POP3 transport listener initialized: {}:{}", host, port);
        } catch (NoSuchProviderException e) {
            logger.error("Failed to initialize POP3 listener", e);
            throw new TransportException("Failed to initialize POP3 listener", e);
        }
    }
    
    @Override
    protected void pollForMail() {
        if (!running.get() || callback == null) {
            return;
        }
        
        try {
            connect();
            
            // Get folder and open in read-only mode (true for read-write mode)
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            
            logger.debug("Checking for new emails. Total messages: {}", folder.getMessageCount());
            
            Message[] messages = folder.getMessages();
            for (javax.mail.Message mailMessage : messages) {
                // Skip messages that have been seen before
                if (mailMessage.getReceivedDate() != null && 
                    mailMessage.getReceivedDate().before(lastChecked)) {
                    continue;
                }
                
                try {
                    // Convert the email to a Synapse Message
                    Message synapseMessage = convertToSynapseMessage(mailMessage);
                    
                    // Process the message using the callback
                    if (callback != null) {
                        logger.debug("Processing email with subject: {}", mailMessage.getSubject());
                        Message response = callback.onMessage(synapseMessage);
                        // Handle response if needed
                    }
                } catch (Exception e) {
                    logger.error("Error processing mail message", e);
                }
            }
            
            // Update the last checked timestamp
            this.lastChecked = new Date();
            
            closeFolder();
            disconnect();
        } catch (Exception e) {
            logger.error("Error polling for emails", e);
            try {
                closeFolder();
                disconnect();
            } catch (Exception ex) {
                logger.error("Error closing folder/connection", ex);
            }
        }
    }
    
    /**
     * Connect to the mail server
     * 
     * @throws MessagingException if connection fails
     */
    private void connect() throws MessagingException {
        if (!store.isConnected()) {
            store.connect(host, username, password);
            logger.debug("Connected to POP3 server: {}:{}", host, port);
        }
    }
    
    /**
     * Disconnect from the mail server
     */
    private void disconnect() {
        try {
            if (store != null && store.isConnected()) {
                store.close();
                logger.debug("Disconnected from POP3 server");
            }
        } catch (MessagingException e) {
            logger.error("Error disconnecting from POP3 server", e);
        }
    }
    
    /**
     * Close the current folder
     */
    private void closeFolder() {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(false); // false = don't expunge deleted messages
                logger.debug("Closed folder");
            }
        } catch (MessagingException e) {
            logger.error("Error closing folder", e);
        }
    }
    
    /**
     * Convert a JavaMail message to a Synapse message
     * 
     * @param mailMessage The JavaMail message
     * @return The Synapse message
     */
    private Message convertToSynapseMessage(javax.mail.Message mailMessage) throws MessagingException, java.io.IOException {
        Message synapseMessage = new Message(UUID.randomUUID().toString());
        synapseMessage.setDirection(Message.Direction.REQUEST);
        
        // Extract email metadata and add as properties
        if (mailMessage.getSubject() != null) {
            synapseMessage.setProperty("mail.subject", mailMessage.getSubject());
        }
        if (mailMessage.getFrom() != null && mailMessage.getFrom().length > 0) {
            synapseMessage.setProperty("mail.from", mailMessage.getFrom()[0].toString());
        }
        if (mailMessage.getReceivedDate() != null) {
            synapseMessage.setProperty("mail.received-date", mailMessage.getReceivedDate());
        }
        if (mailMessage.getSentDate() != null) {
            synapseMessage.setProperty("mail.sent-date", mailMessage.getSentDate());
        }
        
        // Store the JavaMail message for later use (e.g., accessing attachments)
        synapseMessage.setProperty("mail.mime-message", mailMessage);

        // Process content and extract relevant data
        MailContentExtractor extractor = new MailContentExtractor();
        extractor.processContent(mailMessage, synapseMessage);

        return synapseMessage;
    }
} 