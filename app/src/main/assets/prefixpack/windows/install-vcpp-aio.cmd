@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=vcpp_aio"
set "SOURCE_PAGE=https://github.com/abbodi1406/vcredist/releases/tag/v0.103.0"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\vcpp-aio-install.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "VisualCppRedist_AIO_x86_x64.exe"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing VC++ AIO payload"
  echo Missing VC++ AIO payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\VisualCppRedist_AIO_x86_x64.exe
  echo.
  echo Source page:
  echo   %SOURCE_PAGE%
  echo Prefetch it first with the rootfs loader or repo-side tools.
  exit /b 1
)
set "INSTALLER=%PREFIX_PACK_ACTIVE_PAYLOAD%"

echo Installing VC++ Runtime AIO from:
echo   %INSTALLER%
echo.
echo Source page:
echo   %SOURCE_PAGE%
echo Log file:
echo   %LOG_FILE%
echo.
set "PREFIX_PACK_PRIMARY_PAYLOAD=%INSTALLER%"
set "PREFIX_PACK_NEXT_ACTION=Inspect the VC++ AIO log if setup fails."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :launch_exe "%TARGET%" "VC++ Runtime AIO" "%INSTALLER%" "/aiV /gm2" "%LOG_FILE%" "Starting VC++ AIO installer"
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "VC++ AIO failed"
) else (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%LOG_FILE%" "VC++ AIO installed (%PREFIX_PACK_RC_STATUS%)"
)
echo.
echo VC++ AIO exit code: %RC%
echo Log file:
echo   %LOG_FILE%
exit /b %RC%
