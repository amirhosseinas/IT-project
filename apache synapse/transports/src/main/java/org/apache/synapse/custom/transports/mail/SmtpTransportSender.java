package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Transport sender for sending emails via SMTP protocol.
 */
public class SmtpTransportSender implements TransportSender {
    
    private static final Logger logger = LoggerFactory.getLogger(SmtpTransportSender.class);
    
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final String defaultSenderAddress;
    private final String defaultSenderName;
    
    private Session session;
    
    /**
     * Create a new SMTP transport sender
     * 
     * @param host The SMTP server host
     * @param port The SMTP server port
     * @param username The username for authentication (optional)
     * @param password The password for authentication (optional)
     * @param ssl Whether to use SSL/TLS
     */
    public SmtpTransportSender(String host, int port, String username, String password, boolean ssl) {
        this(host, port, username, password, ssl, 10000, 10000, null, null);
    }
    
    /**
     * Create a new SMTP transport sender with full configuration
     * 
     * @param host The SMTP server host
     * @param port The SMTP server port
     * @param username The username for authentication (optional)
     * @param password The password for authentication (optional)
     * @param ssl Whether to use SSL/TLS
     * @param connectionTimeout Connection timeout in milliseconds
     * @param socketTimeout Socket timeout in milliseconds
     * @param defaultSenderAddress The default sender email address
     * @param defaultSenderName The default sender name
     */
    public SmtpTransportSender(String host, int port, String username, String password, 
                             boolean ssl, int connectionTimeout, int socketTimeout,
                             String defaultSenderAddress, String defaultSenderName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.defaultSenderAddress = defaultSenderAddress;
        this.defaultSenderName = defaultSenderName;
    }
    
    @Override
    public void init() throws TransportException {
        logger.info("Initializing SMTP transport sender: {}:{}", host, port);
        
        Properties properties = new Properties();
        
        // Set mail server properties
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(port));
        
        // Set timeouts
        properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        properties.setProperty("mail.smtp.timeout", String.valueOf(socketTimeout));
        properties.setProperty("mail.smtp.writetimeout", String.valueOf(socketTimeout));
        
        // Set authentication if username is provided
        if (username != null && !username.isEmpty()) {
            properties.setProperty("mail.smtp.auth", "true");
        }
        
        // Set SSL/TLS properties if needed
        if (ssl) {
            properties.setProperty("mail.smtp.ssl.enable", "true");
            properties.setProperty("mail.smtp.ssl.trust", "*");
            properties.setProperty("mail.smtp.starttls.enable", "true");
        }
        
        // Create a session with authentication
        if (username != null && !username.isEmpty() && password != null) {
            session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            session = Session.getInstance(properties);
        }
    }
    
    @Override
    public Message send(Message message, String endpoint) throws TransportException {
        logger.debug("Sending email message to endpoint: {}", endpoint);
        
        try {
            // Convert Synapse message to JavaMail message
            MimeMessage mimeMessage = createMimeMessage(message, endpoint);
            
            // Send the message
            Transport.send(mimeMessage);
            
            logger.info("Email sent successfully to: {}", endpoint);
            
            // Return a confirmation message if needed
            Message response = new Message(message.getMessageId() + "-response");
            response.setDirection(Message.Direction.RESPONSE);
            response.setProperty("mail.sent", true);
            response.setProperty("mail.recipient", endpoint);
            return response;
            
        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}", endpoint, e);
            throw new TransportException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canHandle(String endpoint) {
        return endpoint != null && 
               (endpoint.startsWith("mailto:") || 
                endpoint.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));
    }
    
    @Override
    public void close() {
        logger.info("Closing SMTP transport sender");
        // No specific resources to close with JavaMail
    }
    
    /**
     * Create a JavaMail MIME message from a Synapse message
     * 
     * @param message The Synapse message
     * @param endpoint The email recipient address
     * @return The JavaMail MIME message
     */
    private MimeMessage createMimeMessage(Message message, String endpoint) throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(session);
        
        // Set recipient (extract email from endpoint if it's a mailto: URL)
        String recipientEmail = endpoint;
        if (endpoint.startsWith("mailto:")) {
            recipientEmail = endpoint.substring(7);
        }
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        
        // Set sender
        String from = (String) message.getProperty("mail.from");
        if (from == null && defaultSenderAddress != null) {
            from = defaultSenderAddress;
        }
        
        if (from != null) {
            String senderName = (String) message.getProperty("mail.from.name");
            if (senderName == null && defaultSenderName != null) {
                senderName = defaultSenderName;
            }
            
            if (senderName != null) {
                mimeMessage.setFrom(new InternetAddress(from, senderName));
            } else {
                mimeMessage.setFrom(new InternetAddress(from));
            }
        }
        
        // Set subject
        String subject = (String) message.getProperty("mail.subject");
        if (subject != null) {
            mimeMessage.setSubject(subject, "UTF-8");
        } else {
            mimeMessage.setSubject("Message from Apache Synapse");
        }
        
        // Set date
        mimeMessage.setSentDate(new Date());
        
        // Create a multipart message to support attachments
        Multipart multipart = new MimeMultipart();
        
        // Add main content part
        String contentType = message.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "text/plain; charset=UTF-8";
        }
        
        // Main body content
        byte[] payload = message.getPayload();
        if (payload != null) {
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(new String(payload, "UTF-8"), contentType);
            multipart.addBodyPart(messageBodyPart);
        }
        
        // Add attachments if any
        @SuppressWarnings("unchecked")
        Map<String, DataHandler> attachments = 
            (Map<String, DataHandler>) message.getProperty("mail.attachments");
        
        if (attachments != null) {
            for (Map.Entry<String, DataHandler> entry : attachments.entrySet()) {
                String fileName = entry.getKey();
                DataHandler dataHandler = entry.getValue();
                
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(dataHandler);
                attachmentPart.setFileName(fileName);
                
                multipart.addBodyPart(attachmentPart);
            }
        }
        
        // Set the complete message parts
        mimeMessage.setContent(multipart);
        
        return mimeMessage;
    }
} 