package org.apache.synapse.custom.transports.vfs;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

/**
 * VFS Transport sender implementation based on Apache Commons VFS.
 * Supports file, FTP, SFTP and other VFS-supported protocols.
 */
public class VfsTransportSender implements TransportSender {
    private static final Logger logger = LoggerFactory.getLogger(VfsTransportSender.class);
    
    private FileSystemManager fsManager;
    
    @Override
    public void init() throws TransportException {
        try {
            // Initialize VFS file system manager
            fsManager = VFS.getManager();
            logger.info("VFS transport sender initialized");
        } catch (Exception e) {
            throw new TransportException("Failed to initialize VFS transport sender", e);
        }
    }
    
    @Override
    public Message send(Message message, String endpoint) throws TransportException {
        if (fsManager == null) {
            throw new TransportException("VFS transport sender not initialized");
        }
        
        try {
            // Get operation from message properties
            String operation = (String) message.getProperty("vfs.operation");
            if (operation == null) {
                operation = "write"; // Default operation
            }
            
            // Create response message
            Message responseMessage = new Message(UUID.randomUUID().toString());
            responseMessage.setDirection(Message.Direction.RESPONSE);
            
            // Create file system options
            FileSystemOptions fileSystemOptions = createFileSystemOptions(message);
            
            // Perform the requested operation
            switch (operation.toLowerCase()) {
                case "write":
                    writeFile(message, endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "read":
                    readFile(endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "delete":
                    deleteFile(endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "move":
                    moveFile(message, endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "copy":
                    copyFile(message, endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "exists":
                    checkFileExists(endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "list":
                    listDirectory(endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                case "mkdir":
                    createDirectory(endpoint, fileSystemOptions, responseMessage);
                    break;
                    
                default:
                    throw new TransportException("Unsupported VFS operation: " + operation);
            }
            
            return responseMessage;
        } catch (Exception e) {
            throw new TransportException("Failed to perform VFS operation", e);
        }
    }
    
    @Override
    public boolean canHandle(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        
        // Check if the endpoint starts with any of the supported protocols
        return endpoint.startsWith("file://") || 
               endpoint.startsWith("ftp://") || 
               endpoint.startsWith("sftp://") || 
               endpoint.startsWith("ftps://") || 
               endpoint.startsWith("ram://") || 
               endpoint.startsWith("zip://") || 
               endpoint.startsWith("jar://") || 
               endpoint.startsWith("res://") || 
               endpoint.startsWith("tmp://");
    }
    
    @Override
    public void close() {
        // Nothing to close for VFS transport sender
        logger.info("VFS transport sender closed");
    }
    
    /**
     * Create file system options based on message properties
     * 
     * @param message The message containing properties
     * @return The file system options
     * @throws FileSystemException if creating options fails
     */
    private FileSystemOptions createFileSystemOptions(Message message) throws FileSystemException {
        FileSystemOptions opts = new FileSystemOptions();
        
        // Get authentication information from message properties
        String username = (String) message.getProperty("vfs.username");
        String password = (String) message.getProperty("vfs.password");
        
        // Set authentication if provided
        if (username != null && password != null) {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(null, username, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        }
        
        // Configure FTP options
        String passiveMode = (String) message.getProperty("vfs.ftp.passive");
        if ("true".equalsIgnoreCase(passiveMode)) {
            FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true);
        }
        
        // Configure SFTP options
        String strictHostKeyChecking = (String) message.getProperty("vfs.sftp.strictHostKeyChecking");
        if ("false".equalsIgnoreCase(strictHostKeyChecking)) {
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
        }
        
        String privateKeyPath = (String) message.getProperty("vfs.sftp.privateKey");
        if (privateKeyPath != null) {
            SftpFileSystemConfigBuilder.getInstance().setIdentities(opts, new String[]{privateKeyPath});
        }
        
        return opts;
    }
    
    /**
     * Write content to a file
     * 
     * @param message The message containing the content
     * @param endpoint The file endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if writing fails
     */
    private void writeFile(Message message, String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject file = fsManager.resolveFile(endpoint, opts);
        
        try {
            // Ensure parent directory exists
            if (!file.getParent().exists()) {
                file.getParent().createFolder();
            }
            
            // Get write mode from message properties
            String writeMode = (String) message.getProperty("vfs.write.mode");
            if (writeMode == null) {
                writeMode = "overwrite"; // Default mode
            }
            
            // Write content based on mode
            try (FileContent content = file.getContent()) {
                OutputStream out;
                
                switch (writeMode.toLowerCase()) {
                    case "append":
                        out = content.getOutputStream(true); // Append mode
                        break;
                    case "overwrite":
                    default:
                        out = content.getOutputStream(); // Overwrite mode
                        break;
                }
                
                // Write payload to file
                if (message.getPayload() != null) {
                    out.write(message.getPayload());
                }
                
                out.flush();
            }
            
            // Set response properties
            responseMessage.setProperty("vfs.operation.success", true);
            responseMessage.setProperty("vfs.file.path", file.getName().getPath());
            responseMessage.setProperty("vfs.file.uri", file.getName().getURI());
            
            logger.debug("File written successfully: {}", file.getName());
        } finally {
            file.close();
        }
    }
    
    /**
     * Read content from a file
     * 
     * @param endpoint The file endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if reading fails
     */
    private void readFile(String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject file = fsManager.resolveFile(endpoint, opts);
        
        try {
            if (!file.exists()) {
                throw new FileSystemException("File does not exist: " + endpoint);
            }
            
            if (file.getType() != FileType.FILE) {
                throw new FileSystemException("Not a file: " + endpoint);
            }
            
            // Read file content
            try (FileContent content = file.getContent()) {
                byte[] data = content.getInputStream().readAllBytes();
                
                // Set response payload and properties
                responseMessage.setPayload(data);
                responseMessage.setProperty("vfs.operation.success", true);
                responseMessage.setProperty("vfs.file.path", file.getName().getPath());
                responseMessage.setProperty("vfs.file.uri", file.getName().getURI());
                responseMessage.setProperty("vfs.file.size", content.getSize());
                responseMessage.setProperty("vfs.file.lastModified", content.getLastModifiedTime());
                
                // Set content type
                String contentType = determineContentType(file);
                responseMessage.setContentType(contentType);
                
                logger.debug("File read successfully: {}", file.getName());
            }
        } finally {
            file.close();
        }
    }
    
    /**
     * Delete a file
     * 
     * @param endpoint The file endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if deletion fails
     */
    private void deleteFile(String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject file = fsManager.resolveFile(endpoint, opts);
        
        try {
            if (!file.exists()) {
                throw new FileSystemException("File does not exist: " + endpoint);
            }
            
            // Delete the file
            boolean deleted = file.delete();
            
            // Set response properties
            responseMessage.setProperty("vfs.operation.success", deleted);
            responseMessage.setProperty("vfs.file.path", file.getName().getPath());
            responseMessage.setProperty("vfs.file.uri", file.getName().getURI());
            
            logger.debug("File deleted successfully: {}", file.getName());
        } finally {
            file.close();
        }
    }
    
    /**
     * Move a file from source to destination
     * 
     * @param message The message containing properties
     * @param endpoint The source file endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if moving fails
     */
    private void moveFile(Message message, String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject sourceFile = fsManager.resolveFile(endpoint, opts);
        
        try {
            if (!sourceFile.exists()) {
                throw new FileSystemException("Source file does not exist: " + endpoint);
            }
            
            // Get destination path
            String destinationPath = (String) message.getProperty("vfs.destination");
            if (destinationPath == null) {
                throw new FileSystemException("Destination path not specified");
            }
            
            FileObject destinationFile = fsManager.resolveFile(destinationPath, opts);
            
            try {
                // Ensure destination parent directory exists
                if (!destinationFile.getParent().exists()) {
                    destinationFile.getParent().createFolder();
                }
                
                // Move the file
                sourceFile.moveTo(destinationFile);
                
                // Set response properties
                responseMessage.setProperty("vfs.operation.success", true);
                responseMessage.setProperty("vfs.source.path", sourceFile.getName().getPath());
                responseMessage.setProperty("vfs.destination.path", destinationFile.getName().getPath());
                
                logger.debug("File moved successfully from {} to {}", sourceFile.getName(), destinationFile.getName());
            } finally {
                destinationFile.close();
            }
        } finally {
            sourceFile.close();
        }
    }
    
    /**
     * Copy a file from source to destination
     * 
     * @param message The message containing properties
     * @param endpoint The source file endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if copying fails
     */
    private void copyFile(Message message, String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject sourceFile = fsManager.resolveFile(endpoint, opts);
        
        try {
            if (!sourceFile.exists()) {
                throw new FileSystemException("Source file does not exist: " + endpoint);
            }
            
            // Get destination path
            String destinationPath = (String) message.getProperty("vfs.destination");
            if (destinationPath == null) {
                throw new FileSystemException("Destination path not specified");
            }
            
            FileObject destinationFile = fsManager.resolveFile(destinationPath, opts);
            
            try {
                // Ensure destination parent directory exists
                if (!destinationFile.getParent().exists()) {
                    destinationFile.getParent().createFolder();
                }
                
                // Copy the file
                destinationFile.copyFrom(sourceFile, Selectors.SELECT_SELF);
                
                // Set response properties
                responseMessage.setProperty("vfs.operation.success", true);
                responseMessage.setProperty("vfs.source.path", sourceFile.getName().getPath());
                responseMessage.setProperty("vfs.destination.path", destinationFile.getName().getPath());
                
                logger.debug("File copied successfully from {} to {}", sourceFile.getName(), destinationFile.getName());
            } finally {
                destinationFile.close();
            }
        } finally {
            sourceFile.close();
        }
    }
    
    /**
     * Check if a file exists
     * 
     * @param endpoint The file endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if checking fails
     */
    private void checkFileExists(String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject file = fsManager.resolveFile(endpoint, opts);
        
        try {
            boolean exists = file.exists();
            
            // Set response properties
            responseMessage.setProperty("vfs.operation.success", true);
            responseMessage.setProperty("vfs.file.exists", exists);
            responseMessage.setProperty("vfs.file.path", file.getName().getPath());
            responseMessage.setProperty("vfs.file.uri", file.getName().getURI());
            
            if (exists) {
                responseMessage.setProperty("vfs.file.type", file.getType().getName());
            }
            
            logger.debug("File existence checked: {} (exists: {})", file.getName(), exists);
        } finally {
            file.close();
        }
    }
    
    /**
     * List files in a directory
     * 
     * @param endpoint The directory endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if listing fails
     */
    private void listDirectory(String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject directory = fsManager.resolveFile(endpoint, opts);
        
        try {
            if (!directory.exists()) {
                throw new FileSystemException("Directory does not exist: " + endpoint);
            }
            
            if (directory.getType() != FileType.FOLDER) {
                throw new FileSystemException("Not a directory: " + endpoint);
            }
            
            // List files in the directory
            FileObject[] children = directory.getChildren();
            
            // Build file listing
            StringBuilder listing = new StringBuilder();
            for (FileObject child : children) {
                listing.append(child.getName().getBaseName())
                       .append(",")
                       .append(child.getType().getName())
                       .append(",")
                       .append(child.getContent().getSize())
                       .append(",")
                       .append(child.getContent().getLastModifiedTime())
                       .append("\n");
            }
            
            // Set response payload and properties
            responseMessage.setPayload(listing.toString().getBytes());
            responseMessage.setContentType("text/plain");
            responseMessage.setProperty("vfs.operation.success", true);
            responseMessage.setProperty("vfs.directory.path", directory.getName().getPath());
            responseMessage.setProperty("vfs.directory.uri", directory.getName().getURI());
            responseMessage.setProperty("vfs.file.count", children.length);
            
            logger.debug("Directory listed successfully: {} ({} files)", directory.getName(), children.length);
        } finally {
            directory.close();
        }
    }
    
    /**
     * Create a directory
     * 
     * @param endpoint The directory endpoint
     * @param opts File system options
     * @param responseMessage The response message to update
     * @throws Exception if creation fails
     */
    private void createDirectory(String endpoint, FileSystemOptions opts, Message responseMessage) throws Exception {
        FileObject directory = fsManager.resolveFile(endpoint, opts);
        
        try {
            if (directory.exists()) {
                // Directory already exists
                responseMessage.setProperty("vfs.operation.success", true);
                responseMessage.setProperty("vfs.directory.path", directory.getName().getPath());
                responseMessage.setProperty("vfs.directory.uri", directory.getName().getURI());
                responseMessage.setProperty("vfs.directory.already.exists", true);
                
                logger.debug("Directory already exists: {}", directory.getName());
                return;
            }
            
            // Create the directory
            directory.createFolder();
            
            // Set response properties
            responseMessage.setProperty("vfs.operation.success", true);
            responseMessage.setProperty("vfs.directory.path", directory.getName().getPath());
            responseMessage.setProperty("vfs.directory.uri", directory.getName().getURI());
            responseMessage.setProperty("vfs.directory.created", true);
            
            logger.debug("Directory created successfully: {}", directory.getName());
        } finally {
            directory.close();
        }
    }
    
    /**
     * Determine the content type of a file
     * 
     * @param file The file to check
     * @return The content type
     */
    private String determineContentType(FileObject file) {
        String fileName = file.getName().getBaseName();
        
        // Simple content type detection based on file extension
        if (fileName.endsWith(".xml")) {
            return "application/xml";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else {
            return "application/octet-stream";
        }
    }
} 