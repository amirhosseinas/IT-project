# Testing Guide

This document provides information about the available tests for the MCP servers.

## Available Tests

### Wikipedia Tests

- **Wikipedia Search Console** (`wikipedia-mcp-server/wikipedia-search-console.bat`)
  - A command-line interface for searching Wikipedia articles directly
  - Simulates the Wikipedia search functionality without requiring a server connection
  - Usage: Run the batch file and follow the prompts to enter search queries

### YouTube Tests

- **YouTube Video Console** (`youtube-mcp-server/youtube-video-console.bat`)
  - A command-line interface for searching YouTube videos
  - Requires PowerShell to be installed
  - Usage: Run the batch file and follow the prompts to enter search queries

### Inter-Service Communication

- **Inter-Service Communication Simulator** (`inter-service-communication-simulator.bat`)
  - Demonstrates how the Wikipedia and YouTube servers communicate with each other
  - Simulates the authentication and data exchange between servers
  - Provides three simulation modes:
    1. Wikipedia to YouTube communication
    2. YouTube to Wikipedia communication
    3. Client to both servers
  - Usage: Run the batch file and select the desired simulation mode

## Integration Tests

- **Service Error Handling** (`tests/integration/service-error-handling.test.js`)
  - Tests error handling scenarios for both services
  - Validates proper error responses and recovery mechanisms

## Running Tests

### Service Tests

Each server has its own test script that verifies the functionality of the server:

- Wikipedia Service Test: `wikipedia-mcp-server/wikipedia-service-test.js`
  ```
  cd wikipedia-mcp-server
  node wikipedia-service-test.js
  ```

- YouTube Service Test: `youtube-mcp-server/youtube-service-test.js`
  ```
  cd youtube-mcp-server
  node youtube-service-test.js
  ```

### Jest Tests

The project includes Jest tests for unit and integration testing:

```
cd tests
npm test
```

## Test Configuration

Test configuration can be adjusted in the following files:

- `tests/jest.config.js`: Jest configuration
- Environment variables in `.env` files 