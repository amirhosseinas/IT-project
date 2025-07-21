# Apache Synapse Message Processing

This module contains components for processing messages in Apache Synapse ESB.

## Components

### Message Builders

The message builders are responsible for constructing Message objects from different content types and formats. The following builders are available:

1. **JSON Message Builder** - Uses Jackson for JSON processing
2. **XML/SOAP Message Builder** - Uses AXIOM for XML and SOAP processing
3. **Plain Text Message Builder** - For text content including CSV, HTML, etc.
4. **Binary Message Builder** - For handling binary data and file attachments
5. **Hessian Message Builder** - For Java serialized objects using Hessian protocol

For more details, see the [Message Builders README](src/main/java/org/apache/synapse/custom/message/builder/README.md).

### Message Handlers

Message handlers process messages after they are built. They can transform, validate, or perform other operations on messages.

### Message Processor

The central component that manages the flow of messages through registered handlers.

## Usage

### Building Messages

```java
// Using the utility class
import org.apache.synapse.custom.message.builder.MessageBuilderUtil;
import org.apache.synapse.custom.message.Message;

// Build from strings
Message jsonMessage = MessageBuilderUtil.buildJsonMessage("{\"name\":\"John\"}");
Message xmlMessage = MessageBuilderUtil.buildXmlMessage("<person><name>John</name></person>");
Message textMessage = MessageBuilderUtil.buildTextMessage("Hello, world!");

// Build from input streams
Message message = MessageBuilderUtil.buildMessage(inputStream, contentType);
```

### Processing Messages

```java
import org.apache.synapse.custom.message.MessageProcessor;
import org.apache.synapse.custom.message.MessageHandler;
import org.apache.synapse.custom.message.Message;

// Create a processor
MessageProcessor processor = new MessageProcessor();

// Register handlers
processor.registerHandler(new MyCustomHandler());

// Process a message
Message processedMessage = processor.process(message);
```

## Dependencies

- Jackson for JSON processing
- AXIOM for XML processing
- SLF4J for logging
- Apache Commons IO and Lang
- Hessian for Java object serialization 