package org.apache.synapse.custom.transports.jms;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.Properties;

/**
 * Factory for creating and managing JMS connections.
 * Provides connection pooling to efficiently reuse JMS connections.
 */
public class JmsConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(JmsConnectionFactory.class);

    private final ConnectionFactory connectionFactory;
    private final GenericObjectPool<Connection> connectionPool;
    private final String username;
    private final String password;
    private final Properties properties;

    /**
     * Create a new JMS connection factory
     *
     * @param connectionFactory The underlying JMS connection factory
     * @param properties Configuration properties
     */
    public JmsConnectionFactory(ConnectionFactory connectionFactory, Properties properties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        this.username = properties.getProperty("jms.username");
        this.password = properties.getProperty("jms.password");

        // Configure connection pool
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(Integer.parseInt(properties.getProperty("jms.pool.size", "10")));
        poolConfig.setMaxIdle(Integer.parseInt(properties.getProperty("jms.pool.maxIdle", "5")));
        poolConfig.setMinIdle(Integer.parseInt(properties.getProperty("jms.pool.minIdle", "1")));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        // Create connection pool
        this.connectionPool = new GenericObjectPool<>(new ConnectionFactory(), poolConfig);
        logger.info("JMS connection factory initialized with pool size: {}", poolConfig.getMaxTotal());
    }

    /**
     * Get a JMS connection from the pool
     *
     * @return A JMS connection
     * @throws JMSException if getting a connection fails
     */
    public Connection getConnection() throws JMSException {
        try {
            return connectionPool.borrowObject();
        } catch (Exception e) {
            logger.error("Failed to get JMS connection from pool", e);
            if (e.getCause() instanceof JMSException) {
                throw (JMSException) e.getCause();
            }
            throw new JMSException("Failed to get JMS connection: " + e.getMessage());
        }
    }

    /**
     * Return a connection to the pool
     *
     * @param connection The connection to return
     */
    public void returnConnection(Connection connection) {
        if (connection != null) {
            connectionPool.returnObject(connection);
        }
    }

    /**
     * Create a JMS session
     *
     * @param connection The JMS connection
     * @param transacted Whether the session is transacted
     * @param acknowledgeMode The acknowledge mode
     * @return A new JMS session
     * @throws JMSException if creating the session fails
     */
    public Session createSession(Connection connection, boolean transacted, int acknowledgeMode) 
            throws JMSException {
        return connection.createSession(transacted, acknowledgeMode);
    }

    /**
     * Close the connection factory and release resources
     */
    public void close() {
        connectionPool.close();
        logger.info("JMS connection factory closed");
    }

    /**
     * Pooled object factory for JMS connections
     */
    private class ConnectionFactory extends BasePooledObjectFactory<Connection> {
        @Override
        public Connection create() throws Exception {
            logger.debug("Creating new JMS connection");
            if (username != null && password != null) {
                return connectionFactory.createConnection(username, password);
            } else {
                return connectionFactory.createConnection();
            }
        }

        @Override
        public PooledObject<Connection> wrap(Connection connection) {
            return new DefaultPooledObject<>(connection);
        }

        @Override
        public void destroyObject(PooledObject<Connection> pooledObject) throws Exception {
            Connection connection = pooledObject.getObject();
            if (connection != null) {
                try {
                    connection.close();
                    logger.debug("Closed JMS connection");
                } catch (JMSException e) {
                    logger.warn("Error closing JMS connection", e);
                }
            }
        }

        @Override
        public boolean validateObject(PooledObject<Connection> pooledObject) {
            try {
                pooledObject.getObject().getMetaData();
                return true;
            } catch (Exception e) {
                logger.warn("JMS connection validation failed", e);
                return false;
            }
        }
    }
} 