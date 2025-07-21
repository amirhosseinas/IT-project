const { Agent } = require('@modelcontextprotocol/sdk');
const axios = require('axios');

/**
 * Wikipedia Agent that handles search requests and integrates with YouTube videos
 */
const wikipediaAgent = new Agent({
  name: 'wikipedia_agent',
  description: 'Agent that searches Wikipedia for information and can retrieve related YouTube videos',
  tools: ['search_wikipedia_topics', 'request_youtube_videos'],
  handler: async (query, tools) => {
    try {
      // Step 1: Search Wikipedia for relevant articles
      const wikipediaResponse = await tools.search_wikipedia_topics({
        query: query,
        limit: 5
      });

      if (!wikipediaResponse.success || wikipediaResponse.results.length === 0) {
        return {
          success: false,
          message: `No Wikipedia articles found for "${query}"`,
          results: []
        };
      }

      // Step 2: Try to get related YouTube videos if available
      let youtubeVideos = null;
      try {
        youtubeVideos = await tools.request_youtube_videos({
          topic: query,
          maxResults: 3,
          includeChannelInfo: true
        });
      } catch (error) {
        console.warn('Could not retrieve YouTube videos:', error.message);
        // Continue without YouTube videos
      }

      // Step 3: Format the final response
      return {
        success: true,
        message: `Found ${wikipediaResponse.results.length} Wikipedia articles for "${query}"`,
        articles: wikipediaResponse.results,
        youtubeVideos: youtubeVideos?.success ? youtubeVideos.results : [],
        topVideo: youtubeVideos?.topVideo || null,
        query: query,
        pagination: wikipediaResponse.pagination
      };
    } catch (error) {
      console.error('Error in Wikipedia agent:', error);
      return {
        success: false,
        message: `Error processing Wikipedia request: ${error.message}`,
        error: true
      };
    }
  }
});

module.exports = {
  wikipediaAgent
}; 