# JMS Transport for Apache Synapse

This module provides JMS (Java Message Service) transport capabilities for Apache Synapse, allowing it to connect to JMS providers such as ActiveMQ, RabbitMQ, and others.

## Features

- Connects to JMS providers (ActiveMQ, RabbitMQ, etc.)
- Handles JMS message sending and receiving
- Supports both queues and topics
- Converts JMS messages to Synapse MessageContext
- Connection pooling for efficient resource usage
- Error handling and retry logic
- Support for request-response messaging pattern

## Configuration

The JMS transport can be configured with the following properties:

### Connection Properties

- `jms.initialContextFactory`: The JNDI initial context factory class (e.g., `org.apache.activemq.jndi.ActiveMQInitialContextFactory`)
- `jms.providerUrl`: The URL of the JMS provider (e.g., `tcp://localhost:61616`)
- `jms.connectionFactoryName`: The name of the connection factory (default: `ConnectionFactory`)
- `jms.username`: Username for authentication (optional)
- `jms.password`: Password for authentication (optional)

### Destination Properties

- `jms.destinationName`: The name of the JMS destination (queue or topic)
- `jms.destinationType`: The type of destination (`queue` or `topic`, default: `queue`)

### Session Properties

- `jms.transacted`: Whether to use transacted sessions (default: `false`)
- `jms.acknowledgeMode`: The acknowledgment mode (default: `AUTO_ACKNOWLEDGE`)

### Connection Pooling Properties

- `jms.connectionPoolSize`: Maximum number of connections in the pool (default: `10`)
- `jms.connectionIdleTimeout`: Timeout for idle connections in milliseconds (default: `300000` - 5 minutes)

### Retry Properties

- `jms.maxRetries`: Maximum number of retry attempts (default: `3`)
- `jms.retryInterval`: Interval between retry attempts in milliseconds (default: `5000` - 5 seconds)

### Message Properties

- `jms.messageSelector`: JMS message selector for filtering messages (optional)
- `jms.receiveTimeout`: Timeout for receiving messages in milliseconds (default: `1000` - 1 second)

## Usage

### Setting up the JMS Transport

```java
// Create transport manager
TransportManager transportManager = new TransportManager();

// Create JMS transport factory
JmsTransportFactory transportFactory = new JmsTransportFactory(transportManager);

// Configure JMS properties
Properties properties = new Properties();
properties.setProperty("jms.initialContextFactory", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
properties.setProperty("jms.providerUrl", "tcp://localhost:61616");
properties.setProperty("jms.connectionFactoryName", "ConnectionFactory");
properties.setProperty("jms.username", "admin");
properties.setProperty("jms.password", "admin");
properties.setProperty("jms.queue.name", "example.queue");
properties.setProperty("jms.topic.name", "example.topic");

// Create JMS transports
transportFactory.createDefaultTransports(properties);

// Initialize and start listeners
transportManager.initializeListeners();
transportManager.startListeners();
```

### Sending Messages to a Queue

```java
// Create a message
Message message = new Message(UUID.randomUUID().toString());
message.setPayload("Hello, JMS Queue!".getBytes());
message.setContentType("text/plain");

// Send the message to a queue
String endpoint = "queue:example.queue";
transportManager.getSender("jms").send(message, endpoint);
```

### Publishing Messages to a Topic

```java
// Create a message
Message message = new Message(UUID.randomUUID().toString());
message.setPayload("Hello, JMS Topic!".getBytes());
message.setContentType("text/plain");

// Publish the message to a topic
String endpoint = "topic:example.topic";
transportManager.getSender("jms").send(message, endpoint);
```

### Request-Response Messaging

```java
// Create a request message
Message message = new Message(UUID.randomUUID().toString());
message.setPayload("Request message".getBytes());
message.setContentType("text/plain");
message.setProperty("JMS_WAIT_FOR_RESPONSE", true);
message.setProperty("JMS_RESPONSE_TIMEOUT", 10000L); // 10 seconds

// Send the request and get the response
String endpoint = "queue:example.queue";
Message response = transportManager.getSender("jms").send(message, endpoint);

// Process the response
if (response != null) {
    System.out.println("Received response: " + new String(response.getPayload()));
}
```

### Receiving Messages

```java
// Set up a message callback for a queue listener
TransportListener queueListener = transportManager.getListener("jms-queue");
queueListener.setMessageCallback(message -> {
    System.out.println("Received queue message: " + new String(message.getPayload()));
    
    // Return a response message if needed
    Message response = new Message(UUID.randomUUID().toString());
    response.setPayload("Response message".getBytes());
    response.setContentType("text/plain");
    return response;
});
```

## Endpoint URI Format

JMS endpoints can be specified in the following formats:

- `jms:<destination>` - Uses the default destination type (queue)
- `queue:<queue-name>` - Specifies a queue destination
- `topic:<topic-name>` - Specifies a topic destination

Additional parameters can be added to the URI:

```
queue:example.queue?initialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory&providerUrl=tcp://localhost:61616
```

## Dependencies

- JMS API (javax.jms-api)
- A JMS provider implementation (e.g., ActiveMQ, RabbitMQ)
- Apache Commons Pool for connection pooling

## Example

See the `JmsTransportExample` class for a complete example of using the JMS transport. 