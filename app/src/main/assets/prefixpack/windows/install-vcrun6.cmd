@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=vcrun6"
set "SOURCE_PAGE=https://ftp.zx.net.nz/pub/archive/ftp.microsoft.com/developr/visual_c/visual-public/"
set "STAGE_DIR=%PREFIX_PACK_STAGE_ROOT%\vcrun6"

set "EXTRACT_LOG=%PREFIX_PACK_LOG_ROOT%\vcrun6-extract.log"
set "INSTALL_LOG=%PREFIX_PACK_LOG_ROOT%\vcrun6-install.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "VC6RedistSetup_enu.EXE"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing VC6 runtime payload"
  echo Missing VC6 runtime payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\VC6RedistSetup_enu.EXE
  echo.
  echo Source page:
  echo   %SOURCE_PAGE%
  echo Prefetch it first with the rootfs loader or repo-side tools.
  exit /b 1
)
set "INSTALLER=%PREFIX_PACK_ACTIVE_PAYLOAD%"

if exist "%STAGE_DIR%" rmdir /S /Q "%STAGE_DIR%"
mkdir "%STAGE_DIR%"

echo Extracting VC6 runtime package from:
echo   %INSTALLER%
echo.
echo Extract log:
echo   %EXTRACT_LOG%
echo Install log:
echo   %INSTALL_LOG%
echo.
set "PREFIX_PACK_PRIMARY_PAYLOAD=%INSTALLER%"
set "PREFIX_PACK_NEXT_ACTION=Inspect the extract and install logs if VC6 setup fails."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%INSTALL_LOG%" "Extracting and installing VC6 runtime"
"%INSTALLER%" /Q /T:"%STAGE_DIR%" > "%EXTRACT_LOG%" 2>&1
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%EXTRACT_LOG%" "VC6 extract step failed"
  exit /b %RC%
)

if not exist "%STAGE_DIR%\vcredist.exe" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed 1 "%EXTRACT_LOG%" "VC6 extract step did not produce vcredist.exe"
  echo Extract step did not produce vcredist.exe
  echo See:
  echo   %EXTRACT_LOG%
  exit /b 1
)

"%STAGE_DIR%\vcredist.exe" /q > "%INSTALL_LOG%" 2>&1
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%INSTALL_LOG%" "VC6 runtime failed"
) else (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%INSTALL_LOG%" "VC6 runtime installed (%PREFIX_PACK_RC_STATUS%)"
)
echo.
echo VC6 runtime exit code: %RC%
echo Install log:
echo   %INSTALL_LOG%
exit /b %RC%
