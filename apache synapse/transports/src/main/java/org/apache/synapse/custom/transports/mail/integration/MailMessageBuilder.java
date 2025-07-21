package org.apache.synapse.custom.transports.mail.integration;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.mail.MailTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example class demonstrating integration between mail transport and message builders.
 * This class shows how to receive emails and convert them to Synapse messages for processing.
 */
public class MailMessageBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(MailMessageBuilder.class);
    
    /**
     * Process an incoming email message and convert it to a format suitable for the ESB
     * 
     * @param message The received mail message
     * @return The processed message or null if no response is needed
     */
    public Message processMailMessage(Message message) {
        try {
            logger.info("Processing mail message with subject: {}", message.getProperty("mail.subject"));
            
            // Extract email metadata
            String subject = (String) message.getProperty("mail.subject");
            String from = (String) message.getProperty("mail.from");
            String contentType = message.getContentType();
            
            // Log the message details
            logger.info("From: {}", from);
            logger.info("Content-Type: {}", contentType);
            
            // Check for HTML content
            if (message.getProperty("mail.html") != null) {
                logger.info("Message contains HTML content");
                // Process HTML content if needed
            }
            
            // Check for text content
            if (message.getProperty("mail.text") != null) {
                logger.info("Message contains text content: {}", message.getProperty("mail.text"));
                // Process text content if needed
            }
            
            // Check for attachments
            Integer attachmentCount = (Integer) message.getProperty("mail.attachment.count", 0);
            if (attachmentCount > 0) {
                logger.info("Message contains {} attachment(s)", attachmentCount);
                
                @SuppressWarnings("unchecked")
                Map<String, DataHandler> attachments = 
                    (Map<String, DataHandler>) message.getProperty("mail.attachments");
                
                if (attachments != null) {
                    // Process each attachment
                    for (Map.Entry<String, DataHandler> entry : attachments.entrySet()) {
                        String fileName = entry.getKey();
                        DataHandler dataHandler = entry.getValue();
                        
                        logger.info("Processing attachment: {}", fileName);
                        
                        // Read attachment content
                        try (InputStream is = dataHandler.getInputStream()) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                baos.write(buffer, 0, bytesRead);
                            }
                            
                            // Process the attachment data
                            byte[] attachmentData = baos.toByteArray();
                            logger.info("Attachment size: {} bytes", attachmentData.length);
                            
                            // Here you would typically:
                            // 1. Store the attachment
                            // 2. Process it according to business rules
                            // 3. Extract data from it if needed
                        }
                    }
                }
            }
            
            // Access the original JavaMail message if needed for advanced processing
            MimeMessage mimeMessage = (MimeMessage) message.getProperty("mail.mime-message");
            if (mimeMessage != null) {
                // Perform additional processing with the JavaMail API if needed
                logger.debug("Original MimeMessage is available for advanced processing");
            }
            
            // Return null since no response is needed for received emails
            return null;
            
        } catch (Exception e) {
            logger.error("Error processing mail message", e);
            return null;
        }
    }
    
    /**
     * Example of how to set up a mail listener and process incoming messages
     */
    public void setupMailListener() throws TransportException, InterruptedException {
        // Create an IMAP transport listener
        TransportListener imapListener = MailTransportFactory.createListener(
            "imap", "imap.example.com", 993, "username", "password", 60000, true);
        
        // Set message callback to process incoming emails
        imapListener.setMessageCallback(this::processMailMessage);
        
        // Initialize and start the listener
        imapListener.init();
        imapListener.start();
        
        logger.info("Mail listener started. Waiting for incoming emails...");
        
        // In a real application, you would keep the listener running
        // For this example, we'll wait for a while and then stop
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.MINUTES);
        
        // Stop the listener
        imapListener.stop();
        logger.info("Mail listener stopped");
    }
    
    /**
     * Main method for demonstration
     */
    public static void main(String[] args) {
        try {
            MailMessageBuilder builder = new MailMessageBuilder();
            builder.setupMailListener();
        } catch (Exception e) {
            logger.error("Error in mail message builder example", e);
        }
    }
} 