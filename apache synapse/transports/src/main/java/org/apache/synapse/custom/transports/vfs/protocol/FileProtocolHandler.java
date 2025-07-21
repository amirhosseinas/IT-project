package org.apache.synapse.custom.transports.vfs.protocol;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Protocol handler for local file system (file://).
 */
public class FileProtocolHandler implements VfsProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(FileProtocolHandler.class);
    
    @Override
    public String getProtocolScheme() {
        return "file";
    }
    
    @Override
    public boolean canHandle(String uri) {
        return uri != null && (uri.startsWith("file://") || uri.startsWith("file:///"));
    }
    
    @Override
    public void configureFileSystemOptions(FileSystemOptions opts, String username, String password, 
                                         Map<String, Object> parameters) throws FileSystemException {
        // No special configuration needed for local file system
        logger.debug("Configuring file system options for local file system");
    }
    
    @Override
    public int getDefaultPort() {
        // Not applicable for local file system
        return -1;
    }
    
    @Override
    public boolean supportsMonitoring() {
        // Local file system supports monitoring
        return true;
    }
    
    @Override
    public boolean supportsLocking() {
        // Local file system supports locking
        return true;
    }
    
    @Override
    public void validateFileObject(FileObject file) throws FileSystemException {
        // No special validation needed for local file system
    }
    
    @Override
    public int getConnectionTimeout() {
        // Not applicable for local file system
        return 0;
    }
    
    @Override
    public int getSocketTimeout() {
        // Not applicable for local file system
        return 0;
    }
} 