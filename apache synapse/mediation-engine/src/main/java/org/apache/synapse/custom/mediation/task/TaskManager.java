package org.apache.synapse.custom.mediation.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manager for scheduled tasks in Apache Synapse.
 * Handles task scheduling, execution, and lifecycle management.
 */
public class TaskManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    private final Map<String, ScheduledTask> tasks;
    private final Map<String, ScheduledFuture<?>> scheduledFutures;
    private final ScheduledExecutorService scheduler;
    private boolean initialized;
    
    /**
     * Create a new task manager
     */
    public TaskManager() {
        this.tasks = new HashMap<>();
        this.scheduledFutures = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.initialized = false;
    }
    
    /**
     * Create a new task manager with the specified thread pool size
     * 
     * @param threadPoolSize The thread pool size
     */
    public TaskManager(int threadPoolSize) {
        this.tasks = new HashMap<>();
        this.scheduledFutures = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize);
        this.initialized = false;
    }
    
    /**
     * Initialize the task manager
     */
    public void init() {
        if (initialized) {
            logger.warn("Task manager is already initialized");
            return;
        }
        
        logger.info("Initializing task manager");
        initialized = true;
    }
    
    /**
     * Shutdown the task manager
     */
    public void shutdown() {
        if (!initialized) {
            logger.warn("Task manager is not initialized");
            return;
        }
        
        logger.info("Shutting down task manager");
        
        // Cancel all tasks
        for (String taskName : scheduledFutures.keySet()) {
            unscheduleTask(taskName);
        }
        
        // Shutdown the scheduler
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        
        initialized = false;
    }
    
    /**
     * Schedule a task
     * 
     * @param task The task to schedule
     * @throws IllegalStateException if the task manager is not initialized
     */
    public void scheduleTask(ScheduledTask task) {
        if (!initialized) {
            throw new IllegalStateException("Task manager is not initialized");
        }
        
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        String taskName = task.getName();
        if (tasks.containsKey(taskName)) {
            logger.warn("Task {} is already scheduled, unscheduling first", taskName);
            unscheduleTask(taskName);
        }
        
        logger.info("Scheduling task: {}", taskName);
        
        tasks.put(taskName, task);
        
        ScheduledFuture<?> future;
        if (task.getSchedule().isPeriodic()) {
            // Periodic task
            long initialDelay = task.getSchedule().getInitialDelay();
            long period = task.getSchedule().getPeriod();
            TimeUnit timeUnit = task.getSchedule().getTimeUnit();
            
            future = scheduler.scheduleAtFixedRate(
                () -> executeTask(task),
                initialDelay,
                period,
                timeUnit
            );
        } else if (task.getSchedule().isCron()) {
            // Cron task (not directly supported by ScheduledExecutorService)
            // For a real implementation, we would use a library like Quartz
            // For now, we'll just log a warning and schedule it as a one-time task
            logger.warn("Cron scheduling not fully implemented, scheduling as one-time task");
            future = scheduler.schedule(
                () -> executeTask(task),
                task.getSchedule().getInitialDelay(),
                task.getSchedule().getTimeUnit()
            );
        } else {
            // One-time task
            future = scheduler.schedule(
                () -> executeTask(task),
                task.getSchedule().getInitialDelay(),
                task.getSchedule().getTimeUnit()
            );
        }
        
        scheduledFutures.put(taskName, future);
    }
    
    /**
     * Unschedule a task
     * 
     * @param taskName The task name
     * @return true if the task was unscheduled, false otherwise
     */
    public boolean unscheduleTask(String taskName) {
        if (!initialized) {
            throw new IllegalStateException("Task manager is not initialized");
        }
        
        ScheduledFuture<?> future = scheduledFutures.remove(taskName);
        if (future != null) {
            future.cancel(false);
            tasks.remove(taskName);
            logger.info("Unscheduled task: {}", taskName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Execute a task
     * 
     * @param task The task to execute
     */
    private void executeTask(ScheduledTask task) {
        try {
            logger.debug("Executing task: {}", task.getName());
            task.execute();
        } catch (Exception e) {
            logger.error("Error executing task: " + task.getName(), e);
            
            // Handle task error
            try {
                task.onError(e);
            } catch (Exception ex) {
                logger.error("Error in task error handler", ex);
            }
        }
    }
    
    /**
     * Get a task by name
     * 
     * @param taskName The task name
     * @return The task, or null if not found
     */
    public ScheduledTask getTask(String taskName) {
        return tasks.get(taskName);
    }
    
    /**
     * Check if a task is scheduled
     * 
     * @param taskName The task name
     * @return true if the task is scheduled, false otherwise
     */
    public boolean isTaskScheduled(String taskName) {
        return scheduledFutures.containsKey(taskName);
    }
    
    /**
     * Get the number of scheduled tasks
     * 
     * @return The number of scheduled tasks
     */
    public int getTaskCount() {
        return tasks.size();
    }
} 