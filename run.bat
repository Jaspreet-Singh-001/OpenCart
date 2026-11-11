@echo off
setlocal
cd /d "%~dp0"

echo Running the TestNG suite configured in pom.xml...
echo.

where mvn >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven was not found on PATH.
    echo Install Maven or add Maven's bin folder to PATH, then run this file again.
    echo.
    pause
    exit /b 1
)

call mvn clean test
set "TEST_EXIT_CODE=%ERRORLEVEL%"

echo.
if "%TEST_EXIT_CODE%"=="0" (
    echo Test suite completed successfully.
) else (
    echo Test suite failed with exit code %TEST_EXIT_CODE%.
)
echo.
pause
exit /b %TEST_EXIT_CODE%