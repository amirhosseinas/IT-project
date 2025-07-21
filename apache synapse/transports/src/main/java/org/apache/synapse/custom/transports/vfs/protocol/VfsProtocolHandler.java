package org.apache.synapse.custom.transports.vfs.protocol;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;

/**
 * Interface for VFS protocol handlers.
 * Provides protocol-specific functionality for different file systems.
 */
public interface VfsProtocolHandler {
    
    /**
     * Get the protocol scheme this handler supports
     * 
     * @return The protocol scheme (e.g., "file", "ftp", "sftp")
     */
    String getProtocolScheme();
    
    /**
     * Check if this handler can handle the given URI
     * 
     * @param uri The URI to check
     * @return true if this handler can handle the URI, false otherwise
     */
    boolean canHandle(String uri);
    
    /**
     * Configure file system options for this protocol
     * 
     * @param opts The file system options to configure
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @throws FileSystemException if configuration fails
     */
    void configureFileSystemOptions(FileSystemOptions opts, String username, String password, 
                                   java.util.Map<String, Object> parameters) throws FileSystemException;
    
    /**
     * Get the default port for this protocol
     * 
     * @return The default port number
     */
    int getDefaultPort();
    
    /**
     * Check if the file system supports file monitoring
     * 
     * @return true if file monitoring is supported, false otherwise
     */
    boolean supportsMonitoring();
    
    /**
     * Check if the file system supports file locking
     * 
     * @return true if file locking is supported, false otherwise
     */
    boolean supportsLocking();
    
    /**
     * Perform protocol-specific validation of a file object
     * 
     * @param file The file object to validate
     * @throws FileSystemException if validation fails
     */
    void validateFileObject(FileObject file) throws FileSystemException;
    
    /**
     * Get the connection timeout for this protocol in milliseconds
     * 
     * @return The connection timeout
     */
    int getConnectionTimeout();
    
    /**
     * Get the socket timeout for this protocol in milliseconds
     * 
     * @return The socket timeout
     */
    int getSocketTimeout();
} 