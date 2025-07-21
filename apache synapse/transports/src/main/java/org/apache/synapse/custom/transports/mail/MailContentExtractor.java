package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for extracting content from email messages.
 * Handles parsing and extracting text content and attachments from email.
 */
public class MailContentExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(MailContentExtractor.class);
    
    /**
     * Process the content of an email message and extract its parts.
     * 
     * @param mailMessage The JavaMail message to process
     * @param synapseMessage The Synapse message to populate with content
     * @throws MessagingException If there's an error processing the message
     * @throws IOException If there's an I/O error reading the message content
     */
    public void processContent(javax.mail.Message mailMessage, Message synapseMessage) 
            throws MessagingException, IOException {
        logger.debug("Processing mail content");
        
        // Extract content based on the message type
        Object content = mailMessage.getContent();
        
        if (content instanceof String) {
            // Simple text message
            processTextContent((String) content, mailMessage.getContentType(), synapseMessage);
        } else if (content instanceof Multipart) {
            // Multipart message (with potential attachments)
            processMultipartContent((Multipart) content, synapseMessage);
        } else if (content instanceof InputStream) {
            // Binary content
            processBinaryContent((InputStream) content, mailMessage.getContentType(), synapseMessage);
        } else {
            logger.warn("Unsupported mail content type: {}", content.getClass().getName());
            synapseMessage.setProperty("mail.content.unsupported", true);
        }
    }
    
    /**
     * Process text content from an email
     * 
     * @param text The text content
     * @param contentType The content type of the text
     * @param synapseMessage The Synapse message to populate
     */
    private void processTextContent(String text, String contentType, Message synapseMessage) {
        logger.debug("Processing text content of type: {}", contentType);
        
        // Set the content type and payload
        synapseMessage.setContentType(contentType);
        synapseMessage.setPayload(text.getBytes());
        
        // Store text content in a property as well for easy access
        synapseMessage.setProperty("mail.text", text);
    }
    
    /**
     * Process binary content from an email
     * 
     * @param inputStream The input stream containing the content
     * @param contentType The content type
     * @param synapseMessage The Synapse message to populate
     * @throws IOException If there's an error reading the stream
     */
    private void processBinaryContent(InputStream inputStream, String contentType, Message synapseMessage) 
            throws IOException {
        logger.debug("Processing binary content of type: {}", contentType);
        
        // Read the binary content into a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        // Set the content type and payload
        synapseMessage.setContentType(contentType);
        synapseMessage.setPayload(baos.toByteArray());
    }
    
    /**
     * Process multipart content from an email (handling attachments)
     * 
     * @param multipart The multipart content
     * @param synapseMessage The Synapse message to populate
     * @throws MessagingException If there's an error processing the message
     * @throws IOException If there's an I/O error reading the content
     */
    private void processMultipartContent(Multipart multipart, Message synapseMessage) 
            throws MessagingException, IOException {
        logger.debug("Processing multipart content with {} parts", multipart.getCount());
        
        // Initialize containers for extracted content
        StringBuilder textContent = new StringBuilder();
        StringBuilder htmlContent = new StringBuilder();
        Map<String, DataHandler> attachments = new HashMap<>();
        
        // Process each part of the multipart message
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String disposition = bodyPart.getDisposition();
            String contentType = bodyPart.getContentType();
            
            // Check if this part is an attachment
            if (disposition != null && 
                (disposition.equalsIgnoreCase(Part.ATTACHMENT) || 
                 disposition.equalsIgnoreCase(Part.INLINE))) {
                
                // It's an attachment
                String fileName = bodyPart.getFileName();
                if (fileName != null) {
                    logger.debug("Found attachment: {}", fileName);
                    attachments.put(fileName, bodyPart.getDataHandler());
                }
            } else {
                // It's content - could be text, HTML, or nested multipart
                Object content = bodyPart.getContent();
                
                if (content instanceof String) {
                    // Text or HTML content
                    if (contentType.toLowerCase().contains("text/plain")) {
                        textContent.append(content).append("\n");
                    } else if (contentType.toLowerCase().contains("text/html")) {
                        htmlContent.append(content).append("\n");
                    }
                } else if (content instanceof Multipart) {
                    // Nested multipart - recursive processing
                    processMultipartContent((Multipart) content, synapseMessage);
                }
            }
        }
        
        // Set the main content in the message
        if (htmlContent.length() > 0) {
            // Prefer HTML content if available
            synapseMessage.setContentType("text/html; charset=UTF-8");
            synapseMessage.setPayload(htmlContent.toString().getBytes());
            synapseMessage.setProperty("mail.html", htmlContent.toString());
            
            // Also store plain text if available
            if (textContent.length() > 0) {
                synapseMessage.setProperty("mail.text", textContent.toString());
            }
        } else if (textContent.length() > 0) {
            // Fall back to plain text
            synapseMessage.setContentType("text/plain; charset=UTF-8");
            synapseMessage.setPayload(textContent.toString().getBytes());
            synapseMessage.setProperty("mail.text", textContent.toString());
        }
        
        // Store attachments in the message properties
        if (!attachments.isEmpty()) {
            synapseMessage.setProperty("mail.attachments", attachments);
            synapseMessage.setProperty("mail.attachment.count", attachments.size());
        }
    }
} 