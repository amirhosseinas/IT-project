# Mail Transport for Apache Synapse

This module provides mail transport capabilities for Apache Synapse, enabling the ESB to send and receive emails using standard mail protocols.

## Features

1. **POP3/IMAP Support**: Receive emails via POP3 or IMAP protocols
2. **SMTP Support**: Send emails via SMTP protocol
3. **Email Content Parsing**: Extract text, HTML, and binary content from emails
4. **Attachment Handling**: Process and manage email attachments
5. **Mail Session Management**: Efficient reuse of mail sessions
6. **Integration with Message Processing**: Seamless conversion between mail messages and Synapse messages

## Components

- **MailTransportFactory**: Creates mail transport components (listeners and senders)
- **AbstractMailTransportListener**: Base class for mail transport listeners
- **Pop3TransportListener**: Implementation for receiving emails via POP3
- **ImapTransportListener**: Implementation for receiving emails via IMAP
- **SmtpTransportSender**: Implementation for sending emails via SMTP
- **MailContentExtractor**: Utility for extracting content from emails
- **MailSessionManager**: Manager for creating and caching mail sessions

## Usage Examples

### Receiving Emails via POP3

```java
// Create POP3 transport listener
TransportListener pop3Listener = MailTransportFactory.createListener(
    "pop3", "pop3.example.com", 110, "username", "password", 60000, false);

// Set message callback
pop3Listener.setMessageCallback(message -> {
    System.out.println("Received email with subject: " + message.getProperty("mail.subject"));
    System.out.println("From: " + message.getProperty("mail.from"));
    System.out.println("Content: " + message.getProperty("mail.text"));
    
    // Process the message here...
    return null; // No response needed for received emails
});

// Initialize and start the listener
pop3Listener.init();
pop3Listener.start();
```

### Receiving Emails via IMAP

```java
// Create IMAP transport listener with SSL
TransportListener imapListener = MailTransportFactory.createListener(
    "imap", "imap.example.com", 993, "username", "password", 60000, true);

// Set message callback
imapListener.setMessageCallback(message -> {
    System.out.println("Received email with subject: " + message.getProperty("mail.subject"));
    System.out.println("From: " + message.getProperty("mail.from"));
    
    // Check for HTML content
    String htmlContent = (String) message.getProperty("mail.html");
    if (htmlContent != null) {
        System.out.println("Email contains HTML content");
    } else {
        System.out.println("Content: " + message.getProperty("mail.text"));
    }
    
    // Process the message here...
    return null;
});

// Initialize and start the listener
imapListener.init();
imapListener.start();
```

### Sending Emails via SMTP

```java
// Create SMTP transport sender
TransportSender smtpSender = MailTransportFactory.createSender(
    "smtp.example.com", 25, "username", "password", false);

// Initialize the sender
smtpSender.init();

// Create a message to send
Message message = new Message(UUID.randomUUID().toString());
message.setProperty("mail.subject", "Test email from Apache Synapse");
message.setProperty("mail.from", "sender@example.com");
message.setProperty("mail.from.name", "Apache Synapse");

// Set the message content
String content = "This is a test email sent from Apache Synapse Mail Transport.";
message.setContentType("text/plain; charset=UTF-8");
message.setPayload(content.getBytes());

// Send the message
String recipientEmail = "recipient@example.com";
Message response = smtpSender.send(message, recipientEmail);
```

### Sending Emails with Attachments

```java
// Create a message with attachment
Message message = new Message(UUID.randomUUID().toString());
message.setProperty("mail.subject", "Test email with attachment");
message.setProperty("mail.from", "sender@example.com");
message.setContentType("text/plain; charset=UTF-8");
message.setPayload("Email body text".getBytes());

// Add attachment
Map<String, DataHandler> attachments = new HashMap<>();
attachments.put("report.pdf", new DataHandler(new FileDataSource("path/to/report.pdf")));
message.setProperty("mail.attachments", attachments);

// Send the message
smtpSender.send(message, "recipient@example.com");
```

## Configuration Options

### POP3/IMAP Options

- **host**: Mail server hostname
- **port**: Mail server port
- **username**: Authentication username
- **password**: Authentication password
- **pollingInterval**: Interval in milliseconds to check for new mail
- **ssl**: Whether to use SSL/TLS

### SMTP Options

- **host**: SMTP server hostname
- **port**: SMTP server port
- **username**: Authentication username
- **password**: Authentication password
- **ssl**: Whether to use SSL/TLS
- **connectionTimeout**: Connection timeout in milliseconds
- **socketTimeout**: Socket timeout in milliseconds
- **defaultSenderAddress**: Default sender email address
- **defaultSenderName**: Default sender name

## Dependencies

This module requires the following dependencies:

- JavaMail API (javax.mail:javax.mail-api)
- JavaMail Implementation (com.sun.mail:javax.mail)
- JavaBeans Activation Framework (javax.activation:activation)

## Integration with Message Builders

The mail transport integrates with Synapse message builders by:

1. Converting received emails to Synapse Message objects
2. Extracting content and metadata from emails into message properties
3. Providing access to attachments through the message property system
4. Supporting conversion of Synapse messages to outgoing emails 