const express = require('express');
const { google } = require('googleapis');
const axios = require('axios');
const cors = require('cors');
const crypto = require('crypto');
require('dotenv').config();

// Import validation utilities
const { validateServerConfig } = require('./utils/validation');

// Import tools
const { searchYouTubeVideos, requestWikipediaTopics } = require('./tools');

// Import agents
const { youtubeAgent } = require('./agents/youtubeAgent');

// Import communication modules
const { WikipediaCommunication } = require('./communication');

// Configuration
const PORT = process.env.PORT || 3001;
const SHARED_SECRET = process.env.SHARED_SECRET || 'default_secret_change_in_production';
const REQUEST_TIMEOUT = parseInt(process.env.REQUEST_TIMEOUT) || 30000; // 30 seconds
const MAX_RETRIES = parseInt(process.env.MAX_RETRIES) || 3;

// Initialize YouTube API client
const youtube = google.youtube({
  version: 'v3',
  auth: process.env.YOUTUBE_API_KEY
});

// Initialize Express server
const app = express();

// Add middleware for CORS and JSON parsing
app.use(cors());
app.use(express.json());
app.use((req, res, next) => {
  // Log incoming requests
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// Authentication middleware for server-to-server communication
const authenticateServer = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      message: 'Authentication required',
      error: 'UNAUTHORIZED'
    });
  }
  
  const token = authHeader.split(' ')[1];
  
  try {
    // Verify timestamp to prevent replay attacks
    const [timestamp, signature] = token.split('.');
    const timestampNum = parseInt(timestamp);
    
    // Check if timestamp is within 5 minutes
    const now = Math.floor(Date.now() / 1000);
    if (now - timestampNum > 300) { // 5 minutes
      return res.status(401).json({
        success: false,
        message: 'Token expired',
        error: 'TOKEN_EXPIRED'
      });
    }
    
    // Verify signature
    const expectedSignature = crypto
      .createHmac('sha256', SHARED_SECRET)
      .update(timestamp)
      .digest('hex');
    
    if (signature !== expectedSignature) {
      return res.status(401).json({
        success: false,
        message: 'Invalid authentication',
        error: 'INVALID_SIGNATURE'
      });
    }
    
    next();
  } catch (error) {
    console.error('Authentication error:', error);
    return res.status(401).json({
      success: false,
      message: 'Authentication failed',
      error: 'AUTH_ERROR'
    });
  }
};

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({ 
    status: 'ok', 
    service: 'youtube-mcp-server',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

// YouTube API status endpoint
app.get('/api/youtube-status', async (req, res) => {
  try {
    // Test the YouTube API with a simple request
    const response = await youtube.search.list({
      part: 'snippet',
      q: 'test',
      maxResults: 1,
      type: 'video'
    });
    
    res.status(200).json({
      status: 'ok',
      message: 'YouTube API is working correctly',
      quotaUsed: response.data.pageInfo ? true : false,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      status: 'error',
      message: 'YouTube API error',
      error: error.message,
      details: error.response?.data?.error?.errors || [],
      timestamp: new Date().toISOString()
    });
  }
});

// Search videos endpoint for server-to-server communication
app.post('/api/search-videos', authenticateServer, async (req, res) => {
  try {
    const { query, maxResults = 5, sortBy = 'relevance', publishedAfter, videoDuration = 'any' } = req.body;
    
    // Validate request
    if (!query) {
      return res.status(400).json({
        success: false,
        message: 'Query parameter is required',
        error: 'INVALID_REQUEST',
        timestamp: new Date().toISOString()
      });
    }
    
    // Log the request
    console.log(`[${new Date().toISOString()}] Search videos request: "${query}" (maxResults: ${maxResults}, sortBy: ${sortBy})`);
    
    // Use the YouTube search tool
    const searchResults = await searchYouTubeVideos.handler({
      query,
      maxResults,
      sortBy,
      publishedAfter,
      videoDuration
    });
    
    // Return the results
    return res.status(200).json({
      success: true,
      message: `Found ${searchResults.results?.length || 0} YouTube videos for query: "${query}"`,
      requestId: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      data: searchResults
    });
  } catch (error) {
    console.error('Error handling search-videos request:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Agent endpoint
app.post('/api/agent/youtube_agent', async (req, res) => {
  try {
    const { input } = req.body;
    
    if (!input) {
      return res.status(400).json({
        success: false,
        message: 'Input parameter is required',
        error: 'INVALID_REQUEST'
      });
    }
    
    console.log(`[${new Date().toISOString()}] YouTube agent request: "${input}"`);
    
    // Call the agent handler
    const result = await youtubeAgent.handler({ input });
    
    return res.status(200).json({
      success: true,
      message: `YouTube agent processed query: "${input}"`,
      ...result
    });
  } catch (error) {
    console.error('Error in YouTube agent:', error);
    return res.status(500).json({
      success: false,
      message: 'Error processing agent request',
      error: error.message
    });
  }
});

// Start server
async function startServer() {
  try {
    console.log('Starting YouTube MCP server...');
    console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
    
    // Validate server configuration
    console.log('Validating server configuration...');
    const isConfigValid = await validateServerConfig();
    
    if (!isConfigValid) {
      console.warn('⚠️ Server configuration validation failed. Starting anyway with limited functionality.');
    } else {
      console.log('✅ Server configuration validation passed.');
    }
    
    app.listen(PORT, () => {
      console.log(`YouTube MCP server started on port ${PORT}`);
      console.log(`Server URL: ${process.env.SERVER_URL || `http://localhost:${PORT}`}`);
      console.log(`Health check: ${process.env.SERVER_URL || `http://localhost:${PORT}`}/api/health`);
      console.log(`YouTube API status: ${process.env.SERVER_URL || `http://localhost:${PORT}`}/api/youtube-status`);
      console.log(`Search videos endpoint: ${process.env.SERVER_URL || `http://localhost:${PORT}`}/api/search-videos`);
      console.log(`Available tools: search_youtube_videos, request_wikipedia_topics`);
      console.log(`Available agents: youtube_agent`);
    });
  } catch (error) {
    console.error('Failed to start YouTube MCP server:', error);
    process.exit(1);
  }
}

startServer(); 