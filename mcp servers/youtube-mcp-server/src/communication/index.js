const axios = require('axios');
const crypto = require('crypto');
const rateLimit = require('axios-rate-limit');

// Configuration
const WIKIPEDIA_MCP_SERVER_URL = process.env.WIKIPEDIA_MCP_SERVER_URL || 'http://localhost:3001';
const SHARED_SECRET = process.env.SHARED_SECRET || 'default_secret_change_in_production';
const MAX_RETRIES = parseInt(process.env.MAX_RETRIES) || 3;
const RETRY_DELAY = parseInt(process.env.RETRY_DELAY) || 1000; // 1 second
const MAX_REQUESTS_PER_SECOND = parseInt(process.env.MAX_REQUESTS_PER_SECOND) || 5;
const TOKEN_REFRESH_INTERVAL = 60 * 60 * 1000; // 1 hour

// Create rate-limited axios instance
const axiosInstance = rateLimit(axios.create({
  timeout: parseInt(process.env.REQUEST_TIMEOUT) || 30000 // 30 seconds
}), {
  maxRequests: MAX_REQUESTS_PER_SECOND,
  perMilliseconds: 1000 // per second
});

/**
 * Communicates with the Wikipedia MCP server
 */
class WikipediaCommunication {
  static _token = null;
  static _tokenExpiry = null;
  static _tokenRefreshTimeout = null;

  /**
   * Initialize the communication module
   */
  static init() {
    // Set up token refresh if needed
    if (process.env.WIKIPEDIA_API_AUTH_ENABLED === 'true') {
      WikipediaCommunication._refreshToken();
    }
  }

  /**
   * Generate authentication token
   * @returns {string} - Authentication token
   * @private
   */
  static _generateAuthToken() {
    // Use timestamp to prevent replay attacks
    const timestamp = Math.floor(Date.now() / 1000).toString();
    
    // Generate signature
    const signature = crypto
      .createHmac('sha256', SHARED_SECRET)
      .update(timestamp)
      .digest('hex');
    
    // Return token as timestamp.signature
    return `${timestamp}.${signature}`;
  }

  /**
   * Refresh authentication token
   * @private
   */
  static async _refreshToken() {
    if (process.env.WIKIPEDIA_API_AUTH_ENABLED !== 'true') {
      return;
    }

    try {
      // Clear existing timeout if any
      if (WikipediaCommunication._tokenRefreshTimeout) {
        clearTimeout(WikipediaCommunication._tokenRefreshTimeout);
      }

      // Get new token
      const response = await axiosInstance.post(`${WIKIPEDIA_MCP_SERVER_URL}/api/auth/token`, {
        clientId: process.env.WIKIPEDIA_API_CLIENT_ID,
        clientSecret: process.env.WIKIPEDIA_API_CLIENT_SECRET
      });

      if (response.data && response.data.token) {
        WikipediaCommunication._token = response.data.token;
        WikipediaCommunication._tokenExpiry = new Date(Date.now() + TOKEN_REFRESH_INTERVAL);
        
        // Set up next token refresh
        WikipediaCommunication._tokenRefreshTimeout = setTimeout(
          () => WikipediaCommunication._refreshToken(),
          TOKEN_REFRESH_INTERVAL - 60000 // Refresh 1 minute before expiry
        );
        
        console.log('Wikipedia API authentication token refreshed');
      }
    } catch (error) {
      console.error('Failed to refresh Wikipedia API token:', error.message);
      
      // Try again in 5 minutes
      WikipediaCommunication._tokenRefreshTimeout = setTimeout(
        () => WikipediaCommunication._refreshToken(),
        5 * 60 * 1000
      );
    }
  }

  /**
   * Get authentication headers
   * @returns {Object} - Headers object
   * @private
   */
  static _getHeaders() {
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${WikipediaCommunication._generateAuthToken()}`
    };

    return headers;
  }

  /**
   * Search Wikipedia for related topics
   * @param {string} query - The query to search for
   * @param {number} limit - Maximum number of results to return
   * @param {string} language - Language code for Wikipedia
   * @returns {Promise<Object>} - Wikipedia search results
   */
  static async searchRelatedTopics(query, limit = 5, language = 'en') {
    return WikipediaCommunication._makeRequestWithRetry(
      'POST',
      `${WIKIPEDIA_MCP_SERVER_URL}/api/search-topics`,
      {
        query,
        limit,
        language
      }
    );
  }

  /**
   * Get Wikipedia article content
   * @param {string} title - The Wikipedia article title
   * @returns {Promise<Object>} - Article content
   */
  static async getArticleContent(title) {
    return WikipediaCommunication._makeRequestWithRetry(
      'POST',
      `${WIKIPEDIA_MCP_SERVER_URL}/api/agent/wikipedia_agent`,
      {
        input: title
      }
    );
  }

  /**
   * Check if the Wikipedia MCP server is available
   * @returns {Promise<boolean>} - True if the server is available
   */
  static async isAvailable() {
    try {
      const response = await axiosInstance.get(`${WIKIPEDIA_MCP_SERVER_URL}/api/health`, {
        timeout: 5000 // 5 seconds timeout
      });
      return response.status === 200;
    } catch (error) {
      console.error('Wikipedia MCP server is not available:', error.message);
      return false;
    }
  }

  /**
   * Make HTTP request with retry logic and rate limiting
   * @param {string} method - HTTP method (GET, POST, etc.)
   * @param {string} url - Request URL
   * @param {Object} data - Request data (for POST, PUT, etc.)
   * @param {number} retries - Number of retries left
   * @returns {Promise<Object>} - Response data
   * @private
   */
  static async _makeRequestWithRetry(method, url, data, retries = MAX_RETRIES) {
    try {
      const config = {
        timeout: parseInt(process.env.REQUEST_TIMEOUT) || 30000, // 30 seconds timeout
        headers: WikipediaCommunication._getHeaders()
      };

      let response;
      if (method.toUpperCase() === 'GET') {
        response = await axiosInstance.get(url, { ...config, params: data });
      } else {
        response = await axiosInstance.post(url, data, config);
      }

      // Log successful communication
      console.log(`[${new Date().toISOString()}] Successful communication with Wikipedia MCP server: ${method} ${url}`);

      return response.data?.data || response.data;
    } catch (error) {
      // Handle rate limiting
      if (error.response?.status === 429) {
        const retryAfter = error.response.headers['retry-after'] || 5;
        console.warn(`Rate limited by Wikipedia MCP server. Retrying after ${retryAfter} seconds...`);
        
        // Wait for the specified time
        await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
        
        // Retry without decrementing retry count
        return WikipediaCommunication._makeRequestWithRetry(method, url, data, retries);
      }
      
      // Handle authentication errors
      if (error.response?.status === 401) {
        console.warn('Authentication error with Wikipedia MCP server:', error.response.data?.message);
      }
      
      // Normal retry logic
      if (retries > 0) {
        console.warn(`Request to ${url} failed. Retrying... (${retries} retries left)`);
        
        // Wait before retrying
        await new Promise(resolve => setTimeout(resolve, RETRY_DELAY));
        
        // Retry with one less retry attempt
        return WikipediaCommunication._makeRequestWithRetry(method, url, data, retries - 1);
      }
      
      // No more retries left, throw the error
      console.error(`Request to ${url} failed after ${MAX_RETRIES} retries:`, error.message);
      throw new Error(`Communication with Wikipedia MCP server failed: ${error.message}`);
    }
  }
}

// Initialize the communication module
WikipediaCommunication.init();

module.exports = {
  WikipediaCommunication
}; 