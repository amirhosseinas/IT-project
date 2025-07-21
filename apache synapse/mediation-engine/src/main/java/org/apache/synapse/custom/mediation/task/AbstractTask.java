package org.apache.synapse.custom.mediation.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for scheduled tasks.
 * Provides common functionality and default implementations.
 */
public abstract class AbstractTask implements ScheduledTask {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final String name;
    private final TaskSchedule schedule;
    
    /**
     * Create a new abstract task
     * 
     * @param name The task name
     * @param schedule The task schedule
     */
    public AbstractTask(String name, TaskSchedule schedule) {
        this.name = name;
        this.schedule = schedule;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public TaskSchedule getSchedule() {
        return schedule;
    }
    
    @Override
    public void onError(Exception exception) {
        logger.error("Error executing task: " + name, exception);
    }
    
    @Override
    public void init() throws Exception {
        logger.debug("Initializing task: {}", name);
    }
    
    @Override
    public void destroy() {
        logger.debug("Destroying task: {}", name);
    }
} 