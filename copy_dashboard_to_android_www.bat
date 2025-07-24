@echo off
REM Copy React dashboard build to Android assets/www for Go Live feature
set DASHBOARD_BUILD="d:\React_App\csi-live-dashboard\build"
set ANDROID_WWW="d:\React_App\Android-CSI-Labelling-App-master\app\src\main\assets\www"

REM Remove old www folder if exists
if exist %ANDROID_WWW% rmdir /s /q %ANDROID_WWW%

REM Recreate www folder
mkdir %ANDROID_WWW%

REM Copy all build files to www
xcopy /e /i /y %DASHBOARD_BUILD% %ANDROID_WWW%

echo Dashboard build copied to Android assets/www.
pause
