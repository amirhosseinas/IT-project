package org.apache.synapse.custom.mediation.server;

import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.mediation.MediationRegistry;
import org.apache.synapse.custom.mediation.config.ConfigurationManager;
import org.apache.synapse.custom.mediation.registry.MemoryRegistry;
import org.apache.synapse.custom.mediation.registry.Registry;
import org.apache.synapse.custom.message.MessageProcessor;
import org.apache.synapse.custom.qos.QosManager;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main server class for Apache Synapse.
 * Handles server startup, component initialization, and shutdown.
 */
public class SynapseServer {
    private static final Logger logger = LoggerFactory.getLogger(SynapseServer.class);
    
    private final String serverName;
    private final String configDir;
    private final Properties serverProperties;
    
    private Registry registry;
    private MediationRegistry mediationRegistry;
    private MediationEngine mediationEngine;
    private MessageProcessor messageProcessor;
    private TransportManager transportManager;
    private QosManager qosManager;
    private ConfigurationManager configManager;
    
    private final List<ShutdownHook> shutdownHooks;
    private final CountDownLatch shutdownLatch;
    private final ExecutorService executorService;
    
    private volatile boolean running = false;
    
    /**
     * Create a new Synapse server
     * 
     * @param serverName The server name
     * @param configDir The configuration directory
     */
    public SynapseServer(String serverName, String configDir) {
        this.serverName = serverName;
        this.configDir = configDir;
        this.serverProperties = new Properties();
        this.shutdownHooks = new ArrayList<>();
        this.shutdownLatch = new CountDownLatch(1);
        this.executorService = Executors.newCachedThreadPool();
        
        // Register JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    /**
     * Start the server
     * 
     * @throws ServerException if startup fails
     */
    public void start() throws ServerException {
        if (running) {
            logger.warn("Server already running");
            return;
        }
        
        logger.info("Starting Apache Synapse server: {}", serverName);
        
        try {
            // Initialize components
            initializeRegistry();
            initializeMediationRegistry();
            initializeConfigManager();
            initializeMediationEngine();
            initializeMessageProcessor();
            initializeQosManager();
            initializeTransportManager();
            
            // Load configurations
            configManager.loadConfigurations();
            
            // Start transport listeners
            startTransportListeners();
            
            running = true;
            logger.info("Apache Synapse server started successfully: {}", serverName);
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            shutdown();
            throw new ServerException("Failed to start server", e);
        }
    }
    
    /**
     * Initialize the registry
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeRegistry() throws ServerException {
        try {
            logger.info("Initializing registry");
            registry = new MemoryRegistry();
            registry.init();
        } catch (Exception e) {
            throw new ServerException("Failed to initialize registry", e);
        }
    }
    
    /**
     * Initialize the mediation registry
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeMediationRegistry() throws ServerException {
        try {
            logger.info("Initializing mediation registry");
            mediationRegistry = new MediationRegistry(registry);
            mediationRegistry.init();
        } catch (Exception e) {
            throw new ServerException("Failed to initialize mediation registry", e);
        }
    }
    
    /**
     * Initialize the configuration manager
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeConfigManager() throws ServerException {
        try {
            logger.info("Initializing configuration manager");
            configManager = new ConfigurationManager(registry, configDir);
            configManager.init();
            
            // Enable dynamic configuration reloading
            configManager.enableWatching();
        } catch (Exception e) {
            throw new ServerException("Failed to initialize configuration manager", e);
        }
    }
    
    /**
     * Initialize the mediation engine
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeMediationEngine() throws ServerException {
        try {
            logger.info("Initializing mediation engine");
            mediationEngine = new MediationEngine(mediationRegistry);
            mediationEngine.init();
            
            // Register shutdown hook
            addShutdownHook(mediationEngine::destroy);
        } catch (Exception e) {
            throw new ServerException("Failed to initialize mediation engine", e);
        }
    }
    
    /**
     * Initialize the message processor
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeMessageProcessor() throws ServerException {
        try {
            logger.info("Initializing message processor");
            messageProcessor = new MessageProcessor();
            messageProcessor.setMediationEngine(mediationEngine);
            messageProcessor.init();
            
            // Register shutdown hook
            addShutdownHook(messageProcessor::destroy);
        } catch (Exception e) {
            throw new ServerException("Failed to initialize message processor", e);
        }
    }
    
    /**
     * Initialize the QoS manager
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeQosManager() throws ServerException {
        try {
            logger.info("Initializing QoS manager");
            qosManager = new QosManager();
            qosManager.init();
            
            // Register shutdown hook
            addShutdownHook(qosManager::destroy);
        } catch (Exception e) {
            throw new ServerException("Failed to initialize QoS manager", e);
        }
    }
    
    /**
     * Initialize the transport manager
     * 
     * @throws ServerException if initialization fails
     */
    private void initializeTransportManager() throws ServerException {
        try {
            logger.info("Initializing transport manager");
            transportManager = new TransportManager();
            transportManager.setMessageProcessor(messageProcessor);
            transportManager.init();
            
            // Register shutdown hook
            addShutdownHook(transportManager::destroy);
        } catch (Exception e) {
            throw new ServerException("Failed to initialize transport manager", e);
        }
    }
    
    /**
     * Start all transport listeners
     * 
     * @throws ServerException if startup fails
     */
    private void startTransportListeners() throws ServerException {
        try {
            logger.info("Starting transport listeners");
            Map<String, TransportListener> listeners = transportManager.getListeners();
            
            for (Map.Entry<String, TransportListener> entry : listeners.entrySet()) {
                String name = entry.getKey();
                TransportListener listener = entry.getValue();
                
                logger.info("Starting transport listener: {}", name);
                listener.start();
            }
        } catch (Exception e) {
            throw new ServerException("Failed to start transport listeners", e);
        }
    }
    
    /**
     * Shutdown the server
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        
        logger.info("Shutting down Apache Synapse server: {}", serverName);
        
        try {
            // Execute all shutdown hooks
            for (ShutdownHook hook : shutdownHooks) {
                try {
                    hook.execute();
                } catch (Exception e) {
                    logger.error("Error executing shutdown hook", e);
                }
            }
            
            // Shutdown executor service
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            running = false;
            logger.info("Apache Synapse server shutdown complete: {}", serverName);
        } catch (Exception e) {
            logger.error("Error during server shutdown", e);
        } finally {
            shutdownLatch.countDown();
        }
    }
    
    /**
     * Wait for the server to shut down
     * 
     * @throws InterruptedException if interrupted while waiting
     */
    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
    
    /**
     * Wait for the server to shut down with a timeout
     * 
     * @param timeout The timeout value
     * @param unit The timeout unit
     * @return true if the server shut down, false if the timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
        return shutdownLatch.await(timeout, unit);
    }
    
    /**
     * Add a shutdown hook
     * 
     * @param hook The shutdown hook
     */
    public void addShutdownHook(ShutdownHook hook) {
        shutdownHooks.add(hook);
    }
    
    /**
     * Check if the server is running
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get the server name
     * 
     * @return The server name
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Get the configuration directory
     * 
     * @return The configuration directory
     */
    public String getConfigDir() {
        return configDir;
    }
    
    /**
     * Get the registry
     * 
     * @return The registry
     */
    public Registry getRegistry() {
        return registry;
    }
    
    /**
     * Get the mediation registry
     * 
     * @return The mediation registry
     */
    public MediationRegistry getMediationRegistry() {
        return mediationRegistry;
    }
    
    /**
     * Get the mediation engine
     * 
     * @return The mediation engine
     */
    public MediationEngine getMediationEngine() {
        return mediationEngine;
    }
    
    /**
     * Get the message processor
     * 
     * @return The message processor
     */
    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
    
    /**
     * Get the transport manager
     * 
     * @return The transport manager
     */
    public TransportManager getTransportManager() {
        return transportManager;
    }
    
    /**
     * Get the QoS manager
     * 
     * @return The QoS manager
     */
    public QosManager getQosManager() {
        return qosManager;
    }
    
    /**
     * Get the configuration manager
     * 
     * @return The configuration manager
     */
    public ConfigurationManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the server properties
     * 
     * @return The server properties
     */
    public Properties getServerProperties() {
        return serverProperties;
    }
    
    /**
     * Set a server property
     * 
     * @param name The property name
     * @param value The property value
     */
    public void setServerProperty(String name, String value) {
        serverProperties.setProperty(name, value);
    }
    
    /**
     * Get a server property
     * 
     * @param name The property name
     * @return The property value or null if not found
     */
    public String getServerProperty(String name) {
        return serverProperties.getProperty(name);
    }
    
    /**
     * Get the executor service
     * 
     * @return The executor service
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    /**
     * Main method to start the server
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            String serverName = "SynapseServer";
            String configDir = "conf";
            
            for (int i = 0; i < args.length; i++) {
                if ("-name".equals(args[i]) && i + 1 < args.length) {
                    serverName = args[++i];
                } else if ("-conf".equals(args[i]) && i + 1 < args.length) {
                    configDir = args[++i];
                }
            }
            
            // Check if configuration directory exists
            File confDir = new File(configDir);
            if (!confDir.exists() || !confDir.isDirectory()) {
                System.err.println("Configuration directory does not exist: " + configDir);
                System.exit(1);
            }
            
            // Create and start server
            SynapseServer server = new SynapseServer(serverName, configDir);
            server.start();
            
            // Wait for server to shut down
            server.awaitShutdown();
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Shutdown hook interface
     */
    @FunctionalInterface
    public interface ShutdownHook {
        /**
         * Execute the shutdown hook
         * 
         * @throws Exception if execution fails
         */
        void execute() throws Exception;
    }
    
    /**
     * Server exception
     */
    public static class ServerException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public ServerException(String message) {
            super(message);
        }
        
        public ServerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 