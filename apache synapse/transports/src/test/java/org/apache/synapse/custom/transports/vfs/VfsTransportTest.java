package org.apache.synapse.custom.transports.vfs;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportSender;
import org.apache.synapse.custom.transports.vfs.protocol.VfsProtocolRegistry;
import org.apache.synapse.custom.transports.vfs.util.VfsFileOperationUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for VFS transport.
 */
public class VfsTransportTest {
    
    @TempDir
    Path tempDir;
    
    private Path inboxDir;
    private Path outboxDir;
    private Path processedDir;
    private Path failedDir;
    
    private VfsTransportSender sender;
    private VfsTransportListener listener;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create test directories
        inboxDir = tempDir.resolve("inbox");
        outboxDir = tempDir.resolve("outbox");
        processedDir = tempDir.resolve("processed");
        failedDir = tempDir.resolve("failed");
        
        Files.createDirectories(inboxDir);
        Files.createDirectories(outboxDir);
        Files.createDirectories(processedDir);
        Files.createDirectories(failedDir);
        
        // Create VFS sender
        sender = new VfsTransportSender();
        sender.init();
        
        // Create parameters for VFS listener
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vfs.after.process", "move");
        parameters.put("vfs.move.destination", "file://" + processedDir.toAbsolutePath());
        parameters.put("vfs.after.failure", "move");
        parameters.put("vfs.failure.destination", "file://" + failedDir.toAbsolutePath());
        
        // Create VFS listener
        listener = new VfsTransportListener(
                "file://" + inboxDir.toAbsolutePath(),
                "*.txt",
                false,
                1000,
                VfsTransportListener.VfsListenerMode.POLL,
                2,
                parameters
        );
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        // Stop listener if running
        if (listener != null && listener.isRunning()) {
            listener.stop();
        }
    }
    
    @Test
    public void testSendAndReceiveFile() throws Exception {
        // Set up a latch to wait for file processing
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder receivedContent = new StringBuilder();
        
        // Set message callback for listener
        listener.setMessageCallback(new TransportListener.MessageCallback() {
            @Override
            public Message onMessage(Message message) {
                try {
                    // Get file content
                    byte[] content = message.getPayload();
                    receivedContent.append(new String(content, StandardCharsets.UTF_8));
                    
                    // Create response
                    Message response = new Message(UUID.randomUUID().toString());
                    response.setDirection(Message.Direction.RESPONSE);
                    response.setProperty("vfs.response.file", "file://" + outboxDir.toAbsolutePath() + "/response.txt");
                    response.setPayload("Response content".getBytes(StandardCharsets.UTF_8));
                    
                    // Count down latch to signal file received
                    latch.countDown();
                    
                    return response;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        
        // Initialize and start listener
        listener.init();
        listener.start();
        
        // Create test file content
        String testContent = "Test VFS transport file content";
        
        // Create message to send
        Message message = new Message(UUID.randomUUID().toString());
        message.setDirection(Message.Direction.REQUEST);
        message.setContentType("text/plain");
        message.setPayload(testContent.getBytes(StandardCharsets.UTF_8));
        message.setProperty("vfs.operation", "write");
        
        // Send file to inbox directory
        String fileUri = "file://" + inboxDir.toAbsolutePath() + "/test.txt";
        Message response = sender.send(message, fileUri);
        
        // Wait for file to be processed
        boolean processed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(processed, "File should be processed within timeout");
        
        // Verify file content was received correctly
        assertEquals(testContent, receivedContent.toString(), "Received content should match sent content");
        
        // Verify response file was created
        File responseFile = outboxDir.resolve("response.txt").toFile();
        assertTrue(responseFile.exists(), "Response file should exist");
        
        // Verify original file was moved to processed directory
        File processedFile = processedDir.resolve("test.txt").toFile();
        assertTrue(processedFile.exists(), "File should be moved to processed directory");
    }
    
    @Test
    public void testFileOperations() throws Exception {
        // Test directory creation
        String dirUri = "file://" + tempDir.toAbsolutePath() + "/testdir";
        Message createDirMessage = new Message(UUID.randomUUID().toString());
        createDirMessage.setDirection(Message.Direction.REQUEST);
        createDirMessage.setProperty("vfs.operation", "mkdir");
        
        Message createDirResponse = sender.send(createDirMessage, dirUri);
        assertTrue((Boolean) createDirResponse.getProperty("vfs.operation.success"), "Directory creation should succeed");
        
        // Test file writing
        String fileUri = dirUri + "/testfile.txt";
        String fileContent = "Test file content";
        Message writeMessage = new Message(UUID.randomUUID().toString());
        writeMessage.setDirection(Message.Direction.REQUEST);
        writeMessage.setProperty("vfs.operation", "write");
        writeMessage.setPayload(fileContent.getBytes(StandardCharsets.UTF_8));
        
        Message writeResponse = sender.send(writeMessage, fileUri);
        assertTrue((Boolean) writeResponse.getProperty("vfs.operation.success"), "File write should succeed");
        
        // Test file reading
        Message readMessage = new Message(UUID.randomUUID().toString());
        readMessage.setDirection(Message.Direction.REQUEST);
        readMessage.setProperty("vfs.operation", "read");
        
        Message readResponse = sender.send(readMessage, fileUri);
        String readContent = new String(readResponse.getPayload(), StandardCharsets.UTF_8);
        assertEquals(fileContent, readContent, "Read content should match written content");
        
        // Test file copying
        String copyUri = dirUri + "/testfile-copy.txt";
        Message copyMessage = new Message(UUID.randomUUID().toString());
        copyMessage.setDirection(Message.Direction.REQUEST);
        copyMessage.setProperty("vfs.operation", "copy");
        copyMessage.setProperty("vfs.destination", copyUri);
        
        Message copyResponse = sender.send(copyMessage, fileUri);
        assertTrue((Boolean) copyResponse.getProperty("vfs.operation.success"), "File copy should succeed");
        
        // Verify copied file exists and has correct content
        Message readCopyMessage = new Message(UUID.randomUUID().toString());
        readCopyMessage.setDirection(Message.Direction.REQUEST);
        readCopyMessage.setProperty("vfs.operation", "read");
        
        Message readCopyResponse = sender.send(readCopyMessage, copyUri);
        String readCopyContent = new String(readCopyResponse.getPayload(), StandardCharsets.UTF_8);
        assertEquals(fileContent, readCopyContent, "Copied file content should match original");
        
        // Test file moving
        String moveUri = dirUri + "/testfile-moved.txt";
        Message moveMessage = new Message(UUID.randomUUID().toString());
        moveMessage.setDirection(Message.Direction.REQUEST);
        moveMessage.setProperty("vfs.operation", "move");
        moveMessage.setProperty("vfs.destination", moveUri);
        
        Message moveResponse = sender.send(moveMessage, copyUri);
        assertTrue((Boolean) moveResponse.getProperty("vfs.operation.success"), "File move should succeed");
        
        // Verify source file no longer exists
        Message checkSourceMessage = new Message(UUID.randomUUID().toString());
        checkSourceMessage.setDirection(Message.Direction.REQUEST);
        checkSourceMessage.setProperty("vfs.operation", "exists");
        
        Message checkSourceResponse = sender.send(checkSourceMessage, copyUri);
        assertFalse((Boolean) checkSourceResponse.getProperty("vfs.file.exists"), "Source file should not exist after move");
        
        // Verify destination file exists
        Message checkDestMessage = new Message(UUID.randomUUID().toString());
        checkDestMessage.setDirection(Message.Direction.REQUEST);
        checkDestMessage.setProperty("vfs.operation", "exists");
        
        Message checkDestResponse = sender.send(checkDestMessage, moveUri);
        assertTrue((Boolean) checkDestResponse.getProperty("vfs.file.exists"), "Destination file should exist after move");
        
        // Test directory listing
        Message listMessage = new Message(UUID.randomUUID().toString());
        listMessage.setDirection(Message.Direction.REQUEST);
        listMessage.setProperty("vfs.operation", "list");
        
        Message listResponse = sender.send(listMessage, dirUri);
        String listing = new String(listResponse.getPayload(), StandardCharsets.UTF_8);
        assertTrue(listing.contains("testfile.txt"), "Directory listing should contain original file");
        assertTrue(listing.contains("testfile-moved.txt"), "Directory listing should contain moved file");
        
        // Test file deletion
        Message deleteMessage = new Message(UUID.randomUUID().toString());
        deleteMessage.setDirection(Message.Direction.REQUEST);
        deleteMessage.setProperty("vfs.operation", "delete");
        
        Message deleteResponse = sender.send(deleteMessage, moveUri);
        assertTrue((Boolean) deleteResponse.getProperty("vfs.operation.success"), "File deletion should succeed");
        
        // Verify file was deleted
        Message checkDeletedMessage = new Message(UUID.randomUUID().toString());
        checkDeletedMessage.setDirection(Message.Direction.REQUEST);
        checkDeletedMessage.setProperty("vfs.operation", "exists");
        
        Message checkDeletedResponse = sender.send(checkDeletedMessage, moveUri);
        assertFalse((Boolean) checkDeletedResponse.getProperty("vfs.file.exists"), "File should not exist after deletion");
    }
    
    @Test
    public void testVfsFileOperationUtil() throws Exception {
        // Test file operations using utility class
        FileSystemManager fsManager = VFS.getManager();
        
        // Create test file
        String testContent = "Test VFS file operation utility";
        Path testFile = tempDir.resolve("utiltest.txt");
        Files.write(testFile, testContent.getBytes(StandardCharsets.UTF_8));
        
        // Test resolving file
        FileObject fileObject = VfsFileOperationUtil.resolveFile(
                "file://" + testFile.toAbsolutePath(),
                null, null, null
        );
        
        // Test reading file
        byte[] content = VfsFileOperationUtil.readFile(fileObject);
        assertEquals(testContent, new String(content, StandardCharsets.UTF_8), "Read content should match written content");
        
        // Test writing file
        String newContent = "Updated content";
        VfsFileOperationUtil.writeFile(fileObject, newContent.getBytes(StandardCharsets.UTF_8), false);
        
        // Verify content was updated
        byte[] updatedContent = VfsFileOperationUtil.readFile(fileObject);
        assertEquals(newContent, new String(updatedContent, StandardCharsets.UTF_8), "Updated content should match");
        
        // Test file info
        Map<String, Object> fileInfo = VfsFileOperationUtil.getFileInfo(fileObject);
        assertEquals("utiltest.txt", fileInfo.get("name"), "File name should match");
        
        // Test file exists
        boolean exists = VfsFileOperationUtil.fileExists(
                "file://" + testFile.toAbsolutePath(),
                null, null, null
        );
        assertTrue(exists, "File should exist");
        
        // Test deleting file
        boolean deleted = VfsFileOperationUtil.deleteFile(fileObject);
        assertTrue(deleted, "File should be deleted");
        
        // Test file no longer exists
        exists = VfsFileOperationUtil.fileExists(
                "file://" + testFile.toAbsolutePath(),
                null, null, null
        );
        assertFalse(exists, "File should not exist after deletion");
    }
    
    @Test
    public void testProtocolRegistry() {
        // Test protocol registry
        VfsProtocolRegistry registry = VfsProtocolRegistry.getInstance();
        
        // Verify file protocol is registered
        assertTrue(registry.isProtocolRegistered("file"), "File protocol should be registered");
        
        // Verify FTP protocol is registered
        assertTrue(registry.isProtocolRegistered("ftp"), "FTP protocol should be registered");
        
        // Verify SFTP protocol is registered
        assertTrue(registry.isProtocolRegistered("sftp"), "SFTP protocol should be registered");
        
        // Test getting protocol handler for URI
        assertNotNull(registry.getProtocolHandler("file:///tmp/test.txt"), "Should get handler for file URI");
        assertNotNull(registry.getProtocolHandler("ftp://example.com/test.txt"), "Should get handler for FTP URI");
        assertNotNull(registry.getProtocolHandler("sftp://example.com/test.txt"), "Should get handler for SFTP URI");
        
        // Test getting protocol handler by scheme
        assertNotNull(registry.getProtocolHandlerByScheme("file"), "Should get handler for file scheme");
        assertNotNull(registry.getProtocolHandlerByScheme("ftp"), "Should get handler for FTP scheme");
        assertNotNull(registry.getProtocolHandlerByScheme("sftp"), "Should get handler for SFTP scheme");
    }
} 