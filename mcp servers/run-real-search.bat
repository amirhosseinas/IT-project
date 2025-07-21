@echo off
REM =====  Real MCP Agents Demo  =====
echo Starting Wikipedia server …
start "Wikipedia" cmd /k "node working-wikipedia-server.js"

echo Starting YouTube server (dynamic, real search) …
start "YouTube"   cmd /k "node simple-youtube-server.js"

echo Waiting 8 seconds for servers to initialize …
timeout /t 8 /nobreak >nul

echo Running interactive agent test …
node simple-agent-test.js

echo.
echo Demo finished – review the two server windows for live logs.
pause
