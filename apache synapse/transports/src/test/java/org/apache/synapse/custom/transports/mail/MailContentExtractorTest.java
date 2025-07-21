package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailContentExtractorTest {

    private MailContentExtractor extractor;
    
    @Mock
    private MimeMessage mockMessage;
    
    private Message synapseMessage;
    
    @BeforeEach
    public void setUp() {
        extractor = new MailContentExtractor();
        synapseMessage = new Message(UUID.randomUUID().toString());
    }
    
    @Test
    public void testProcessTextContent() throws Exception {
        // Set up mock
        String textContent = "This is a test email content";
        when(mockMessage.getContent()).thenReturn(textContent);
        when(mockMessage.getContentType()).thenReturn("text/plain; charset=UTF-8");
        
        // Process the content
        extractor.processContent(mockMessage, synapseMessage);
        
        // Verify results
        assertEquals("text/plain; charset=UTF-8", synapseMessage.getContentType());
        assertEquals(textContent, synapseMessage.getProperty("mail.text"));
        assertArrayEquals(textContent.getBytes(), synapseMessage.getPayload());
    }
    
    @Test
    public void testProcessBinaryContent() throws Exception {
        // Set up mock
        byte[] binaryData = "Binary content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        when(mockMessage.getContent()).thenReturn(inputStream);
        when(mockMessage.getContentType()).thenReturn("application/octet-stream");
        
        // Process the content
        extractor.processContent(mockMessage, synapseMessage);
        
        // Verify results
        assertEquals("application/octet-stream", synapseMessage.getContentType());
        assertArrayEquals(binaryData, synapseMessage.getPayload());
    }
    
    @Test
    public void testProcessMultipartContentWithTextAndHtml() throws Exception {
        // Create a multipart message with text and HTML parts
        MimeMultipart multipart = new MimeMultipart();
        
        // Add text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent("This is plain text", "text/plain");
        multipart.addBodyPart(textPart);
        
        // Add HTML part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<html><body>This is HTML</body></html>", "text/html");
        multipart.addBodyPart(htmlPart);
        
        // Set up mock
        when(mockMessage.getContent()).thenReturn(multipart);
        
        // Process the content
        extractor.processContent(mockMessage, synapseMessage);
        
        // Verify results - HTML should be preferred
        assertEquals("text/html; charset=UTF-8", synapseMessage.getContentType());
        assertEquals("<html><body>This is HTML</body></html>\n", synapseMessage.getProperty("mail.html"));
        assertEquals("This is plain text\n", synapseMessage.getProperty("mail.text"));
    }
    
    @Test
    public void testProcessMultipartContentWithAttachment() throws Exception {
        // Create a multipart message with text and attachment
        MimeMultipart multipart = new MimeMultipart();
        
        // Add text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent("This is the message body", "text/plain");
        multipart.addBodyPart(textPart);
        
        // Add attachment part
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setFileName("test.txt");
        attachmentPart.setContent("Attachment content", "text/plain");
        attachmentPart.setDisposition(Part.ATTACHMENT);
        multipart.addBodyPart(attachmentPart);
        
        // Set up mock
        when(mockMessage.getContent()).thenReturn(multipart);
        
        // Process the content
        extractor.processContent(mockMessage, synapseMessage);
        
        // Verify results
        assertEquals("text/plain; charset=UTF-8", synapseMessage.getContentType());
        assertEquals("This is the message body\n", synapseMessage.getProperty("mail.text"));
        
        // Verify attachment was captured
        assertEquals(1, synapseMessage.getProperty("mail.attachment.count"));
        Map<String, DataHandler> attachments = 
            (Map<String, DataHandler>) synapseMessage.getProperty("mail.attachments");
        assertNotNull(attachments);
        assertTrue(attachments.containsKey("test.txt"));
    }
    
    @Test
    public void testProcessUnsupportedContent() throws Exception {
        // Set up mock with an unsupported content type
        when(mockMessage.getContent()).thenReturn(new Object());
        
        // Process the content
        extractor.processContent(mockMessage, synapseMessage);
        
        // Verify results
        assertTrue((Boolean) synapseMessage.getProperty("mail.content.unsupported"));
    }
} 