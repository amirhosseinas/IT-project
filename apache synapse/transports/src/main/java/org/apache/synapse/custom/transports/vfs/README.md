# Apache Synapse VFS Transport

The Virtual File System (VFS) transport module for Apache Synapse provides support for file-based transports, including local file systems, FTP, SFTP, and other VFS-supported protocols.

## Features

1. **File System Operations**
   - Read, write, move, delete, copy files
   - Create directories
   - List directory contents
   - Check file existence

2. **Protocol Support**
   - Local file system (`file://`)
   - FTP (`ftp://`)
   - SFTP (`sftp://`)
   - Extensible for other protocols

3. **Protocol Abstraction Layer**
   - Common interface for all file system types
   - Protocol-specific configuration options
   - Easy to add new protocol handlers

4. **File Polling and Watching**
   - Monitor directories for new files
   - Watch mode for file system events (when supported)
   - Polling mode for periodic checking
   - Hybrid mode combining both approaches

5. **Integration with Message Processing**
   - Convert files to messages for processing
   - Generate response files from messages
   - File content type detection

6. **Error Handling**
   - Multiple error handling strategies
   - Move failed files to error directory
   - Delete failed files
   - Retry failed operations
   - Custom error handling

## Usage

### Setting up a VFS Transport Listener

```java
// Create parameters for file listener
Map<String, Object> parameters = new HashMap<>();
parameters.put("vfs.after.process", "move");
parameters.put("vfs.move.destination", "file:///tmp/processed");
parameters.put("vfs.after.failure", "move");
parameters.put("vfs.failure.destination", "file:///tmp/failed");

// Create file listener
TransportListener fileListener = VfsTransportFactory.createListener(
        "file:///tmp/inbox",             // Directory to monitor
        "*.xml",                         // File pattern
        true,                           // Recursive
        5000,                           // Polling interval (ms)
        VfsTransportListener.VfsListenerMode.HYBRID, // Listener mode
        5,                              // Worker threads
        parameters                      // Parameters
);

// Set message callback
fileListener.setMessageCallback(new TransportListener.MessageCallback() {
    @Override
    public Message onMessage(Message message) {
        // Process the message
        // ...
        return response;
    }
});

// Register with transport manager
TransportManager transportManager = new TransportManager();
transportManager.registerListener("file-listener", fileListener);
```

### Using VFS Transport Sender

```java
// Create VFS sender
TransportSender sender = VfsTransportFactory.createSender();
sender.init();

// Create a message
Message message = new Message(UUID.randomUUID().toString());
message.setDirection(Message.Direction.REQUEST);
message.setContentType("text/plain");
message.setPayload("Hello, VFS transport!".getBytes(StandardCharsets.UTF_8));

// Set operation to write
message.setProperty("vfs.operation", "write");

// Send the message to a file
Message response = sender.send(message, "file:///tmp/outbox/example.txt");
```

### File Operations

```java
// Read a file
Message readMessage = new Message(UUID.randomUUID().toString());
readMessage.setDirection(Message.Direction.REQUEST);
readMessage.setProperty("vfs.operation", "read");
Message readResponse = sender.send(readMessage, "file:///tmp/example.txt");

// Move a file
Message moveMessage = new Message(UUID.randomUUID().toString());
moveMessage.setDirection(Message.Direction.REQUEST);
moveMessage.setProperty("vfs.operation", "move");
moveMessage.setProperty("vfs.destination", "file:///tmp/moved.txt");
Message moveResponse = sender.send(moveMessage, "file:///tmp/source.txt");

// Delete a file
Message deleteMessage = new Message(UUID.randomUUID().toString());
deleteMessage.setDirection(Message.Direction.REQUEST);
deleteMessage.setProperty("vfs.operation", "delete");
Message deleteResponse = sender.send(deleteMessage, "file:///tmp/file-to-delete.txt");
```

## Configuration Options

### Listener Parameters

| Parameter | Description |
|-----------|-------------|
| `vfs.after.process` | Action to take after processing a file: `none`, `delete`, `move` |
| `vfs.move.destination` | Destination directory for processed files (when `vfs.after.process=move`) |
| `vfs.after.failure` | Action to take after a file processing failure: `none`, `delete`, `move` |
| `vfs.failure.destination` | Destination directory for failed files (when `vfs.after.failure=move`) |

### FTP-Specific Parameters

| Parameter | Description |
|-----------|-------------|
| `ftp.passive` | Enable passive mode (`true`/`false`) |
| `ftp.dataTimeout` | Data timeout in milliseconds |
| `ftp.socketTimeout` | Socket timeout in milliseconds |
| `ftp.connectionTimeout` | Connection timeout in milliseconds |
| `ftp.controlEncoding` | Control encoding (e.g., `UTF-8`) |
| `ftp.fileType` | File transfer type: `binary` or `ascii` |

### SFTP-Specific Parameters

| Parameter | Description |
|-----------|-------------|
| `sftp.strictHostKeyChecking` | Enable strict host key checking (`true`/`false`) |
| `sftp.privateKey` | Path to private key file for authentication |
| `sftp.passphrase` | Passphrase for private key |
| `sftp.knownHosts` | Path to known hosts file |
| `sftp.connectionTimeout` | Connection timeout in milliseconds |
| `sftp.sessionTimeout` | Session timeout in milliseconds |
| `sftp.compression` | Enable compression (`true`/`false`) |

## Error Handling

The VFS transport provides several error handling strategies:

1. **NONE**: Do nothing when an error occurs
2. **DELETE**: Delete the file that caused the error
3. **MOVE**: Move the file to an error directory
4. **RETRY**: Retry the operation with a configurable delay and max retries

Example error handler configuration:

```java
// Create error handler with move strategy
VfsErrorHandler errorHandler = VfsErrorHandler.createMoveHandler(
    "file:///tmp/errors",  // Error directory
    null,                  // Username (if needed)
    null,                  // Password (if needed)
    parameters             // Additional parameters
);

// Create error handler with retry strategy
VfsErrorHandler retryHandler = VfsErrorHandler.createRetryHandler(
    3,                     // Max retries
    5000,                  // Retry delay in milliseconds
    "file:///tmp/errors",  // Error directory after max retries
    null,                  // Username (if needed)
    null,                  // Password (if needed)
    parameters             // Additional parameters
);
```

## Dependencies

The VFS transport requires the following dependencies:

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-vfs2</artifactId>
    <version>2.9.0</version>
</dependency>
<dependency>
    <groupId>commons-net</groupId>
    <artifactId>commons-net</artifactId>
    <version>3.9.0</version>
</dependency>
<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
``` 