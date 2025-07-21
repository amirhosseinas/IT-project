package org.apache.synapse.custom.mediation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Event source that generates events at regular intervals.
 */
public class TimerEventSource implements EventSource {
    private static final Logger logger = LoggerFactory.getLogger(TimerEventSource.class);
    
    private final String name;
    private final long interval;
    private final TimeUnit timeUnit;
    private final List<EventListener> listeners;
    private boolean started;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;
    
    /**
     * Create a new timer event source
     * 
     * @param name The event source name
     * @param interval The interval between events
     * @param timeUnit The time unit for the interval
     */
    public TimerEventSource(String name, long interval, TimeUnit timeUnit) {
        this.name = name;
        this.interval = interval;
        this.timeUnit = timeUnit;
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
            logger.warn("Timer event source {} is already started", name);
            return;
        }
        
        logger.info("Starting timer event source: {}", name);
        
        try {
            executor = Executors.newSingleThreadScheduledExecutor();
            scheduledFuture = executor.scheduleAtFixedRate(
                this::fireEvent,
                0,
                interval,
                timeUnit
            );
            
            started = true;
        } catch (Exception e) {
            logger.error("Error starting timer event source: " + name, e);
            throw new EventException("Error starting timer event source: " + name, e);
        }
    }
    
    @Override
    public void stop() throws EventException {
        if (!started) {
            logger.warn("Timer event source {} is not started", name);
            return;
        }
        
        logger.info("Stopping timer event source: {}", name);
        
        try {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            
            if (executor != null) {
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
            
            started = false;
        } catch (Exception e) {
            logger.error("Error stopping timer event source: " + name, e);
            throw new EventException("Error stopping timer event source: " + name, e);
        }
    }
    
    @Override
    public void addEventListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        listeners.add(listener);
        logger.debug("Added event listener to timer event source {}: {}", name, listener.getName());
    }
    
    @Override
    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
        logger.debug("Removed event listener from timer event source {}: {}", name, listener.getName());
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
     * Fire an event to all listeners
     */
    private void fireEvent() {
        try {
            Event event = new Event("timer", this);
            event.setProperty("timestamp", System.currentTimeMillis());
            
            logger.debug("Firing timer event from source: {}", name);
            
            for (EventListener listener : listeners) {
                try {
                    listener.handleEvent(event);
                } catch (Exception e) {
                    logger.error("Error in event listener: " + listener.getName(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error firing timer event", e);
        }
    }
    
    /**
     * Get the interval
     * 
     * @return The interval
     */
    public long getInterval() {
        return interval;
    }
    
    /**
     * Get the time unit
     * 
     * @return The time unit
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
} 