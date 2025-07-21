package org.apache.synapse.custom.message.builder;

import org.apache.synapse.custom.message.Message;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the message builders.
 */
public class MessageBuilderTest {
    
    @Test
    public void testJsonMessageBuilder() throws Exception {
        String jsonContent = "{ \"name\": \"John Doe\", \"age\": 30 }";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        
        JsonMessageBuilder builder = new JsonMessageBuilder();
        Message message = builder.buildMessage(inputStream, "application/json");
        
        assertNotNull(message);
        assertEquals("application/json", message.getContentType());
        assertEquals("object", message.getProperty("json.rootType"));
        assertEquals(2, message.getProperty("json.fieldCount"));
    }
    
    @Test
    public void testXmlMessageBuilder() throws Exception {
        String xmlContent = "<person><name>John Doe</name><age>30</age></person>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        
        XmlMessageBuilder builder = new XmlMessageBuilder();
        Message message = builder.buildMessage(inputStream, "application/xml");
        
        assertNotNull(message);
        assertEquals("application/xml", message.getContentType());
        assertEquals("person", message.getProperty("xml.rootElement"));
        assertEquals(2, message.getProperty("xml.childElementCount"));
        assertEquals(false, message.getProperty("xml.isSoap"));
    }
    
    @Test
    public void testSoapMessageBuilder() throws Exception {
        String soapContent = 
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "  <soapenv:Header/>" +
                "  <soapenv:Body>" +
                "    <person><name>John Doe</name><age>30</age></person>" +
                "  </soapenv:Body>" +
                "</soapenv:Envelope>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(soapContent.getBytes(StandardCharsets.UTF_8));
        
        XmlMessageBuilder builder = new XmlMessageBuilder();
        Message message = builder.buildMessage(inputStream, "application/soap+xml");
        
        assertNotNull(message);
        assertEquals("application/soap+xml", message.getContentType());
        assertEquals(true, message.getProperty("xml.isSoap"));
        assertEquals(true, message.getProperty("soap.hasBody"));
        assertEquals("http://schemas.xmlsoap.org/soap/envelope/", message.getProperty("soap.version"));
    }
    
    @Test
    public void testPlainTextMessageBuilder() throws Exception {
        String textContent = "Hello, world!\nThis is a test.";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(textContent.getBytes(StandardCharsets.UTF_8));
        
        PlainTextMessageBuilder builder = new PlainTextMessageBuilder();
        Message message = builder.buildMessage(inputStream, "text/plain");
        
        assertNotNull(message);
        assertEquals("text/plain", message.getContentType());
        assertEquals(2, message.getProperty("text.lineCount"));
        assertEquals(textContent.length(), message.getProperty("text.length"));
    }
    
    @Test
    public void testBinaryMessageBuilder() throws Exception {
        byte[] binaryContent = new byte[100];
        for (int i = 0; i < binaryContent.length; i++) {
            binaryContent[i] = (byte) i;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(binaryContent);
        
        BinaryMessageBuilder builder = new BinaryMessageBuilder();
        Message message = builder.buildMessage(inputStream, "application/octet-stream");
        
        assertNotNull(message);
        assertEquals("application/octet-stream", message.getContentType());
        assertEquals(100, message.getProperty("binary.size"));
        assertEquals("binary", message.getProperty("binary.category"));
    }
    
    @Test
    public void testMessageBuilderFactory() throws Exception {
        MessageBuilderFactory factory = MessageBuilderFactory.getInstance();
        
        MessageBuilder jsonBuilder = factory.getBuilder("application/json");
        assertTrue(jsonBuilder instanceof JsonMessageBuilder);
        
        MessageBuilder xmlBuilder = factory.getBuilder("application/xml");
        assertTrue(xmlBuilder instanceof XmlMessageBuilder);
        
        MessageBuilder soapBuilder = factory.getBuilder("application/soap+xml");
        assertTrue(soapBuilder instanceof XmlMessageBuilder);
        
        MessageBuilder textBuilder = factory.getBuilder("text/plain");
        assertTrue(textBuilder instanceof PlainTextMessageBuilder);
        
        MessageBuilder binaryBuilder = factory.getBuilder("application/octet-stream");
        assertTrue(binaryBuilder instanceof BinaryMessageBuilder);
        
        // Test unknown content type falls back to default builder
        MessageBuilder unknownBuilder = factory.getBuilder("application/unknown");
        assertTrue(unknownBuilder instanceof BinaryMessageBuilder);
    }
    
    @Test
    public void testMessageBuilderUtil() throws Exception {
        String jsonContent = "{ \"name\": \"John Doe\", \"age\": 30 }";
        Message jsonMessage = MessageBuilderUtil.buildJsonMessage(jsonContent);
        
        assertNotNull(jsonMessage);
        assertEquals("application/json", jsonMessage.getContentType());
        
        String xmlContent = "<person><name>John Doe</name><age>30</age></person>";
        Message xmlMessage = MessageBuilderUtil.buildXmlMessage(xmlContent);
        
        assertNotNull(xmlMessage);
        assertEquals("application/xml", xmlMessage.getContentType());
    }
} 