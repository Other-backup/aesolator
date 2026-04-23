@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=legacy_dx_sdk"
set "SOURCE_PAGE=https://www.microsoft.com/en-us/download/details.aspx?id=6812"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\directx-sdk-jun10-install.log"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "%PREFIX_PACK_INSTALLER_CACHE%\DXSDK_Jun10.exe" "Follow the DirectX SDK installer window. If it does not appear, inspect the staged launcher log and rerun the lane after the managed-runtime prerequisite is healthy."

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_dotnet_legacy_runtime
if errorlevel 1 goto :redirect_dotnet
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_disabled_managed_overrides
if not errorlevel 1 goto :redirect_dotnet

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "DXSDK_Jun10.exe"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing DirectX SDK June 2010 payload"
  echo Missing DirectX SDK June 2010 payload:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\DXSDK_Jun10.exe
  echo Source page:
  echo   %SOURCE_PAGE%
  exit /b 1
)
set "INSTALLER=%PREFIX_PACK_ACTIVE_PAYLOAD%"

set "PREFIX_PACK_PRIMARY_PAYLOAD=%INSTALLER%"
set "PREFIX_PACK_NEXT_ACTION=Follow the DirectX SDK installer window or inspect the lane log for the detached launcher trace."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "%INSTALLER%" "%PREFIX_PACK_NEXT_ACTION%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" queued 0 "%LOG_FILE%" "Preparing the DirectX SDK June 2010 GUI installer hand-off"
> "%LOG_FILE%" echo DirectX SDK June 2010 GUI hand-off lane
>> "%LOG_FILE%" echo source=%SOURCE_PAGE%
>> "%LOG_FILE%" echo installer=%INSTALLER%
>> "%LOG_FILE%" echo note=Legacy .NET proof and managed DLL overrides are already healthy, so the lane can open the real DXSDK installer directly.
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :spawn_exe "%TARGET%" "DirectX SDK June 2010" "%INSTALLER%" "" "%LOG_FILE%" "DirectX SDK installer may open a Windows GUI"
set "RC=%ERRORLEVEL%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "DirectX SDK June 2010 installer dispatch failed"
  exit /b %RC%
)
if exist "C:\Program Files (x86)\Microsoft DirectX SDK (June 2010)\Utilities\bin\x86\DXCapsViewer.exe" goto :proof_ready
if exist "C:\Program Files\Microsoft DirectX SDK (June 2010)\Utilities\bin\x86\DXCapsViewer.exe" goto :proof_ready
if exist "C:\Program Files (x86)\Microsoft DirectX SDK (June 2010)\Utilities\bin\x86\dxcpl.exe" goto :proof_ready
if exist "C:\Program Files\Microsoft DirectX SDK (June 2010)\Utilities\bin\x86\dxcpl.exe" goto :proof_ready
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" interactive "%RC%" "%LOG_FILE%" "DirectX SDK GUI dispatch was accepted, but DX SDK tool proof is not visible yet. Follow the installer window or rerun Diagnostics after the GUI flow finishes."
exit /b 0

:proof_ready
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%LOG_FILE%" "DirectX SDK June 2010 tools are now visible in the prefix."
exit /b 0

:redirect_dotnet
> "%LOG_FILE%" echo DirectX SDK June 2010 still needs the managed-runtime prerequisite lane.
>> "%LOG_FILE%" echo Redirecting into the dedicated .NET Framework lane before opening DXSDK_Jun10.exe.
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" queued 0 "%LOG_FILE%" "DirectX SDK still needs the managed-runtime prerequisite lane. Repair .NET and managed DLL overrides first, then rerun DXSDK."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-loader.cmd" install dotnet_framework
exit /b %ERRORLEVEL%
