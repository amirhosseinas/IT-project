# Apache Synapse ESB Implementation

A custom implementation of Apache Synapse ESB with a clean modular architecture, built with Java 11+ and Maven 3.8+.

## Project Structure

The project is organized into the following modules:

- **transports**: Handles transport-level communication (HTTP, JMS, etc.)
- **message-processing**: Provides message handling, transformation, and validation capabilities
- **qos**: Implements Quality of Service features like throttling, circuit breaking, and rate limiting
- **mediation-engine**: The core ESB engine that coordinates the flow of messages through the system

## Requirements

- Java 11+
- Maven 3.8+

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Running the Server

After building the project, you can run the server using:

```bash
# Create a configuration directory if it doesn't exist
mkdir -p conf

# Copy sample configuration files
cp mediation-engine/src/main/resources/sample-configs/synapse.xml conf/
cp mediation-engine/src/main/resources/sample-configs/synapse-env.properties conf/synapse.properties

# Run the server
java -cp "mediation-engine/target/classes:mediation-engine/target/dependency/*:message-processing/target/classes:qos/target/classes:transports/target/classes" org.apache.synapse.custom.mediation.server.SynapseBootstrap -conf conf
```

For Windows:

```batch
mkdir conf

copy mediation-engine\src\main\resources\sample-configs\synapse.xml conf\
copy mediation-engine\src\main\resources\sample-configs\synapse-env.properties conf\synapse.properties

java -cp "mediation-engine/target/classes;mediation-engine/target/dependency/*;message-processing/target/classes;qos/target/classes;transports/target/classes" org.apache.synapse.custom.mediation.server.SynapseBootstrap -conf conf
```

## Testing the Server

Once the server is running, you can test it by sending HTTP requests to the default endpoint:

```bash
# Using curl
curl -X POST -H "Content-Type: text/xml" -d "<request><echo>Hello World</echo></request>" http://localhost:8280/services/echo

# Using a web browser
# Navigate to http://localhost:8280/services/echo
```

## Module Details

### Transports

The transport module handles the low-level communication with external systems. It provides:

- Transport listeners for incoming messages
- Transport senders for outgoing messages
- Support for various transport protocols

### Message Processing

The message processing module handles message transformation and manipulation. It includes:

- Message parsing and validation
- Content transformation
- Protocol adaptation
- Message enrichment

### QoS (Quality of Service)

The QoS module implements essential service quality capabilities:

- Rate limiting and throttling
- Circuit breaker pattern implementation
- Service level enforcement
- Request prioritization

### Mediation Engine

The mediation engine is the core of the ESB, coordinating the message flow:

- Mediator framework for message processing
- Sequence management for defining message flows
- Registry for sequences and endpoints
- Routing and dispatching capabilities

## Design Principles

This implementation follows these key design principles:

1. **Modularity**: Each module has a clear responsibility with well-defined interfaces
2. **Simplicity**: Clean architecture without monitoring or JMX dependencies
3. **Extensibility**: Easy to extend with new transport protocols or mediators
4. **Performance**: Designed for high throughput and low latency
5. **Reliability**: Implements patterns to ensure system resilience

## Getting Started

To use this ESB implementation in your project:

1. Build the entire project using Maven
2. Configure your mediation sequences and endpoints
3. Implement any custom mediators you need
4. Start the mediation engine with your configuration

## Troubleshooting

If you encounter any issues:

1. Ensure Java 11+ and Maven 3.8+ are installed and properly configured
2. Check that the configuration files in the conf directory are valid
3. Verify that the required ports (default: 8280, 8443) are available
4. Check the console output for any error messages

## License

This project is licensed under the Apache License 2.0. 