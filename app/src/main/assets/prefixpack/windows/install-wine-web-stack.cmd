@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=wine_web_stack"
set "MONO_PAGE=https://dl.winehq.org/wine/wine-mono/11.0.0/"
set "GECKO_PAGE=https://dl.winehq.org/wine/wine-gecko/2.47.4/"
set "META_LOG=%PREFIX_PACK_LOG_ROOT%\wine-web-stack.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "wine-mono-11.0.0-x86.msi"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing Wine Mono payload"
  echo Missing Wine Mono payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\wine-mono-11.0.0-x86.msi
  echo Source page:
  echo   %MONO_PAGE%
  exit /b 1
)
set "MONO=%PREFIX_PACK_ACTIVE_PAYLOAD%"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "wine-gecko-2.47.4-x86.msi"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing Wine Gecko x86 payload"
  echo Missing Wine Gecko x86 payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\wine-gecko-2.47.4-x86.msi
  echo Source page:
  echo   %GECKO_PAGE%
  exit /b 1
)
set "GECKO32=%PREFIX_PACK_ACTIVE_PAYLOAD%"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "wine-gecko-2.47.4-x86_64.msi"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing Wine Gecko x86_64 payload"
  echo Missing Wine Gecko x86_64 payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\wine-gecko-2.47.4-x86_64.msi
  echo Source page:
  echo   %GECKO_PAGE%
  exit /b 1
)
set "GECKO64=%PREFIX_PACK_ACTIVE_PAYLOAD%"

> "%META_LOG%" echo Installing Wine Mono and Gecko offline payloads...
set "PREFIX_PACK_PRIMARY_PAYLOAD=%MONO%"
set "PREFIX_PACK_NEXT_ACTION=Inspect the Wine Mono or Gecko MSI logs if setup fails."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%META_LOG%" "Installing Wine Mono and Gecko"
echo Installing Wine Mono...
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "Wine Mono x86" "%MONO%" "/qn" "%PREFIX_PACK_LOG_ROOT%\wine-mono-11.0.0-x86.log" "Installing Wine Mono x86"
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%PREFIX_PACK_LOG_ROOT%\wine-mono-11.0.0-x86.log" "Wine Mono install failed"
  exit /b %RC%
)

echo Installing Wine Gecko x86...
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "Wine Gecko x86" "%GECKO32%" "/qn" "%PREFIX_PACK_LOG_ROOT%\wine-gecko-2.47.4-x86.log" "Installing Wine Gecko x86"
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%PREFIX_PACK_LOG_ROOT%\wine-gecko-2.47.4-x86.log" "Wine Gecko x86 install failed"
  exit /b %RC%
)

echo Installing Wine Gecko x86_64...
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "Wine Gecko x86_64" "%GECKO64%" "/qn" "%PREFIX_PACK_LOG_ROOT%\wine-gecko-2.47.4-x86_64.log" "Installing Wine Gecko x86_64"
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%PREFIX_PACK_LOG_ROOT%\wine-gecko-2.47.4-x86_64.log" "Wine Gecko x86_64 install failed"
  exit /b %RC%
)

echo Wine web stack install finished.
echo Logs:
echo   %PREFIX_PACK_LOG_ROOT%\wine-mono-11.0.0-x86.log
echo   %PREFIX_PACK_LOG_ROOT%\wine-gecko-2.47.4-x86.log
echo   %PREFIX_PACK_LOG_ROOT%\wine-gecko-2.47.4-x86_64.log
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%META_LOG%" "Wine Mono and Gecko installed"
exit /b 0
