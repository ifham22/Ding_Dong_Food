@echo off
echo ==============================================
echo 🍕 Foodie Express Server Build ^& Run Script
echo ==============================================

echo Compiling Java Backend...
javac java\*.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation successful!
echo.
echo Starting Server...
echo Database: Reading purely from CSV!
echo Access the site at: http://localhost:8081
echo.

java -cp "java" MainServer

pause
