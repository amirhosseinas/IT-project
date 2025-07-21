package org.apache.synapse.custom.message.builder;

import com.caucho.hessian.io.HessianOutput;
import org.apache.synapse.custom.message.Message;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Hessian message builder.
 */
public class HessianMessageBuilderTest {
    
    @Test
    public void testHessianMessageBuilder() throws Exception {
        // Create and serialize a test object
        TestObject testObject = new TestObject("Test Name", 42);
        byte[] serializedData = serializeObject(testObject);
        
        // Create input stream from serialized data
        ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
        
        // Build message using HessianMessageBuilder
        HessianMessageBuilder builder = new HessianMessageBuilder();
        Message message = builder.buildMessage(inputStream, "application/x-hessian");
        
        // Verify message properties
        assertNotNull(message);
        assertEquals("application/x-hessian", message.getContentType());
        assertEquals(TestObject.class.getName(), message.getProperty("hessian.objectClass"));
        
        // Verify deserialized object
        Object deserializedObject = message.getProperty("hessian.object");
        assertNotNull(deserializedObject);
        assertTrue(deserializedObject instanceof TestObject);
        
        TestObject result = (TestObject) deserializedObject;
        assertEquals("Test Name", result.getName());
        assertEquals(42, result.getValue());
    }
    
    @Test
    public void testContentTypeHandling() {
        HessianMessageBuilder builder = new HessianMessageBuilder();
        
        assertTrue(builder.canHandle("application/x-hessian"));
        assertTrue(builder.canHandle("application/hessian"));
        assertTrue(builder.canHandle("application/x-hessian; charset=utf-8"));
        
        assertFalse(builder.canHandle("application/json"));
        assertFalse(builder.canHandle("text/plain"));
        assertFalse(builder.canHandle(null));
    }
    
    @Test
    public void testInvalidHessianData() throws Exception {
        // Create invalid Hessian data
        byte[] invalidData = "This is not valid Hessian data".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidData);
        
        // Build message using HessianMessageBuilder
        HessianMessageBuilder builder = new HessianMessageBuilder();
        Message message = builder.buildMessage(inputStream, "application/x-hessian");
        
        // Verify message properties
        assertNotNull(message);
        assertEquals("application/x-hessian", message.getContentType());
        assertEquals(false, message.getProperty("hessian.valid"));
        assertNotNull(message.getProperty("hessian.error"));
    }
    
    private byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HessianOutput hessianOutput = new HessianOutput(baos);
        hessianOutput.writeObject(obj);
        return baos.toByteArray();
    }
    
    /**
     * Test serializable class for Hessian serialization.
     */
    public static class TestObject implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private int value;
        
        public TestObject() {
            // Default constructor required for Hessian serialization
        }
        
        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getValue() {
            return value;
        }
        
        public void setValue(int value) {
            this.value = value;
        }
    }
} 