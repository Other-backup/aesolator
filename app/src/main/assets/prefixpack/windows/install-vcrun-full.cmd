@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env
set "TARGET=vcrun_full"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\vcrun-full-install.log"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "" "Inspect the VC/VCRun lane logs under AePrefixPack save_data if setup fails."

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%LOG_FILE%" "Installing VC6 and VC++ AIO stack"
> "%LOG_FILE%" echo Installing legacy VC / VCRun stack...

echo Installing legacy VC6 runtime first for MFC42-era installers...
call "%PREFIX_PACK_SCRIPT_DIR%install-vcrun6.cmd"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "VC6 stage failed"
  exit /b %RC%
)

echo Installing current VC/VCRun AIO runtime set...
call "%PREFIX_PACK_SCRIPT_DIR%install-vcpp-aio.cmd"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "VC++ AIO stage failed"
  exit /b %RC%
)

echo Full VC / VCRun stack finished.
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%LOG_FILE%" "VC6 and VC++ AIO stack installed"
exit /b 0
