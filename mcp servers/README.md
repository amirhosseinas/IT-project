# MCP Servers

This repository contains Model Context Protocol (MCP) servers for various services.

## YouTube MCP Server

The YouTube MCP server allows AI agents to search and retrieve information from YouTube.

### Features

- Search YouTube videos using the YouTube Data API v3
- Filter and rank results by relevance, view count, and recency
- Integration with Wikipedia for related topics

### Testing

The YouTube MCP server includes tests that don't require Node.js:

1. Check if the server is running:
   ```
   cd youtube-mcp-server
   .\check-server.bat
   ```

2. Run the YouTube agent test:
   ```
   cd youtube-mcp-server
   .\run-youtube-test.bat
   ```

For detailed instructions, see [youtube-mcp-server/TESTING_INSTRUCTIONS.md](youtube-mcp-server/TESTING_INSTRUCTIONS.md).

## Wikipedia MCP Server

The Wikipedia MCP server allows AI agents to search and retrieve information from Wikipedia.

### Features

- Search Wikipedia articles
- Get article summaries and content
- Extract categories and related topics

## Inter-Service Communication

The MCP servers can communicate with each other to provide enhanced functionality:

- YouTube agent can request related Wikipedia topics
- Wikipedia agent can request relevant YouTube videos

For more information on the communication protocol, see [COMMUNICATION_PROTOCOL.md](COMMUNICATION_PROTOCOL.md). 