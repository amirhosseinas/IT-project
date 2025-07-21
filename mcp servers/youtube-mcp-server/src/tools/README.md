# YouTube MCP Server Tools

This directory contains the tool implementations for the YouTube MCP server.

## Tools

### 1. YouTube Search Tool (`youtube-search.js`)

This tool searches YouTube for relevant videos and returns structured data.

#### Features:
- Uses YouTube Data API v3
- Returns video titles, URLs, durations, and channel info
- Filters results by relevance, view count, and recency
- Formats statistics with human-readable values (e.g., 1.5M views)
- Calculates relevance score based on multiple factors

#### Usage:
```javascript
const response = await tools.search_youtube_videos({
  query: "machine learning tutorial",
  maxResults: 5,
  sortBy: "relevance",
  publishedAfter: "2022-01-01T00:00:00Z",
  videoDuration: "medium"
});
```

#### Parameters:
- `query` (string, required): The search query for YouTube videos
- `maxResults` (number, optional): Maximum number of results to return (default: 5)
- `sortBy` (string, optional): Sort order for results (default: "relevance")
  - Options: "relevance", "date", "rating", "viewCount"
- `publishedAfter` (string, optional): Only include videos published after this date (ISO 8601 format)
- `videoDuration` (string, optional): Filter by video duration (default: "any")
  - Options: "any", "short", "medium", "long"

#### Response:
```javascript
{
  success: true,
  message: "Found 5 YouTube videos for query: 'machine learning tutorial'",
  query: "machine learning tutorial",
  results: [
    {
      id: "abc123xyz",
      title: "Machine Learning Tutorial for Beginners",
      description: "This tutorial covers the basics of machine learning...",
      publishedAt: "2023-05-20T14:30:00Z",
      channelId: "UC123456789",
      channelTitle: "Tech Tutorials",
      thumbnails: {
        default: { url: "https://i.ytimg.com/vi/abc123xyz/default.jpg", width: 120, height: 90 },
        medium: { url: "https://i.ytimg.com/vi/abc123xyz/mqdefault.jpg", width: 320, height: 180 },
        high: { url: "https://i.ytimg.com/vi/abc123xyz/hqdefault.jpg", width: 480, height: 360 }
      },
      duration: {
        raw: "PT1H30M15S",
        formatted: "1:30:15"
      },
      statistics: {
        viewCount: "1500000",
        formattedViewCount: "1.5M",
        likeCount: "25000",
        formattedLikeCount: "25K",
        commentCount: "1200",
        formattedCommentCount: "1.2K"
      },
      url: "https://www.youtube.com/watch?v=abc123xyz",
      embedUrl: "https://www.youtube.com/embed/abc123xyz",
      relevanceScore: 85.5,
      ageInDays: 120
    },
    // ... more results
  ],
  sortBy: "relevance",
  filters: {
    publishedAfter: "2022-01-01T00:00:00Z",
    videoDuration: "medium"
  }
}
```

### 2. Wikipedia Request Tool (`wikipedia-request.js`)

This tool sends requests to the Wikipedia MCP server to get related topics for a query.

#### Features:
- Makes HTTP requests to Wikipedia MCP server endpoint
- Handles authentication and rate limiting
- Includes comprehensive error handling
- Tracks and reports request latency

#### Usage:
```javascript
const response = await tools.request_wikipedia_topics({
  query: "machine learning",
  limit: 5,
  language: "en"
});
```

#### Parameters:
- `query` (string, required): The query to search for related Wikipedia topics
- `limit` (number, optional): Maximum number of topics to return (default: 5)
- `language` (string, optional): Language code for Wikipedia (default: "en")

#### Response:
```javascript
{
  success: true,
  message: "Found 5 related Wikipedia topics for query: 'machine learning'",
  query: "machine learning",
  results: [
    {
      title: "Machine learning",
      pageid: 1234567,
      url: "https://en.wikipedia.org/wiki/Machine_learning",
      summary: "Machine learning is a field of inquiry devoted to understanding...",
      categories: ["Artificial intelligence", "Computer science"],
      pageViews: {
        average: 15000,
        trend: [
          { date: "2023-07-10", views: 14500 },
          { date: "2023-07-11", views: 15200 }
        ]
      },
      relevanceScore: 95.2
    },
    // ... more results
  ],
  latency: 120,
  pagination: {
    offset: 0,
    limit: 5,
    total: 50,
    hasMore: true
  }
}
```

## Tool Registration

The tools are exported from the `index.js` file and registered in the MCP server in `src/index.js`. 