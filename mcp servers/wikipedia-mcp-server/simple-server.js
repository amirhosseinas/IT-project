const http = require('http');
const express = require('express');

const app = express();

// Enable JSON parsing
app.use(express.json());

// Health check endpoint
app.get('/api/health', (req, res) =
  res.json({
    status: 'ok',
    service: 'wikipedia-mcp-server',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

// Wikipedia agent endpoint
app.post('/api/agent/wikipedia_agent', (req, res) =
  const query = req.body.input;
  console.log('Received query:', query);

  // Simulate Wikipedia results
  const articles = [
    { title: 'Article about ' + query, url: 'https://en.wikipedia.org/wiki/' + query.replace(/ /g, '_'), summary: 'This is a summary about ' + query },
    { title: 'Related topic to ' + query, url: 'https://en.wikipedia.org/wiki/Related_' + query.replace(/ /g, '_'), summary: 'This is related to ' + query }
  ];

  // Simulate YouTube videos from YouTube server
  const youtubeVideos = [
    { title: 'Video about ' + query, url: 'https://youtube.com/watch?v=123', description: 'A video about ' + query },
    { title: 'Tutorial on ' + query, url: 'https://youtube.com/watch?v=456', description: 'Learn about ' + query }
  ];

  res.json({
    articles,
    youtubeVideos
  });
});

// Start the server
app.listen(PORT, () =
  console.log(`Wikipedia MCP server running on port ${PORT}`);
});
