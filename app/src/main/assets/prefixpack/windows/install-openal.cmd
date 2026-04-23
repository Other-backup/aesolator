@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=openal"
set "SOURCE_PAGE=https://www.openal.org/downloads/"
set "META_LOG=%PREFIX_PACK_LOG_ROOT%\openal-install.log"
set "EXTRACT_LOG=%PREFIX_PACK_LOG_ROOT%\openal-extract.log"
set "STAGE_DIR=%PREFIX_PACK_STAGE_ROOT%\openal"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "oalinst.zip"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing OpenAL payload"
  echo Missing OpenAL payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\oalinst.zip
  echo Source page:
  echo   %SOURCE_PAGE%
  exit /b 1
)
set "PAYLOAD=%PREFIX_PACK_ACTIVE_PAYLOAD%"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_extract_tool
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed 1 "" "7-Zip extractor not found in rootfs"
  echo 7-Zip extractor was not found under Z:\opt\7-Zip
  exit /b 1
)

if exist "%STAGE_DIR%" rd /s /q "%STAGE_DIR%"
mkdir "%STAGE_DIR%"

set "PREFIX_PACK_PRIMARY_PAYLOAD=%PAYLOAD%"
set "PREFIX_PACK_NEXT_ACTION=Inspect the OpenAL extract or install log if setup fails."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%META_LOG%" "Extracting and installing OpenAL"
"%PREFIX_PACK_EXTRACTOR%" x -y "-o%STAGE_DIR%" "%PAYLOAD%" > "%EXTRACT_LOG%" 2>&1
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%EXTRACT_LOG%" "OpenAL extract failed"
  exit /b %RC%
)

if not exist "%STAGE_DIR%\oalinst.exe" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed 1 "%EXTRACT_LOG%" "OpenAL extract did not produce oalinst.exe"
  echo OpenAL extract did not produce oalinst.exe
  exit /b 1
)

"%STAGE_DIR%\oalinst.exe" /s > "%META_LOG%" 2>&1
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%META_LOG%" "OpenAL installer failed"
) else (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%META_LOG%" "OpenAL installed (%PREFIX_PACK_RC_STATUS%)"
)
exit /b %RC%
