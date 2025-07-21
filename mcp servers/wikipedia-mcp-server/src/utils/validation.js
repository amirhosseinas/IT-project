/**
 * Validation utilities for Wikipedia MCP server
 */
const axios = require('axios');

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
    'YOUTUBE_MCP_SERVER_URL',
    'SHARED_SECRET'
  ];
  
  for (const envVar of requiredEnvVars) {
    if (!process.env[envVar]) {
      console.error(`ERROR: Missing required environment variable: ${envVar}`);
      isValid = false;
    }
  }
  
  // Check if YouTube MCP server is reachable
  console.log(`Checking connection to YouTube MCP server at ${process.env.YOUTUBE_MCP_SERVER_URL}...`);
  try {
    const response = await axios.get(`${process.env.YOUTUBE_MCP_SERVER_URL}/api/health`, {
      timeout: 5000 // 5 seconds timeout
    });
    
    if (response.status === 200) {
      console.log('✅ Successfully connected to YouTube MCP server');
    } else {
      console.warn(`⚠️ YouTube MCP server returned status ${response.status}`);
      isValid = false;
    }
  } catch (error) {
    console.error('❌ Failed to connect to YouTube MCP server:', error.message);
    isValid = false;
  }
  
  // Check Wikipedia API access
  console.log('Testing Wikipedia API access...');
  try {
    const wikipediaJs = require('wikipedia-js');
    const options = {
      query: 'test',
      format: 'json',
      summaryOnly: true,
      lang: 'en'
    };
    
    await new Promise((resolve, reject) => {
      wikipediaJs.searchArticle(options, (err, data) => {
        if (err) {
          reject(err);
        } else {
          resolve(data);
        }
      });
    });
    
    console.log('✅ Wikipedia API access is working');
  } catch (error) {
    console.error('❌ Wikipedia API access failed:', error.message);
    isValid = false;
  }
  
  // Check for required dependencies
  console.log('Checking required dependencies...');
  const requiredDeps = ['axios', 'wikipedia-js', 'dotenv', 'cors'];
  
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