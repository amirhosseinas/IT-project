package org.apache.synapse.custom.transports.mail.examples;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportSender;
import org.apache.synapse.custom.transports.mail.MailTransportFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example demonstrating how to use mail transport in Apache Synapse.
 * Shows both receiving emails via POP3/IMAP and sending emails via SMTP.
 */
public class MailTransportExample {

    public static void main(String[] args) {
        try {
            // Example of receiving emails via POP3
            receivePop3Example();
            
            // Example of receiving emails via IMAP
            receiveImapExample();
            
            // Example of sending an email via SMTP
            sendSmtpExample();
            
            // Example of sending an email with attachment
            sendSmtpWithAttachmentExample();
            
        } catch (Exception e) {
            System.err.println("Error in mail transport example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Example of receiving emails via POP3
     */
    private static void receivePop3Example() throws Exception {
        System.out.println("\n=== POP3 Email Receiver Example ===");
        
        // Create POP3 transport listener
        TransportListener pop3Listener = MailTransportFactory.createListener(
            "pop3", "pop3.example.com", 110, "username", "password", 60000, false);
        
        // Set message callback
        pop3Listener.setMessageCallback(message -> {
            System.out.println("Received email with subject: " + message.getProperty("mail.subject"));
            System.out.println("From: " + message.getProperty("mail.from"));
            System.out.println("Content: " + message.getProperty("mail.text"));
            
            // Check for attachments
            int attachmentCount = (Integer) message.getProperty("mail.attachment.count", 0);
            if (attachmentCount > 0) {
                System.out.println("Email has " + attachmentCount + " attachment(s)");
            }
            
            // Process the message here...
            return null; // No response needed for received emails
        });
        
        // Initialize and start the listener
        pop3Listener.init();
        pop3Listener.start();
        
        System.out.println("POP3 listener started. It will poll for emails every 60 seconds.");
        System.out.println("Press Ctrl+C to stop.");
        
        // In a real application, you would leave this running
        // For demo purposes, we'll sleep for a short time
        Thread.sleep(10000);
        
        // Stop the listener when done
        pop3Listener.stop();
        System.out.println("POP3 listener stopped.");
    }
    
    /**
     * Example of receiving emails via IMAP
     */
    private static void receiveImapExample() throws Exception {
        System.out.println("\n=== IMAP Email Receiver Example ===");
        
        // Create IMAP transport listener with SSL
        TransportListener imapListener = MailTransportFactory.createListener(
            "imap", "imap.example.com", 993, "username", "password", 60000, true);
        
        // Set message callback
        imapListener.setMessageCallback(message -> {
            System.out.println("Received email with subject: " + message.getProperty("mail.subject"));
            System.out.println("From: " + message.getProperty("mail.from"));
            
            // Check for HTML content
            String htmlContent = (String) message.getProperty("mail.html");
            if (htmlContent != null) {
                System.out.println("Email contains HTML content");
            } else {
                System.out.println("Content: " + message.getProperty("mail.text"));
            }
            
            // Process the message here...
            return null; // No response needed for received emails
        });
        
        // Initialize and start the listener
        imapListener.init();
        imapListener.start();
        
        System.out.println("IMAP listener started. It will poll for emails every 60 seconds.");
        System.out.println("Press Ctrl+C to stop.");
        
        // In a real application, you would leave this running
        // For demo purposes, we'll sleep for a short time
        Thread.sleep(10000);
        
        // Stop the listener when done
        imapListener.stop();
        System.out.println("IMAP listener stopped.");
    }
    
    /**
     * Example of sending an email via SMTP
     */
    private static void sendSmtpExample() throws Exception {
        System.out.println("\n=== SMTP Email Sender Example ===");
        
        // Create SMTP transport sender
        TransportSender smtpSender = MailTransportFactory.createSender(
            "smtp.example.com", 25, "username", "password", false);
        
        // Initialize the sender
        smtpSender.init();
        
        // Create a message to send
        Message message = new Message(UUID.randomUUID().toString());
        message.setProperty("mail.subject", "Test email from Apache Synapse");
        message.setProperty("mail.from", "sender@example.com");
        message.setProperty("mail.from.name", "Apache Synapse");
        
        // Set the message content
        String content = "This is a test email sent from Apache Synapse Mail Transport.";
        message.setContentType("text/plain; charset=UTF-8");
        message.setPayload(content.getBytes());
        
        // Send the message
        String recipientEmail = "recipient@example.com";
        Message response = smtpSender.send(message, recipientEmail);
        
        // Check if the message was sent successfully
        if ((Boolean) response.getProperty("mail.sent", false)) {
            System.out.println("Email sent successfully to: " + response.getProperty("mail.recipient"));
        } else {
            System.out.println("Failed to send email");
        }
        
        // Close the sender when done
        smtpSender.close();
    }
    
    /**
     * Example of sending an email with attachment via SMTP
     */
    private static void sendSmtpWithAttachmentExample() throws Exception {
        System.out.println("\n=== SMTP Email with Attachment Example ===");
        
        // Create SMTP transport sender with SSL
        TransportSender smtpSender = MailTransportFactory.createSender(
            "smtp.example.com", 587, "username", "password", true);
        
        // Initialize the sender
        smtpSender.init();
        
        // Create a message to send
        Message message = new Message(UUID.randomUUID().toString());
        message.setProperty("mail.subject", "Test email with attachment from Apache Synapse");
        message.setProperty("mail.from", "sender@example.com");
        message.setProperty("mail.from.name", "Apache Synapse");
        
        // Set the message content
        String content = "This is a test email with attachment sent from Apache Synapse Mail Transport.";
        message.setContentType("text/plain; charset=UTF-8");
        message.setPayload(content.getBytes());
        
        // Add attachment (example with a file)
        Map<String, DataHandler> attachments = new HashMap<>();
        attachments.put("report.pdf", new DataHandler(new FileDataSource("path/to/report.pdf")));
        message.setProperty("mail.attachments", attachments);
        
        // Send the message
        String recipientEmail = "recipient@example.com";
        Message response = smtpSender.send(message, recipientEmail);
        
        // Check if the message was sent successfully
        if ((Boolean) response.getProperty("mail.sent", false)) {
            System.out.println("Email with attachment sent successfully to: " + 
                response.getProperty("mail.recipient"));
        } else {
            System.out.println("Failed to send email with attachment");
        }
        
        // Close the sender when done
        smtpSender.close();
    }
} 