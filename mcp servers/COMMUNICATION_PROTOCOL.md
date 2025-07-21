# MCP Servers Communication Protocol

This document describes the communication protocol between the Wikipedia MCP server and the YouTube MCP server.

## Overview

The communication protocol enables bidirectional communication between the two MCP servers:
- Wikipedia MCP server (port 3001)
- YouTube MCP server (port 3002)

Each server exposes HTTP endpoints that can be called by the other server to retrieve information.

## Authentication

All server-to-server communication is authenticated using a shared secret and HMAC-based token system.

### Token Generation

1. Generate a timestamp (Unix epoch seconds)
2. Create a signature by hashing the timestamp with HMAC-SHA256 using the shared secret
3. Combine as `timestamp.signature`

```javascript
function generateAuthToken() {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signature = crypto
    .createHmac('sha256', SHARED_SECRET)
    .update(timestamp)
    .digest('hex');
  return `${timestamp}.${signature}`;
}
```

### Authentication Process

1. The client adds an `Authorization: Bearer <token>` header to each request
2. The server verifies:
   - The timestamp is within 5 minutes (to prevent replay attacks)
   - The signature matches the expected signature for the timestamp

## Endpoints

### Wikipedia MCP Server (localhost:3001)

#### `GET /api/health`
- **Description**: Health check endpoint
- **Authentication**: None
- **Response**: `{ status: "ok", service: "wikipedia-mcp-server", version: "1.0.0", timestamp: "..." }`

#### `POST /api/search-topics`
- **Description**: Search Wikipedia for topics
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "query": "search query",
    "limit": 5,
    "language": "en"
  }
  ```
- **Response**: 
  ```json
  {
    "success": true,
    "message": "Found X Wikipedia topics for query: 'search query'",
    "requestId": "uuid",
    "timestamp": "ISO date string",
    "data": {
      "results": [
        {
          "title": "Article title",
          "url": "Article URL",
          "summary": "Article summary",
          "categories": ["Category 1", "Category 2"],
          "relevanceScore": 95.5
        }
      ],
      "pagination": {
        "offset": 0,
        "limit": 5,
        "total": 100,
        "hasMore": true
      }
    }
  }
  ```

### YouTube MCP Server (localhost:3002)

#### `GET /api/health`
- **Description**: Health check endpoint
- **Authentication**: None
- **Response**: `{ status: "ok", service: "youtube-mcp-server", version: "1.0.0", timestamp: "..." }`

#### `GET /api/youtube-status`
- **Description**: Check YouTube API status
- **Authentication**: None
- **Response**: `{ status: "ok", message: "YouTube API is working correctly", quotaUsed: true, timestamp: "..." }`

#### `POST /api/search-videos`
- **Description**: Search YouTube for videos
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "query": "search query",
    "maxResults": 5,
    "sortBy": "relevance",
    "publishedAfter": "2022-01-01T00:00:00Z",
    "videoDuration": "any"
  }
  ```
- **Response**: 
  ```json
  {
    "success": true,
    "message": "Found X YouTube videos for query: 'search query'",
    "requestId": "uuid",
    "timestamp": "ISO date string",
    "data": {
      "results": [
        {
          "id": "video ID",
          "title": "Video title",
          "description": "Video description",
          "url": "Video URL",
          "thumbnails": { ... },
          "statistics": {
            "viewCount": "1000000",
            "formattedViewCount": "1M"
          }
        }
      ],
      "sortBy": "relevance",
      "filters": {
        "publishedAfter": "2022-01-01T00:00:00Z",
        "videoDuration": "any"
      }
    }
  }
  ```

## Error Handling

### Error Response Format

```json
{
  "success": false,
  "message": "Error message",
  "error": "ERROR_CODE",
  "timestamp": "ISO date string"
}
```

### Common Error Codes

- `UNAUTHORIZED`: Missing or invalid authentication
- `TOKEN_EXPIRED`: Authentication token has expired
- `INVALID_SIGNATURE`: Invalid token signature
- `INVALID_REQUEST`: Invalid request parameters
- `SERVER_ERROR`: Internal server error

## Rate Limiting

The communication protocol includes rate limiting to prevent overloading either server:

- Maximum requests per second: 5 (configurable)
- Retry mechanism with exponential backoff
- Response header: `Retry-After` indicates seconds to wait before retrying

## Configuration

Configuration for both servers is managed through environment variables:

```
# Server configuration
PORT=3001 or 3002

# Inter-server communication
WIKIPEDIA_MCP_SERVER_URL=http://localhost:3001
YOUTUBE_MCP_SERVER_URL=http://localhost:3002
SHARED_SECRET=your_shared_secret_here

# Request settings
REQUEST_TIMEOUT=30000
MAX_RETRIES=3
RETRY_DELAY=1000
MAX_REQUESTS_PER_SECOND=5
```

## Testing

A test script (`test-communication.js`) is provided to verify the communication protocol between the two servers:

```
node test-communication.js
``` 

## Running Tests in Windows

1. **Using PowerShell or Command Prompt**:
   ```
   cd "path\to\mcp servers"
   
   # Run the basic communication test
   node test-communication.js
   
   # For Wikipedia server tests
   cd wikipedia-mcp-server
   npm test
   npm run test:integration
   
   # For YouTube server tests
   cd ..\youtube-mcp-server
   npm test
   npm run test:integration
   ```

2. **For environment variables in Windows**:
   ```
   # PowerShell
   $env:NODE_ENV="test"
   $env:SHARED_SECRET="your_secret"
   
   # Command Prompt
   set NODE_ENV=test
   set SHARED_SECRET=your_secret
   ```

## Running Tests in Linux

1. **Using Terminal**:
   ```
   cd path/to/mcp\ servers
   
   # Run the basic communication test
   node test-communication.js
   
   # For Wikipedia server tests
   cd wikipedia-mcp-server
   npm test
   npm run test:integration
   
   # For YouTube server tests
   cd ../youtube-mcp-server
   npm test
   npm run test:integration
   ```

2. **For environment variables in Linux**:
   ```
   export NODE_ENV=test
   export SHARED_SECRET=your_secret
   ```

## Using Docker (both platforms)

You can also run tests using Docker, which ensures consistent behavior across platforms:

```
# Start the servers
docker-compose up -d

# Run tests against the running containers
docker exec -it wikipedia-mcp-server npm test
docker exec -it youtube-mcp-server npm test
```

Before running tests, make sure all dependencies are installed by running `npm install` in each directory (root, wikipedia-mcp-server, youtube-mcp-server). 