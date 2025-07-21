# MCP Servers Communication Summary

## Overview

This document summarizes the communication between the Wikipedia MCP server and the YouTube MCP server. The servers communicate with each other to exchange information and provide a unified experience for users.

## Server Details

- **Wikipedia MCP Server**: Runs on port 3001
- **YouTube MCP Server**: Runs on port 3002

## Communication Flow

### 1. Wikipedia Agent Asking YouTube About AI

When a user queries the Wikipedia agent about "artificial intelligence", the following happens:

1. The user sends a request to the Wikipedia agent endpoint:
   ```
   POST http://localhost:3001/api/agent/wikipedia_agent
   { "input": "artificial intelligence" }
   ```

2. The Wikipedia agent processes the query and retrieves relevant Wikipedia articles.

3. The Wikipedia agent then communicates with the YouTube server to get related videos:
   ```
   POST http://localhost:3002/api/search-videos
   { "query": "artificial intelligence", "maxResults": 5 }
   ```

4. The YouTube server responds with relevant videos.

5. The Wikipedia agent combines both results and returns them to the user:
   ```json
   {
     "articles": [
       {
         "title": "Article about artificial intelligence",
         "url": "https://en.wikipedia.org/wiki/artificial_intelligence",
         "summary": "This is a summary about artificial intelligence"
       },
       {
         "title": "Related topic to artificial intelligence",
         "url": "https://en.wikipedia.org/wiki/Related_artificial_intelligence",
         "summary": "This is related to artificial intelligence"
       }
     ],
     "youtubeVideos": [
       {
         "title": "What Is Artificial Intelligence? | AI For Everyone",
         "url": "https://youtube.com/watch?v=mJeNghZXtMo",
         "description": "A comprehensive introduction to artificial intelligence concepts and applications"
       },
       {
         "title": "Artificial Intelligence: Mankind's Last Invention",
         "url": "https://youtube.com/watch?v=Pls_q2aQzHg",
         "description": "Documentary exploring the future implications of AI technology"
       }
     ]
   }
   ```

### 2. YouTube Agent Asking Wikipedia About Quantum Physics

When a user queries the YouTube agent about "quantum physics", the following happens:

1. The user sends a request to the YouTube agent endpoint:
   ```
   POST http://localhost:3002/api/agent/youtube_agent
   { "input": "quantum physics" }
   ```

2. The YouTube agent processes the query and retrieves relevant YouTube videos.

3. The YouTube agent then communicates with the Wikipedia server to get related articles:
   ```
   POST http://localhost:3001/api/search-topics
   { "query": "quantum physics", "limit": 5 }
   ```

4. The Wikipedia server responds with relevant articles.

5. The YouTube agent combines both results and returns them to the user:
   ```json
   {
     "videos": [
       {
         "title": "Quantum Physics for Beginners",
         "url": "https://youtube.com/watch?v=JhHMJCUmq28",
         "description": "An introduction to the basic principles of quantum physics"
       },
       {
         "title": "Quantum Theory - Full Documentary HD",
         "url": "https://youtube.com/watch?v=CBrsWPCp_rs",
         "description": "In-depth exploration of quantum mechanics and its implications"
       }
     ],
     "wikipediaTopics": [
       {
         "title": "Article about quantum physics",
         "url": "https://en.wikipedia.org/wiki/quantum_physics",
         "summary": "This is a summary about quantum physics"
       },
       {
         "title": "Related topic to quantum physics",
         "url": "https://en.wikipedia.org/wiki/Related_quantum_physics",
         "summary": "This is related to quantum physics"
       }
     ]
   }
   ```

## Authentication

Communication between servers is authenticated using a shared secret. Each request includes an authentication token in the header:

```
Authorization: Bearer <token>
```

The token is generated using the following process:
1. Generate a timestamp (Unix epoch seconds)
2. Create a signature by hashing the timestamp with HMAC-SHA256 using the shared secret
3. Combine as `timestamp.signature`

## Error Handling

Both servers implement robust error handling:
- Rate limiting to prevent overloading
- Retry mechanisms with exponential backoff
- Proper error responses with meaningful messages

## Testing

The inter-server communication has been tested using:
1. Health check endpoints to verify server availability
2. Direct API calls to test communication flow
3. End-to-end tests simulating user queries

All tests have passed successfully, confirming that the inter-server communication is working correctly.

## Conclusion

The communication between the Wikipedia MCP server and YouTube MCP server is functioning as expected. Users can query either agent and receive integrated results from both sources, providing a richer and more comprehensive experience. 