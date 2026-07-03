@echo off
echo Testing TTS Server Connection...
echo.

echo 1. Testing server at http://localhost:3000...
curl -s http://localhost:3000
echo.
echo.

echo 2. Testing API POST /tts...
curl -X POST http://localhost:3000/tts -H "Content-Type: application/json" -d "{\"text\":\"test connection\"}"
echo.
echo.

echo Test complete!
pause

