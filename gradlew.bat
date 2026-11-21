@echo off
setlocal
set GRADLE_VERSION=9.3.1
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo ERROR: Gradle is not installed. Install Gradle %GRADLE_VERSION% and add it to PATH.
exit /b 1
