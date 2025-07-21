package org.apache.synapse.custom.transports.mail;

import org.apache.synapse.custom.transports.TransportException;
import org.apache.synapse.custom.transports.TransportListener;
import org.apache.synapse.custom.transports.TransportSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating mail transport components.
 * Supports POP3, IMAP for receiving and SMTP for sending emails.
 */
public class MailTransportFactory {
    private static final Logger logger = LoggerFactory.getLogger(MailTransportFactory.class);

    /**
     * Create a mail transport listener for receiving emails
     * 
     * @param protocol The mail protocol ("pop3" or "imap")
     * @param host The mail server host
     * @param port The mail server port
     * @param username The username for authentication
     * @param password The password for authentication
     * @param pollingInterval The interval in milliseconds to check for new mail
     * @param ssl Whether to use SSL/TLS
     * @return The mail transport listener
     * @throws TransportException if the protocol is not supported
     */
    public static TransportListener createListener(String protocol, String host, int port,
                                                  String username, String password,
                                                  long pollingInterval, boolean ssl) throws TransportException {
        if ("pop3".equalsIgnoreCase(protocol)) {
            return new Pop3TransportListener(host, port, username, password, pollingInterval, ssl);
        } else if ("imap".equalsIgnoreCase(protocol)) {
            return new ImapTransportListener(host, port, username, password, pollingInterval, ssl);
        } else {
            throw new TransportException("Unsupported mail protocol for listener: " + protocol);
        }
    }

    /**
     * Create a mail transport sender for sending emails
     * 
     * @param host The SMTP server host
     * @param port The SMTP server port
     * @param username The username for authentication (optional)
     * @param password The password for authentication (optional)
     * @param ssl Whether to use SSL/TLS
     * @return The SMTP transport sender
     */
    public static TransportSender createSender(String host, int port,
                                              String username, String password,
                                              boolean ssl) {
        return new SmtpTransportSender(host, port, username, password, ssl);
    }

    /**
     * Create a mail transport sender with full configuration
     * 
     * @param host The SMTP server host
     * @param port The SMTP server port
     * @param username The username for authentication (optional)
     * @param password The password for authentication (optional)
     * @param ssl Whether to use SSL/TLS
     * @param connectionTimeout Connection timeout in milliseconds
     * @param socketTimeout Socket timeout in milliseconds
     * @param defaultSenderAddress The default sender email address
     * @param defaultSenderName The default sender name
     * @return The SMTP transport sender
     */
    public static TransportSender createSender(String host, int port,
                                              String username, String password,
                                              boolean ssl, int connectionTimeout, int socketTimeout,
                                              String defaultSenderAddress, String defaultSenderName) {
        return new SmtpTransportSender(host, port, username, password,
                                      ssl, connectionTimeout, socketTimeout,
                                      defaultSenderAddress, defaultSenderName);
    }
} 