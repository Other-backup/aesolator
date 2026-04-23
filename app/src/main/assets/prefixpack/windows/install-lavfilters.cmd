@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=lavfilters"
set "SOURCE_PAGE=https://github.com/Nevcairiel/LAVFilters/releases/tag/0.81"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\lavfilters-install.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "LAVFilters-0.81-Installer.exe"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing LAVFilters payload"
  echo Missing LAVFilters payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\LAVFilters-0.81-Installer.exe
  echo Source page:
  echo   %SOURCE_PAGE%
  exit /b 1
)
set "INSTALLER=%PREFIX_PACK_ACTIVE_PAYLOAD%"

echo Launching LAVFilters installer...
echo   %INSTALLER%
echo Source page:
echo   %SOURCE_PAGE%
set "PREFIX_PACK_PRIMARY_PAYLOAD=%INSTALLER%"
set "PREFIX_PACK_NEXT_ACTION=Inspect the LAVFilters log if setup fails."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%LOG_FILE%" "Installing LAVFilters silently"
"%INSTALLER%" /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /SP- /LOG="%LOG_FILE%" >nul 2>&1
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "LAVFilters installer failed"
) else (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%LOG_FILE%" "LAVFilters installed (%PREFIX_PACK_RC_STATUS%)"
)
echo LAVFilters installer exit code: %RC%
exit /b %RC%
