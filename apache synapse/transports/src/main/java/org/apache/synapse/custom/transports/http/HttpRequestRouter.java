package org.apache.synapse.custom.transports.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.custom.mediation.MediationEngine;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Routes HTTP requests to the appropriate mediation sequence based on URL patterns.
 * This class connects the HTTP transport layer with the mediation engine.
 */
public class HttpRequestRouter implements TransportListener.MessageCallback {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestRouter.class);
    
    private final MediationEngine mediationEngine;
    private final Map<Pattern, String> routingRules;
    private final String defaultSequence;
    
    /**
     * Create a new HTTP request router
     * 
     * @param mediationEngine The mediation engine to route requests to
     * @param defaultSequence The default sequence to use when no routing rule matches
     */
    public HttpRequestRouter(MediationEngine mediationEngine, String defaultSequence) {
        this.mediationEngine = mediationEngine;
        this.routingRules = new HashMap<>();
        this.defaultSequence = defaultSequence;
    }
    
    /**
     * Add a routing rule
     * 
     * @param urlPattern The URL pattern to match (regex)
     * @param sequenceName The name of the sequence to route to
     */
    public void addRoutingRule(String urlPattern, String sequenceName) {
        if (StringUtils.isBlank(urlPattern) || StringUtils.isBlank(sequenceName)) {
            throw new IllegalArgumentException("URL pattern and sequence name cannot be null or empty");
        }
        
        Pattern pattern = Pattern.compile(urlPattern);
        routingRules.put(pattern, sequenceName);
        logger.info("Added routing rule: {} -> {}", urlPattern, sequenceName);
    }
    
    @Override
    public Message onMessage(Message message) {
        try {
            // Get the URI from the message
            String uri = (String) message.getProperty("http.uri");
            if (uri == null) {
                logger.warn("No URI found in message, using default sequence");
                return mediationEngine.mediate(message, defaultSequence);
            }
            
            // Find matching routing rule
            String sequenceName = findMatchingSequence(uri);
            
            // Route the message to the appropriate sequence
            logger.debug("Routing request {} to sequence {}", uri, sequenceName);
            return mediationEngine.mediate(message, sequenceName);
        } catch (Exception e) {
            logger.error("Error routing HTTP request", e);
            
            // Create error response
            Message errorResponse = new Message();
            errorResponse.setDirection(Message.Direction.RESPONSE);
            errorResponse.setProperty("http.status.code", 500);
            errorResponse.setContentType("text/plain");
            errorResponse.setPayload(("Internal server error: " + e.getMessage()).getBytes());
            
            return errorResponse;
        }
    }
    
    /**
     * Find the sequence that matches the given URI
     * 
     * @param uri The URI to match
     * @return The name of the matching sequence or the default sequence if no match is found
     */
    private String findMatchingSequence(String uri) {
        for (Map.Entry<Pattern, String> entry : routingRules.entrySet()) {
            if (entry.getKey().matcher(uri).matches()) {
                return entry.getValue();
            }
        }
        
        return defaultSequence;
    }
} 