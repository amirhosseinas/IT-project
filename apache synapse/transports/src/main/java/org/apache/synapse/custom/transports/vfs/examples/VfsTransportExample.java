package org.apache.synapse.custom.transports.vfs.examples;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportManager;
import org.apache.synapse.custom.transports.TransportSender;
import org.apache.synapse.custom.transports.vfs.VfsTransportFactory;
import org.apache.synapse.custom.transports.vfs.VfsTransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example demonstrating how to use the VFS transport.
 */
public class VfsTransportExample {
    private static final Logger logger = LoggerFactory.getLogger(VfsTransportExample.class);
    
    /**
     * Main method to run the example
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Create transport manager
            TransportManager transportManager = new TransportManager();
            
            // Example 1: Set up a file listener
            setupFileListener(transportManager);
            
            // Example 2: Set up an FTP listener
            setupFtpListener(transportManager);
            
            // Example 3: Send a file using the VFS transport
            sendFileExample(transportManager);
            
            // Example 4: Perform file operations using the VFS transport
            fileOperationsExample(transportManager);
            
            // Start all listeners
            transportManager.initializeListeners();
            transportManager.startListeners();
            
            logger.info("VFS transport example running. Press Ctrl+C to exit.");
            
            // Keep the application running
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            logger.error("Error running VFS transport example", e);
        }
    }
    
    /**
     * Set up a file listener
     * 
     * @param transportManager The transport manager
     * @throws TransportException if setup fails
     */
    private static void setupFileListener(TransportManager transportManager) throws TransportException {
        // Create parameters for file listener
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vfs.after.process", "move");
        parameters.put("vfs.move.destination", "file:///tmp/processed");
        parameters.put("vfs.after.failure", "move");
        parameters.put("vfs.failure.destination", "file:///tmp/failed");
        
        // Create file listener
        TransportListener fileListener = VfsTransportFactory.createListener(
                "file:///tmp/inbox",             // Directory to monitor
                "*.xml",                         // File pattern
                true,                           // Recursive
                5000,                           // Polling interval (ms)
                VfsTransportListener.VfsListenerMode.HYBRID, // Listener mode
                5,                              // Worker threads
                parameters                      // Parameters
        );
        
        // Set message callback
        fileListener.setMessageCallback(new TransportListener.MessageCallback() {
            @Override
            public Message onMessage(Message message) {
                try {
                    logger.info("Received file: {}", message.getProperty("vfs.file.name"));
                    
                    // Process the message
                    // ...
                    
                    // Create response message
                    Message response = new Message(UUID.randomUUID().toString());
                    response.setDirection(Message.Direction.RESPONSE);
                    response.setProperty("vfs.response.file", "file:///tmp/outbox/response-" + 
                                       message.getProperty("vfs.file.name"));
                    response.setPayload("Processed successfully".getBytes(StandardCharsets.UTF_8));
                    
                    return response;
                } catch (Exception e) {
                    logger.error("Error processing file", e);
                    return null;
                }
            }
        });
        
        // Register the listener
        transportManager.registerListener("file-listener", fileListener);
        logger.info("File listener registered");
    }
    
    /**
     * Set up an FTP listener
     * 
     * @param transportManager The transport manager
     * @throws TransportException if setup fails
     */
    private static void setupFtpListener(TransportManager transportManager) throws TransportException {
        // Create parameters for FTP listener
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vfs.after.process", "delete");
        parameters.put("ftp.passive", "true");
        parameters.put("ftp.controlEncoding", "UTF-8");
        
        // Create FTP listener
        TransportListener ftpListener = VfsTransportFactory.createListener(
                "ftp://username:password@ftpserver.example.com/incoming", // Directory to monitor
                "*.csv",                        // File pattern
                false,                          // Not recursive
                60000,                          // Polling interval (ms)
                VfsTransportListener.VfsListenerMode.POLL, // Listener mode (FTP doesn't support watching)
                3,                              // Worker threads
                parameters                      // Parameters
        );
        
        // Set message callback
        ftpListener.setMessageCallback(new TransportListener.MessageCallback() {
            @Override
            public Message onMessage(Message message) {
                try {
                    logger.info("Received FTP file: {}", message.getProperty("vfs.file.name"));
                    
                    // Process the message
                    // ...
                    
                    // No response needed
                    return null;
                } catch (Exception e) {
                    logger.error("Error processing FTP file", e);
                    return null;
                }
            }
        });
        
        // Register the listener
        transportManager.registerListener("ftp-listener", ftpListener);
        logger.info("FTP listener registered");
    }
    
    /**
     * Example of sending a file using the VFS transport
     * 
     * @param transportManager The transport manager
     * @throws TransportException if sending fails
     */
    private static void sendFileExample(TransportManager transportManager) throws TransportException {
        // Create VFS sender
        TransportSender sender = VfsTransportFactory.createSender();
        
        // Initialize sender
        sender.init();
        
        // Register the sender
        transportManager.registerSender("vfs-sender", sender);
        
        // Create a message to send
        Message message = new Message(UUID.randomUUID().toString());
        message.setDirection(Message.Direction.REQUEST);
        message.setContentType("text/plain");
        message.setPayload("Hello, VFS transport!".getBytes(StandardCharsets.UTF_8));
        
        // Set operation to write
        message.setProperty("vfs.operation", "write");
        
        // Set write mode to overwrite
        message.setProperty("vfs.write.mode", "overwrite");
        
        // Send the message to a file
        Message response = sender.send(message, "file:///tmp/outbox/example.txt");
        
        logger.info("File sent successfully: {}", response.getProperty("vfs.file.path"));
    }
    
    /**
     * Example of performing file operations using the VFS transport
     * 
     * @param transportManager The transport manager
     * @throws TransportException if operations fail
     */
    private static void fileOperationsExample(TransportManager transportManager) throws TransportException {
        // Get the VFS sender
        TransportSender sender = transportManager.getSender("vfs-sender");
        
        // Example 1: Create a directory
        Message createDirMessage = new Message(UUID.randomUUID().toString());
        createDirMessage.setDirection(Message.Direction.REQUEST);
        createDirMessage.setProperty("vfs.operation", "mkdir");
        
        Message createDirResponse = sender.send(createDirMessage, "file:///tmp/vfs-example");
        
        logger.info("Directory created: {}", createDirResponse.getProperty("vfs.directory.created"));
        
        // Example 2: Write a file
        Message writeMessage = new Message(UUID.randomUUID().toString());
        writeMessage.setDirection(Message.Direction.REQUEST);
        writeMessage.setProperty("vfs.operation", "write");
        writeMessage.setContentType("application/json");
        writeMessage.setPayload("{\"example\": \"data\"}".getBytes(StandardCharsets.UTF_8));
        
        Message writeResponse = sender.send(writeMessage, "file:///tmp/vfs-example/data.json");
        
        logger.info("File written: {}", writeResponse.getProperty("vfs.file.path"));
        
        // Example 3: Read a file
        Message readMessage = new Message(UUID.randomUUID().toString());
        readMessage.setDirection(Message.Direction.REQUEST);
        readMessage.setProperty("vfs.operation", "read");
        
        Message readResponse = sender.send(readMessage, "file:///tmp/vfs-example/data.json");
        
        logger.info("File read, content: {}", new String(readResponse.getPayload(), StandardCharsets.UTF_8));
        
        // Example 4: Copy a file
        Message copyMessage = new Message(UUID.randomUUID().toString());
        copyMessage.setDirection(Message.Direction.REQUEST);
        copyMessage.setProperty("vfs.operation", "copy");
        copyMessage.setProperty("vfs.destination", "file:///tmp/vfs-example/data-copy.json");
        
        Message copyResponse = sender.send(copyMessage, "file:///tmp/vfs-example/data.json");
        
        logger.info("File copied from {} to {}", 
                  copyResponse.getProperty("vfs.source.path"),
                  copyResponse.getProperty("vfs.destination.path"));
        
        // Example 5: Move a file
        Message moveMessage = new Message(UUID.randomUUID().toString());
        moveMessage.setDirection(Message.Direction.REQUEST);
        moveMessage.setProperty("vfs.operation", "move");
        moveMessage.setProperty("vfs.destination", "file:///tmp/vfs-example/moved-data.json");
        
        Message moveResponse = sender.send(moveMessage, "file:///tmp/vfs-example/data-copy.json");
        
        logger.info("File moved from {} to {}", 
                  moveResponse.getProperty("vfs.source.path"),
                  moveResponse.getProperty("vfs.destination.path"));
        
        // Example 6: List directory
        Message listMessage = new Message(UUID.randomUUID().toString());
        listMessage.setDirection(Message.Direction.REQUEST);
        listMessage.setProperty("vfs.operation", "list");
        
        Message listResponse = sender.send(listMessage, "file:///tmp/vfs-example");
        
        logger.info("Directory listing: {} files", listResponse.getProperty("vfs.file.count"));
        logger.info("Listing content: {}", new String(listResponse.getPayload(), StandardCharsets.UTF_8));
        
        // Example 7: Delete a file
        Message deleteMessage = new Message(UUID.randomUUID().toString());
        deleteMessage.setDirection(Message.Direction.REQUEST);
        deleteMessage.setProperty("vfs.operation", "delete");
        
        Message deleteResponse = sender.send(deleteMessage, "file:///tmp/vfs-example/moved-data.json");
        
        logger.info("File deleted: {}", deleteResponse.getProperty("vfs.operation.success"));
    }
} 