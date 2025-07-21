package org.apache.synapse.custom.mediation.config;

import org.apache.synapse.custom.mediation.registry.Registry;
import org.apache.synapse.custom.mediation.registry.XMLConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages configuration for Apache Synapse.
 * Handles loading, validation, and dynamic reloading of configurations.
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    
    private final XMLConfigurationParser parser;
    private final Registry registry;
    private final Map<String, ConfigurationListener> listeners;
    private final Map<String, Long> lastModifiedTimes;
    private final String configDirectory;
    private final String schemaPath;
    private final Properties environmentProperties;
    private final ExecutorService watcherService;
    private boolean watchingEnabled = false;
    private WatchService watchService;
    
    /**
     * Create a new configuration manager
     * 
     * @param registry The registry to store configurations
     * @param configDirectory The directory containing configuration files
     */
    public ConfigurationManager(Registry registry, String configDirectory) {
        this.registry = registry;
        this.configDirectory = configDirectory;
        this.parser = new XMLConfigurationParser();
        this.listeners = new ConcurrentHashMap<>();
        this.lastModifiedTimes = new HashMap<>();
        this.schemaPath = "/org/apache/synapse/custom/mediation/config/schema/synapse-config.xsd";
        this.environmentProperties = new Properties();
        this.watcherService = Executors.newSingleThreadExecutor();
        
        // Load environment properties
        loadEnvironmentProperties();
    }
    
    /**
     * Initialize the configuration manager
     * 
     * @throws ConfigurationException if initialization fails
     */
    public void init() throws ConfigurationException {
        // Load all configuration files
        loadConfigurations();
        
        // Start watching for changes if enabled
        if (watchingEnabled) {
            startWatching();
        }
    }
    
    /**
     * Load all configuration files from the configuration directory
     * 
     * @throws ConfigurationException if loading fails
     */
    public void loadConfigurations() throws ConfigurationException {
        File configDir = new File(configDirectory);
        if (!configDir.exists() || !configDir.isDirectory()) {
            throw new ConfigurationException("Configuration directory does not exist: " + configDirectory);
        }
        
        // Load main configuration file first
        File mainConfig = new File(configDir, "synapse.xml");
        if (mainConfig.exists()) {
            loadConfiguration(mainConfig);
        } else {
            logger.warn("Main configuration file not found: {}", mainConfig.getAbsolutePath());
        }
        
        // Load all XML files in the sequences directory
        File sequencesDir = new File(configDir, "sequences");
        if (sequencesDir.exists() && sequencesDir.isDirectory()) {
            loadConfigurationsFromDirectory(sequencesDir);
        }
        
        // Load all XML files in the endpoints directory
        File endpointsDir = new File(configDir, "endpoints");
        if (endpointsDir.exists() && endpointsDir.isDirectory()) {
            loadConfigurationsFromDirectory(endpointsDir);
        }
        
        // Load all XML files in the proxies directory
        File proxiesDir = new File(configDir, "proxies");
        if (proxiesDir.exists() && proxiesDir.isDirectory()) {
            loadConfigurationsFromDirectory(proxiesDir);
        }
        
        // Load all XML files in the tasks directory
        File tasksDir = new File(configDir, "tasks");
        if (tasksDir.exists() && tasksDir.isDirectory()) {
            loadConfigurationsFromDirectory(tasksDir);
        }
    }
    
    /**
     * Load all XML configuration files from a directory
     * 
     * @param directory The directory to load from
     * @throws ConfigurationException if loading fails
     */
    private void loadConfigurationsFromDirectory(File directory) throws ConfigurationException {
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            loadConfiguration(file);
        }
    }
    
    /**
     * Load a configuration file
     * 
     * @param file The file to load
     * @throws ConfigurationException if loading fails
     */
    public void loadConfiguration(File file) throws ConfigurationException {
        try (FileInputStream fis = new FileInputStream(file)) {
            // Remember last modified time
            lastModifiedTimes.put(file.getAbsolutePath(), file.lastModified());
            
            // Load and validate configuration
            String content = readAndReplaceVariables(fis);
            parser.validateConfiguration(content, schemaPath);
            
            // Parse configuration and store in registry
            Map<String, Object> config = parser.parseConfiguration(content);
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                registry.put(entry.getKey(), entry.getValue());
                logger.info("Loaded configuration: {}", entry.getKey());
            }
            
            // Notify listeners
            notifyListeners(file.getName());
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration file: " + file.getAbsolutePath(), e);
        } catch (XMLConfigurationParser.ConfigurationException e) {
            throw new ConfigurationException("Failed to parse configuration file: " + file.getAbsolutePath(), e);
        }
    }
    
    /**
     * Read input stream and replace variables with environment properties
     * 
     * @param inputStream The input stream to read
     * @return The content with variables replaced
     * @throws IOException if reading fails
     */
    private String readAndReplaceVariables(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes);
        
        // Replace ${var} with environment properties
        for (String key : environmentProperties.stringPropertyNames()) {
            String value = environmentProperties.getProperty(key);
            content = content.replace("${" + key + "}", value);
        }
        
        return content;
    }
    
    /**
     * Load environment properties from synapse-env.properties file
     */
    private void loadEnvironmentProperties() {
        File envFile = new File(configDirectory, "synapse-env.properties");
        if (envFile.exists()) {
            try (FileInputStream fis = new FileInputStream(envFile)) {
                environmentProperties.load(fis);
                logger.info("Loaded environment properties from: {}", envFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to load environment properties", e);
            }
        } else {
            logger.info("Environment properties file not found: {}", envFile.getAbsolutePath());
        }
        
        // Add system properties
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            environmentProperties.put(key, value);
        }
    }
    
    /**
     * Enable watching for configuration changes
     */
    public void enableWatching() {
        this.watchingEnabled = true;
        if (watchService != null) {
            startWatching();
        }
    }
    
    /**
     * Disable watching for configuration changes
     */
    public void disableWatching() {
        this.watchingEnabled = false;
        stopWatching();
    }
    
    /**
     * Start watching for configuration changes
     */
    private void startWatching() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            
            // Register directories to watch
            File configDir = new File(configDirectory);
            Path configPath = configDir.toPath();
            configPath.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE, 
                    StandardWatchEventKinds.ENTRY_MODIFY);
            
            // Register subdirectories
            registerSubdirectory(configDir, "sequences");
            registerSubdirectory(configDir, "endpoints");
            registerSubdirectory(configDir, "proxies");
            registerSubdirectory(configDir, "tasks");
            
            // Start watcher thread
            watcherService.submit(this::watcherLoop);
            logger.info("Started watching for configuration changes");
        } catch (IOException e) {
            logger.error("Failed to start watching for configuration changes", e);
        }
    }
    
    /**
     * Register a subdirectory for watching
     * 
     * @param parent The parent directory
     * @param name The subdirectory name
     */
    private void registerSubdirectory(File parent, String name) {
        File dir = new File(parent, name);
        if (dir.exists() && dir.isDirectory()) {
            try {
                dir.toPath().register(watchService, 
                        StandardWatchEventKinds.ENTRY_CREATE, 
                        StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException e) {
                logger.error("Failed to register directory for watching: {}", dir.getAbsolutePath(), e);
            }
        }
    }
    
    /**
     * Stop watching for configuration changes
     */
    private void stopWatching() {
        if (watchService != null) {
            try {
                watchService.close();
                watchService = null;
                logger.info("Stopped watching for configuration changes");
            } catch (IOException e) {
                logger.error("Failed to stop watching for configuration changes", e);
            }
        }
    }
    
    /**
     * Watcher thread loop
     */
    private void watcherLoop() {
        try {
            while (watchingEnabled) {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    
                    // Only process XML files
                    if (!filename.toString().toLowerCase().endsWith(".xml")) {
                        continue;
                    }
                    
                    // Get the full path of the changed file
                    Path dir = (Path) key.watchable();
                    Path fullPath = dir.resolve(filename);
                    File file = fullPath.toFile();
                    
                    // Check if the file has been modified since last load
                    Long lastModified = lastModifiedTimes.get(file.getAbsolutePath());
                    if (lastModified != null && file.lastModified() == lastModified) {
                        continue;
                    }
                    
                    // Reload the configuration
                    logger.info("Configuration file changed: {}", file.getAbsolutePath());
                    try {
                        loadConfiguration(file);
                    } catch (ConfigurationException e) {
                        logger.error("Failed to reload configuration", e);
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.info("Watcher thread interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Add a configuration listener
     * 
     * @param name The listener name
     * @param listener The listener
     */
    public void addListener(String name, ConfigurationListener listener) {
        listeners.put(name, listener);
    }
    
    /**
     * Remove a configuration listener
     * 
     * @param name The listener name
     */
    public void removeListener(String name) {
        listeners.remove(name);
    }
    
    /**
     * Notify all listeners of a configuration change
     * 
     * @param configName The name of the changed configuration
     */
    private void notifyListeners(String configName) {
        for (ConfigurationListener listener : listeners.values()) {
            try {
                listener.onConfigurationChanged(configName);
            } catch (Exception e) {
                logger.error("Error notifying configuration listener", e);
            }
        }
    }
    
    /**
     * Destroy the configuration manager and release resources
     */
    public void destroy() {
        stopWatching();
        watcherService.shutdown();
    }
    
    /**
     * Get the configuration parser
     * 
     * @return The configuration parser
     */
    public XMLConfigurationParser getParser() {
        return parser;
    }
    
    /**
     * Get the environment properties
     * 
     * @return The environment properties
     */
    public Properties getEnvironmentProperties() {
        return new Properties(environmentProperties);
    }
    
    /**
     * Configuration listener interface
     */
    public interface ConfigurationListener {
        /**
         * Called when a configuration changes
         * 
         * @param configName The name of the changed configuration
         */
        void onConfigurationChanged(String configName);
    }
    
    /**
     * Configuration exception
     */
    public static class ConfigurationException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 