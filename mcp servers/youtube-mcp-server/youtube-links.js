/**
 * Very simple script to generate YouTube search links
 * No API keys or network requests required
 */

/**
 * Generate YouTube search links for a query
 * @param {string} query - The search query
 * @returns {Object} - Object containing search links
 */
function getYouTubeLinks(query) {
  const encodedQuery = encodeURIComponent(query);
  
  return {
    searchUrl: `https://www.youtube.com/results?search_query=${encodedQuery}`,
    searchLinks: [
      {
        title: `YouTube Search: ${query}`,
        url: `https://www.youtube.com/results?search_query=${encodedQuery}`
      },
      {
        title: `YouTube Search (Videos): ${query}`,
        url: `https://www.youtube.com/results?search_query=${encodedQuery}&sp=EgIQAQ%253D%253D`
      },
      {
        title: `YouTube Search (Channels): ${query}`,
        url: `https://www.youtube.com/results?search_query=${encodedQuery}&sp=EgIQAg%253D%253D`
      },
      {
        title: `YouTube Search (Playlists): ${query}`,
        url: `https://www.youtube.com/results?search_query=${encodedQuery}&sp=EgIQAw%253D%253D`
      }
    ]
  };
}

// If this script is run directly
if (require.main === module) {
  const searchQuery = process.argv[2] || 'javascript tutorial';
  const results = getYouTubeLinks(searchQuery);
  
  console.log('\n===================================================');
  console.log(`YouTube Links for: "${searchQuery}"`);
  console.log('===================================================\n');
  
  console.log('Main Search URL:');
  console.log(results.searchUrl);
  
  console.log('\nSpecific Search Links:');
  results.searchLinks.forEach((link, index) => {
    console.log(`\n${index + 1}. ${link.title}`);
    console.log(`   ${link.url}`);
  });
  
  console.log('\n===================================================');
}

module.exports = { getYouTubeLinks }; 