package org.apache.synapse.custom.transports.fix;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.field.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the FIX transport components.
 */
public class FixTransportTest {
    
    @TempDir
    Path tempDir;
    
    private String configFile;
    
    @BeforeEach
    public void setUp() throws IOException {
        // Create a test configuration file
        configFile = tempDir.resolve("fix-test.cfg").toString();
        createTestConfig(configFile);
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up any resources
    }
    
    @Test
    public void testMessageConverter() throws Exception {
        // Create a FIX message
        quickfix.Message fixMessage = FixMessageConverter.createNewFixMessage(MsgType.NEW_ORDER_SINGLE);
        
        // Set required fields
        String clOrdId = UUID.randomUUID().toString().substring(0, 8);
        fixMessage.setField(new ClOrdID(clOrdId));
        fixMessage.setField(new Symbol("AAPL"));
        fixMessage.setField(new Side(Side.BUY));
        fixMessage.setField(new OrdType(OrdType.LIMIT));
        fixMessage.setField(new Price(150.50));
        fixMessage.setField(new OrderQty(100));
        fixMessage.setField(new TimeInForce(TimeInForce.DAY));
        fixMessage.setField(new TransactTime(new Date()));
        
        // Set header fields
        SessionID sessionID = new SessionID("FIX.4.4", "SENDER", "TARGET");
        
        // Convert to Synapse message
        Message synapseMessage = FixMessageConverter.toSynapseMessage(fixMessage, sessionID);
        
        // Verify conversion
        assertNotNull(synapseMessage);
        assertEquals(FixMessageConverter.FIX_CONTENT_TYPE, synapseMessage.getContentType());
        
        // Convert back to FIX message
        quickfix.Message convertedFixMessage = FixMessageConverter.toFixMessage(synapseMessage);
        
        // Verify round-trip conversion
        assertNotNull(convertedFixMessage);
        assertEquals(clOrdId, convertedFixMessage.getString(ClOrdID.FIELD));
        assertEquals("AAPL", convertedFixMessage.getString(Symbol.FIELD));
        assertEquals(Side.BUY, convertedFixMessage.getChar(Side.FIELD));
    }
    
    @Test
    public void testMessageValidator() throws Exception {
        // Create a valid FIX message
        quickfix.Message validMessage = createValidFixMessage();
        
        // Validate the message
        FixMessageValidator.ValidationResult validResult = FixMessageValidator.validate(validMessage);
        assertTrue(validResult.isValid(), "Valid message should pass validation");
        
        // Create an invalid message (missing required field)
        quickfix.Message invalidMessage = createValidFixMessage();
        invalidMessage.getHeader().removeField(SenderCompID.FIELD);
        
        // Validate the invalid message
        FixMessageValidator.ValidationResult invalidResult = FixMessageValidator.validate(invalidMessage);
        assertFalse(invalidResult.isValid(), "Invalid message should fail validation");
    }
    
    @Test
    public void testFixTransportFactory() throws ConfigError, IOException {
        // Test creating a listener
        FixTransportListener listener = FixTransportFactory.createListener(configFile);
        assertNotNull(listener);
        
        // Test creating a sender
        FixTransportSender sender = FixTransportFactory.createSender(configFile);
        assertNotNull(sender);
        
        // Test creating a default config
        String defaultConfigFile = tempDir.resolve("fix-default.cfg").toString();
        boolean created = FixTransportFactory.createDefaultConfig(
                defaultConfigFile, "SENDER", "TARGET", "localhost", 9876);
        assertTrue(created);
        assertTrue(Files.exists(Path.of(defaultConfigFile)));
    }
    
    // Helper methods
    
    private void createTestConfig(String filePath) throws IOException {
        StringBuilder config = new StringBuilder();
        
        // Default session settings
        config.append("[DEFAULT]\n");
        config.append("FileStorePath=").append(tempDir).append("/store\n");
        config.append("FileLogPath=").append(tempDir).append("/log\n");
        config.append("ConnectionType=initiator\n");
        config.append("ReconnectInterval=5\n");
        config.append("SenderCompID=SENDER\n");
        config.append("TargetCompID=TARGET\n");
        config.append("SocketConnectHost=localhost\n");
        config.append("SocketConnectPort=9876\n");
        config.append("StartTime=00:00:00\n");
        config.append("EndTime=00:00:00\n");
        config.append("HeartBtInt=30\n");
        config.append("ValidateUserDefinedFields=N\n");
        config.append("\n");
        
        // FIX 4.4 session
        config.append("[SESSION]\n");
        config.append("BeginString=FIX.4.4\n");
        
        // Write to file
        Files.write(Path.of(filePath), config.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private quickfix.Message createValidFixMessage() throws Exception {
        // Create a new FIX message
        quickfix.Message fixMessage = new quickfix.Message();
        
        // Set header fields
        fixMessage.getHeader().setField(new BeginString("FIX.4.4"));
        fixMessage.getHeader().setField(new SenderCompID("SENDER"));
        fixMessage.getHeader().setField(new TargetCompID("TARGET"));
        fixMessage.getHeader().setField(new MsgType(MsgType.NEW_ORDER_SINGLE));
        fixMessage.getHeader().setField(new MsgSeqNum(1));
        fixMessage.getHeader().setField(new SendingTime(new Date()));
        
        // Set body fields
        fixMessage.setField(new ClOrdID("TEST123"));
        fixMessage.setField(new Symbol("AAPL"));
        fixMessage.setField(new Side(Side.BUY));
        fixMessage.setField(new OrdType(OrdType.LIMIT));
        fixMessage.setField(new Price(150.50));
        fixMessage.setField(new OrderQty(100));
        fixMessage.setField(new TimeInForce(TimeInForce.DAY));
        fixMessage.setField(new TransactTime(new Date()));
        
        // Set trailer fields
        fixMessage.getTrailer().setField(new CheckSum("000"));
        
        return fixMessage;
    }
} 