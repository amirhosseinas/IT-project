package org.apache.synapse.custom.transports.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerRegistry;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.synapse.custom.message.Message;
import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;

/**
 * HTTP Transport listener implementation based on Apache HttpCore NIO.
 * Provides non-blocking HTTP server functionality.
 */
public class HttpTransportListener implements TransportListener {
    private static final Logger logger = LoggerFactory.getLogger(HttpTransportListener.class);
    
    private final String host;
    private final int port;
    private final boolean ssl;
    private final int ioThreadCount;
    private final int workerThreadCount;
    private final int socketTimeout;
    private final int connectionTimeout;
    
    private ListeningIOReactor ioReactor;
    private ExecutorService workerPool;
    private ExecutorService reactorThread;
    private MessageCallback messageCallback;
    private volatile boolean running;
    
    /**
     * Create a new HTTP transport listener
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @param ssl Whether to use SSL/TLS
     */
    public HttpTransportListener(String host, int port, boolean ssl) {
        this(host, port, ssl, 2, 5, 30000, 30000);
    }
    
    /**
     * Create a new HTTP transport listener with custom configuration
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @param ssl Whether to use SSL/TLS
     * @param ioThreadCount Number of I/O threads
     * @param workerThreadCount Number of worker threads
     * @param socketTimeout Socket timeout in milliseconds
     * @param connectionTimeout Connection timeout in milliseconds
     */
    public HttpTransportListener(String host, int port, boolean ssl, 
                               int ioThreadCount, int workerThreadCount, 
                               int socketTimeout, int connectionTimeout) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.ioThreadCount = ioThreadCount;
        this.workerThreadCount = workerThreadCount;
        this.socketTimeout = socketTimeout;
        this.connectionTimeout = connectionTimeout;
    }
    
    @Override
    public void init() throws TransportException {
        try {
            // Create HTTP parameters
            HttpParams params = new SyncBasicHttpParams();
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout)
                  .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout)
                  .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                  .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                  .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "Apache-Synapse-Custom/1.0");
            
            // Create I/O reactor configuration
            IOReactorConfig config = new IOReactorConfig();
            config.setIoThreadCount(ioThreadCount);
            config.setSoTimeout(socketTimeout);
            config.setConnectTimeout(connectionTimeout);
            config.setTcpNoDelay(true);
            
            // Create the I/O reactor
            ioReactor = new DefaultListeningIOReactor(config);
            
            // Create connection factory
            NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory = 
                new DefaultNHttpServerConnectionFactory(params);
            
            // Create HTTP protocol processor
            HttpProcessor httpProcessor = new ImmutableHttpProcessor(
                    new ResponseDate(),
                    new ResponseServer(),
                    new ResponseContent(),
                    new ResponseConnControl());
            
            // Create request handler registry
            HttpAsyncRequestHandlerRegistry registry = new HttpAsyncRequestHandlerRegistry();
            registry.register("*", new SynapseHttpRequestHandler());
            
            // Create HTTP service
            HttpAsyncService httpService = new HttpAsyncService(httpProcessor, null, registry, params);
            
            // Create I/O event dispatch
            IOEventDispatch ioEventDispatch = new DefaultHttpServerIODispatch(httpService, connFactory);
            
            // Create worker thread pool
            workerPool = Executors.newFixedThreadPool(workerThreadCount, new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(0);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("synapse-http-worker-" + count.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
            
            // Create reactor thread
            reactorThread = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("synapse-http-reactor");
                t.setDaemon(true);
                return t;
            });
            
            logger.info("HTTP transport listener initialized on {}:{} (SSL: {})", host, port, ssl);
        } catch (IOReactorException e) {
            throw new TransportException("Failed to initialize HTTP transport listener", e);
        }
    }
    
    @Override
    public void start() throws TransportException {
        if (running) {
            logger.warn("HTTP transport listener already running");
            return;
        }
        
        if (ioReactor == null) {
            throw new TransportException("HTTP transport listener not initialized");
        }
        
        try {
            // Start the I/O reactor in a separate thread
            reactorThread.execute(() -> {
                try {
                    // Listen on the configured host and port
                    ioReactor.listen(new InetSocketAddress(host, port));
                    // Start the I/O reactor
                    ioReactor.execute(new DefaultHttpServerIODispatch(
                            new HttpAsyncService(
                                    new ImmutableHttpProcessor(
                                            new ResponseDate(),
                                            new ResponseServer(),
                                            new ResponseContent(),
                                            new ResponseConnControl()),
                                    null,
                                    new HttpAsyncRequestHandlerRegistry() {{
                                        register("*", new SynapseHttpRequestHandler());
                                    }},
                                    new SyncBasicHttpParams()),
                            new DefaultNHttpServerConnectionFactory(new SyncBasicHttpParams())));
                } catch (IOException e) {
                    logger.error("I/O reactor error", e);
                }
            });
            
            running = true;
            logger.info("HTTP transport listener started on {}:{} (SSL: {})", host, port, ssl);
        } catch (Exception e) {
            throw new TransportException("Failed to start HTTP transport listener", e);
        }
    }
    
    @Override
    public void stop() throws TransportException {
        if (!running) {
            logger.warn("HTTP transport listener not running");
            return;
        }
        
        try {
            // Shut down the I/O reactor
            ioReactor.shutdown();
            
            // Shut down thread pools
            workerPool.shutdown();
            reactorThread.shutdown();
            
            running = false;
            logger.info("HTTP transport listener stopped");
        } catch (IOException e) {
            throw new TransportException("Failed to stop HTTP transport listener", e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }
    
    /**
     * HTTP request handler that processes incoming HTTP requests.
     */
    private class SynapseHttpRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {
        
        @Override
        public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context) {
            return new BasicAsyncRequestConsumer();
        }
        
        @Override
        public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context) {
            workerPool.execute(() -> {
                try {
                    // Create response object
                    final HttpResponse response = httpExchange.getResponse();
                    
                    // Convert HTTP request to Synapse message
                    Message synapseMessage = convertHttpRequestToSynapseMessage(request);
                    
                    // Process the message if a callback is registered
                    Message responseMessage = null;
                    if (messageCallback != null) {
                        responseMessage = messageCallback.onMessage(synapseMessage);
                    }
                    
                    // Convert Synapse response message to HTTP response
                    if (responseMessage != null) {
                        convertSynapseMessageToHttpResponse(responseMessage, response);
                    } else {
                        // No response message, send 202 Accepted
                        response.setStatusCode(HttpStatus.SC_ACCEPTED);
                        response.setEntity(new NStringEntity("Request accepted", StandardCharsets.UTF_8));
                    }
                    
                    // Send the response
                    httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
                } catch (Exception e) {
                    logger.error("Error processing HTTP request", e);
                    
                    // Send error response
                    HttpResponse response = httpExchange.getResponse();
                    response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    try {
                        response.setEntity(new NStringEntity("Internal server error", StandardCharsets.UTF_8));
                    } catch (Exception ex) {
                        logger.error("Error creating error response", ex);
                    }
                    httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
                }
            });
        }
        
        /**
         * Convert an HTTP request to a Synapse message
         * 
         * @param request The HTTP request
         * @return The Synapse message
         */
        private Message convertHttpRequestToSynapseMessage(HttpRequest request) throws IOException {
            Message message = new Message(UUID.randomUUID().toString());
            message.setDirection(Message.Direction.REQUEST);
            
            // Set content type
            String contentType = request.getFirstHeader("Content-Type") != null ?
                    request.getFirstHeader("Content-Type").getValue() : "application/octet-stream";
            message.setContentType(contentType);
            
            // Copy headers
            for (org.apache.http.Header header : request.getAllHeaders()) {
                message.setHeader(header.getName(), header.getValue());
            }
            
            // Copy payload if present
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                if (entity != null) {
                    message.setPayload(IOUtils.toByteArray(entity.getContent()));
                }
            }
            
            // Set request-specific properties
            message.setProperty("http.method", request.getRequestLine().getMethod());
            message.setProperty("http.uri", request.getRequestLine().getUri());
            message.setProperty("http.protocol", request.getRequestLine().getProtocolVersion().toString());
            
            return message;
        }
        
        /**
         * Convert a Synapse message to an HTTP response
         * 
         * @param message The Synapse message
         * @param response The HTTP response to populate
         */
        private void convertSynapseMessageToHttpResponse(Message message, HttpResponse response) {
            // Set status code
            int statusCode = HttpStatus.SC_OK;
            if (message.getProperty("http.status.code") != null) {
                statusCode = (Integer) message.getProperty("http.status.code");
            }
            response.setStatusCode(statusCode);
            
            // Copy headers
            for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                response.setHeader(entry.getKey(), entry.getValue());
            }
            
            // Set payload if present
            if (message.getPayload() != null) {
                ByteArrayEntity entity = new ByteArrayEntity(message.getPayload());
                if (message.getContentType() != null) {
                    entity.setContentType(message.getContentType());
                }
                response.setEntity(entity);
            }
        }
    }
} 