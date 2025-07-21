@echo off
echo ===================================================
echo YouTube Links Generator
echo ===================================================
echo.

REM Check if Node.js is installed
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Node.js is installed but not in PATH.
    echo Using direct path to Node.js executable.
    set NODE_EXE="C:\Program Files\nodejs\node.exe"
) else (
    set NODE_EXE=node
)

REM Get search query from user if not provided
if "%~1"=="" (
    set /p SEARCH_QUERY="Enter search query: "
) else (
    set SEARCH_QUERY=%*
)

REM Run the script
%NODE_EXE% youtube-links.js "%SEARCH_QUERY%"

echo.
pause 