@echo off
echo Setting up ADB port forwarding for Android Emulator...
echo.

REM Tìm ADB từ Android SDK
set ADB_PATH=
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe
) else if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    set ADB_PATH=%ANDROID_HOME%\platform-tools\adb.exe
) else (
    echo ADB not found! Please run this from Android Studio Terminal or add ADB to PATH
    echo.
    echo You can also run manually from Android Studio:
    echo 1. Open Android Studio
    echo 2. Open Terminal tab
    echo 3. Run: adb reverse tcp:3000 tcp:3000
    pause
    exit /b 1
)

echo Using ADB: %ADB_PATH%
echo.

echo Forwarding port 3000 from emulator to host...
"%ADB_PATH%" reverse tcp:3000 tcp:3000

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS! Port forwarding setup complete!
    echo Now you can access server at http://localhost:3000 from emulator
    echo.
    echo Code has been updated to use http://localhost:3000
    echo Make sure your server is running and listening on 0.0.0.0:3000
) else (
    echo.
    echo ERROR! Failed to setup port forwarding
    echo Make sure:
    echo 1. Android emulator is running
    echo 2. ADB is connected to emulator
    echo 3. Run this script from Android Studio Terminal: adb reverse tcp:3000 tcp:3000
)

echo.
pause

