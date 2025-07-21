package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for mail transport listeners that receive incoming emails.
 */
public abstract class AbstractMailTransportListener implements TransportListener {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractMailTransportListener.class);
    
    protected final String host;
    protected final int port;
    protected final String username;
    protected final String password;
    protected final long pollingInterval;
    protected final boolean ssl;
    
    protected MessageCallback callback;
    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected ScheduledExecutorService scheduler;
    
    /**
     * Create a new mail transport listener
     * 
     * @param host The mail server host
     * @param port The mail server port
     * @param username The username for authentication
     * @param password The password for authentication
     * @param pollingInterval The interval in milliseconds to check for new mail
     * @param ssl Whether to use SSL/TLS
     */
    public AbstractMailTransportListener(String host, int port, String username, String password, 
                                      long pollingInterval, boolean ssl) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.pollingInterval = pollingInterval;
        this.ssl = ssl;
    }
    
    @Override
    public void init() throws TransportException {
        logger.info("Initializing mail transport listener: {}:{}", host, port);
        // Subclasses should implement initialization logic
    }
    
    @Override
    public void start() throws TransportException {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting mail transport listener: {}:{}", host, port);
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(this::pollForMail, 0, pollingInterval, TimeUnit.MILLISECONDS);
            logger.info("Mail transport listener started with polling interval: {} ms", pollingInterval);
        } else {
            logger.warn("Mail transport listener already running: {}:{}", host, port);
        }
    }
    
    @Override
    public void stop() throws TransportException {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping mail transport listener: {}:{}", host, port);
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scheduler = null;
            }
            logger.info("Mail transport listener stopped: {}:{}", host, port);
        } else {
            logger.warn("Mail transport listener already stopped: {}:{}", host, port);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public void setMessageCallback(MessageCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Poll for new mail messages
     * This method is called periodically by the scheduler
     */
    protected abstract void pollForMail();
} 