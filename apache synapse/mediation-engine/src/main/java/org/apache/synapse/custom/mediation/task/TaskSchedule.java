package org.apache.synapse.custom.mediation.task;

import java.util.concurrent.TimeUnit;

/**
 * Represents a schedule for a task.
 * Supports one-time, periodic, and cron-based scheduling.
 */
public class TaskSchedule {
    
    private final long initialDelay;
    private final long period;
    private final TimeUnit timeUnit;
    private final String cronExpression;
    private final boolean periodic;
    private final boolean cron;
    
    /**
     * Create a one-time task schedule
     * 
     * @param initialDelay The delay before execution
     * @param timeUnit The time unit for the delay
     * @return The task schedule
     */
    public static TaskSchedule oneTime(long initialDelay, TimeUnit timeUnit) {
        return new TaskSchedule(initialDelay, 0, timeUnit, null, false, false);
    }
    
    /**
     * Create a periodic task schedule
     * 
     * @param initialDelay The delay before the first execution
     * @param period The period between executions
     * @param timeUnit The time unit for the delay and period
     * @return The task schedule
     */
    public static TaskSchedule periodic(long initialDelay, long period, TimeUnit timeUnit) {
        return new TaskSchedule(initialDelay, period, timeUnit, null, true, false);
    }
    
    /**
     * Create a cron-based task schedule
     * 
     * @param cronExpression The cron expression
     * @return The task schedule
     */
    public static TaskSchedule cron(String cronExpression) {
        return new TaskSchedule(0, 0, TimeUnit.MILLISECONDS, cronExpression, false, true);
    }
    
    /**
     * Create a new task schedule
     * 
     * @param initialDelay The delay before the first execution
     * @param period The period between executions
     * @param timeUnit The time unit for the delay and period
     * @param cronExpression The cron expression
     * @param periodic Whether this is a periodic schedule
     * @param cron Whether this is a cron-based schedule
     */
    private TaskSchedule(long initialDelay, long period, TimeUnit timeUnit, 
                        String cronExpression, boolean periodic, boolean cron) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.timeUnit = timeUnit;
        this.cronExpression = cronExpression;
        this.periodic = periodic;
        this.cron = cron;
    }
    
    /**
     * Get the initial delay
     * 
     * @return The initial delay
     */
    public long getInitialDelay() {
        return initialDelay;
    }
    
    /**
     * Get the period
     * 
     * @return The period
     */
    public long getPeriod() {
        return period;
    }
    
    /**
     * Get the time unit
     * 
     * @return The time unit
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
    
    /**
     * Get the cron expression
     * 
     * @return The cron expression
     */
    public String getCronExpression() {
        return cronExpression;
    }
    
    /**
     * Check if this is a periodic schedule
     * 
     * @return true if this is a periodic schedule, false otherwise
     */
    public boolean isPeriodic() {
        return periodic;
    }
    
    /**
     * Check if this is a cron-based schedule
     * 
     * @return true if this is a cron-based schedule, false otherwise
     */
    public boolean isCron() {
        return cron;
    }
    
    /**
     * Check if this is a one-time schedule
     * 
     * @return true if this is a one-time schedule, false otherwise
     */
    public boolean isOneTime() {
        return !periodic && !cron;
    }
    
    @Override
    public String toString() {
        if (isPeriodic()) {
            return "Periodic: initialDelay=" + initialDelay + ", period=" + period + " " + timeUnit;
        } else if (isCron()) {
            return "Cron: " + cronExpression;
        } else {
            return "OneTime: delay=" + initialDelay + " " + timeUnit;
        }
    }
} 