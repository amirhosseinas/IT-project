package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImapTransportListenerTest {

    private ImapTransportListener imapListener;
    
    @Mock
    private Session mockSession;
    
    @Mock
    private Store mockStore;
    
    @Mock
    private Folder mockFolder;
    
    @Mock
    private TransportListener.MessageCallback mockCallback;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create a test instance with dummy values
        imapListener = new ImapTransportListener(
            "imap.example.com", 993, "user", "pass", 60000, true);
        
        // Use reflection to set the mocked objects
        setPrivateField(imapListener, "session", mockSession);
        setPrivateField(imapListener, "store", mockStore);
        
        // Set up the mock callback
        imapListener.setMessageCallback(mockCallback);
    }
    
    @Test
    public void testInit() throws Exception {
        // Create a new instance to test init method
        ImapTransportListener listener = new ImapTransportListener(
            "imap.example.com", 993, "user", "pass", 60000, true);
        
        // No exception should be thrown
        listener.init();
    }
    
    @Test
    public void testStartStop() throws Exception {
        // Test start
        imapListener.start();
        assertTrue(imapListener.isRunning());
        
        // Test stop
        imapListener.stop();
        assertFalse(imapListener.isRunning());
    }
    
    @Test
    public void testPollForMailWithNoMessages() throws Exception {
        // Set up mocks
        when(mockStore.isConnected()).thenReturn(false).thenReturn(true);
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        when(mockFolder.search(any(FlagTerm.class))).thenReturn(new Message[0]);
        
        // Use reflection to call the protected pollForMail method
        imapListener.start();
        invokePrivateMethod(imapListener, "pollForMail");
        
        // Verify interactions
        verify(mockStore).connect("imap.example.com", "user", "pass");
        verify(mockFolder).open(Folder.READ_WRITE);
        verify(mockFolder).close(false);
        verify(mockStore).close();
    }
    
    @Test
    public void testPollForMailWithMessages() throws Exception {
        // Create a test message
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mockMimeMessage.getSubject()).thenReturn("Test Subject");
        when(mockMimeMessage.getFrom()).thenReturn(new Address[] { mock(Address.class) });
        when(mockMimeMessage.getContent()).thenReturn("Test Content");
        when(mockMimeMessage.getContentType()).thenReturn("text/plain");
        
        // Set up mocks
        when(mockStore.isConnected()).thenReturn(false).thenReturn(true);
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        when(mockFolder.search(any(FlagTerm.class))).thenReturn(new javax.mail.Message[] { mockMimeMessage });
        
        // Set up callback to return a response
        when(mockCallback.onMessage(any(Message.class))).thenReturn(new Message("response-id"));
        
        // Use reflection to call the protected pollForMail method
        imapListener.start();
        invokePrivateMethod(imapListener, "pollForMail");
        
        // Verify interactions
        verify(mockStore).connect("imap.example.com", "user", "pass");
        verify(mockFolder).open(Folder.READ_WRITE);
        verify(mockMimeMessage).setFlag(Flags.Flag.SEEN, true);
        verify(mockCallback).onMessage(any(Message.class));
        verify(mockFolder).close(false);
        verify(mockStore).close();
    }
    
    @Test
    public void testSetMarkAsRead() {
        imapListener.setMarkAsRead(false);
        // Use reflection to verify the value was set
        boolean markAsRead = (boolean) getPrivateField(imapListener, "markAsRead");
        assertFalse(markAsRead);
    }
    
    // Helper methods for accessing private fields and methods
    
    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
    
    private Object getPrivateField(Object object, String fieldName) {
        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            fail("Failed to get private field: " + e.getMessage());
            return null;
        }
    }
    
    private void invokePrivateMethod(Object object, String methodName) throws Exception {
        java.lang.reflect.Method method = object.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(object);
    }
} 