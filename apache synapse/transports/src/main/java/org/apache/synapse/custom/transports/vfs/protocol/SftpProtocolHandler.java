package org.apache.synapse.custom.transports.vfs.protocol;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Protocol handler for SFTP file system (sftp://).
 */
public class SftpProtocolHandler implements VfsProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(SftpProtocolHandler.class);
    
    // Default timeouts
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000;
    
    @Override
    public String getProtocolScheme() {
        return "sftp";
    }
    
    @Override
    public boolean canHandle(String uri) {
        return uri != null && uri.startsWith("sftp://");
    }
    
    @Override
    public void configureFileSystemOptions(FileSystemOptions opts, String username, String password, 
                                         Map<String, Object> parameters) throws FileSystemException {
        // Set authentication if provided
        if (username != null && password != null) {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(null, username, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
            logger.debug("SFTP authentication configured for user: {}", username);
        }
        
        SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();
        
        // Configure strict host key checking
        String strictHostKeyChecking = getParameter(parameters, "sftp.strictHostKeyChecking", "false");
        builder.setStrictHostKeyChecking(opts, strictHostKeyChecking);
        
        // Configure private key authentication
        String privateKeyPath = getParameter(parameters, "sftp.privateKey", null);
        if (privateKeyPath != null) {
            File privateKey = new File(privateKeyPath);
            if (privateKey.exists() && privateKey.isFile()) {
                builder.setIdentities(opts, new File[] { privateKey });
                logger.debug("SFTP private key configured: {}", privateKeyPath);
            } else {
                logger.warn("SFTP private key file not found: {}", privateKeyPath);
            }
        }
        
        // Configure private key passphrase
        String passphrase = getParameter(parameters, "sftp.passphrase", null);
        if (passphrase != null) {
            builder.setUserInfo(opts, new SftpUserInfo(passphrase));
            logger.debug("SFTP passphrase configured");
        }
        
        // Configure known hosts file
        String knownHosts = getParameter(parameters, "sftp.knownHosts", null);
        if (knownHosts != null) {
            File knownHostsFile = new File(knownHosts);
            if (knownHostsFile.exists() && knownHostsFile.isFile()) {
                builder.setKnownHosts(opts, knownHostsFile);
                logger.debug("SFTP known hosts configured: {}", knownHosts);
            } else {
                logger.warn("SFTP known hosts file not found: {}", knownHosts);
            }
        }
        
        // Configure connection timeout
        String connectionTimeout = getParameter(parameters, "sftp.connectionTimeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        builder.setConnectTimeout(opts, Integer.parseInt(connectionTimeout));
        
        // Configure session timeout
        String sessionTimeout = getParameter(parameters, "sftp.sessionTimeout", String.valueOf(DEFAULT_SOCKET_TIMEOUT));
        builder.setTimeout(opts, Integer.parseInt(sessionTimeout));
        
        // Configure compression
        String compression = getParameter(parameters, "sftp.compression", "false");
        builder.setCompression(opts, Boolean.parseBoolean(compression));
        
        // Configure preferred authentication methods
        String preferredAuthentications = getParameter(parameters, "sftp.preferredAuthentications", null);
        if (preferredAuthentications != null) {
            builder.setPreferredAuthentications(opts, preferredAuthentications);
        }
        
        logger.debug("SFTP file system options configured");
    }
    
    @Override
    public int getDefaultPort() {
        return 22;
    }
    
    @Override
    public boolean supportsMonitoring() {
        // SFTP doesn't support native monitoring
        return false;
    }
    
    @Override
    public boolean supportsLocking() {
        // SFTP doesn't support reliable locking
        return false;
    }
    
    @Override
    public void validateFileObject(FileObject file) throws FileSystemException {
        // Basic validation for SFTP file objects
        if (file == null) {
            throw new FileSystemException("vfs.provider.sftp/invalid-file-object.error");
        }
    }
    
    @Override
    public int getConnectionTimeout() {
        return DEFAULT_CONNECTION_TIMEOUT;
    }
    
    @Override
    public int getSocketTimeout() {
        return DEFAULT_SOCKET_TIMEOUT;
    }
    
    /**
     * Get a parameter from the parameters map with a default value
     * 
     * @param parameters The parameters map
     * @param name The parameter name
     * @param defaultValue The default value if not found
     * @return The parameter value
     */
    private String getParameter(Map<String, Object> parameters, String name, String defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        
        Object value = parameters.get(name);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * User info implementation for SFTP authentication with passphrase
     */
    private static class SftpUserInfo implements com.jcraft.jsch.UserInfo {
        private final String passphrase;
        
        public SftpUserInfo(String passphrase) {
            this.passphrase = passphrase;
        }
        
        @Override
        public String getPassphrase() {
            return passphrase;
        }
        
        @Override
        public String getPassword() {
            return null;
        }
        
        @Override
        public boolean promptPassword(String message) {
            return false;
        }
        
        @Override
        public boolean promptPassphrase(String message) {
            return true;
        }
        
        @Override
        public boolean promptYesNo(String message) {
            return true;
        }
        
        @Override
        public void showMessage(String message) {
            logger.info("SFTP message: {}", message);
        }
    }
} 