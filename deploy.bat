@echo off
setlocal

cd /d C:\dev\teleprompter
if errorlevel 1 (
    echo [FAIL] Could not change to project directory C:\dev\teleprompter
    goto :fail
)
echo [OK] Project directory set.

echo.
echo Checking for connected device...
set DEVSTATE=
for /f "delims=" %%i in ('adb get-state 2^>nul') do set DEVSTATE=%%i
if not "%DEVSTATE%"=="device" (
    echo [FAIL] No authorized device connected. Check that USB debugging is enabled and the device is authorized ^(accept the RSA prompt on the device^).
    goto :fail
)
echo [OK] Device connected.

echo.
echo Building debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo [FAIL] Gradle build failed.
    goto :fail
)
echo [OK] Build succeeded.

echo.
echo Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo [FAIL] APK install failed.
    goto :fail
)
echo [OK] APK installed.

:askTest
echo.
set /p TESTRESULT="Did testing pass? (Y/N): "
if /i "%TESTRESULT%"=="Y" (
    echo [OK] Testing passed - deployment confirmed.
    goto :end
)
if /i "%TESTRESULT%"=="N" (
    echo [FAIL] Testing failed - deployment marked as failed.
    goto :end
)
echo Please enter Y or N.
goto :askTest

:fail
echo.
echo Deployment aborted due to a failed step.

:end
echo.
pause
endlocal
