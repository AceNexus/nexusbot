@echo off
setlocal EnableDelayedExpansion
title NexusBot Auto Start Launcher

echo.
echo =========================================
echo   NexusBot Auto Start
echo =========================================
echo   Date/Time: %date% %time%
echo   Profile: local (H2 Database)
echo =========================================
echo.

REM ====== 1. 載入本地配置 ======
echo [1/7] Loading local configuration
set SCRIPT_DIR=%~dp0
if exist "%SCRIPT_DIR%config.local.bat" (
  call "%SCRIPT_DIR%config.local.bat"
  echo Configuration loaded successfully
) else (
  echo.
  echo ERROR: config.local.bat not found
  echo.
  echo Please create configuration file:
  echo   1. Copy: %SCRIPT_DIR%config.local.bat.example
  echo   2. Rename to: config.local.bat
  echo   3. Fill in your credentials
  echo.
  pause
  exit /b 1
)

REM 設定伺服器端口
set SERVER_PORT=5002

REM ====== 2. 檢查必要檔案 ======
echo [2/7] Checking required files
set NGROK_DIR=D:\ngrok
set BOT_SOURCE=D:\java\tata\nexusbot\build\libs\nexusbot.jar
set BOT_RUN_DIR=D:\java\tata\nexusbot\runtime
set BOT_JAR=%BOT_RUN_DIR%\nexusbot.jar

if not exist "%NGROK_DIR%\ngrok.exe" (
  echo ERROR: ngrok.exe not found
  call :cleanup
  pause >nul
  exit /b 1
)

if not exist "%BOT_SOURCE%" (
  echo ERROR: nexusbot.jar not found at source
  call :cleanup
  pause >nul
  exit /b 1
)

REM 建立執行目錄並複製 JAR
if not exist "%BOT_RUN_DIR%" (
  echo Creating runtime directory...
  mkdir "%BOT_RUN_DIR%"
  if %errorlevel% neq 0 (
    echo ERROR: Failed to create runtime directory
    call :cleanup
    pause >nul
    exit /b 1
  )
)

echo Copying JAR to runtime directory...
copy /Y "%BOT_SOURCE%" "%BOT_JAR%" >nul
if %errorlevel% neq 0 (
  echo ERROR: Failed to copy JAR file
  call :cleanup
  pause >nul
  exit /b 1
)

REM ====== 3. 檢查並清理 Port ======
echo [3/7] Checking port %SERVER_PORT%
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%SERVER_PORT%"') do (
  echo Port %SERVER_PORT% occupied by PID %%a, killing...
  taskkill /F /PID %%a >nul 2>&1
)
timeout /t 3 >nul

REM ====== 4. 重啟 ngrok ======
echo [4/7] Restarting ngrok
tasklist | findstr /I "ngrok.exe" >nul && (
  echo Killing existing ngrok...
  taskkill /F /IM ngrok.exe >nul 2>&1
  timeout /t 2 >nul
)

cd /d "%NGROK_DIR%"
powershell -WindowStyle Hidden -Command "Start-Process '%NGROK_DIR%\ngrok.exe' -ArgumentList 'http %SERVER_PORT%' -WindowStyle Hidden"
if %errorlevel% neq 0 (
  echo ERROR: Failed to start ngrok
  call :cleanup
  pause >nul
  exit /b 1
)

REM 等待 ngrok API 就緒
echo Waiting for ngrok API...
set WAIT_COUNT=0
:wait_ngrok
timeout /t 2 >nul
curl -s http://127.0.0.1:4040/api/tunnels >nul 2>&1
if %errorlevel% neq 0 (
  set /a WAIT_COUNT+=1
  if !WAIT_COUNT! gtr 15 (
    echo ERROR: ngrok API not ready after 30 seconds
    call :cleanup
    pause >nul
    exit /b 1
  )
  echo Retry !WAIT_COUNT!/15...
  goto :wait_ngrok
)
echo ngrok API ready

REM ====== 5. 取得 ngrok URL ======
echo [5/7] Retrieving ngrok URL
set RETRY=0
:retry_url
set URL=
for /f "delims=" %%a in ('powershell -Command "(Invoke-RestMethod 'http://127.0.0.1:4040/api/tunnels').tunnels | Where-Object {$_.proto -eq 'https'} | Select-Object -First 1 -ExpandProperty public_url"') do set URL=%%a

if "%URL%"=="" (
  set /a RETRY+=1
  if !RETRY! gtr 5 (
    echo ERROR: Failed to get ngrok URL after 5 attempts
    call :cleanup
    pause >nul
    exit /b 1
  )
  echo Retry !RETRY!/5...
  timeout /t 3 >nul
  goto :retry_url
)

echo ngrok URL: %URL%
set WEBHOOK_URL=%URL%/webhook

REM ====== 6. 啟動 Bot ======
echo [6/7] Starting NexusBot
java -version >nul 2>&1
if %errorlevel% neq 0 (
  echo ERROR: Java not found
  call :cleanup
  pause >nul
  exit /b 1
)

cd /d "%BOT_RUN_DIR%"
start "NexusBot [Port:%SERVER_PORT%]" cmd /k "set WEBHOOK_URL=%WEBHOOK_URL% && set CONFIRMATION_BASE_URL=%URL% && java -jar nexusbot.jar"
if %errorlevel% neq 0 (
  echo ERROR: Failed to start NexusBot
  call :cleanup
  pause >nul
  exit /b 1
)

REM 等待 Spring Boot 啟動
echo Waiting for bot to start...
echo (This may take 10-30 seconds...)
set BOT_RETRY=0
:wait_bot
timeout /t 3 >nul

REM 增加重試次數
set /a BOT_RETRY+=1

REM 嘗試健康檢查
curl -s -o nul -w "%%{http_code}" http://localhost:%SERVER_PORT%/actuator/health > "%BOT_RUN_DIR%\health_status.txt" 2>&1
set /p HEALTH_CODE=<"%BOT_RUN_DIR%\health_status.txt"
set HEALTH_CODE=%HEALTH_CODE: =%
del "%BOT_RUN_DIR%\health_status.txt" 2>nul

REM 顯示當前嘗試狀態
if "%HEALTH_CODE%"=="200" (
  echo [Attempt !BOT_RETRY!/20] Health check PASSED ^(HTTP 200^)
  echo Bot started successfully!
  goto :update_webhook
) else if "%HEALTH_CODE%"=="000" (
  echo [Attempt !BOT_RETRY!/20] Waiting... ^(Connection refused - bot still starting^)
) else (
  echo [Attempt !BOT_RETRY!/20] Waiting... ^(HTTP %HEALTH_CODE%^)
)

if !BOT_RETRY! gtr 20 (
  echo.
  echo WARNING: Bot health check timeout after 60 seconds
  echo Checking if bot process is running...

  netstat -ano | findstr ":%SERVER_PORT%" >nul
  if %errorlevel% neq 0 (
    echo ERROR: Bot is not listening on port %SERVER_PORT%
    echo Please check bot logs in: NexusBot [Port:%SERVER_PORT%]
    call :cleanup
    pause >nul
    exit /b 1
  )

  echo Bot process is running on port %SERVER_PORT%, continuing anyway...
)
goto :wait_bot

REM ====== 7. 更新 LINE Webhook ======
:update_webhook
echo [7/7] Updating LINE webhook
echo Webhook URL: %WEBHOOK_URL%

REM 更新 webhook endpoint
curl -s -o "%BOT_RUN_DIR%\webhook_response.txt" -w "%%{http_code}" ^
  -X PUT "https://api.line.me/v2/bot/channel/webhook/endpoint" ^
  -H "Authorization: Bearer %LINE_CHANNEL_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"endpoint\":\"%WEBHOOK_URL%\"}" > "%BOT_RUN_DIR%\webhook_status.txt" 2>&1

set /p HTTP_CODE=<"%BOT_RUN_DIR%\webhook_status.txt"

REM 移除可能的空白和特殊字符
set HTTP_CODE=%HTTP_CODE: =%

echo Webhook update response: HTTP %HTTP_CODE%

if "%HTTP_CODE%"=="200" (
  echo Webhook updated successfully
) else (
  echo.
  echo ERROR: Webhook update failed with HTTP status %HTTP_CODE%
  echo.
  echo Response body:
  type "%BOT_RUN_DIR%\webhook_response.txt"
  echo.
  echo Possible causes:
  echo   1. Invalid LINE_CHANNEL_TOKEN - Check your LINE Developers Console
  echo   2. Invalid webhook URL format - Must be HTTPS with valid domain
  echo   3. Network connectivity issues - Check firewall/proxy settings
  echo   4. LINE API rate limit exceeded - Wait a few minutes
  echo   5. LINE API endpoint changed - Verify API documentation
  echo.
  echo Troubleshooting:
  echo   - Verify token at: https://developers.line.biz/console/
  echo   - Test ngrok URL directly: %WEBHOOK_URL%
  echo   - Check LINE API status: https://status.line.me/
  echo.
  del "%BOT_RUN_DIR%\webhook_status.txt" "%BOT_RUN_DIR%\webhook_response.txt" 2>nul
  call :cleanup
  pause >nul
  exit /b 1
)

del "%BOT_RUN_DIR%\webhook_status.txt" "%BOT_RUN_DIR%\webhook_response.txt" 2>nul

REM 測試 webhook 連線
echo.
echo Testing webhook connection...
curl -s -o "%BOT_RUN_DIR%\test_result.txt" -w "%%{http_code}" ^
  -X POST "https://api.line.me/v2/bot/channel/webhook/test" ^
  -H "Authorization: Bearer %LINE_CHANNEL_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"endpoint\":\"%WEBHOOK_URL%\"}" > "%BOT_RUN_DIR%\test_status.txt" 2>&1

set /p TEST_CODE=<"%BOT_RUN_DIR%\test_status.txt"
set TEST_CODE=%TEST_CODE: =%

if "%TEST_CODE%"=="200" (
  findstr /C:"\"success\":true" "%BOT_RUN_DIR%\test_result.txt" >nul
  if !errorlevel! equ 0 (
    echo Webhook test PASSED - LINE can reach your bot
  ) else (
    echo WARNING: Webhook test returned HTTP 200 but success=false
    echo Response:
    type "%BOT_RUN_DIR%\test_result.txt"
    echo.
    echo This usually means:
    echo   - Bot is not responding correctly to LINE webhooks
    echo   - Check bot logs for errors
  )
) else (
  echo WARNING: Webhook test failed with HTTP %TEST_CODE%
  echo Response:
  type "%BOT_RUN_DIR%\test_result.txt"
  echo.
  echo Note: Test failure doesn't prevent bot operation
  echo The bot may still work if the endpoint is reachable
)
del "%BOT_RUN_DIR%\test_result.txt" "%BOT_RUN_DIR%\test_status.txt" 2>nul

echo.
echo =========================================
echo         SUCCESS! All services started
echo =========================================
echo.
echo Service Information:
echo   - Webhook URL   : %WEBHOOK_URL%
echo   - Bot Port      : %SERVER_PORT%
echo   - Bot Console   : NexusBot [Port:%SERVER_PORT%]
echo   - ngrok Dashboard: http://127.0.0.1:4040
echo   - H2 Console    : http://localhost:%SERVER_PORT%/h2-console
echo   - Health Check  : http://localhost:%SERVER_PORT%/actuator/health
echo.
echo Important URLs:
echo   - LINE Developers: https://developers.line.biz/console/
echo   - Bot Runtime Dir: %BOT_RUN_DIR%
echo.
echo Next Steps:
echo   1. Check bot logs in the "NexusBot [Port:%SERVER_PORT%]" window
echo   2. Test your bot by sending messages in LINE
echo   3. Monitor ngrok traffic at http://127.0.0.1:4040
echo.
echo Note: Closing this window will NOT stop the bot or ngrok
echo       To stop services, run: taskkill /F /IM java.exe /IM ngrok.exe
echo =========================================
echo.
echo Press any key to close this launcher window...
pause >nul
exit /b 0

REM ====== 清理函數 ======
:cleanup
echo.
echo [CLEANUP] Terminating all started processes...
tasklist | findstr /I "ngrok.exe" >nul && (
  echo Killing ngrok...
  taskkill /F /IM ngrok.exe >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%SERVER_PORT%"') do (
  echo Killing process on port %SERVER_PORT%...
  taskkill /F /PID %%a >nul 2>&1
)
echo [CLEANUP] Done
goto :eof