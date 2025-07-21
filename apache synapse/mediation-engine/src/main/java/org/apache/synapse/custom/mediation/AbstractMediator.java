package org.apache.synapse.custom.mediation;

import org.apache.synapse.custom.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all mediators in the system.
 * Provides common functionality and default implementations.
 */
public abstract class AbstractMediator implements Mediator {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected String name;
    protected boolean traceEnabled;
    protected boolean statisticsEnabled;
    
    /**
     * Create a new mediator with the specified name
     * 
     * @param name The mediator name
     */
    public AbstractMediator(String name) {
        this.name = name;
        this.traceEnabled = false;
        this.statisticsEnabled = false;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this mediator
     * 
     * @param name The mediator name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Check if tracing is enabled for this mediator
     * 
     * @return true if tracing is enabled, false otherwise
     */
    public boolean isTraceEnabled() {
        return traceEnabled;
    }
    
    /**
     * Enable or disable tracing for this mediator
     * 
     * @param traceEnabled true to enable tracing, false to disable
     */
    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }
    
    /**
     * Check if statistics collection is enabled for this mediator
     * 
     * @return true if statistics collection is enabled, false otherwise
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
    
    /**
     * Enable or disable statistics collection for this mediator
     * 
     * @param statisticsEnabled true to enable statistics collection, false to disable
     */
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }
    
    /**
     * Apply the mediation logic to a message
     * 
     * @param message The message to mediate
     * @return The mediated message
     * @throws MediationEngine.MediationException if mediation fails
     */
    @Override
    public Message mediate(Message message) throws MediationEngine.MediationException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        long startTime = 0;
        boolean success = false;
        
        try {
            if (traceEnabled) {
                logger.debug("Mediator {} start", name);
            }
            
            if (statisticsEnabled) {
                startTime = System.currentTimeMillis();
            }
            
            // Perform the actual mediation
            Message result = doMediate(message);
            
            success = true;
            return result;
        } catch (Exception e) {
            logger.error("Error in mediator " + name, e);
            throw new MediationEngine.MediationException("Error in mediator " + name, e);
        } finally {
            if (traceEnabled) {
                logger.debug("Mediator {} end: {}", name, success ? "success" : "failure");
            }
            
            if (statisticsEnabled) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                // Record statistics (would be implemented in a real system)
                logger.debug("Mediator {} execution time: {} ms", name, duration);
            }
        }
    }
    
    /**
     * Perform the actual mediation logic
     * 
     * @param message The message to mediate
     * @return The mediated message
     * @throws Exception if mediation fails
     */
    protected abstract Message doMediate(Message message) throws Exception;
} 