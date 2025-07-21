package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SmtpTransportSenderTest {

    private SmtpTransportSender smtpSender;
    
    @Mock
    private Session mockSession;
    
    @BeforeEach
    public void setUp() {
        // Create a test instance with dummy values
        smtpSender = new SmtpTransportSender("smtp.example.com", 25, "user", "pass", false);
        
        // Use reflection to set the mocked session
        try {
            java.lang.reflect.Field sessionField = SmtpTransportSender.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(smtpSender, mockSession);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }
    }
    
    @Test
    public void testInitWithoutSSL() throws TransportException {
        // Create a new instance to test init method
        SmtpTransportSender sender = new SmtpTransportSender("smtp.example.com", 25, "user", "pass", false);
        
        // No exception should be thrown
        sender.init();
    }
    
    @Test
    public void testInitWithSSL() throws TransportException {
        // Create a new instance with SSL to test init method
        SmtpTransportSender sender = new SmtpTransportSender("smtp.example.com", 465, "user", "pass", true);
        
        // No exception should be thrown
        sender.init();
    }
    
    @Test
    public void testCanHandleValidEmailAddress() {
        assertTrue(smtpSender.canHandle("user@example.com"));
    }
    
    @Test
    public void testCanHandleMailtoUrl() {
        assertTrue(smtpSender.canHandle("mailto:user@example.com"));
    }
    
    @Test
    public void testCanHandleInvalidEndpoint() {
        assertFalse(smtpSender.canHandle("http://example.com"));
        assertFalse(smtpSender.canHandle(null));
    }
    
    @Test
    public void testSendEmailSuccess() throws Exception {
        // Create a test message
        Message message = new Message(UUID.randomUUID().toString());
        message.setProperty("mail.subject", "Test Subject");
        message.setProperty("mail.from", "sender@example.com");
        message.setContentType("text/plain");
        message.setPayload("Test content".getBytes());
        
        // Mock the Transport class
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            // Execute the send method
            Message response = smtpSender.send(message, "recipient@example.com");
            
            // Verify Transport.send was called
            mockedTransport.verify(() -> Transport.send(any(MimeMessage.class)));
            
            // Check response properties
            assertNotNull(response);
            assertEquals(Message.Direction.RESPONSE, response.getDirection());
            assertTrue((Boolean) response.getProperty("mail.sent"));
            assertEquals("recipient@example.com", response.getProperty("mail.recipient"));
        }
    }
    
    @Test
    public void testSendEmailFailure() throws Exception {
        // Create a test message
        Message message = new Message(UUID.randomUUID().toString());
        message.setProperty("mail.subject", "Test Subject");
        message.setContentType("text/plain");
        message.setPayload("Test content".getBytes());
        
        // Mock the Transport class to throw an exception
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any(MimeMessage.class)))
                .thenThrow(new MessagingException("Test exception"));
            
            // Execute and verify exception is thrown
            TransportException exception = assertThrows(TransportException.class, () -> 
                smtpSender.send(message, "recipient@example.com"));
            
            assertTrue(exception.getMessage().contains("Failed to send email"));
        }
    }
    
    @Test
    public void testClose() {
        // No exception should be thrown
        smtpSender.close();
    }
} 