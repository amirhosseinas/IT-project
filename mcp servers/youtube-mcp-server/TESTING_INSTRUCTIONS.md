# YouTube MCP Server Testing Instructions

This document provides instructions for testing the YouTube MCP server using the agent test.

## Prerequisites

1. Ensure the YouTube MCP server is running
2. Verify that your YouTube API key is set in the `.env` file

## Running the Test

### Using PowerShell (No Node.js Required)

1. Navigate to the `youtube-mcp-server` directory
2. Run the batch file:
   ```
   run-youtube-test.bat
   ```

   Or run the PowerShell script directly:
   ```
   powershell -ExecutionPolicy Bypass -File youtube-agent-test.ps1
   ```

### Using Node.js

If you have Node.js installed:

1. Navigate to the `youtube-mcp-server` directory
2. Run the batch file:
   ```
   run-youtube-agent-test.bat
   ```

3. Or run the test script with Node.js directly:
   ```
   node youtube-agent-test.js
   ```

4. Or use the npm script:
   ```
   npm run test:agent
   ```

## Test Configuration

You can modify the test configuration by:

### For PowerShell Version

1. Editing the `run-youtube-test.bat` file to change environment variables:
   ```
   set YOUTUBE_MCP_SERVER_URL=http://localhost:3001
   set SEARCH_QUERY=machine learning tutorial
   ```

2. Or editing the `youtube-agent-test.ps1` file directly:
   ```powershell
   $YOUTUBE_MCP_SERVER_URL = "http://localhost:3001"
   $SEARCH_QUERY = "artificial intelligence"
   ```

### For Node.js Version

1. Editing the `run-youtube-agent-test.bat` file to change environment variables:
   ```
   set YOUTUBE_MCP_SERVER_URL=http://localhost:3001
   set SEARCH_QUERY=machine learning tutorial
   ```

2. Setting environment variables before running the test:
   ```
   $env:YOUTUBE_MCP_SERVER_URL="http://localhost:3001"
   $env:SEARCH_QUERY="machine learning tutorial"
   node youtube-agent-test.js
   ```

## What the Test Does

The YouTube agent test performs the following steps:

1. Checks if the YouTube MCP server is running by calling the health endpoint
2. Verifies the YouTube API connectivity
3. Calls the YouTube agent with a search query
4. Processes and displays the agent's response, including:
   - Top video details
   - Channel information
   - Related Wikipedia topics (if available)

## Troubleshooting

If the test fails, check the following:

1. Ensure the YouTube MCP server is running on the correct port
2. Verify that your YouTube API key is valid and has sufficient quota
3. Check if the Wikipedia MCP server is running (for related topics functionality)
4. Look for error messages in the test output for specific issues

### PowerShell Execution Policy

If you encounter issues running the PowerShell script, you may need to adjust the execution policy:

1. Run PowerShell as Administrator
2. Execute the following command:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
   ```
3. Then run the script:
   ```powershell
   .\youtube-agent-test.ps1
   ``` 