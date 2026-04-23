@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=directx_jun2010"
set "SOURCE_PAGE=https://www.microsoft.com/en-us/download/details.aspx?id=8109"
set "DOWNLOAD_URL=https://download.microsoft.com/download/8/4/a/84a35bf1-dafe-4ae8-82af-ad2ae20b6b14/directx_Jun2010_redist.exe"

set "EXTRACT_DIR=%PREFIX_PACK_STAGE_ROOT%\directx_jun2010"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\directx-jun2010-install.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "directx_Jun2010_redist.exe"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing DirectX June 2010 payload"
  echo Missing DirectX June 2010 payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\directx_Jun2010_redist.exe
  echo.
  echo Direct download:
  echo   %DOWNLOAD_URL%
  echo Download the official package from:
  echo   %SOURCE_PAGE%
  echo Then place the EXE into:
  echo   %PREFIX_PACK_INSTALLER_CACHE%
  exit /b 1
)
set "PAYLOAD=%PREFIX_PACK_ACTIVE_PAYLOAD%"

if exist "%EXTRACT_DIR%" rd /s /q "%EXTRACT_DIR%"
mkdir "%EXTRACT_DIR%"

echo Extracting DirectX June 2010 runtime...
set "PREFIX_PACK_PRIMARY_PAYLOAD=%PAYLOAD%"
set "PREFIX_PACK_NEXT_ACTION=Inspect DXSETUP and extraction logs if the DirectX runtime step fails."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%LOG_FILE%" "Extracting and installing DirectX June 2010"
"%PAYLOAD%" /Q /T:"%EXTRACT_DIR%"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "DirectX extract failed"
  exit /b %RC%
)

if not exist "%EXTRACT_DIR%\DXSETUP.exe" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed 1 "%LOG_FILE%" "DXSETUP.exe missing after extraction"
  echo DXSETUP.exe was not found after extraction.
  exit /b 1
)

echo Running DXSETUP...
"%EXTRACT_DIR%\DXSETUP.exe" /silent > "%LOG_FILE%" 2>&1
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "DirectX June 2010 failed"
) else (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%LOG_FILE%" "DirectX June 2010 installed (%PREFIX_PACK_RC_STATUS%)"
)
echo DirectX installer exit code: %RC%
echo Log file:
echo   %LOG_FILE%
exit /b %RC%
