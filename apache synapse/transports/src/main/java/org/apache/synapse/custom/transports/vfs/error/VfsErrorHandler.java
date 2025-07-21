package org.apache.synapse.custom.transports.vfs.error;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.vfs.util.VfsFileOperationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Error handler for VFS operations.
 * Provides error handling strategies for file operations.
 */
public class VfsErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(VfsErrorHandler.class);
    
    /**
     * Error handling strategies
     */
    public enum ErrorHandlingStrategy {
        /**
         * Do nothing with the file
         */
        NONE,
        
        /**
         * Delete the file
         */
        DELETE,
        
        /**
         * Move the file to an error directory
         */
        MOVE,
        
        /**
         * Retry the operation
         */
        RETRY
    }
    
    private final ErrorHandlingStrategy strategy;
    private final String errorDirectory;
    private final String username;
    private final String password;
    private final Map<String, Object> parameters;
    private final int maxRetries;
    private final long retryDelay;
    
    /**
     * Create a new VFS error handler
     * 
     * @param strategy The error handling strategy
     * @param errorDirectory The directory to move failed files to (for MOVE strategy)
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @param maxRetries Maximum number of retries (for RETRY strategy)
     * @param retryDelay Delay between retries in milliseconds (for RETRY strategy)
     */
    public VfsErrorHandler(ErrorHandlingStrategy strategy, String errorDirectory,
                         String username, String password, Map<String, Object> parameters,
                         int maxRetries, long retryDelay) {
        this.strategy = strategy;
        this.errorDirectory = errorDirectory;
        this.username = username;
        this.password = password;
        this.parameters = parameters;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }
    
    /**
     * Handle a file operation error
     * 
     * @param file The file that caused the error
     * @param error The error that occurred
     * @param message The message being processed (may be null)
     * @throws TransportException if error handling fails
     */
    public void handleError(FileObject file, Throwable error, Message message) throws TransportException {
        logger.error("File operation error: {}", file.getName(), error);
        
        // Add error information to message if available
        if (message != null) {
            message.setProperty("vfs.error", error.getMessage());
            message.setProperty("vfs.error.type", error.getClass().getName());
        }
        
        // Handle error based on strategy
        switch (strategy) {
            case DELETE:
                handleDeleteStrategy(file);
                break;
                
            case MOVE:
                handleMoveStrategy(file);
                break;
                
            case RETRY:
                handleRetryStrategy(file, error, message);
                break;
                
            case NONE:
            default:
                // Do nothing
                logger.debug("No error handling strategy applied for file: {}", file.getName());
                break;
        }
    }
    
    /**
     * Handle the DELETE error strategy
     * 
     * @param file The file to delete
     * @throws TransportException if deletion fails
     */
    private void handleDeleteStrategy(FileObject file) throws TransportException {
        try {
            if (file.exists()) {
                boolean deleted = file.delete();
                
                if (deleted) {
                    logger.info("Deleted file after error: {}", file.getName());
                } else {
                    logger.warn("Failed to delete file after error: {}", file.getName());
                }
            }
        } catch (FileSystemException e) {
            throw new TransportException("Error handling failed: could not delete file", e);
        }
    }
    
    /**
     * Handle the MOVE error strategy
     * 
     * @param file The file to move
     * @throws TransportException if moving fails
     */
    private void handleMoveStrategy(FileObject file) throws TransportException {
        try {
            if (errorDirectory == null || errorDirectory.isEmpty()) {
                throw new TransportException("Error directory not specified for MOVE strategy");
            }
            
            if (file.exists()) {
                // Create destination URI
                String destinationUri = errorDirectory;
                if (!destinationUri.endsWith("/")) {
                    destinationUri += "/";
                }
                destinationUri += file.getName().getBaseName();
                
                // Move the file to the error directory
                VfsFileOperationUtil.moveFile(file, destinationUri, username, password, parameters);
                
                logger.info("Moved file to error directory after error: {} -> {}", file.getName(), destinationUri);
            }
        } catch (Exception e) {
            throw new TransportException("Error handling failed: could not move file to error directory", e);
        }
    }
    
    /**
     * Handle the RETRY error strategy
     * 
     * @param file The file to retry
     * @param error The original error
     * @param message The message being processed (may be null)
     * @throws TransportException if retrying fails
     */
    private void handleRetryStrategy(FileObject file, Throwable error, Message message) throws TransportException {
        // Get current retry count
        int retryCount = 0;
        if (message != null) {
            Object retryObj = message.getProperty("vfs.retry.count");
            if (retryObj instanceof Integer) {
                retryCount = (Integer) retryObj;
            }
        }
        
        // Check if max retries reached
        if (retryCount >= maxRetries) {
            logger.warn("Maximum retries ({}) reached for file: {}", maxRetries, file.getName());
            
            // Fall back to move strategy if error directory is specified
            if (errorDirectory != null && !errorDirectory.isEmpty()) {
                logger.info("Falling back to MOVE strategy after max retries");
                handleMoveStrategy(file);
            }
            
            return;
        }
        
        // Increment retry count
        retryCount++;
        if (message != null) {
            message.setProperty("vfs.retry.count", retryCount);
            message.setProperty("vfs.retry.delay", retryDelay);
        }
        
        logger.info("Scheduling retry {} of {} for file: {} (delay: {} ms)",
                  retryCount, maxRetries, file.getName(), retryDelay);
        
        // Schedule retry after delay
        if (retryDelay > 0) {
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransportException("Retry interrupted", e);
            }
        }
        
        // Retry is handled by the caller
    }
    
    /**
     * Create a default error handler with NONE strategy
     * 
     * @return The default error handler
     */
    public static VfsErrorHandler createDefaultHandler() {
        return new VfsErrorHandler(ErrorHandlingStrategy.NONE, null, null, null, null, 0, 0);
    }
    
    /**
     * Create an error handler with DELETE strategy
     * 
     * @return The error handler
     */
    public static VfsErrorHandler createDeleteHandler() {
        return new VfsErrorHandler(ErrorHandlingStrategy.DELETE, null, null, null, null, 0, 0);
    }
    
    /**
     * Create an error handler with MOVE strategy
     * 
     * @param errorDirectory The directory to move failed files to
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @return The error handler
     */
    public static VfsErrorHandler createMoveHandler(String errorDirectory, String username, String password,
                                                 Map<String, Object> parameters) {
        return new VfsErrorHandler(ErrorHandlingStrategy.MOVE, errorDirectory, username, password, parameters, 0, 0);
    }
    
    /**
     * Create an error handler with RETRY strategy
     * 
     * @param maxRetries Maximum number of retries
     * @param retryDelay Delay between retries in milliseconds
     * @param errorDirectory The directory to move failed files to after max retries (may be null)
     * @param username Username for authentication (may be null)
     * @param password Password for authentication (may be null)
     * @param parameters Additional parameters for configuration
     * @return The error handler
     */
    public static VfsErrorHandler createRetryHandler(int maxRetries, long retryDelay,
                                                  String errorDirectory, String username, String password,
                                                  Map<String, Object> parameters) {
        return new VfsErrorHandler(ErrorHandlingStrategy.RETRY, errorDirectory, username, password, parameters,
                                 maxRetries, retryDelay);
    }
} 