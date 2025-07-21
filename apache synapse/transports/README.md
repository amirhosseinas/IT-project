# Apache Synapse HTTP/HTTPS Transport

This module implements the HTTP and HTTPS transport for Apache Synapse, providing a non-blocking, NIO-based HTTP server and client implementation.

## Features

1. **Non-blocking NIO-based HTTP server** using Apache HttpCore
2. **HTTP request/response handling** with support for all HTTP methods
3. **SSL/TLS support** for secure communication
4. **Connection pooling and management** for efficient resource usage
5. **Request routing to mediation engine** based on URL patterns
6. **Clean architecture** following Apache Synapse transport patterns

## Architecture

The HTTP/HTTPS transport implementation follows a layered architecture:

1. **Transport Layer**: Handles low-level network I/O using Apache HttpCore NIO
2. **Protocol Layer**: Implements HTTP/HTTPS protocol handling
3. **Integration Layer**: Connects the transport to the Synapse mediation engine

### Key Components

- **TransportManager**: Central component managing all transport listeners and senders
- **TransportListener**: Interface for transport listeners that receive incoming messages
- **TransportSender**: Interface for transport senders that send outgoing messages
- **HttpTransportListener**: Non-blocking HTTP server implementation
- **HttpsTransportListener**: HTTPS server with SSL/TLS support
- **HttpTransportSender**: HTTP client with connection pooling
- **HttpsTransportSender**: HTTPS client with SSL/TLS support
- **HttpRequestRouter**: Routes HTTP requests to the appropriate mediation sequence
- **HttpTransportFactory**: Factory for creating HTTP/HTTPS transport components

## Usage

### Setting Up HTTP Transport

```java
// Create an HTTP transport listener
TransportListener httpListener = HttpTransportFactory.createListener("http", "localhost", 8080);

// Create a router to route HTTP requests to the mediation engine
HttpRequestRouter router = new HttpRequestRouter(mediationEngine, "main");

// Add routing rules
router.addRoutingRule("/api/v1/.*", "api");
router.addRoutingRule("/echo/.*", "main");

// Set the router as the message callback
httpListener.setMessageCallback(router);

// Register the listener with the transport manager
transportManager.registerListener("http", httpListener);

// Create an HTTP transport sender
TransportSender httpSender = HttpTransportFactory.createSender("http");

// Register the sender with the transport manager
transportManager.registerSender("http", httpSender);

// Initialize the transport components
httpListener.init();
httpSender.init();

// Start the listener
httpListener.start();
```

### Setting Up HTTPS Transport

```java
// Create an HTTPS transport listener with SSL/TLS support
String keystorePath = "/path/to/keystore.jks";
String keystorePassword = "password";
String keystoreType = "JKS";

TransportListener httpsListener = HttpTransportFactory.createHttpsListener(
    "localhost", 8443, keystorePath, keystorePassword, keystoreType);

// Create a router to route HTTPS requests to the mediation engine
HttpRequestRouter secureRouter = new HttpRequestRouter(mediationEngine, "secure");

// Add routing rules
secureRouter.addRoutingRule("/secure/.*", "secure");
secureRouter.addRoutingRule("/api/v1/secure/.*", "api");

// Set the router as the message callback
httpsListener.setMessageCallback(secureRouter);

// Register the listener with the transport manager
transportManager.registerListener("https", httpsListener);

// Create an HTTPS transport sender with SSL/TLS support
String truststorePath = "/path/to/truststore.jks";
String truststorePassword = "password";
String truststoreType = "JKS";

TransportSender httpsSender = HttpTransportFactory.createHttpsSender(
    keystorePath, keystorePassword, keystoreType);

// Register the sender with the transport manager
transportManager.registerSender("https", httpsSender);

// Initialize the transport components
httpsListener.init();
httpsSender.init();

// Start the listener
httpsListener.start();
```

### Sending HTTP Requests

```java
// Create a request message
Message requestMessage = new Message();
requestMessage.setDirection(Message.Direction.REQUEST);
requestMessage.setContentType("application/json");
requestMessage.setPayload("{\"key\":\"value\"}".getBytes());
requestMessage.setProperty("http.method", "POST");

// Get the HTTP sender
TransportSender httpSender = transportManager.getSender("http");

// Send the request
Message responseMessage = httpSender.send(requestMessage, "http://example.com/api");
```

## Configuration Options

### HTTP Listener Configuration

- **Host**: The host to bind to
- **Port**: The port to listen on
- **I/O Thread Count**: Number of I/O threads (default: 2)
- **Worker Thread Count**: Number of worker threads (default: 5)
- **Socket Timeout**: Socket timeout in milliseconds (default: 30000)
- **Connection Timeout**: Connection timeout in milliseconds (default: 30000)

### HTTPS Listener Configuration

- All HTTP listener configuration options
- **Keystore Path**: Path to the keystore file
- **Keystore Password**: Password for the keystore
- **Keystore Type**: Type of keystore (e.g., "JKS", "PKCS12")
- **Truststore Path**: Path to the truststore file (optional)
- **Truststore Password**: Password for the truststore (optional)
- **Truststore Type**: Type of truststore (optional)
- **Enabled Protocols**: Array of enabled SSL/TLS protocols (optional)
- **Enabled Cipher Suites**: Array of enabled cipher suites (optional)

### HTTP Sender Configuration

- **Max Total Connections**: Maximum total connections in the pool (default: 100)
- **Max Connections Per Route**: Maximum connections per route (default: 20)
- **Connection Timeout**: Connection timeout in milliseconds (default: 30000)
- **Socket Timeout**: Socket timeout in milliseconds (default: 30000)
- **Connection Request Timeout**: Connection request timeout in milliseconds (default: 30000)

### HTTPS Sender Configuration

- All HTTP sender configuration options
- **Keystore Path**: Path to the keystore file
- **Keystore Password**: Password for the keystore
- **Keystore Type**: Type of keystore (e.g., "JKS", "PKCS12")
- **Truststore Path**: Path to the truststore file (optional)
- **Truststore Password**: Password for the truststore (optional)
- **Truststore Type**: Type of truststore (optional)
- **Enabled Protocols**: Array of enabled SSL/TLS protocols (optional)
- **Enabled Cipher Suites**: Array of enabled cipher suites (optional)
- **Hostname Verification**: Whether to verify hostnames (default: true)

## Examples

See the `org.apache.synapse.custom.transports.http.examples` package for example code demonstrating how to use the HTTP/HTTPS transport. 