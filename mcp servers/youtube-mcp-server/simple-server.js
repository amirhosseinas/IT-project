const http = require('http');
const express = require('express');

const app = express();

// Enable JSON parsing
app.use(express.json());

// Health check endpoint
app.get('/api/health', (req, res) =
  res.json({
    status: 'ok',
    service: 'youtube-mcp-server',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

// YouTube agent endpoint
app.post('/api/agent/youtube_agent', (req, res) =
  const query = req.body.input;
  console.log('Received query:', query);

  // Simulate YouTube results
  const videos = [
    { title: 'Video about ' + query, url: 'https://youtube.com/watch?v=123', description: 'A video about ' + query },
    { title: 'Tutorial on ' + query, url: 'https://youtube.com/watch?v=456', description: 'Learn about ' + query }
  ];

  // Simulate Wikipedia topics
  const wikipediaTopics = [
    { title: 'Article about ' + query, url: 'https://en.wikipedia.org/wiki/' + query.replace(/ /g, '_'), summary: 'This is a summary about ' + query },
    { title: 'Related topic to ' + query, url: 'https://en.wikipedia.org/wiki/Related_' + query.replace(/ /g, '_'), summary: 'This is related to ' + query }
  ];

  res.json({
    videos,
    wikipediaTopics
  });
});

// Start the server
app.listen(PORT, () =
  console.log(`YouTube MCP server running on port ${PORT}`);
});
