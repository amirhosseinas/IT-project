const { Agent } = require('@modelcontextprotocol/sdk');

// Wikipedia search agent
const wikipediaSearchAgent = new Agent({
  name: 'wikipedia_search_agent',
  description: 'Agent that searches Wikipedia for information',
  tools: ['search_wikipedia', 'get_article_content', 'get_article_summary'],
  handler: async (query, tools) => {
    try {
      // Search for articles related to the query
      const searchResults = await tools.search_wikipedia({
        query: query,
        limit: 3
      });

      if (!searchResults || searchResults.length === 0) {
        return {
          message: `No Wikipedia articles found for "${query}"`,
          results: []
        };
      }

      // Get the summary of the first result
      const firstResult = searchResults[0];
      const articleTitle = firstResult.title;
      
      const summary = await tools.get_article_summary({
        title: articleTitle
      });

      return {
        message: `Found information on Wikipedia for "${query}"`,
        title: articleTitle,
        summary: summary,
        allResults: searchResults
      };
    } catch (error) {
      console.error('Error in Wikipedia search agent:', error);
      return {
        message: `Error searching Wikipedia: ${error.message}`,
        error: true
      };
    }
  }
});

// Wikipedia content agent
const wikipediaContentAgent = new Agent({
  name: 'wikipedia_content_agent',
  description: 'Agent that retrieves and processes Wikipedia article content',
  tools: ['get_article_content', 'get_article_summary'],
  handler: async (title, tools) => {
    try {
      // Get the full content of the article
      const content = await tools.get_article_content({
        title: title
      });

      // Get the summary for quick reference
      const summary = await tools.get_article_summary({
        title: title
      });

      return {
        message: `Retrieved content for Wikipedia article "${title}"`,
        title: title,
        content: content,
        summary: summary
      };
    } catch (error) {
      console.error('Error in Wikipedia content agent:', error);
      return {
        message: `Error retrieving Wikipedia content: ${error.message}`,
        error: true
      };
    }
  }
});

module.exports = {
  wikipediaSearchAgent,
  wikipediaContentAgent
}; 