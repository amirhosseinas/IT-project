@echo off
title Wikipedia Search Console
color 0A

:search_again
cls
echo =======================================
echo        WIKIPEDIA SEARCH CONSOLE
echo =======================================
echo.
echo Type your search query or 'exit' to quit:
echo.

set query=
set /p query=^> 

if /i "%query%"=="exit" goto :end_script
if "%query%"=="" set query=artificial intelligence

cls
echo =======================================
echo  Search Results for: %query%
echo =======================================
echo.

if /i "%query%"=="artificial intelligence" (
    echo 1. Artificial intelligence
    echo    Artificial intelligence (AI) is intelligence demonstrated by machines...
    echo    https://en.wikipedia.org/wiki/Artificial_intelligence
    echo.
    echo 2. Machine learning
    echo    Machine learning (ML) is a field of inquiry devoted to understanding...
    echo    https://en.wikipedia.org/wiki/Machine_learning
    echo.
    echo 3. Deep learning
    echo    Deep learning is part of a broader family of machine learning methods...
    echo    https://en.wikipedia.org/wiki/Deep_learning
) else (
    echo 1. %query%
    echo    Main article about %query%...
    echo    https://en.wikipedia.org/wiki/%query%
    echo.
    echo 2. History of %query%
    echo    Historical context and development of %query%...
    echo    https://en.wikipedia.org/wiki/History_of_%query%
    echo.
    echo 3. Applications of %query%
    echo    Practical applications and uses of %query%...
    echo    https://en.wikipedia.org/wiki/Applications_of_%query%
)

echo.
echo =======================================
echo.
echo Search completed!
echo.
echo [1] Search again
echo [2] Exit
echo.

choice /c:12 /n /m "Select option (1-2): "

if errorlevel 2 goto :end_script
if errorlevel 1 goto :search_again

:end_script
cls
echo Thank you for using Wikipedia Search Console!
echo.
pause
exit 