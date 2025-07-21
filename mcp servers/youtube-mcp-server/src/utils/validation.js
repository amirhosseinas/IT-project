/**
 * Validation utilities for YouTube MCP server
 */
const axios = require('axios');
const { google } = require('googleapis');

/**
 * Validates the server configuration on startup
 * @returns {Promise<boolean>} - True if validation passes
 */
async function validateServerConfig() {
  let isValid = true;
  
  // Check for required environment variables
  console.log('Validating environment variables...');
  const requiredEnvVars = [
    'PORT',
    'YOUTUBE_API_KEY',
    'WIKIPEDIA_MCP_SERVER_URL',
    'SHARED_SECRET'
  ];
  
  for (const envVar of requiredEnvVars) {
    if (!process.env[envVar]) {
      console.error(`ERROR: Missing required environment variable: ${envVar}`);
      isValid = false;
    }
  }
  
  // Validate YouTube API key
  console.log('Validating YouTube API key...');
  try {
    const youtube = google.youtube({
      version: 'v3',
      auth: process.env.YOUTUBE_API_KEY
    });
    
    const response = await youtube.search.list({
      part: 'snippet',
      q: 'test',
      maxResults: 1
    });
    
    console.log('✅ YouTube API key is valid');
  } catch (error) {
    console.error('❌ YouTube API key validation failed:', error.message);
    if (error.response?.data?.error?.errors) {
      console.error('API Error details:', error.response.data.error.errors);
    }
    isValid = false;
  }
  
  // Check if Wikipedia MCP server is reachable
  console.log(`Checking connection to Wikipedia MCP server at ${process.env.WIKIPEDIA_MCP_SERVER_URL}...`);
  try {
    const response = await axios.get(`${process.env.WIKIPEDIA_MCP_SERVER_URL}/api/health`, {
      timeout: 5000 // 5 seconds timeout
    });
    
    if (response.status === 200) {
      console.log('✅ Successfully connected to Wikipedia MCP server');
    } else {
      console.warn(`⚠️ Wikipedia MCP server returned status ${response.status}`);
      isValid = false;
    }
  } catch (error) {
    console.error('❌ Failed to connect to Wikipedia MCP server:', error.message);
    isValid = false;
  }
  
  // Check for required dependencies
  console.log('Checking required dependencies...');
  const requiredDeps = ['axios', 'googleapis', 'dotenv', 'cors'];
  
  for (const dep of requiredDeps) {
    try {
      require(dep);
      console.log(`✅ Dependency ${dep} is installed`);
    } catch (error) {
      console.error(`❌ Missing required dependency: ${dep}`);
      isValid = false;
    }
  }
  
  return isValid;
}

module.exports = {
  validateServerConfig
}; 