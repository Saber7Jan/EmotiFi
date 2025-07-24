@echo off
REM CSI Live Dashboard & Backend Automated Setup Script

REM 1. Build React Dashboard
cd /d "d:\rootApp\csi-live-dashboard"
echo Installing React dashboard dependencies...
npm install
if %errorlevel% neq 0 exit /b %errorlevel%
echo Building React dashboard...
npm run build
if %errorlevel% neq 0 exit /b %errorlevel%

REM 2. Copy build folder to backend
cd /d "d:\rootApp\csi-live-backend"
echo Copying dashboard build to backend...
if exist build rmdir /s /q build
xcopy /e /i /y "d:\rootApp\csi-live-dashboard\build" build

REM 3. Install backend dependencies
npm install
if %errorlevel% neq 0 exit /b %errorlevel%

REM 4. Start backend server
start cmd /k "cd /d d:\rootApp\csi-live-backend && node server.js"

echo.
echo CSI Live Dashboard and backend are now running.
echo Open http://192.168.50.123:5000 in your browser or from your app.
pause
