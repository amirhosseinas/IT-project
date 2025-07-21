const { MCPServer } = require('@modelcontextprotocol/sdk');
const axios = require('axios');
const cors = require('cors');
const crypto = require('crypto');
require('dotenv').config();

// Import validation utilities
const { validateServerConfig } = require('./utils/validation');

// Import tools
const { wikipediaSearchTool, youtubeRequestTool } = require('./tools');

// Import agents
const { wikipediaAgent } = require('./agents/wikipediaAgent');

// Import communication modules
const { YouTubeCommunication } = require('./communication');

// Configuration
const PORT = process.env.PORT || 3001;
const SHARED_SECRET = process.env.SHARED_SECRET || 'default_secret_change_in_production';
const REQUEST_TIMEOUT = parseInt(process.env.REQUEST_TIMEOUT) || 30000; // 30 seconds
const MAX_RETRIES = parseInt(process.env.MAX_RETRIES) || 3;

// Initialize MCP server
const server = new MCPServer({
  name: 'wikipedia-mcp-server',
  port: PORT,
});

// Add middleware for CORS and JSON parsing
server.app.use(cors());
server.app.use((req, res, next) => {
  // Log incoming requests
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// Register tools
server.registerTool(wikipediaSearchTool);
server.registerTool(youtubeRequestTool);

// Register agent
server.registerAgent(wikipediaAgent);

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
server.app.get('/api/health', (req, res) => {
  res.status(200).json({ 
    status: 'ok', 
    service: 'wikipedia-mcp-server',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

// Search topics endpoint for server-to-server communication
server.app.post('/api/search-topics', authenticateServer, async (req, res) => {
  try {
    const { query, limit = 5, language = 'en' } = req.body;
    
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
    console.log(`[${new Date().toISOString()}] Search topics request: "${query}" (limit: ${limit}, language: ${language})`);
    
    // Use the Wikipedia search tool
    const searchResults = await wikipediaSearchTool.handler({
      query,
      limit,
      language
    });
    
    // Return the results
    return res.status(200).json({
      success: true,
      message: `Found ${searchResults.results?.length || 0} Wikipedia topics for query: "${query}"`,
      requestId: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      data: searchResults
    });
  } catch (error) {
    console.error('Error handling search-topics request:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Start server
async function startServer() {
  try {
    console.log('Starting Wikipedia MCP server...');
    console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
    
    // Validate server configuration
    console.log('Validating server configuration...');
    const isConfigValid = await validateServerConfig();
    
    if (!isConfigValid) {
      console.warn('⚠️ Server configuration validation failed. Starting anyway with limited functionality.');
    } else {
      console.log('✅ Server configuration validation passed.');
    }
    
    await server.start();
    console.log(`Wikipedia MCP server started on port ${server.port}`);
    console.log(`Server URL: ${process.env.SERVER_URL || `http://localhost:${server.port}`}`);
    console.log(`Health check: ${process.env.SERVER_URL || `http://localhost:${server.port}`}/api/health`);
    console.log(`Search topics endpoint: ${process.env.SERVER_URL || `http://localhost:${server.port}`}/api/search-topics`);
    console.log(`Available tools: search_wikipedia_topics, request_youtube_videos`);
    console.log(`Available agents: wikipedia_agent`);
  } catch (error) {
    console.error('Failed to start Wikipedia MCP server:', error);
    process.exit(1);
  }
}

startServer(); 