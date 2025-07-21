# Wikipedia MCP Server

This server implements the Model Context Protocol (MCP) for Wikipedia integration, allowing AI agents to search and retrieve information from Wikipedia.

## Features

- Search Wikipedia articles using the MediaWiki API
- Retrieve article content, summaries, and metadata
- Filter and rank results by relevance and page views
- No API credentials needed - Wikipedia API is free and open

## Tools

1. **search_wikipedia_topics**: Searches Wikipedia for relevant articles and returns article links, summaries, and metadata

## Setup

1. Clone the repository
2. Install dependencies:
   ```
   npm install
   ```
3. Configure environment variables:
   - Copy `.env.example` to `.env` (if not already done)
   - Update any necessary configuration

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

Run the test script to verify the server functionality:
```
node wikipedia-service-test.js
```

You can also use the Wikipedia Search Console for direct testing:
```
wikipedia-search-console.bat
```

## Project Structure

```
wikipedia-mcp-server/
├── src/
│   ├── index.js                  # Main entry point
│   ├── tools/
│   │   └── wikipediaTools.js     # Tool implementations
│   ├── agents/
│   │   └── wikipediaAgent.js     # Agent logic
│   └── communication/
│       └── index.js              # Inter-server communication
├── .env                          # Environment variables
├── .env.example                  # Example environment variables
├── wikipedia-service-test.js     # Test script
├── wikipedia-search-console.bat  # Command-line search interface
└── package.json                  # Dependencies and scripts
```

## API

The server exposes MCP-compatible endpoints for AI agents to interact with Wikipedia:

- `GET /api/health` - Health check endpoint
- `POST /api/tool/search_wikipedia_topics` - Search Wikipedia articles
- `POST /api/agent/wikipedia_agent` - Use the Wikipedia agent for combined functionality

## Dependencies

- @modelcontextprotocol/sdk: MCP implementation
- axios: HTTP client for API requests
- dotenv: Environment configuration 