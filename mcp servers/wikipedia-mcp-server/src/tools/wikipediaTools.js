const { Tool } = require('@modelcontextprotocol/sdk');
const axios = require('axios');
const { YouTubeCommunication } = require('../communication');

/**
 * Tool to search Wikipedia for relevant articles
 */
const searchWikipediaTool = new Tool({
  name: 'search_wikipedia_topics',
  description: 'Searches Wikipedia for relevant articles and returns article links, summaries, and metadata',
  parameters: {
    type: 'object',
    properties: {
      query: {
        type: 'string',
        description: 'The search query for Wikipedia articles'
      },
      limit: {
        type: 'number',
        description: 'Maximum number of results to return',
        default: 5
      },
      language: {
        type: 'string',
        description: 'Language code for Wikipedia (e.g., en, es, fr)',
        default: 'en'
      }
    },
    required: ['query']
  },
  handler: async ({ query, limit = 5, language = 'en' }) => {
    try {
      // Search for Wikipedia articles using the MediaWiki API
      const searchResponse = await axios.get(`https://${language}.wikipedia.org/w/api.php`, {
        params: {
          action: 'query',
          list: 'search',
          srsearch: query,
          format: 'json',
          srlimit: limit,
          origin: '*'
        }
      });

      const searchResults = searchResponse.data.query.search;
      
      if (!searchResults || searchResults.length === 0) {
        return {
          success: false,
          message: `No Wikipedia articles found for query: "${query}"`,
          results: []
        };
      }

      // Get detailed information for each article including page views
      const articleTitles = searchResults.map(result => result.title).join('|');
      const detailsResponse = await axios.get(`https://${language}.wikipedia.org/w/api.php`, {
        params: {
          action: 'query',
          titles: articleTitles,
          prop: 'info|extracts|categories|pageviews',
          inprop: 'url',
          exintro: true,
          explaintext: true,
          clshow: '!hidden',
          cllimit: 5,
          format: 'json',
          origin: '*'
        }
      });

      const pages = detailsResponse.data.query.pages;
      
      // Process and rank results
      const results = Object.values(pages)
        .map(page => {
          // Calculate average page views for the last 7 days if available
          let avgPageViews = 0;
          if (page.pageviews) {
            const pageViewValues = Object.values(page.pageviews).filter(views => views !== null);
            if (pageViewValues.length > 0) {
              avgPageViews = pageViewValues.reduce((sum, views) => sum + views, 0) / pageViewValues.length;
            }
          }

          // Extract categories
          const categories = page.categories 
            ? page.categories.map(cat => cat.title.replace('Category:', ''))
            : [];

          return {
            title: page.title,
            pageid: page.pageid,
            url: page.fullurl,
            summary: page.extract ? page.extract.substring(0, 300) + (page.extract.length > 300 ? '...' : '') : '',
            categories: categories,
            pageViews: avgPageViews,
            lastModified: page.touched
          };
        })
        // Sort by page views (popularity) as a relevance indicator
        .sort((a, b) => b.pageViews - a.pageViews);

      return {
        success: true,
        message: `Found ${results.length} Wikipedia articles for query: "${query}"`,
        results: results
      };
    } catch (error) {
      console.error('Error searching Wikipedia:', error);
      return {
        success: false,
        message: `Error searching Wikipedia: ${error.message}`,
        error: true
      };
    }
  }
});

/**
 * Tool to request related YouTube videos
 */
const requestYoutubeVideosTool = new Tool({
  name: 'request_youtube_videos',
  description: 'Sends request to YouTube MCP server to get related videos for a topic',
  parameters: {
    type: 'object',
    properties: {
      topic: {
        type: 'string',
        description: 'The topic to search for related YouTube videos'
      },
      maxResults: {
        type: 'number',
        description: 'Maximum number of video results to return',
        default: 5
      }
    },
    required: ['topic']
  },
  handler: async ({ topic, maxResults = 5 }) => {
    try {
      // Check if YouTube MCP server is available
      const isAvailable = await YouTubeCommunication.isAvailable();
      
      if (!isAvailable) {
        return {
          success: false,
          message: 'YouTube MCP server is not available',
          results: []
        };
      }

      // Request related videos from YouTube MCP server
      const response = await YouTubeCommunication.searchRelatedVideos(topic, maxResults);
      
      return {
        success: true,
        message: `Found ${response.allResults?.length || 0} related YouTube videos for topic: "${topic}"`,
        results: response.allResults || [],
        topVideo: response.topVideo
      };
    } catch (error) {
      console.error('Error requesting YouTube videos:', error);
      return {
        success: false,
        message: `Error requesting YouTube videos: ${error.message}`,
        error: true
      };
    }
  }
});

module.exports = {
  searchWikipediaTool,
  requestYoutubeVideosTool
}; 