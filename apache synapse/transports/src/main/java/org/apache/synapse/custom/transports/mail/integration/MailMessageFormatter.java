package org.apache.synapse.custom.transports.mail.integration;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportSender;
import org.apache.synapse.custom.transports.mail.MailTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example class demonstrating how to format Synapse messages as email messages.
 * This class shows how to convert Synapse messages to emails for sending via SMTP.
 */
public class MailMessageFormatter {
    
    private static final Logger logger = LoggerFactory.getLogger(MailMessageFormatter.class);
    
    private TransportSender smtpSender;
    
    /**
     * Initialize the mail message formatter
     * 
     * @param smtpHost The SMTP server host
     * @param smtpPort The SMTP server port
     * @param username The username for authentication
     * @param password The password for authentication
     * @param ssl Whether to use SSL/TLS
     * @throws TransportException if initialization fails
     */
    public void initialize(String smtpHost, int smtpPort, String username, String password, boolean ssl) 
            throws TransportException {
        // Create and initialize the SMTP transport sender
        smtpSender = MailTransportFactory.createSender(smtpHost, smtpPort, username, password, ssl);
        smtpSender.init();
        logger.info("Mail message formatter initialized with SMTP sender: {}:{}", smtpHost, smtpPort);
    }
    
    /**
     * Format a Synapse message as an email and send it
     * 
     * @param message The Synapse message to format and send
     * @param recipient The recipient email address
     * @return The response message
     * @throws TransportException if sending fails
     */
    public Message formatAndSendMessage(Message message, String recipient) throws TransportException {
        // Set email-specific properties if not already set
        if (message.getProperty("mail.subject") == null) {
            message.setProperty("mail.subject", "Message from Apache Synapse");
        }
        
        if (message.getProperty("mail.from") == null) {
            message.setProperty("mail.from", "synapse@example.com");
            message.setProperty("mail.from.name", "Apache Synapse");
        }
        
        // Ensure content type is set
        if (message.getContentType() == null) {
            message.setContentType("text/plain; charset=UTF-8");
        }
        
        // If no payload is set, use a default message
        if (message.getPayload() == null) {
            String defaultContent = "This is an automatically generated message from Apache Synapse.";
            message.setPayload(defaultContent.getBytes());
        }
        
        // Send the message
        logger.info("Sending email to: {}", recipient);
        return smtpSender.send(message, recipient);
    }
    
    /**
     * Create an email message with HTML content
     * 
     * @param subject The email subject
     * @param htmlContent The HTML content
     * @param recipient The recipient email address
     * @return The response message
     * @throws TransportException if sending fails
     */
    public Message sendHtmlEmail(String subject, String htmlContent, String recipient) throws TransportException {
        Message message = new Message(UUID.randomUUID().toString());
        message.setProperty("mail.subject", subject);
        message.setContentType("text/html; charset=UTF-8");
        message.setPayload(htmlContent.getBytes());
        
        return formatAndSendMessage(message, recipient);
    }
    
    /**
     * Create an email message with attachments
     * 
     * @param subject The email subject
     * @param textContent The text content
     * @param attachmentFiles Array of files to attach
     * @param recipient The recipient email address
     * @return The response message
     * @throws TransportException if sending fails
     */
    public Message sendEmailWithAttachments(String subject, String textContent, 
                                          File[] attachmentFiles, String recipient) throws TransportException {
        Message message = new Message(UUID.randomUUID().toString());
        message.setProperty("mail.subject", subject);
        message.setContentType("text/plain; charset=UTF-8");
        message.setPayload(textContent.getBytes());
        
        // Add attachments
        if (attachmentFiles != null && attachmentFiles.length > 0) {
            Map<String, DataHandler> attachments = new HashMap<>();
            
            for (File file : attachmentFiles) {
                if (file.exists() && file.isFile()) {
                    DataSource source = new FileDataSource(file);
                    attachments.put(file.getName(), new DataHandler(source));
                }
            }
            
            message.setProperty("mail.attachments", attachments);
        }
        
        return formatAndSendMessage(message, recipient);
    }
    
    /**
     * Close the formatter and release resources
     */
    public void close() {
        if (smtpSender != null) {
            smtpSender.close();
            logger.info("Mail message formatter closed");
        }
    }
    
    /**
     * Main method for demonstration
     */
    public static void main(String[] args) {
        MailMessageFormatter formatter = new MailMessageFormatter();
        
        try {
            // Initialize the formatter
            formatter.initialize("smtp.example.com", 587, "username", "password", true);
            
            // Example 1: Send a simple text email
            Message textMessage = new Message(UUID.randomUUID().toString());
            textMessage.setProperty("mail.subject", "Simple Text Email");
            textMessage.setContentType("text/plain; charset=UTF-8");
            textMessage.setPayload("This is a simple text email from Apache Synapse.".getBytes());
            
            formatter.formatAndSendMessage(textMessage, "recipient@example.com");
            
            // Example 2: Send an HTML email
            String htmlContent = "<html><body><h1>HTML Email</h1><p>This is an <b>HTML</b> email from Apache Synapse.</p></body></html>";
            formatter.sendHtmlEmail("HTML Email Example", htmlContent, "recipient@example.com");
            
            // Example 3: Send an email with attachments
            File[] attachments = {
                new File("example.txt"),
                new File("document.pdf")
            };
            
            formatter.sendEmailWithAttachments(
                "Email with Attachments",
                "Please find the attached files.",
                attachments,
                "recipient@example.com"
            );
            
        } catch (Exception e) {
            logger.error("Error in mail message formatter example", e);
        } finally {
            formatter.close();
        }
    }
} 