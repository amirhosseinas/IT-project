const express = require('express');
const app = express();
const PORT = 3001;

app.use(express.json());

app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'wikipedia-mcp-server',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

app.post('/api/search-topics', (req, res) => {
  const { query, limit = 5 } = req.body;
  console.log(`Wikipedia Agent received request for: "${query}"`);
  
  const results = [
    {
      title: `${query} - Wikipedia`,
      url: `https://en.wikipedia.org/wiki/${query.replace(/ /g, '_')}`,
      summary: `Comprehensive article about ${query} from Wikipedia`
    },
    {
      title: `History of ${query}`,
      url: `https://en.wikipedia.org/wiki/History_of_${query.replace(/ /g, '_')}`,
      summary: `Historical overview of ${query}`
    },
    {
      title: `${query} applications`,
      url: `https://en.wikipedia.org/wiki/${query.replace(/ /g, '_')}_applications`,
      summary: `Real-world applications of ${query}`
    }
  ];

  res.json({
    success: true,
    message: `Found ${results.length} Wikipedia topics for query: "${query}"`,
    data: { results }
  });
});

app.listen(PORT, () => {
  console.log(`✅ Wikipedia MCP server running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/api/health`);
});
