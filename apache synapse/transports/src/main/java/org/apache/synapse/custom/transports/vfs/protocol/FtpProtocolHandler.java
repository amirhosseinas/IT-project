package org.apache.synapse.custom.transports.vfs.protocol;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Protocol handler for FTP file system (ftp://).
 */
public class FtpProtocolHandler implements VfsProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(FtpProtocolHandler.class);
    
    // Default timeouts
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000;
    
    @Override
    public String getProtocolScheme() {
        return "ftp";
    }
    
    @Override
    public boolean canHandle(String uri) {
        return uri != null && uri.startsWith("ftp://");
    }
    
    @Override
    public void configureFileSystemOptions(FileSystemOptions opts, String username, String password, 
                                         Map<String, Object> parameters) throws FileSystemException {
        // Set authentication if provided
        if (username != null && password != null) {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(null, username, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
            logger.debug("FTP authentication configured for user: {}", username);
        }
        
        FtpFileSystemConfigBuilder builder = FtpFileSystemConfigBuilder.getInstance();
        
        // Configure passive mode
        String passiveMode = getParameter(parameters, "ftp.passive", "true");
        builder.setPassiveMode(opts, Boolean.parseBoolean(passiveMode));
        
        // Configure data timeout
        String dataTimeout = getParameter(parameters, "ftp.dataTimeout", "30000");
        builder.setDataTimeout(opts, Integer.parseInt(dataTimeout));
        
        // Configure socket timeout
        String socketTimeout = getParameter(parameters, "ftp.socketTimeout", String.valueOf(DEFAULT_SOCKET_TIMEOUT));
        builder.setSoTimeout(opts, Integer.parseInt(socketTimeout));
        
        // Configure connection timeout
        String connectionTimeout = getParameter(parameters, "ftp.connectionTimeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        builder.setConnectTimeout(opts, Integer.parseInt(connectionTimeout));
        
        // Configure encoding
        String controlEncoding = getParameter(parameters, "ftp.controlEncoding", "UTF-8");
        builder.setControlEncoding(opts, controlEncoding);
        
        // Configure file type
        String fileType = getParameter(parameters, "ftp.fileType", "binary");
        if ("ascii".equalsIgnoreCase(fileType)) {
            builder.setFileType(opts, FtpFileSystemConfigBuilder.TRANSFER_MODE_ASCII);
        } else {
            builder.setFileType(opts, FtpFileSystemConfigBuilder.TRANSFER_MODE_BINARY);
        }
        
        // Configure automatic server detection
        String autodetectUtf8 = getParameter(parameters, "ftp.autodetectUtf8", "true");
        builder.setAutodetectUtf8(opts, Boolean.parseBoolean(autodetectUtf8));
        
        logger.debug("FTP file system options configured");
    }
    
    @Override
    public int getDefaultPort() {
        return 21;
    }
    
    @Override
    public boolean supportsMonitoring() {
        // FTP doesn't support native monitoring
        return false;
    }
    
    @Override
    public boolean supportsLocking() {
        // FTP doesn't support reliable locking
        return false;
    }
    
    @Override
    public void validateFileObject(FileObject file) throws FileSystemException {
        // Basic validation for FTP file objects
        if (file == null) {
            throw new FileSystemException("vfs.provider.ftp/invalid-file-object.error");
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
} 