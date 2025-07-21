# FIX Transport for Apache Synapse

This module provides FIX (Financial Information Exchange) protocol support for Apache Synapse.

## Features

1. **FIX Protocol Message Parsing**: Converts between FIX messages and Synapse messages.
2. **Session Management**: Manages FIX sessions, including initiator and acceptor modes.
3. **Message Validation**: Validates FIX messages according to protocol specifications.
4. **Sequence Number Handling**: Manages FIX message sequence numbers.
5. **Connection Management**: Handles FIX connections and reconnection.
6. **Integration with Mediation Engine**: Seamlessly integrates with the Synapse mediation engine.

## Components

- `FixTransportListener`: Receives incoming FIX messages.
- `FixTransportSender`: Sends outgoing FIX messages.
- `FixSessionManager`: Manages FIX sessions.
- `FixMessageConverter`: Converts between FIX and Synapse messages.
- `FixMessageValidator`: Validates FIX messages.
- `FixApplication`: Handles FIX session events.

## Usage

### Configuration

Create a QuickFIX/J configuration file:

```
[DEFAULT]
FileStorePath=store
FileLogPath=log
ConnectionType=initiator
ReconnectInterval=60
SenderCompID=SYNAPSE
TargetCompID=BROKER
SocketConnectHost=localhost
SocketConnectPort=9876
StartTime=00:00:00
EndTime=00:00:00
HeartBtInt=30
ValidateUserDefinedFields=N

[SESSION]
BeginString=FIX.4.4
DataDictionary=FIX44.xml
```

### Setting Up a FIX Listener

```java
// Create and initialize the FIX transport listener
FixTransportListener listener = FixTransportFactory.createListener("fix.cfg");

// Set the message callback
listener.setMessageCallback(message -> {
    // Process the message
    // ...
    
    // Return a response or null
    return responseMessage;
});

// Initialize and start the listener
listener.init();
listener.start();
```

### Sending FIX Messages

```java
// Create and initialize the FIX transport sender
FixTransportSender sender = FixTransportFactory.createSender("fix.cfg");
sender.init();

// Create a message
Message message = new Message();
message.setPayload(fixMessageBytes);

// Send the message
String endpoint = "fix://BROKER:SYNAPSE";
Message response = sender.send(message, endpoint);

// Process the response
// ...

// Close the sender when done
sender.close();
```

## Integration with Mediation Engine

Register the FIX transport with the Synapse transport manager:

```java
// Create transport components
FixTransportListener listener = FixTransportFactory.createListener("fix.cfg");
FixTransportSender sender = FixTransportFactory.createSender("fix.cfg");

// Register with transport manager
TransportManager transportManager = new TransportManager();
transportManager.registerListener("fix", listener);
transportManager.registerSender("fix", sender);

// Create and start the mediation engine
MediationEngine engine = new MediationEngine(transportManager, messageProcessor, qosManager);
engine.start();
```

## Dependencies

This module depends on QuickFIX/J for FIX protocol implementation:

```xml
<dependency>
    <groupId>org.quickfixj</groupId>
    <artifactId>quickfixj-core</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>org.quickfixj</groupId>
    <artifactId>quickfixj-messages-all</artifactId>
    <version>2.3.1</version>
</dependency>
``` 