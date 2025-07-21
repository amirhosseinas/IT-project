const { google } = require('googleapis');

/**
 * YouTube Agent that handles search requests and integrates with Wikipedia topics
 */
const youtubeAgent = {
  name: 'youtube_agent',
  description: 'Agent that searches YouTube for videos and can retrieve related Wikipedia topics',
  tools: ['search_youtube_videos', 'request_wikipedia_topics'],
  handler: async ({ input }) => {
    try {
      // Get tools
      const { searchYouTubeVideos, requestWikipediaTopics } = require('../tools');
      
      // Step 1: Search YouTube for relevant videos
      const youtubeResponse = await searchYouTubeVideos.handler({
        query: input,
        maxResults: 5,
        sortBy: 'relevance'
      });

      if (!youtubeResponse.success || youtubeResponse.results.length === 0) {
        return {
          success: false,
          message: `No YouTube videos found for "${input}"`,
          results: []
        };
      }

      // Step 2: Try to get related Wikipedia topics if available
      let wikipediaTopics = null;
      try {
        wikipediaTopics = await requestWikipediaTopics.handler({
          query: input,
          limit: 3
        });
      } catch (error) {
        console.warn('Could not retrieve Wikipedia topics:', error.message);
        // Continue without Wikipedia topics
      }

      // Step 3: Get channel details for the top video
      let topVideoChannelDetails = null;
      const topVideo = youtubeResponse.results[0];
      
      if (topVideo && topVideo.channelId) {
        try {
          // Initialize YouTube API client
          const youtube = google.youtube({
            version: 'v3',
            auth: process.env.YOUTUBE_API_KEY
          });
          
          const channelResponse = await youtube.channels.list({
            part: 'snippet,statistics',
            id: topVideo.channelId
          });
          
          if (channelResponse.data.items && channelResponse.data.items.length > 0) {
            const channel = channelResponse.data.items[0];
            topVideoChannelDetails = {
              id: channel.id,
              title: channel.snippet.title,
              description: channel.snippet.description,
              thumbnailUrl: channel.snippet.thumbnails.default.url,
              subscriberCount: channel.statistics.subscriberCount,
              videoCount: channel.statistics.videoCount,
              viewCount: channel.statistics.viewCount
            };
          }
        } catch (error) {
          console.warn('Could not retrieve channel details:', error.message);
          // Continue without channel details
        }
      }

      // Step 4: Format the final response
      return {
        success: true,
        message: `Found ${youtubeResponse.results.length} YouTube videos for "${input}"`,
        query: input,
        videos: youtubeResponse.results,
        topVideoChannelDetails: topVideoChannelDetails,
        wikipediaTopics: wikipediaTopics?.success ? wikipediaTopics.results : [],
        filters: youtubeResponse.filters,
        sortBy: youtubeResponse.sortBy
      };
    } catch (error) {
      console.error('Error in YouTube agent:', error);
      return {
        success: false,
        message: `Error processing YouTube request: ${error.message}`,
        error: true
      };
    }
  }
};

module.exports = {
  youtubeAgent
}; 