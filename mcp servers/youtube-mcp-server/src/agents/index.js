const { Agent } = require('@modelcontextprotocol/sdk');

// YouTube search agent
const youtubeSearchAgent = new Agent({
  name: 'youtube_search_agent',
  description: 'Agent that searches YouTube for videos',
  tools: ['search_youtube', 'get_video_details'],
  handler: async (query, tools) => {
    try {
      // Search for videos related to the query
      const searchResults = await tools.search_youtube({
        query: query,
        maxResults: 5
      });

      if (!searchResults || searchResults.length === 0) {
        return {
          message: `No YouTube videos found for "${query}"`,
          results: []
        };
      }

      // Get detailed information about the first result
      const firstResult = searchResults[0];
      const videoDetails = await tools.get_video_details({
        videoId: firstResult.id
      });

      return {
        message: `Found YouTube videos for "${query}"`,
        topVideo: videoDetails,
        allResults: searchResults
      };
    } catch (error) {
      console.error('Error in YouTube search agent:', error);
      return {
        message: `Error searching YouTube: ${error.message}`,
        error: true
      };
    }
  }
});

// YouTube channel agent
const youtubeChannelAgent = new Agent({
  name: 'youtube_channel_agent',
  description: 'Agent that retrieves information about YouTube channels',
  tools: ['get_channel_details'],
  handler: async (channelId, tools) => {
    try {
      // Get channel details
      const channelDetails = await tools.get_channel_details({
        channelId: channelId
      });

      return {
        message: `Retrieved information for YouTube channel "${channelDetails.title}"`,
        channelDetails: channelDetails
      };
    } catch (error) {
      console.error('Error in YouTube channel agent:', error);
      return {
        message: `Error retrieving YouTube channel: ${error.message}`,
        error: true
      };
    }
  }
});

module.exports = {
  youtubeSearchAgent,
  youtubeChannelAgent
}; 