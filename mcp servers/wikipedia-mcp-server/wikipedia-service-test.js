const axios = require('axios');

// Configuration
const WIKIPEDIA_MCP_SERVER_URL = process.env.WIKIPEDIA_MCP_SERVER_URL || 'http://localhost:3000';

/**
 * Test the Wikipedia MCP server
 */
async function testWikipediaMCPServer() {
  try {
    console.log('Testing Wikipedia MCP server...');
    
    // Test 1: Health check
    console.log('\n1. Testing health endpoint...');
    const healthResponse = await axios.get(`${WIKIPEDIA_MCP_SERVER_URL}/api/health`);
    console.log('Health check response:', healthResponse.data);
    
    // Test 2: Search Wikipedia topics
    console.log('\n2. Testing search_wikipedia_topics tool...');
    const searchResponse = await axios.post(`${WIKIPEDIA_MCP_SERVER_URL}/api/tool/search_wikipedia_topics`, {
      query: 'artificial intelligence',
      limit: 3,
      offset: 0,
      language: 'en'
    });
    console.log('Search results count:', searchResponse.data.results?.length);
    console.log('First result title:', searchResponse.data.results?.[0]?.title);
    console.log('Pagination info:', searchResponse.data.pagination);
    
    // Test 3: Search with pagination
    console.log('\n3. Testing search_wikipedia_topics with pagination...');
    const paginatedResponse = await axios.post(`${WIKIPEDIA_MCP_SERVER_URL}/api/tool/search_wikipedia_topics`, {
      query: 'artificial intelligence',
      limit: 2,
      offset: 2,
      language: 'en'
    });
    console.log('Paginated results count:', paginatedResponse.data.results?.length);
    console.log('First result title:', paginatedResponse.data.results?.[0]?.title);
    console.log('Pagination info:', paginatedResponse.data.pagination);
    
    // Test 4: Request YouTube videos (if YouTube MCP server is available)
    console.log('\n4. Testing request_youtube_videos tool...');
    try {
      const youtubeResponse = await axios.post(`${WIKIPEDIA_MCP_SERVER_URL}/api/tool/request_youtube_videos`, {
        topic: 'artificial intelligence',
        maxResults: 2,
        includeChannelInfo: true
      });
      console.log('YouTube response success:', youtubeResponse.data.success);
      console.log('YouTube results count:', youtubeResponse.data.results?.length);
      if (youtubeResponse.data.topVideo) {
        console.log('Top video title:', youtubeResponse.data.topVideo.title);
      }
    } catch (error) {
      console.log('YouTube MCP server might not be available:', error.message);
    }
    
    // Test 5: Wikipedia agent
    console.log('\n5. Testing wikipedia_agent...');
    const agentResponse = await axios.post(`${WIKIPEDIA_MCP_SERVER_URL}/api/agent/wikipedia_agent`, {
      input: 'machine learning'
    });
    console.log('Agent response success:', agentResponse.data.success);
    console.log('Articles count:', agentResponse.data.articles?.length);
    console.log('YouTube videos count:', agentResponse.data.youtubeVideos?.length);
    
    console.log('\nAll tests completed successfully!');
  } catch (error) {
    console.error('Error testing Wikipedia MCP server:', error.message);
    if (error.response) {
      console.error('Response status:', error.response.status);
      console.error('Response data:', error.response.data);
    }
  }
}

// Run the tests
testWikipediaMCPServer(); 