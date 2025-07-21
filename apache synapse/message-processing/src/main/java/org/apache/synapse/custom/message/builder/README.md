# Apache Synapse Message Builders

This package contains message builder implementations for Apache Synapse. Message builders are responsible for constructing Message objects from different content types and formats.

## Available Message Builders

### 1. JSON Message Builder
- Class: `JsonMessageBuilder`
- Content Types: `application/json`, `text/json`, `application/javascript`, `text/javascript`
- Dependencies: Jackson (`jackson-core`, `jackson-databind`)
- Features:
  - Parses and validates JSON content
  - Detects root type (object or array)
  - Adds metadata properties to the message

### 2. XML/SOAP Message Builder
- Class: `XmlMessageBuilder`
- Content Types: `application/xml`, `text/xml`, `application/soap+xml`, `application/xhtml+xml`
- Dependencies: AXIOM (`axiom-api`, `axiom-impl`, `axiom-dom`)
- Features:
  - Handles both plain XML and SOAP messages
  - Extracts XML root element and namespace information
  - For SOAP messages, extracts SOAP version, header, and body information

### 3. Plain Text Message Builder
- Class: `PlainTextMessageBuilder`
- Content Types: `text/plain`, `text/csv`, `text/html`, `text/css`
- Features:
  - Counts lines and total length
  - Special handling for CSV (counts columns in first row)
  - Special handling for HTML

### 4. Binary Message Builder
- Class: `BinaryMessageBuilder`
- Content Types: `application/octet-stream`, `application/pdf`, `image/*`, `audio/*`, `video/*`, etc.
- Features:
  - Handles binary data and file attachments
  - Categorizes content (image, audio, video, archive, document)
  - Determines file extension based on content type

### 5. Hessian Message Builder
- Class: `HessianMessageBuilder`
- Content Types: `application/x-hessian`, `application/hessian`
- Dependencies: Hessian (`hessian`)
- Features:
  - Deserializes Java objects using Hessian protocol
  - Extracts object class name and hash code
  - Stores deserialized object for direct access

## Usage

### Using the Factory

The `MessageBuilderFactory` provides a centralized way to get the appropriate builder for a given content type:

```java
// Get a builder for a specific content type
MessageBuilder builder = MessageBuilderFactory.getInstance().getBuilder("application/json");
Message message = builder.buildMessage(inputStream, "application/json");
```

### Using the Utility Class

The `MessageBuilderUtil` class provides convenience methods for common use cases:

```java
// Build a message from a string
Message jsonMessage = MessageBuilderUtil.buildJsonMessage("{\"name\":\"John\"}");
Message xmlMessage = MessageBuilderUtil.buildXmlMessage("<person><name>John</name></person>");
Message textMessage = MessageBuilderUtil.buildTextMessage("Hello, world!");

// Build a message from serialized content
Message hessianMessage = MessageBuilderUtil.buildHessianMessage(serializedBytes);

// Build a message from an input stream
Message message = MessageBuilderUtil.buildMessage(inputStream, contentType);
```

## Extending

To create a custom message builder:

1. Implement the `MessageBuilder` interface
2. Register your builder with the factory:

```java
MessageBuilderFactory.getInstance().registerBuilder(new MyCustomBuilder());
```

You can also set a default builder to handle unknown content types:

```java
MessageBuilderFactory.getInstance().setDefaultBuilder(new MyDefaultBuilder());
``` 