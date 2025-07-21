package org.apache.synapse.custom.transports.vfs;

import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VFS Transport listener implementation based on Apache Commons VFS.
 * Provides file system monitoring and polling capabilities.
 */
public class VfsTransportListener implements TransportListener {
    private static final Logger logger = LoggerFactory.getLogger(VfsTransportListener.class);
    
    private final String directory;
    private final String filePattern;
    private final boolean recursive;
    private final long pollingInterval;
    private final VfsListenerMode mode;
    private final int workerThreadCount;
    private final Map<String, Object> parameters;
    
    private FileSystemManager fsManager;
    private DefaultFileMonitor fileMonitor;
    private ScheduledExecutorService pollingExecutor;
    private ExecutorService workerPool;
    private MessageCallback messageCallback;
    private volatile boolean running;
    
    /**
     * VFS Listener operation modes
     */
    public enum VfsListenerMode {
        /**
         * Watch for file changes using file system events (if supported)
         */
        WATCH,
        
        /**
         * Poll the directory for changes at regular intervals
         */
        POLL,
        
        /**
         * Use both watching and polling
         */
        HYBRID
    }
    
    /**
     * Create a new VFS transport listener with default settings
     * 
     * @param directory The directory to monitor
     */
    public VfsTransportListener(String directory) {
        this(directory, "*", false, 5000, VfsListenerMode.HYBRID, 5, new HashMap<>());
    }
    
    /**
     * Create a new VFS transport listener with custom settings
     * 
     * @param directory The directory to monitor
     * @param filePattern File name pattern to filter files
     * @param recursive Whether to monitor subdirectories
     * @param pollingInterval Polling interval in milliseconds
     * @param mode Listener mode (WATCH, POLL, or HYBRID)
     * @param workerThreadCount Number of worker threads
     * @param parameters Additional parameters for VFS configuration
     */
    public VfsTransportListener(String directory, String filePattern, boolean recursive, 
                              long pollingInterval, VfsListenerMode mode, int workerThreadCount,
                              Map<String, Object> parameters) {
        this.directory = directory;
        this.filePattern = filePattern;
        this.recursive = recursive;
        this.pollingInterval = pollingInterval;
        this.mode = mode;
        this.workerThreadCount = workerThreadCount;
        this.parameters = parameters;
    }
    
    @Override
    public void init() throws TransportException {
        try {
            // Initialize VFS file system manager
            fsManager = VFS.getManager();
            
            // Create worker thread pool
            workerPool = Executors.newFixedThreadPool(workerThreadCount, new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(0);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("synapse-vfs-worker-" + count.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
            
            logger.info("VFS transport listener initialized for directory: {}", directory);
        } catch (Exception e) {
            throw new TransportException("Failed to initialize VFS transport listener", e);
        }
    }
    
    @Override
    public void start() throws TransportException {
        if (running) {
            logger.warn("VFS transport listener already running");
            return;
        }
        
        if (fsManager == null) {
            throw new TransportException("VFS transport listener not initialized");
        }
        
        try {
            // Get the directory to monitor
            final FileObject directoryObject = resolveFile(directory);
            
            if (!directoryObject.exists()) {
                throw new TransportException("Directory does not exist: " + directory);
            }
            
            if (directoryObject.getType() != FileType.FOLDER) {
                throw new TransportException("Not a directory: " + directory);
            }
            
            // Set up file monitoring based on the mode
            if (mode == VfsListenerMode.WATCH || mode == VfsListenerMode.HYBRID) {
                setupFileMonitor(directoryObject);
            }
            
            if (mode == VfsListenerMode.POLL || mode == VfsListenerMode.HYBRID) {
                setupPolling(directoryObject);
            }
            
            running = true;
            logger.info("VFS transport listener started for directory: {}", directory);
        } catch (Exception e) {
            throw new TransportException("Failed to start VFS transport listener", e);
        }
    }
    
    @Override
    public void stop() throws TransportException {
        if (!running) {
            logger.warn("VFS transport listener not running");
            return;
        }
        
        try {
            // Stop file monitor if active
            if (fileMonitor != null) {
                fileMonitor.stop();
            }
            
            // Stop polling executor if active
            if (pollingExecutor != null) {
                pollingExecutor.shutdown();
                try {
                    if (!pollingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        pollingExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    pollingExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // Shut down worker pool
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            running = false;
            logger.info("VFS transport listener stopped");
        } catch (Exception e) {
            throw new TransportException("Failed to stop VFS transport listener", e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }
    
    /**
     * Set up file system monitoring using VFS file monitor
     * 
     * @param directory The directory to monitor
     */
    private void setupFileMonitor(FileObject directory) {
        fileMonitor = new DefaultFileMonitor(new VfsFileListener());
        fileMonitor.setRecursive(recursive);
        fileMonitor.addFile(directory);
        fileMonitor.start();
        
        logger.info("File monitor started for directory: {}", directory.getName());
    }
    
    /**
     * Set up polling for file changes
     * 
     * @param directory The directory to poll
     */
    private void setupPolling(final FileObject directory) {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("synapse-vfs-poller");
            t.setDaemon(true);
            return t;
        });
        
        pollingExecutor.scheduleAtFixedRate(() -> {
            try {
                pollDirectory(directory);
            } catch (Exception e) {
                logger.error("Error polling directory", e);
            }
        }, 0, pollingInterval, TimeUnit.MILLISECONDS);
        
        logger.info("Polling started for directory: {} (interval: {} ms)", directory.getName(), pollingInterval);
    }
    
    /**
     * Poll a directory for files and process them
     * 
     * @param directory The directory to poll
     * @throws FileSystemException if accessing the file system fails
     */
    private void pollDirectory(FileObject directory) throws FileSystemException {
        directory.refresh();
        
        FileObject[] files = directory.getChildren();
        for (FileObject file : files) {
            if (file.getType() == FileType.FOLDER && recursive) {
                pollDirectory(file);
            } else if (file.getType() == FileType.FILE && fileMatchesPattern(file.getName().getBaseName())) {
                processFile(file);
            }
        }
    }
    
    /**
     * Check if a file name matches the configured pattern
     * 
     * @param fileName The file name to check
     * @return true if the file matches, false otherwise
     */
    private boolean fileMatchesPattern(String fileName) {
        if ("*".equals(filePattern)) {
            return true;
        }
        
        return fileName.matches(filePattern.replace("*", ".*").replace("?", "."));
    }
    
    /**
     * Process a file by reading its content and creating a message
     * 
     * @param file The file to process
     */
    private void processFile(final FileObject file) {
        // Skip if already being processed
        if (Boolean.TRUE.equals(file.getContent().getAttribute("processing"))) {
            return;
        }
        
        try {
            // Mark file as being processed
            file.getContent().setAttribute("processing", Boolean.TRUE);
            
            // Submit file processing to worker pool
            workerPool.submit(() -> {
                try {
                    // Read file content
                    byte[] content = readFileContent(file);
                    
                    // Create message
                    Message message = new Message(UUID.randomUUID().toString());
                    message.setDirection(Message.Direction.REQUEST);
                    message.setPayload(content);
                    
                    // Set file properties
                    message.setProperty("vfs.file.name", file.getName().getBaseName());
                    message.setProperty("vfs.file.path", file.getName().getPath());
                    message.setProperty("vfs.file.uri", file.getName().getURI());
                    message.setProperty("vfs.file.size", file.getContent().getSize());
                    message.setProperty("vfs.file.lastModified", file.getContent().getLastModifiedTime());
                    
                    // Determine content type
                    String contentType = determineContentType(file);
                    message.setContentType(contentType);
                    
                    // Process message if callback is set
                    if (messageCallback != null) {
                        Message response = messageCallback.onMessage(message);
                        
                        // Handle response if needed
                        if (response != null) {
                            handleResponse(response, file);
                        }
                    }
                    
                    // Handle file after processing based on configuration
                    handleProcessedFile(file);
                } catch (Exception e) {
                    logger.error("Error processing file: {}", file.getName(), e);
                    try {
                        handleFailedFile(file, e);
                    } catch (Exception ex) {
                        logger.error("Error handling failed file: {}", file.getName(), ex);
                    }
                } finally {
                    try {
                        // Unmark file as being processed
                        file.getContent().setAttribute("processing", Boolean.FALSE);
                    } catch (Exception e) {
                        logger.warn("Error unmarking file as processed: {}", file.getName(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Error submitting file for processing: {}", file.getName(), e);
            try {
                // Unmark file as being processed
                file.getContent().setAttribute("processing", Boolean.FALSE);
            } catch (Exception ex) {
                logger.warn("Error unmarking file as processed: {}", file.getName(), ex);
            }
        }
    }
    
    /**
     * Read the content of a file
     * 
     * @param file The file to read
     * @return The file content as a byte array
     * @throws IOException if reading the file fails
     */
    private byte[] readFileContent(FileObject file) throws IOException {
        try (InputStream is = file.getContent().getInputStream()) {
            return is.readAllBytes();
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
    
    /**
     * Handle a response message
     * 
     * @param response The response message
     * @param sourceFile The source file that triggered the response
     * @throws Exception if handling the response fails
     */
    private void handleResponse(Message response, FileObject sourceFile) throws Exception {
        // Check if response should be written to a file
        String responseFilePath = (String) response.getProperty("vfs.response.file");
        if (responseFilePath != null) {
            FileObject responseFile = resolveFile(responseFilePath);
            
            // Ensure parent directory exists
            if (!responseFile.getParent().exists()) {
                responseFile.getParent().createFolder();
            }
            
            // Write response content to file
            if (response.getPayload() != null) {
                responseFile.getContent().getOutputStream().write(response.getPayload());
            }
        }
    }
    
    /**
     * Handle a file after it has been processed
     * 
     * @param file The processed file
     * @throws Exception if handling the file fails
     */
    private void handleProcessedFile(FileObject file) throws Exception {
        // Get action from parameters
        String action = (String) parameters.getOrDefault("vfs.after.process", "none");
        
        switch (action.toLowerCase()) {
            case "delete":
                file.delete();
                logger.debug("Deleted processed file: {}", file.getName());
                break;
                
            case "move":
                String movePath = (String) parameters.get("vfs.move.destination");
                if (movePath != null) {
                    FileObject destination = resolveFile(movePath + "/" + file.getName().getBaseName());
                    
                    // Ensure destination directory exists
                    if (!destination.getParent().exists()) {
                        destination.getParent().createFolder();
                    }
                    
                    file.moveTo(destination);
                    logger.debug("Moved processed file to: {}", destination.getName());
                }
                break;
                
            case "none":
            default:
                // Do nothing
                break;
        }
    }
    
    /**
     * Handle a file that failed processing
     * 
     * @param file The failed file
     * @param error The error that occurred
     * @throws Exception if handling the file fails
     */
    private void handleFailedFile(FileObject file, Exception error) throws Exception {
        // Get action from parameters
        String action = (String) parameters.getOrDefault("vfs.after.failure", "none");
        
        switch (action.toLowerCase()) {
            case "delete":
                file.delete();
                logger.debug("Deleted failed file: {}", file.getName());
                break;
                
            case "move":
                String movePath = (String) parameters.get("vfs.failure.destination");
                if (movePath != null) {
                    FileObject destination = resolveFile(movePath + "/" + file.getName().getBaseName());
                    
                    // Ensure destination directory exists
                    if (!destination.getParent().exists()) {
                        destination.getParent().createFolder();
                    }
                    
                    file.moveTo(destination);
                    logger.debug("Moved failed file to: {}", destination.getName());
                }
                break;
                
            case "none":
            default:
                // Do nothing
                break;
        }
    }
    
    /**
     * Resolve a file path using the VFS file system manager
     * 
     * @param path The file path to resolve
     * @return The resolved file object
     * @throws FileSystemException if resolving the file fails
     */
    private FileObject resolveFile(String path) throws FileSystemException {
        return fsManager.resolveFile(path);
    }
    
    /**
     * File listener for VFS file monitoring
     */
    private class VfsFileListener implements FileListener {
        @Override
        public void fileCreated(FileObject file) throws Exception {
            logger.debug("File created: {}", file.getName());
            if (file.getType() == FileType.FILE && fileMatchesPattern(file.getName().getBaseName())) {
                processFile(file);
            }
        }
        
        @Override
        public void fileDeleted(FileObject file) throws Exception {
            logger.debug("File deleted: {}", file.getName());
        }
        
        @Override
        public void fileChanged(FileObject file) throws Exception {
            logger.debug("File changed: {}", file.getName());
            if (file.getType() == FileType.FILE && fileMatchesPattern(file.getName().getBaseName())) {
                processFile(file);
            }
        }
    }
} 