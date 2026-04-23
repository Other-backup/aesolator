@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=graphics_diag"
set "SOURCE_PAGE=https://www.realtech-vr.com/glview-download/"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\glview-install.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "glview6499-setup.exe"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing GLview payload"
  echo Missing GLview payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\glview6499-setup.exe
  echo Source page:
  echo   %SOURCE_PAGE%
  exit /b 1
)
set "INSTALLER=%PREFIX_PACK_ACTIVE_PAYLOAD%"

echo Launching GLview installer...
echo   %INSTALLER%
echo Source page:
echo   %SOURCE_PAGE%
set "PREFIX_PACK_PRIMARY_PAYLOAD=%INSTALLER%"
set "PREFIX_PACK_NEXT_ACTION=Follow the GLview installer window or inspect the launcher log."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :launch_exe "%TARGET%" "GLview" "%INSTALLER%" "" "%LOG_FILE%" "GLview installer may open a Windows GUI"
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "GLview installer dispatch failed"
  echo GLview installer exit code: %RC%
  exit /b %RC%
)
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%LOG_FILE%" "GLview installer finished (%PREFIX_PACK_RC_STATUS%)"
echo GLview installer exit code: %RC%
exit /b 0
