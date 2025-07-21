package org.apache.synapse.custom.message.builder.examples;

import com.caucho.hessian.io.HessianOutput;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.message.builder.HessianMessageBuilder;
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Example demonstrating how to use the Hessian message builder.
 */
public class HessianMessageBuilderExample {
    private static final Logger logger = LoggerFactory.getLogger(HessianMessageBuilderExample.class);
    
    public static void main(String[] args) {
        try {
            // Create a serializable object
            Person person = new Person("John Doe", 30, "john@example.com");
            
            // Serialize the object using Hessian
            byte[] serializedData = serializeObject(person);
            
            // Build a message from the serialized data
            Message message = MessageBuilderUtil.buildHessianMessage(serializedData);
            
            // Print message info
            printMessageInfo(message);
            
            // Access the deserialized object from the message
            Object deserializedObject = message.getProperty("hessian.object");
            if (deserializedObject instanceof Person) {
                Person deserializedPerson = (Person) deserializedObject;
                logger.info("Deserialized Person: {}", deserializedPerson);
            }
            
            logger.info("Example completed successfully");
        } catch (Exception e) {
            logger.error("Error in Hessian message builder example", e);
        }
    }
    
    private static byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HessianOutput hessianOutput = new HessianOutput(baos);
        hessianOutput.writeObject(obj);
        return baos.toByteArray();
    }
    
    private static void printMessageInfo(Message message) {
        logger.info("Message ID: {}", message.getMessageId());
        logger.info("Content Type: {}", message.getContentType());
        logger.info("Payload Size: {} bytes", message.getPayload().length);
        logger.info("Object Class: {}", message.getProperty("hessian.objectClass"));
        logger.info("Object Hash Code: {}", message.getProperty("hessian.objectHashCode"));
        logger.info("---");
    }
    
    /**
     * Example serializable class for demonstration.
     */
    public static class Person implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private int age;
        private String email;
        
        public Person() {
            // Default constructor required for Hessian serialization
        }
        
        public Person(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getAge() {
            return age;
        }
        
        public void setAge(int age) {
            this.age = age;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }
} 