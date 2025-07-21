package org.apache.synapse.custom.transports.vfs.util;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.vfs.protocol.VfsProtocolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for VFS file operations.
 * Provides common file operations that can be used by both the transport listener and sender.
 */
public class VfsFileOperationUtil {
    private static final Logger logger = LoggerFactory.getLogger(VfsFileOperationUtil.class);
    
    private static final FileSystemManager fsManager;
    
    static {
        try {
            fsManager = VFS.getManager();
        } catch (FileSystemException e) {
            throw new RuntimeException("Failed to initialize VFS file system manager", e);
        }
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private VfsFileOperationUtil() {
    }
    
    /**
     * Resolve a file URI to a file object
     * 
     * @param uri The file URI
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @return The resolved file object
     * @throws TransportException if resolving the file fails
     */
    public static FileObject resolveFile(String uri, String username, String password, 
                                       Map<String, Object> parameters) throws TransportException {
        try {
            // Create file system options
            FileSystemOptions opts = new FileSystemOptions();
            
            // Configure options for the protocol
            VfsProtocolRegistry.getInstance().configureFileSystemOptions(uri, opts, username, password, parameters);
            
            // Resolve the file
            return fsManager.resolveFile(uri, opts);
        } catch (FileSystemException e) {
            throw new TransportException("Failed to resolve file: " + uri, e);
        }
    }
    
    /**
     * Read the content of a file
     * 
     * @param file The file to read
     * @return The file content as a byte array
     * @throws TransportException if reading the file fails
     */
    public static byte[] readFile(FileObject file) throws TransportException {
        try {
            if (!file.exists()) {
                throw new TransportException("File does not exist: " + file.getName());
            }
            
            if (file.getType() != FileType.FILE) {
                throw new TransportException("Not a file: " + file.getName());
            }
            
            try (FileContent content = file.getContent();
                 InputStream is = content.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            throw new TransportException("Failed to read file: " + file.getName(), e);
        }
    }
    
    /**
     * Write content to a file
     * 
     * @param file The file to write to
     * @param data The data to write
     * @param append Whether to append to the file
     * @throws TransportException if writing the file fails
     */
    public static void writeFile(FileObject file, byte[] data, boolean append) throws TransportException {
        try {
            // Ensure parent directory exists
            if (!file.getParent().exists()) {
                file.getParent().createFolder();
            }
            
            try (FileContent content = file.getContent();
                 OutputStream os = append ? content.getOutputStream(true) : content.getOutputStream()) {
                if (data != null) {
                    os.write(data);
                }
                os.flush();
            }
        } catch (IOException e) {
            throw new TransportException("Failed to write file: " + file.getName(), e);
        }
    }
    
    /**
     * Delete a file
     * 
     * @param file The file to delete
     * @return true if the file was deleted, false otherwise
     * @throws TransportException if deleting the file fails
     */
    public static boolean deleteFile(FileObject file) throws TransportException {
        try {
            if (!file.exists()) {
                return false;
            }
            
            return file.delete();
        } catch (FileSystemException e) {
            throw new TransportException("Failed to delete file: " + file.getName(), e);
        }
    }
    
    /**
     * Move a file to a destination
     * 
     * @param sourceFile The source file
     * @param destinationUri The destination URI
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @throws TransportException if moving the file fails
     */
    public static void moveFile(FileObject sourceFile, String destinationUri, 
                              String username, String password, 
                              Map<String, Object> parameters) throws TransportException {
        try {
            if (!sourceFile.exists()) {
                throw new TransportException("Source file does not exist: " + sourceFile.getName());
            }
            
            FileObject destinationFile = resolveFile(destinationUri, username, password, parameters);
            
            try {
                // Ensure destination parent directory exists
                if (!destinationFile.getParent().exists()) {
                    destinationFile.getParent().createFolder();
                }
                
                // Move the file
                sourceFile.moveTo(destinationFile);
            } finally {
                destinationFile.close();
            }
        } catch (FileSystemException e) {
            throw new TransportException("Failed to move file: " + sourceFile.getName(), e);
        }
    }
    
    /**
     * Copy a file to a destination
     * 
     * @param sourceFile The source file
     * @param destinationUri The destination URI
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @throws TransportException if copying the file fails
     */
    public static void copyFile(FileObject sourceFile, String destinationUri, 
                              String username, String password, 
                              Map<String, Object> parameters) throws TransportException {
        try {
            if (!sourceFile.exists()) {
                throw new TransportException("Source file does not exist: " + sourceFile.getName());
            }
            
            FileObject destinationFile = resolveFile(destinationUri, username, password, parameters);
            
            try {
                // Ensure destination parent directory exists
                if (!destinationFile.getParent().exists()) {
                    destinationFile.getParent().createFolder();
                }
                
                // Copy the file
                destinationFile.copyFrom(sourceFile, Selectors.SELECT_SELF);
            } finally {
                destinationFile.close();
            }
        } catch (FileSystemException e) {
            throw new TransportException("Failed to copy file: " + sourceFile.getName(), e);
        }
    }
    
    /**
     * Create a directory
     * 
     * @param directoryUri The directory URI
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @return true if the directory was created, false if it already exists
     * @throws TransportException if creating the directory fails
     */
    public static boolean createDirectory(String directoryUri, String username, String password, 
                                        Map<String, Object> parameters) throws TransportException {
        FileObject directory = null;
        try {
            directory = resolveFile(directoryUri, username, password, parameters);
            
            if (directory.exists()) {
                return false;
            }
            
            directory.createFolder();
            return true;
        } catch (FileSystemException e) {
            throw new TransportException("Failed to create directory: " + directoryUri, e);
        } finally {
            if (directory != null) {
                try {
                    directory.close();
                } catch (FileSystemException e) {
                    logger.warn("Error closing directory: {}", directoryUri, e);
                }
            }
        }
    }
    
    /**
     * List files in a directory
     * 
     * @param directoryUri The directory URI
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @return List of file objects
     * @throws TransportException if listing the directory fails
     */
    public static List<FileObject> listFiles(String directoryUri, String username, String password, 
                                          Map<String, Object> parameters) throws TransportException {
        FileObject directory = null;
        try {
            directory = resolveFile(directoryUri, username, password, parameters);
            
            if (!directory.exists()) {
                throw new TransportException("Directory does not exist: " + directoryUri);
            }
            
            if (directory.getType() != FileType.FOLDER) {
                throw new TransportException("Not a directory: " + directoryUri);
            }
            
            FileObject[] children = directory.getChildren();
            List<FileObject> files = new ArrayList<>(children.length);
            
            for (FileObject child : children) {
                files.add(child);
            }
            
            return files;
        } catch (FileSystemException e) {
            throw new TransportException("Failed to list directory: " + directoryUri, e);
        } finally {
            if (directory != null) {
                try {
                    directory.close();
                } catch (FileSystemException e) {
                    logger.warn("Error closing directory: {}", directoryUri, e);
                }
            }
        }
    }
    
    /**
     * Check if a file exists
     * 
     * @param fileUri The file URI
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @return true if the file exists, false otherwise
     * @throws TransportException if checking the file fails
     */
    public static boolean fileExists(String fileUri, String username, String password, 
                                   Map<String, Object> parameters) throws TransportException {
        FileObject file = null;
        try {
            file = resolveFile(fileUri, username, password, parameters);
            return file.exists();
        } catch (FileSystemException e) {
            throw new TransportException("Failed to check if file exists: " + fileUri, e);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (FileSystemException e) {
                    logger.warn("Error closing file: {}", fileUri, e);
                }
            }
        }
    }
    
    /**
     * Get file information
     * 
     * @param file The file to get information for
     * @return Map of file information
     * @throws TransportException if getting file information fails
     */
    public static Map<String, Object> getFileInfo(FileObject file) throws TransportException {
        try {
            if (!file.exists()) {
                throw new TransportException("File does not exist: " + file.getName());
            }
            
            Map<String, Object> info = new java.util.HashMap<>();
            
            info.put("name", file.getName().getBaseName());
            info.put("path", file.getName().getPath());
            info.put("uri", file.getName().getURI());
            info.put("type", file.getType().getName());
            
            if (file.getType() == FileType.FILE) {
                info.put("size", file.getContent().getSize());
                info.put("lastModified", file.getContent().getLastModifiedTime());
            }
            
            return info;
        } catch (FileSystemException e) {
            throw new TransportException("Failed to get file information: " + file.getName(), e);
        }
    }
    
    /**
     * Determine the content type of a file
     * 
     * @param file The file to check
     * @return The content type
     */
    public static String determineContentType(FileObject file) {
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