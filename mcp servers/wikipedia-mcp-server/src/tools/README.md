# Wikipedia MCP Server Tools

This directory contains the tool implementations for the Wikipedia MCP server.

## Tools

### 1. Wikipedia Search Tool (`wikipedia-search.js`)

This tool searches Wikipedia for relevant articles and returns structured data.

#### Features:
- Uses Wikipedia's OpenSearch and Query APIs
- Returns article titles, URLs, summaries, and categories
- Implements pagination for handling large result sets
- Ranks results by relevance and popularity (page views)
- Filters out irrelevant categories and images

#### Usage:
```javascript
const response = await tools.search_wikipedia_topics({
  query: "artificial intelligence",
  limit: 5,
  offset: 0,
  language: "en"
});
```

#### Parameters:
- `query` (string, required): The search query for Wikipedia articles
- `limit` (number, optional): Maximum number of results to return (default: 5)
- `offset` (number, optional): Offset for pagination (default: 0)
- `language` (string, optional): Language code for Wikipedia (default: "en")

#### Response:
```javascript
{
  success: true,
  message: "Found 5 Wikipedia articles for query: 'artificial intelligence'",
  query: "artificial intelligence",
  results: [
    {
      title: "Artificial intelligence",
      displayTitle: "Artificial intelligence",
      pageid: 1234567,
      url: "https://en.wikipedia.org/wiki/Artificial_intelligence",
      summary: "Artificial intelligence (AI) is intelligence demonstrated by machines...",
      categories: ["Machine learning", "Computer science"],
      links: ["Machine learning", "Neural network", "Deep learning", "AI winter", "Turing test"],
      images: ["Artificial_intelligence_diagram.svg", "Neural_network_example.png"],
      pageViews: {
        average: 15000,
        trend: [
          { date: "2023-07-10", views: 14500 },
          { date: "2023-07-11", views: 15200 }
        ]
      },
      lastModified: "2023-07-15T12:30:45Z",
      relevanceScore: 25.5
    },
    // ... more results
  ],
  pagination: {
    offset: 0,
    limit: 5,
    total: 120,
    hasMore: true
  }
}
```

### 2. YouTube Request Tool (`youtube-request.js`)

This tool sends requests to the YouTube MCP server to get related videos for a topic.

#### Features:
- Makes HTTP POST requests to YouTube MCP server endpoint
- Handles response and formats it properly
- Includes comprehensive error handling for connection issues
- Supports optional channel information retrieval

#### Usage:
```javascript
const response = await tools.request_youtube_videos({
  topic: "artificial intelligence",
  maxResults: 3,
  includeChannelInfo: true
});
```

#### Parameters:
- `topic` (string, required): The topic to search for related YouTube videos
- `maxResults` (number, optional): Maximum number of video results to return (default: 5)
- `includeChannelInfo` (boolean, optional): Whether to include channel information (default: false)

#### Response:
```javascript
{
  success: true,
  message: "Found 3 related YouTube videos for topic: 'artificial intelligence'",
  topic: "artificial intelligence",
  results: [
    {
      id: "abc123xyz",
      title: "Introduction to Artificial Intelligence",
      description: "Learn the basics of AI in this introductory video...",
      thumbnailUrl: "https://i.ytimg.com/vi/abc123xyz/default.jpg",
      channelTitle: "Tech Tutorials",
      publishedAt: "2023-05-20T14:30:00Z"
    },
    // ... more results
  ],
  topVideo: {
    id: "abc123xyz",
    title: "Introduction to Artificial Intelligence",
    description: "Learn the basics of AI in this introductory video...",
    publishedAt: "2023-05-20T14:30:00Z",
    channelTitle: "Tech Tutorials",
    duration: "PT15M30S",
    viewCount: "250000",
    likeCount: "15000",
    commentCount: "1200",
    channelDetails: {
      // Additional channel information if includeChannelInfo is true
    }
  }
}
```

## Tool Registration

The tools are exported from the `index.js` file and registered in the MCP server in `src/index.js`. 