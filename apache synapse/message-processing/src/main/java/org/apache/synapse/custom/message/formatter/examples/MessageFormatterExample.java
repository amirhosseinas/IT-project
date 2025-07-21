package org.apache.synapse.custom.message.formatter.examples;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;
import org.apache.synapse.custom.message.formatter.JsonMessageFormatter;
import org.apache.synapse.custom.message.formatter.MessageFormatter;
import org.apache.synapse.custom.message.formatter.MessageFormatterFactory;
import org.apache.synapse.custom.message.formatter.MessageFormatterUtil;
import org.apache.synapse.custom.message.formatter.XmlMessageFormatter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Example demonstrating the use of message formatters.
 */
public class MessageFormatterExample {
    
    public static void main(String[] args) {
        try {
            // Example 1: Format a JSON message
            System.out.println("Example 1: Format a JSON message");
            String jsonContent = "{\"name\":\"John Doe\",\"age\":30,\"address\":{\"city\":\"New York\",\"country\":\"USA\"}}";
            Message jsonMessage = MessageBuilderUtil.buildJsonMessage(jsonContent);
            formatAndPrintMessage(jsonMessage, "application/json");
            System.out.println();
            
            // Example 2: Format an XML message
            System.out.println("Example 2: Format an XML message");
            String xmlContent = "<person><name>John Doe</name><age>30</age><address><city>New York</city><country>USA</country></address></person>";
            Message xmlMessage = MessageBuilderUtil.buildXmlMessage(xmlContent);
            formatAndPrintMessage(xmlMessage, "application/xml");
            System.out.println();
            
            // Example 3: Format a plain text message
            System.out.println("Example 3: Format a plain text message");
            String textContent = "Hello, this is a plain text message!";
            Message textMessage = MessageBuilderUtil.buildTextMessage(textContent);
            formatAndPrintMessage(textMessage, "text/plain");
            System.out.println();
            
            // Example 4: Content negotiation
            System.out.println("Example 4: Content negotiation");
            Message message = new Message(UUID.randomUUID().toString());
            message.setPayload(jsonContent.getBytes(StandardCharsets.UTF_8));
            message.setContentType("application/json");
            
            // Client accepts XML with higher priority than JSON
            String acceptHeader = "application/xml;q=0.9, application/json;q=0.8";
            byte[] negotiatedContent = MessageFormatterUtil.formatMessageWithContentNegotiation(message, acceptHeader);
            System.out.println("Content negotiated format (XML preferred):");
            System.out.println(new String(negotiatedContent, StandardCharsets.UTF_8));
            System.out.println();
            
            // Example 5: Response optimization with compression
            System.out.println("Example 5: Response optimization with compression");
            // Create a large message to trigger compression
            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                largeContent.append("{\"index\":").append(i).append(",\"data\":\"Some repeated content to make the message larger\"");
                largeContent.append(",\"moreData\":\"Even more content to ensure compression will be beneficial\"},");
            }
            largeContent.append("{\"status\":\"end\"}");
            
            Message largeMessage = new Message(UUID.randomUUID().toString());
            largeMessage.setPayload(largeContent.toString().getBytes(StandardCharsets.UTF_8));
            largeMessage.setContentType("application/json");
            
            // Client supports gzip compression
            String acceptEncoding = "gzip, deflate";
            Message optimizedMessage = MessageFormatterUtil.optimizeResponse(largeMessage, acceptEncoding);
            
            System.out.println("Original size: " + largeContent.length() + " bytes");
            System.out.println("Compressed size: " + optimizedMessage.getPayload().length + " bytes");
            System.out.println("Compression ratio: " + optimizedMessage.getProperty("compression.ratio"));
            System.out.println("Content-Encoding: " + optimizedMessage.getHeader("Content-Encoding"));
            
        } catch (Exception e) {
            System.err.println("Error in message formatter example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void formatAndPrintMessage(Message message, String expectedContentType) throws Exception {
        // Format using specific formatter
        MessageFormatter formatter;
        if ("application/json".equals(expectedContentType)) {
            formatter = new JsonMessageFormatter();
        } else if ("application/xml".equals(expectedContentType)) {
            formatter = new XmlMessageFormatter();
        } else {
            formatter = MessageFormatterFactory.getInstance().getFormatter(message);
        }
        
        // Format to byte array
        byte[] formattedContent = formatter.formatMessage(message);
        System.out.println("Formatted content using " + formatter.getClass().getSimpleName() + ":");
        System.out.println(new String(formattedContent, StandardCharsets.UTF_8));
        
        // Format to output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.formatMessage(message, outputStream);
        System.out.println("Content-Type: " + formatter.getContentType(message));
    }
}
