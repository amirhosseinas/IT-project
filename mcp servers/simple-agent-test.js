const http = require('http');
const crypto = require('crypto');

// Configuration
const SHARED_SECRET = 'default_secret_change_in_production';
const WIKIPEDIA_PORT = 3001;
const YOUTUBE_PORT = 3002;

// Helper function to create authentication token
function createAuthToken() {
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = crypto
    .createHmac('sha256', SHARED_SECRET)
    .update(timestamp.toString())
    .digest('hex');
  return `${timestamp}.${signature}`;
}

// Helper function to make HTTP requests
function makeRequest(options, data) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const parsedData = JSON.parse(responseData);
          resolve({ status: res.statusCode, data: parsedData });
        } catch (e) {
          resolve({ status: res.statusCode, data: responseData });
        }
      });
    });
    
    req.on('error', (error) => {
      reject(error);
    });
    
    if (data) {
      req.write(JSON.stringify(data));
    }
    req.end();
  });
}

// Simulate Wikipedia Agent asking YouTube Agent about quantum physics
async function wikipediaAsksYoutube() {
  console.log('\n🔵 SCENARIO 1: Wikipedia Agent asks YouTube Agent about "quantum physics"');
  console.log('=' .repeat(70));
  
  try {
    const options = {
      hostname: 'localhost',
      port: YOUTUBE_PORT,
      path: '/api/search-videos',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${createAuthToken()}`
      }
    };
    
    const requestData = {
      query: 'artificial intelligence',
      maxResults: 5,
      sortBy: 'relevance'
    };
    
    console.log('📤 Wikipedia Agent requesting YouTube videos about quantum physics...');
    const response = await makeRequest(options, requestData);
    
    console.log(`📥 Response Status: ${response.status}`);
    if (response.status === 200 && response.data.success) {
      console.log('✅ SUCCESS: YouTube Agent returned quantum physics videos!');
      console.log(`📊 Found ${response.data.data?.results?.length || 0} videos`);
      
      if (response.data.data?.results) {
        response.data.data.results.slice(0, 3).forEach((video, index) => {
          console.log(`   ${index + 1}. ${video.title}`);
          console.log(`      🔗 https://youtube.com/watch?v=${video.videoId}`);
        });
      }
    } else {
      console.log('❌ FAILED: YouTube Agent could not process request');
      console.log('Response:', JSON.stringify(response.data, null, 2));
    }
  } catch (error) {
    console.log('❌ ERROR: Could not connect to YouTube Agent');
    console.log('Error:', error.message);
  }
}

// Simulate YouTube Agent asking Wikipedia Agent about artificial intelligence
async function youtubeAsksWikipedia() {
  console.log('\n🟠 SCENARIO 2: YouTube Agent asks Wikipedia Agent about "artificial intelligence"');
  console.log('=' .repeat(70));
  
  try {
    const options = {
      hostname: 'localhost',
      port: WIKIPEDIA_PORT,
      path: '/api/search-topics',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${createAuthToken()}`
      }
    };
    
    const requestData = {
      query: 'artificial intelligence',
      limit: 5,
      language: 'en'
    };
    
    console.log('📤 YouTube Agent requesting Wikipedia articles about artificial intelligence...');
    const response = await makeRequest(options, requestData);
    
    console.log(`📥 Response Status: ${response.status}`);
    if (response.status === 200 && response.data.success) {
      console.log('✅ SUCCESS: Wikipedia Agent returned AI articles!');
      console.log(`📊 Found ${response.data.data?.results?.length || 0} articles`);
      
      if (response.data.data?.results) {
        response.data.data.results.slice(0, 3).forEach((article, index) => {
          console.log(`   ${index + 1}. ${article.title}`);
          console.log(`      🔗 ${article.url}`);
        });
      }
    } else {
      console.log('❌ FAILED: Wikipedia Agent could not process request');
      console.log('Response:', JSON.stringify(response.data, null, 2));
    }
  } catch (error) {
    console.log('❌ ERROR: Could not connect to Wikipedia Agent');
    console.log('Error:', error.message);
  }
}

// Check if servers are running
async function checkServers() {
  console.log('🔍 Checking if MCP servers are running...');
  
  const checkServer = async (port, name) => {
    try {
      const options = {
        hostname: 'localhost',
        port: port,
        path: '/api/health',
        method: 'GET'
      };
      
      const response = await makeRequest(options);
      if (response.status === 200) {
        console.log(`✅ ${name} server is running on port ${port}`);
        return true;
      } else {
        console.log(`❌ ${name} server responded with status ${response.status}`);
        return false;
      }
    } catch (error) {
      console.log(`❌ ${name} server is not running on port ${port}`);
      return false;
    }
  };
  
  const wikipediaRunning = await checkServer(WIKIPEDIA_PORT, 'Wikipedia');
  const youtubeRunning = await checkServer(YOUTUBE_PORT, 'YouTube');
  
  return { wikipediaRunning, youtubeRunning };
}

// Main test function
async function runInteractiveTest() {
  console.log('🚀 MCP AGENTS INTERACTIVE TEST');
  console.log('=' .repeat(70));
  console.log('This test demonstrates two MCP agents communicating with each other:');
  console.log('1. Wikipedia Agent asks YouTube Agent for quantum physics videos');
  console.log('2. YouTube Agent asks Wikipedia Agent for AI articles');
  console.log('=' .repeat(70));
  
  // Check if servers are running
  const serverStatus = await checkServers();
  
  if (!serverStatus.wikipediaRunning || !serverStatus.youtubeRunning) {
    console.log('\n⚠️  SERVERS NOT RUNNING');
    console.log('Please start both servers first:');
    console.log('1. Wikipedia server: cd wikipedia-mcp-server && node src/index.js');
    console.log('2. YouTube server: cd youtube-mcp-server && node src/index.js');
    console.log('\nOr use: .\\start-both-servers.bat');
    return;
  }
  
  console.log('\n🎯 Both servers are running! Starting interactive test...');
  
  // Run the scenarios
  await wikipediaAsksYoutube();
  await new Promise(resolve => setTimeout(resolve, 2000)); // Wait 2 seconds
  await youtubeAsksWikipedia();
  
  console.log('\n🎉 INTERACTIVE TEST COMPLETE!');
  console.log('=' .repeat(70));
  console.log('The agents have successfully communicated with each other.');
  console.log('Check the server logs to see the detailed interaction.');
}

// Run the test
runInteractiveTest().catch(console.error);
