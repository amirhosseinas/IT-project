package org.apache.synapse.custom.mediation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Event source that monitors a directory for file system events.
 */
public class FileEventSource implements EventSource {
    private static final Logger logger = LoggerFactory.getLogger(FileEventSource.class);
    
    private final String name;
    private final String directory;
    private final String filePattern;
    private final List<EventListener> listeners;
    private boolean started;
    private ExecutorService executor;
    private WatchService watchService;
    
    /**
     * Create a new file event source
     * 
     * @param name The event source name
     * @param directory The directory to monitor
     * @param filePattern The file pattern to match (glob pattern)
     */
    public FileEventSource(String name, String directory, String filePattern) {
        this.name = name;
        this.directory = directory;
        this.filePattern = filePattern;
        this.listeners = new ArrayList<>();
        this.started = false;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void start() throws EventException {
        if (started) {
            logger.warn("File event source {} is already started", name);
            return;
        }
        
        logger.info("Starting file event source: {}", name);
        
        try {
            Path dir = Paths.get(directory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            
            watchService = FileSystems.getDefault().newWatchService();
            dir.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            );
            
            executor = Executors.newSingleThreadExecutor();
            executor.submit(this::watchDirectory);
            
            started = true;
        } catch (Exception e) {
            logger.error("Error starting file event source: " + name, e);
            throw new EventException("Error starting file event source: " + name, e);
        }
    }
    
    @Override
    public void stop() throws EventException {
        if (!started) {
            logger.warn("File event source {} is not started", name);
            return;
        }
        
        logger.info("Stopping file event source: {}", name);
        
        try {
            if (watchService != null) {
                watchService.close();
            }
            
            if (executor != null) {
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
            
            started = false;
        } catch (Exception e) {
            logger.error("Error stopping file event source: " + name, e);
            throw new EventException("Error stopping file event source: " + name, e);
        }
    }
    
    @Override
    public void addEventListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        listeners.add(listener);
        logger.debug("Added event listener to file event source {}: {}", name, listener.getName());
    }
    
    @Override
    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
        logger.debug("Removed event listener from file event source {}: {}", name, listener.getName());
    }
    
    @Override
    public List<EventListener> getEventListeners() {
        return new ArrayList<>(listeners);
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    /**
     * Watch the directory for file events
     */
    private void watchDirectory() {
        try {
            logger.debug("Watching directory: {}", directory);
            
            while (started) {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    
                    // Check if the file matches the pattern
                    if (filePattern != null && !filePattern.isEmpty()) {
                        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);
                        if (!matcher.matches(filename)) {
                            continue;
                        }
                    }
                    
                    // Create and fire the event
                    String eventType;
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        eventType = "file-created";
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        eventType = "file-modified";
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        eventType = "file-deleted";
                    } else {
                        eventType = "file-unknown";
                    }
                    
                    Event fileEvent = new Event(eventType, this);
                    fileEvent.setProperty("filename", filename.toString());
                    fileEvent.setProperty("directory", directory);
                    fileEvent.setProperty("fullPath", Paths.get(directory, filename.toString()).toString());
                    
                    fireEvent(fileEvent);
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("File watch thread interrupted");
        } catch (Exception e) {
            logger.error("Error in file watch thread", e);
        }
    }
    
    /**
     * Fire an event to all listeners
     * 
     * @param event The event to fire
     */
    private void fireEvent(Event event) {
        logger.debug("Firing file event from source {}: {}", name, event.getType());
        
        for (EventListener listener : listeners) {
            try {
                listener.handleEvent(event);
            } catch (Exception e) {
                logger.error("Error in event listener: " + listener.getName(), e);
            }
        }
    }
    
    /**
     * Get the directory
     * 
     * @return The directory
     */
    public String getDirectory() {
        return directory;
    }
    
    /**
     * Get the file pattern
     * 
     * @return The file pattern
     */
    public String getFilePattern() {
        return filePattern;
    }
} 