const axios = require('axios');
const crypto = require('crypto');
const rateLimit = require('axios-rate-limit');

// Configuration
const YOUTUBE_MCP_SERVER_URL = process.env.YOUTUBE_MCP_SERVER_URL || 'http://localhost:3002';
const SHARED_SECRET = process.env.SHARED_SECRET || 'default_secret_change_in_production';
const MAX_RETRIES = parseInt(process.env.MAX_RETRIES) || 3;
const RETRY_DELAY = parseInt(process.env.RETRY_DELAY) || 1000; // 1 second
const MAX_REQUESTS_PER_SECOND = parseInt(process.env.MAX_REQUESTS_PER_SECOND) || 5;

// Create rate-limited axios instance
const axiosInstance = rateLimit(axios.create({
  timeout: parseInt(process.env.REQUEST_TIMEOUT) || 30000 // 30 seconds
}), {
  maxRequests: MAX_REQUESTS_PER_SECOND,
  perMilliseconds: 1000 // per second
});

/**
 * Communicates with the YouTube MCP server
 */
class YouTubeCommunication {
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
   * Search YouTube videos related to a topic
   * @param {string} topic - The topic to search for on YouTube
   * @param {number} maxResults - Maximum number of results to return
   * @returns {Promise<Object>} - YouTube search results
   */
  static async searchRelatedVideos(topic, maxResults = 5) {
    return YouTubeCommunication._makeRequestWithRetry(
      'POST',
      `${YOUTUBE_MCP_SERVER_URL}/api/search-videos`,
      {
        query: topic,
        maxResults: maxResults
      }
    );
  }

  /**
   * Get YouTube video details
   * @param {string} videoId - The YouTube video ID
   * @returns {Promise<Object>} - Video details
   */
  static async getVideoDetails(videoId) {
    return YouTubeCommunication._makeRequestWithRetry(
      'POST',
      `${YOUTUBE_MCP_SERVER_URL}/api/tool/get_video_details`,
      {
        videoId: videoId
      }
    );
  }

  /**
   * Check if the YouTube MCP server is available
   * @returns {Promise<boolean>} - True if the server is available
   */
  static async isAvailable() {
    try {
      const response = await axiosInstance.get(`${YOUTUBE_MCP_SERVER_URL}/api/health`, {
        timeout: 5000 // 5 seconds timeout
      });
      return response.status === 200;
    } catch (error) {
      console.error('YouTube MCP server is not available:', error.message);
      return false;
    }
  }

  /**
   * Make HTTP request with retry logic
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
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${YouTubeCommunication._generateAuthToken()}`
        }
      };

      let response;
      if (method.toUpperCase() === 'GET') {
        response = await axiosInstance.get(url, { ...config, params: data });
      } else {
        response = await axiosInstance.post(url, data, config);
      }

      // Log successful communication
      console.log(`[${new Date().toISOString()}] Successful communication with YouTube MCP server: ${method} ${url}`);

      return response.data?.data || response.data;
    } catch (error) {
      // Handle rate limiting
      if (error.response?.status === 429) {
        const retryAfter = error.response.headers['retry-after'] || 5;
        console.warn(`Rate limited by YouTube MCP server. Retrying after ${retryAfter} seconds...`);
        
        // Wait for the specified time
        await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
        
        // Retry without decrementing retry count
        return YouTubeCommunication._makeRequestWithRetry(method, url, data, retries);
      }
      
      // Handle authentication errors
      if (error.response?.status === 401) {
        console.warn('Authentication error with YouTube MCP server:', error.response.data?.message);
      }
      
      // Normal retry logic
      if (retries > 0) {
        console.warn(`Request to ${url} failed. Retrying... (${retries} retries left)`);
        
        // Wait before retrying
        await new Promise(resolve => setTimeout(resolve, RETRY_DELAY));
        
        // Retry with one less retry attempt
        return YouTubeCommunication._makeRequestWithRetry(method, url, data, retries - 1);
      }
      
      // No more retries left, throw the error
      console.error(`Request to ${url} failed after ${MAX_RETRIES} retries:`, error.message);
      throw new Error(`Communication with YouTube MCP server failed: ${error.message}`);
    }
  }
}

module.exports = {
  YouTubeCommunication
}; 