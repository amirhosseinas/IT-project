package org.apache.synapse.custom.transports.vfs.protocol;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for VFS protocol handlers.
 * Manages protocol handlers for different file system types.
 */
public class VfsProtocolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(VfsProtocolRegistry.class);
    
    private static final VfsProtocolRegistry INSTANCE = new VfsProtocolRegistry();
    
    private final Map<String, VfsProtocolHandler> protocolHandlers;
    
    /**
     * Private constructor for singleton pattern
     */
    private VfsProtocolRegistry() {
        protocolHandlers = new ConcurrentHashMap<>();
        
        // Register default protocol handlers
        registerDefaultHandlers();
    }
    
    /**
     * Get the singleton instance
     * 
     * @return The VFS protocol registry instance
     */
    public static VfsProtocolRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register default protocol handlers
     */
    private void registerDefaultHandlers() {
        // Register file protocol handler
        registerProtocolHandler(new FileProtocolHandler());
        
        // Register FTP protocol handler
        registerProtocolHandler(new FtpProtocolHandler());
        
        // Register SFTP protocol handler
        registerProtocolHandler(new SftpProtocolHandler());
        
        logger.info("Registered default VFS protocol handlers");
    }
    
    /**
     * Register a protocol handler
     * 
     * @param handler The protocol handler to register
     */
    public void registerProtocolHandler(VfsProtocolHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Protocol handler cannot be null");
        }
        
        String scheme = handler.getProtocolScheme();
        protocolHandlers.put(scheme, handler);
        logger.info("Registered VFS protocol handler for scheme: {}", scheme);
    }
    
    /**
     * Get a protocol handler for a URI
     * 
     * @param uri The URI to get a handler for
     * @return The protocol handler or null if no handler is found
     */
    public VfsProtocolHandler getProtocolHandler(String uri) {
        if (uri == null) {
            return null;
        }
        
        for (VfsProtocolHandler handler : protocolHandlers.values()) {
            if (handler.canHandle(uri)) {
                return handler;
            }
        }
        
        return null;
    }
    
    /**
     * Get a protocol handler by scheme
     * 
     * @param scheme The protocol scheme
     * @return The protocol handler or null if not found
     */
    public VfsProtocolHandler getProtocolHandlerByScheme(String scheme) {
        return protocolHandlers.get(scheme);
    }
    
    /**
     * Configure file system options for a URI
     * 
     * @param uri The URI to configure options for
     * @param opts The file system options to configure
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @throws FileSystemException if configuration fails
     */
    public void configureFileSystemOptions(String uri, FileSystemOptions opts, 
                                         String username, String password, 
                                         Map<String, Object> parameters) throws FileSystemException {
        VfsProtocolHandler handler = getProtocolHandler(uri);
        if (handler == null) {
            throw new FileSystemException("vfs.provider/unsupported-protocol.error", uri);
        }
        
        handler.configureFileSystemOptions(opts, username, password, parameters);
    }
    
    /**
     * Get all registered protocol schemes
     * 
     * @return Array of protocol schemes
     */
    public String[] getRegisteredSchemes() {
        return protocolHandlers.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if a protocol is registered
     * 
     * @param scheme The protocol scheme
     * @return true if the protocol is registered, false otherwise
     */
    public boolean isProtocolRegistered(String scheme) {
        return protocolHandlers.containsKey(scheme);
    }
    
    /**
     * Create a map of protocol-specific parameters
     * 
     * @param protocol The protocol scheme
     * @param parameters The parameters to include
     * @return Map of protocol-specific parameters
     */
    public Map<String, Object> createProtocolParameters(String protocol, Map<String, Object> parameters) {
        Map<String, Object> protocolParams = new HashMap<>();
        
        if (parameters != null) {
            // Copy all parameters with the protocol prefix
            String prefix = protocol + ".";
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    // Remove the protocol prefix
                    String key = entry.getKey().substring(prefix.length());
                    protocolParams.put(key, entry.getValue());
                }
            }
        }
        
        return protocolParams;
    }
} 