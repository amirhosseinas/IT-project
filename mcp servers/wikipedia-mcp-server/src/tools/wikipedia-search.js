const { Tool } = require('@modelcontextprotocol/sdk');
const axios = require('axios');

/**
 * Tool to search Wikipedia for relevant articles
 */
const wikipediaSearchTool = new Tool({
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
      offset: {
        type: 'number',
        description: 'Offset for pagination',
        default: 0
      },
      language: {
        type: 'string',
        description: 'Language code for Wikipedia (e.g., en, es, fr)',
        default: 'en'
      }
    },
    required: ['query']
  },
  handler: async ({ query, limit = 5, offset = 0, language = 'en' }) => {
    try {
      // Step 1: Use opensearch API to get initial results
      const opensearchResponse = await axios.get(`https://${language}.wikipedia.org/w/api.php`, {
        params: {
          action: 'opensearch',
          search: query,
          limit: limit * 2, // Get more results than needed for better filtering
          namespace: 0, // Only search in main namespace (articles)
          format: 'json',
          origin: '*'
        }
      });
      
      // Extract article titles from opensearch response
      // opensearch returns [query, titles[], descriptions[], urls[]]
      const titles = opensearchResponse.data[1];
      
      if (!titles || titles.length === 0) {
        return {
          success: false,
          message: `No Wikipedia articles found for query: "${query}"`,
          results: [],
          pagination: {
            offset,
            limit,
            total: 0
          }
        };
      }
      
      // Step 2: Get detailed information for each article using the query API
      const detailsResponse = await axios.get(`https://${language}.wikipedia.org/w/api.php`, {
        params: {
          action: 'query',
          titles: titles.join('|'),
          prop: 'info|extracts|categories|pageviews|links|images',
          inprop: 'url|displaytitle',
          exintro: true, // Only get the intro section
          explaintext: true, // Get plain text, not HTML
          exlimit: limit,
          cllimit: 10, // Get up to 10 categories per article
          pllimit: 10, // Get up to 10 links per article
          imlimit: 5, // Get up to 5 images per article
          clshow: '!hidden', // Don't show hidden categories
          format: 'json',
          origin: '*'
        }
      });
      
      const pages = detailsResponse.data.query.pages;
      
      // Step 3: Process and rank results
      const results = Object.values(pages)
        .map(page => {
          // Calculate average page views for the last 7 days if available
          let avgPageViews = 0;
          let pageViewTrend = [];
          
          if (page.pageviews) {
            const pageViewValues = Object.entries(page.pageviews)
              .filter(([date, views]) => views !== null)
              .sort((a, b) => new Date(a[0]) - new Date(b[0]));
              
            if (pageViewValues.length > 0) {
              avgPageViews = pageViewValues.reduce((sum, [date, views]) => sum + views, 0) / pageViewValues.length;
              
              // Get page view trend (last 7 days)
              pageViewTrend = pageViewValues.map(([date, views]) => ({
                date,
                views
              }));
            }
          }
          
          // Extract categories
          const categories = page.categories 
            ? page.categories
                .map(cat => cat.title.replace('Category:', ''))
                .filter(cat => !cat.includes('Articles_with'))
            : [];
          
          // Extract links
          const links = page.links
            ? page.links.map(link => link.title)
            : [];
          
          // Extract images
          const images = page.images
            ? page.images
                .filter(img => !img.title.includes('Icon') && 
                              !img.title.includes('Logo') && 
                              !img.title.includes('Symbol'))
                .map(img => img.title)
            : [];
          
          // Calculate relevance score based on:
          // 1. Title match (higher if query appears in title)
          // 2. Page views (higher if more popular)
          // 3. Length of extract (higher if more comprehensive)
          // 4. Number of categories (higher if well-categorized)
          
          const titleMatchScore = page.title.toLowerCase().includes(query.toLowerCase()) ? 10 : 0;
          const pageViewScore = Math.min(avgPageViews / 100, 10); // Cap at 10
          const extractScore = page.extract ? Math.min(page.extract.length / 100, 5) : 0;
          const categoryScore = Math.min(categories.length, 5);
          
          const relevanceScore = titleMatchScore + pageViewScore + extractScore + categoryScore;
          
          return {
            title: page.title,
            displayTitle: page.displaytitle || page.title,
            pageid: page.pageid,
            url: page.fullurl,
            summary: page.extract 
              ? page.extract.substring(0, 300) + (page.extract.length > 300 ? '...' : '') 
              : '',
            categories: categories,
            links: links.slice(0, 5), // Limit to 5 links
            images: images.slice(0, 3), // Limit to 3 images
            pageViews: {
              average: avgPageViews,
              trend: pageViewTrend
            },
            lastModified: page.touched,
            relevanceScore: relevanceScore
          };
        })
        // Sort by relevance score (descending)
        .sort((a, b) => b.relevanceScore - a.relevanceScore)
        // Apply pagination
        .slice(offset, offset + limit);
      
      // Calculate total results (before pagination)
      const total = Object.keys(pages).length;
      
      return {
        success: true,
        message: `Found ${results.length} Wikipedia articles for query: "${query}"`,
        query: query,
        results: results,
        pagination: {
          offset,
          limit,
          total,
          hasMore: offset + limit < total
        }
      };
    } catch (error) {
      console.error('Error searching Wikipedia:', error);
      return {
        success: false,
        message: `Error searching Wikipedia: ${error.message}`,
        query: query,
        results: [],
        error: true
      };
    }
  }
});

module.exports = {
  wikipediaSearchTool
}; 