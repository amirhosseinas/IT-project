const { google } = require('googleapis');

// Initialize YouTube API client
const youtube = google.youtube({
  version: 'v3',
  auth: process.env.YOUTUBE_API_KEY
});

/**
 * Format video duration from ISO 8601 to human-readable format
 * @param {string} isoDuration - ISO 8601 duration format (e.g., PT1H30M15S)
 * @returns {string} - Human-readable duration (e.g., 1:30:15)
 */
function formatDuration(isoDuration) {
  if (!isoDuration) return 'Unknown';
  
  // Remove PT from the beginning
  const duration = isoDuration.replace('PT', '');
  
  // Extract hours, minutes, and seconds
  const hoursMatch = duration.match(/(\d+)H/);
  const minutesMatch = duration.match(/(\d+)M/);
  const secondsMatch = duration.match(/(\d+)S/);
  
  const hours = hoursMatch ? parseInt(hoursMatch[1]) : 0;
  const minutes = minutesMatch ? parseInt(minutesMatch[1]) : 0;
  const seconds = secondsMatch ? parseInt(secondsMatch[1]) : 0;
  
  // Format the duration
  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  } else {
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}

/**
 * Format view count with appropriate suffix (K, M, B)
 * @param {string|number} viewCount - The view count as string or number
 * @returns {string} - Formatted view count (e.g., 1.5M)
 */
function formatViewCount(viewCount) {
  if (!viewCount) return 'Unknown';
  
  const count = parseInt(viewCount);
  
  if (count >= 1000000000) {
    return `${(count / 1000000000).toFixed(1)}B`;
  } else if (count >= 1000000) {
    return `${(count / 1000000).toFixed(1)}M`;
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}K`;
  } else {
    return count.toString();
  }
}

/**
 * Calculate video age in days
 * @param {string} publishedAt - ISO date string
 * @returns {number} - Age in days
 */
function getVideoAge(publishedAt) {
  const publishDate = new Date(publishedAt);
  const now = new Date();
  const diffTime = Math.abs(now - publishDate);
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

/**
 * Calculate relevance score based on various factors
 * @param {Object} video - Video object with metadata
 * @param {string} query - Search query
 * @returns {number} - Relevance score
 */
function calculateRelevanceScore(video, query) {
  if (!video) return 0;
  
  // Base factors
  const viewCountScore = video.statistics?.viewCount ? Math.min(parseInt(video.statistics.viewCount) / 10000, 50) : 0;
  const likeScore = video.statistics?.likeCount ? Math.min(parseInt(video.statistics.likeCount) / 1000, 30) : 0;
  const commentScore = video.statistics?.commentCount ? Math.min(parseInt(video.statistics.commentCount) / 100, 20) : 0;
  
  // Age penalty (newer videos get less penalty)
  const ageInDays = getVideoAge(video.snippet.publishedAt);
  const agePenalty = Math.min(ageInDays / 30, 10); // Max 10 point penalty for old videos
  
  // Title and description relevance
  const titleMatch = video.snippet.title.toLowerCase().includes(query.toLowerCase()) ? 20 : 0;
  const descriptionMatch = video.snippet.description.toLowerCase().includes(query.toLowerCase()) ? 10 : 0;
  
  // Calculate final score
  return viewCountScore + likeScore + commentScore + titleMatch + descriptionMatch - agePenalty;
}

/**
 * Tool to search YouTube for relevant videos
 */
const searchYouTubeVideos = {
  name: 'search_youtube_videos',
  description: 'Searches YouTube for relevant videos and returns video links, metadata, and statistics',
  parameters: {
    type: 'object',
    properties: {
      query: {
        type: 'string',
        description: 'The search query for YouTube videos'
      },
      maxResults: {
        type: 'number',
        description: 'Maximum number of results to return',
        default: 5
      },
      sortBy: {
        type: 'string',
        description: 'Sort order for results (relevance, date, rating, viewCount)',
        default: 'relevance',
        enum: ['relevance', 'date', 'rating', 'viewCount']
      },
      publishedAfter: {
        type: 'string',
        description: 'Only include videos published after this date (ISO 8601 format, e.g., 2022-01-01T00:00:00Z)'
      },
      videoDuration: {
        type: 'string',
        description: 'Filter by video duration (any, short, medium, long)',
        default: 'any',
        enum: ['any', 'short', 'medium', 'long']
      }
    },
    required: ['query']
  },
  handler: async ({ query, maxResults = 5, sortBy = 'relevance', publishedAfter, videoDuration = 'any' }) => {
    try {
      // Step 1: Search for videos using the YouTube API
      const searchParams = {
        part: 'snippet',
        q: query,
        maxResults: Math.min(maxResults * 2, 50), // Get more results than needed for better filtering
        type: 'video',
        order: sortBy,
        videoDefinition: 'high',
        relevanceLanguage: 'en'
      };
      
      // Add optional filters if provided
      if (publishedAfter) {
        searchParams.publishedAfter = publishedAfter;
      }
      
      if (videoDuration !== 'any') {
        searchParams.videoDuration = videoDuration;
      }
      
      const searchResponse = await youtube.search.list(searchParams);
      
      if (!searchResponse.data.items || searchResponse.data.items.length === 0) {
        return {
          success: false,
          message: `No YouTube videos found for query: "${query}"`,
          results: []
        };
      }
      
      // Extract video IDs for detailed information
      const videoIds = searchResponse.data.items.map(item => item.id.videoId);
      
      // Step 2: Get detailed information for each video
      const videoDetailsResponse = await youtube.videos.list({
        part: 'snippet,contentDetails,statistics',
        id: videoIds.join(',')
      });
      
      if (!videoDetailsResponse.data.items) {
        return {
          success: false,
          message: 'Failed to retrieve video details',
          results: []
        };
      }
      
      // Step 3: Process and enrich the results
      const videos = videoDetailsResponse.data.items.map(video => {
        // Calculate relevance score
        const relevanceScore = calculateRelevanceScore(video, query);
        
        return {
          id: video.id,
          title: video.snippet.title,
          description: video.snippet.description.substring(0, 200) + (video.snippet.description.length > 200 ? '...' : ''),
          publishedAt: video.snippet.publishedAt,
          channelId: video.snippet.channelId,
          channelTitle: video.snippet.channelTitle,
          thumbnails: {
            default: video.snippet.thumbnails.default,
            medium: video.snippet.thumbnails.medium,
            high: video.snippet.thumbnails.high
          },
          duration: {
            raw: video.contentDetails.duration,
            formatted: formatDuration(video.contentDetails.duration)
          },
          statistics: {
            viewCount: video.statistics.viewCount,
            formattedViewCount: formatViewCount(video.statistics.viewCount),
            likeCount: video.statistics.likeCount,
            formattedLikeCount: formatViewCount(video.statistics.likeCount),
            commentCount: video.statistics.commentCount,
            formattedCommentCount: formatViewCount(video.statistics.commentCount)
          },
          url: `https://www.youtube.com/watch?v=${video.id}`,
          embedUrl: `https://www.youtube.com/embed/${video.id}`,
          relevanceScore: relevanceScore,
          ageInDays: getVideoAge(video.snippet.publishedAt)
        };
      });
      
      // Step 4: Sort by relevance score and limit results
      const sortedVideos = videos
        .sort((a, b) => b.relevanceScore - a.relevanceScore)
        .slice(0, maxResults);
      
      return {
        success: true,
        message: `Found ${sortedVideos.length} YouTube videos for query: "${query}"`,
        query: query,
        results: sortedVideos,
        sortBy: sortBy,
        filters: {
          publishedAfter: publishedAfter || 'none',
          videoDuration: videoDuration
        }
      };
    } catch (error) {
      console.error('Error searching YouTube:', error);
      
      // Handle quota exceeded error specifically
      if (error.response?.data?.error?.errors?.some(e => e.reason === 'quotaExceeded')) {
        return {
          success: false,
          message: 'YouTube API quota exceeded. Please try again later.',
          error: 'QUOTA_EXCEEDED',
          query: query,
          results: []
        };
      }
      
      return {
        success: false,
        message: `Error searching YouTube: ${error.message}`,
        error: error.message,
        query: query,
        results: []
      };
    }
  }
};

module.exports = {
  searchYouTubeVideos
}; 