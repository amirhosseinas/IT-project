package org.apache.synapse.custom.transports.vfs;

import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating VFS transport components.
 */
public class VfsTransportFactory {
    private static final Logger logger = LoggerFactory.getLogger(VfsTransportFactory.class);
    
    /**
     * Create a VFS transport listener
     * 
     * @param directory The directory to monitor
     * @return The VFS transport listener
     * @throws TransportException if creating the listener fails
     */
    public static TransportListener createListener(String directory) throws TransportException {
        return new VfsTransportListener(directory);
    }
    
    /**
     * Create a VFS transport listener with custom settings
     * 
     * @param directory The directory to monitor
     * @param filePattern File name pattern to filter files
     * @param recursive Whether to monitor subdirectories
     * @param pollingInterval Polling interval in milliseconds
     * @param mode Listener mode (WATCH, POLL, or HYBRID)
     * @param workerThreadCount Number of worker threads
     * @param parameters Additional parameters for VFS configuration
     * @return The VFS transport listener
     * @throws TransportException if creating the listener fails
     */
    public static TransportListener createListener(String directory, String filePattern, boolean recursive, 
                                                 long pollingInterval, VfsTransportListener.VfsListenerMode mode, 
                                                 int workerThreadCount, Map<String, Object> parameters) throws TransportException {
        return new VfsTransportListener(directory, filePattern, recursive, pollingInterval, mode, workerThreadCount, parameters);
    }
    
    /**
     * Create a VFS transport sender
     * 
     * @return The VFS transport sender
     * @throws TransportException if creating the sender fails
     */
    public static TransportSender createSender() throws TransportException {
        return new VfsTransportSender();
    }
    
    /**
     * Create default parameters for VFS transport
     * 
     * @return Map of default parameters
     */
    public static Map<String, Object> createDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        
        // Default file handling after processing
        params.put("vfs.after.process", "none");
        params.put("vfs.after.failure", "none");
        
        return params;
    }
    
    /**
     * Create parameters for moving processed files
     * 
     * @param moveDestination The destination directory for processed files
     * @param failureDestination The destination directory for failed files
     * @return Map of parameters
     */
    public static Map<String, Object> createMoveParameters(String moveDestination, String failureDestination) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("vfs.after.process", "move");
        params.put("vfs.move.destination", moveDestination);
        
        params.put("vfs.after.failure", "move");
        params.put("vfs.failure.destination", failureDestination);
        
        return params;
    }
    
    /**
     * Create parameters for deleting processed files
     * 
     * @param deleteFailedFiles Whether to delete failed files
     * @return Map of parameters
     */
    public static Map<String, Object> createDeleteParameters(boolean deleteFailedFiles) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("vfs.after.process", "delete");
        
        if (deleteFailedFiles) {
            params.put("vfs.after.failure", "delete");
        } else {
            params.put("vfs.after.failure", "none");
        }
        
        return params;
    }
} 