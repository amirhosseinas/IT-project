package org.apache.synapse.custom.mediation.task;

/**
 * Interface for scheduled tasks in Apache Synapse.
 * Tasks are executed according to their schedule by the TaskManager.
 */
public interface ScheduledTask {
    
    /**
     * Get the name of this task
     * 
     * @return The task name
     */
    String getName();
    
    /**
     * Get the schedule for this task
     * 
     * @return The task schedule
     */
    TaskSchedule getSchedule();
    
    /**
     * Execute the task
     * 
     * @throws Exception if execution fails
     */
    void execute() throws Exception;
    
    /**
     * Handle an error that occurred during task execution
     * 
     * @param exception The exception that occurred
     */
    void onError(Exception exception);
    
    /**
     * Initialize the task
     * 
     * @throws Exception if initialization fails
     */
    void init() throws Exception;
    
    /**
     * Destroy the task and release resources
     */
    void destroy();
} 