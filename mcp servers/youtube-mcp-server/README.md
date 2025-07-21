# YouTube MCP Server

This server implements the Model Context Protocol (MCP) for YouTube integration, allowing AI agents to search and retrieve information from YouTube.

## Features

- Search YouTube videos using the YouTube Data API v3
- Filter and rank results by relevance, view count, and recency
- Bidirectional communication with authentication and rate limiting

## Tools

1. **search_youtube_videos**: Searches YouTube for relevant videos and returns video links, metadata, and statistics
2. **request_wikipedia_topics**: Requests related Wikipedia topics from the Wikipedia MCP server

## Setup

1. Clone the repository
2. Install dependencies:
   ```
   npm install
   ```
3. Configure environment variables:
   - Copy `.env.example` to `.env` (if not already done)
   - Add your YouTube API key to the `.env` file
   - Update any other necessary configuration

## YouTube API Key

You need to obtain a YouTube API key from the Google Cloud Console:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable the YouTube Data API v3
4. Create an API key
5. Add the API key to your `.env` file:
   ```
   YOUTUBE_API_KEY=your_api_key_here
   ```

## Environment Variables

```
# Server configuration
PORT=3001

# YouTube API credentials
YOUTUBE_API_KEY=your_youtube_api_key_here
```

## Running the Server

Development mode:
```
npm run dev
```

Production mode:
```
npm start
```

## Testing

### PowerShell Test (No Node.js Required)
Run the PowerShell-based test to verify server functionality:
```
run-youtube-test.bat
```

Or directly using PowerShell:
```
powershell -ExecutionPolicy Bypass -File youtube-agent-test.ps1
```

### Node.js Test
If you have Node.js installed, you can use:
```
run-youtube-agent-test.bat
```

Or directly using Node.js:
```
node youtube-agent-test.js
```

For detailed testing instructions, see [TESTING_INSTRUCTIONS.md](TESTING_INSTRUCTIONS.md).

## Project Structure

```
youtube-mcp-server/
├── src/
│   ├── index.js                    # Main entry point
│   ├── tools/
│   │   ├── index.js                # Tools export
│   │   └── youtube-search.js       # YouTube search tool
│   ├── agents/
│   │   ├── index.js                # Agents export
│   │   └── youtubeAgent.js         # YouTube agent implementation
│   ├── communication/
│   │   └── index.js                # Inter-server communication
│   └── utils/
│       └── validation.js           # Validation utilities
├── youtube-agent-test.js           # Node.js agent test script
├── youtube-agent-test.ps1          # PowerShell agent test script
├── run-youtube-agent-test.bat      # Node.js test runner
├── run-youtube-test.bat            # PowerShell test runner
├── youtube-video-console.bat       # Command-line video interface
├── youtube-agent-console.bat       # Command-line agent interface
└── package.json                    # Dependencies and scripts
```

## API

The server exposes MCP-compatible endpoints for AI agents to interact with YouTube:

- `GET /api/health` - Health check endpoint
- `GET /api/youtube-status` - YouTube API status endpoint
- `POST /api/tool/search_youtube_videos` - Search YouTube videos
- `POST /api/tool/request_wikipedia_topics` - Request Wikipedia topics
- `POST /api/agent/youtube_agent` - Use the YouTube agent for combined functionality

## Dependencies

- @modelcontextprotocol/sdk: MCP implementation
- googleapis: YouTube API client
- axios: HTTP client for API requests
- axios-rate-limit: Rate limiting for API requests
- dotenv: Environment configuration 