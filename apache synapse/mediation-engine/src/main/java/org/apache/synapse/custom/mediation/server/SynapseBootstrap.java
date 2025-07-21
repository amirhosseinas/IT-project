package org.apache.synapse.custom.mediation.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Bootstrap class for Apache Synapse.
 * Handles command line parsing, environment setup, and server initialization.
 */
public class SynapseBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(SynapseBootstrap.class);
    
    private static final String DEFAULT_SERVER_NAME = "SynapseServer";
    private static final String DEFAULT_CONFIG_DIR = "conf";
    private static final String DEFAULT_PROPERTIES_FILE = "synapse.properties";
    
    /**
     * Main method to start the server
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            BootstrapOptions options = parseCommandLine(args);
            
            // Load properties
            Properties properties = loadProperties(options.configDir);
            
            // Override properties with command line options
            if (options.serverName != null) {
                properties.setProperty("synapse.server.name", options.serverName);
            }
            
            // Get server name from properties or use default
            String serverName = properties.getProperty("synapse.server.name", DEFAULT_SERVER_NAME);
            
            // Create and start server
            SynapseServer server = new SynapseServer(serverName, options.configDir);
            
            // Apply properties to server
            for (String name : properties.stringPropertyNames()) {
                server.setServerProperty(name, properties.getProperty(name));
            }
            
            // Start the server
            server.start();
            
            // Wait for server to shut down
            server.awaitShutdown();
        } catch (Exception e) {
            logger.error("Error starting server", e);
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Parse command line arguments
     * 
     * @param args Command line arguments
     * @return Bootstrap options
     */
    private static BootstrapOptions parseCommandLine(String[] args) {
        BootstrapOptions options = new BootstrapOptions();
        options.configDir = DEFAULT_CONFIG_DIR;
        
        for (int i = 0; i < args.length; i++) {
            if ("-name".equals(args[i]) && i + 1 < args.length) {
                options.serverName = args[++i];
            } else if ("-conf".equals(args[i]) && i + 1 < args.length) {
                options.configDir = args[++i];
            } else if ("-help".equals(args[i]) || "-h".equals(args[i])) {
                printUsage();
                System.exit(0);
            }
        }
        
        // Check if configuration directory exists
        File confDir = new File(options.configDir);
        if (!confDir.exists() || !confDir.isDirectory()) {
            logger.error("Configuration directory does not exist: {}", options.configDir);
            System.err.println("Configuration directory does not exist: " + options.configDir);
            System.exit(1);
        }
        
        return options;
    }
    
    /**
     * Load properties from the properties file
     * 
     * @param configDir The configuration directory
     * @return Properties
     */
    private static Properties loadProperties(String configDir) {
        Properties properties = new Properties();
        
        // Load properties from file
        File propsFile = new File(configDir, DEFAULT_PROPERTIES_FILE);
        if (propsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                properties.load(fis);
                logger.info("Loaded properties from: {}", propsFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to load properties file", e);
            }
        } else {
            logger.info("Properties file not found: {}", propsFile.getAbsolutePath());
        }
        
        return properties;
    }
    
    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Apache Synapse Server");
        System.out.println("Usage: java -jar synapse.jar [options]");
        System.out.println("Options:");
        System.out.println("  -name <name>    Server name (default: " + DEFAULT_SERVER_NAME + ")");
        System.out.println("  -conf <dir>     Configuration directory (default: " + DEFAULT_CONFIG_DIR + ")");
        System.out.println("  -help, -h       Print this help message");
    }
    
    /**
     * Bootstrap options
     */
    private static class BootstrapOptions {
        String serverName;
        String configDir;
    }
} 