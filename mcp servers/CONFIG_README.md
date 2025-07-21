# MCP Servers Configuration Guide

This document provides instructions on how to configure and run the Wikipedia and YouTube MCP servers.

## Environment Files

Both servers use environment files (.env) for configuration. There are three environment profiles:

- **Development**: Used during development (default)
- **Production**: Used in production environments
- **Test**: Used for testing

### Environment Variables

#### YouTube MCP Server

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| PORT | Server port | Yes | 3002 |
| NODE_ENV | Environment (development, production, test) | Yes | development |
| SERVER_URL | Public URL of the server | Yes | http://localhost:3002 |
| YOUTUBE_API_KEY | YouTube Data API key | Yes | - |
| WIKIPEDIA_MCP_SERVER_URL | URL of the Wikipedia MCP server | Yes | http://localhost:3001 |
| SHARED_SECRET | Secret for server-to-server authentication | Yes | default_secret_change_in_production |
| REQUEST_TIMEOUT | Request timeout in milliseconds | No | 30000 |
| MAX_RETRIES | Maximum number of retries for failed requests | No | 3 |
| RETRY_DELAY | Delay between retries in milliseconds | No | 1000 |
| MAX_REQUESTS_PER_SECOND | Rate limit for API requests | No | 5 |
| DEBUG | Debug namespaces | No | youtube-mcp-server:* |
| LOG_LEVEL | Log level (debug, info, warn, error) | No | debug |

#### Wikipedia MCP Server

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| PORT | Server port | Yes | 3001 |
| NODE_ENV | Environment (development, production, test) | Yes | development |
| SERVER_URL | Public URL of the server | Yes | http://localhost:3001 |
| YOUTUBE_MCP_SERVER_URL | URL of the YouTube MCP server | Yes | http://localhost:3002 |
| SHARED_SECRET | Secret for server-to-server authentication | Yes | default_secret_change_in_production |
| REQUEST_TIMEOUT | Request timeout in milliseconds | No | 30000 |
| MAX_RETRIES | Maximum number of retries for failed requests | No | 3 |
| RETRY_DELAY | Delay between retries in milliseconds | No | 1000 |
| MAX_REQUESTS_PER_SECOND | Rate limit for API requests | No | 5 |
| DEBUG | Debug namespaces | No | wikipedia-mcp-server:* |
| LOG_LEVEL | Log level (debug, info, warn, error) | No | debug |

## Setup Instructions

### 1. Configure Environment Files

1. Copy the appropriate environment file to `.env` in each server directory:

   ```bash
   # For development
   cp youtube-mcp-server/.env-files/development.env youtube-mcp-server/.env
   cp wikipedia-mcp-server/.env-files/development.env wikipedia-mcp-server/.env
   
   # For production
   cp youtube-mcp-server/.env-files/production.env youtube-mcp-server/.env
   cp wikipedia-mcp-server/.env-files/production.env wikipedia-mcp-server/.env
   
   # For testing
   cp youtube-mcp-server/.env-files/test.env youtube-mcp-server/.env
   cp wikipedia-mcp-server/.env-files/test.env wikipedia-mcp-server/.env
   ```

   Or use the npm scripts:

   ```bash
   # For development
   cd youtube-mcp-server && npm run prepare-env:dev
   cd wikipedia-mcp-server && npm run prepare-env:dev
   
   # For production
   cd youtube-mcp-server && npm run prepare-env:prod
   cd wikipedia-mcp-server && npm run prepare-env:prod
   
   # For testing
   cd youtube-mcp-server && npm run prepare-env:test
   cd wikipedia-mcp-server && npm run prepare-env:test
   ```

2. Edit the `.env` files to set your specific configuration values:
   - Set `YOUTUBE_API_KEY` in the YouTube MCP server's `.env` file
   - Set `SHARED_SECRET` to a secure random string in both servers' `.env` files

### 2. Running the Servers

#### Using npm scripts

```bash
# Start in development mode (with auto-reload)
cd youtube-mcp-server && npm run dev
cd wikipedia-mcp-server && npm run dev

# Start in production mode
cd youtube-mcp-server && npm start
cd wikipedia-mcp-server && npm start

# Run tests
cd youtube-mcp-server && npm test
cd wikipedia-mcp-server && npm test
```

#### Using Docker

1. Set up the `.env.docker` file in the root directory with your configuration:

   ```
   YOUTUBE_API_KEY=your_youtube_api_key_here
   SHARED_SECRET=your_secure_shared_secret_here
   ```

2. Run both servers using Docker Compose:

   ```bash
   docker-compose --env-file .env.docker up -d
   ```

3. To stop the servers:

   ```bash
   docker-compose down
   ```

## Validation

Both servers perform validation checks on startup:

1. **YouTube MCP Server**:
   - Checks for required environment variables
   - Validates the YouTube API key
   - Checks if the Wikipedia MCP server is reachable
   - Verifies required dependencies are installed

2. **Wikipedia MCP Server**:
   - Checks for required environment variables
   - Checks if the YouTube MCP server is reachable
   - Tests Wikipedia API access
   - Verifies required dependencies are installed

If validation fails, the servers will still start but with warnings and possibly limited functionality.

## Health Checks

Both servers provide health check endpoints:

- YouTube MCP Server: `http://localhost:3002/api/health`
- Wikipedia MCP Server: `http://localhost:3001/api/health`

These endpoints return a JSON response with the server status and can be used for monitoring and Docker health checks. 