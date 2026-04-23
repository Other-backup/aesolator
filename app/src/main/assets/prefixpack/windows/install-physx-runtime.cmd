@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=physx"
set "MODERN_PAGE=https://www.nvidia.com/en-us/drivers/physx/physx-9-21-0713-driver/"
set "LEGACY_PAGE=https://www.nvidia.com/en-us/drivers/physx/physx-9-13-0604-legacy-driver/"
set "META_LOG=%PREFIX_PACK_LOG_ROOT%\physx-runtime-pack.log"
set "MODERN_LOG=%PREFIX_PACK_LOG_ROOT%\physx-system-9.21.0713.log"

set "MODERN="
set "LEGACY="
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "PhysX_9.21.0713_SystemSoftware.exe"
if not errorlevel 1 set "MODERN=%PREFIX_PACK_ACTIVE_PAYLOAD%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "PhysX-9.13.0604-SystemSoftware-Legacy.msi"
if not errorlevel 1 set "LEGACY=%PREFIX_PACK_ACTIVE_PAYLOAD%"

if not defined MODERN if not defined LEGACY (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing PhysX payloads"
  echo Missing PhysX payloads:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\PhysX_9.21.0713_SystemSoftware.exe
  echo   %PREFIX_PACK_INSTALLER_CACHE%\PhysX-9.13.0604-SystemSoftware-Legacy.msi
  echo Source pages:
  echo   %MODERN_PAGE%
  echo   %LEGACY_PAGE%
  exit /b 1
)

> "%META_LOG%" echo Installing PhysX runtime pack...
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%META_LOG%" "Installing PhysX runtime pack"

if exist "%LEGACY%" (
  echo Installing NVIDIA PhysX Legacy runtime...
  set "PREFIX_PACK_PRIMARY_PAYLOAD=%LEGACY%"
  set "PREFIX_PACK_NEXT_ACTION=Inspect the PhysX legacy MSI log if setup fails."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "NVIDIA PhysX Legacy Runtime" "%LEGACY%" "/qn /norestart" "%PREFIX_PACK_LOG_ROOT%\physx-legacy-9.13.0604.log" "Installing NVIDIA PhysX legacy runtime"
  set "RC=%ERRORLEVEL%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%PREFIX_PACK_LOG_ROOT%\physx-legacy-9.13.0604.log" "PhysX legacy runtime failed"
    exit /b %RC%
  )
) else (
  echo PhysX Legacy payload is not cached; skipping legacy PhysX step.
)

if exist "%MODERN%" (
  echo Launching NVIDIA PhysX System Software installer...
  echo   %MODERN%
  set "PREFIX_PACK_PRIMARY_PAYLOAD=%MODERN%"
  set "PREFIX_PACK_NEXT_ACTION=Follow the NVIDIA PhysX setup window and inspect the installer log if it stalls."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :launch_exe "%TARGET%" "NVIDIA PhysX System Software" "%MODERN%" "" "%MODERN_LOG%" "Launching NVIDIA PhysX System Software GUI installer"
  set "RC=%ERRORLEVEL%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%MODERN_LOG%" "PhysX System Software installer dispatch failed"
    exit /b %RC%
  )
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success "%RC%" "%MODERN_LOG%" "NVIDIA PhysX System Software installer finished (%PREFIX_PACK_RC_STATUS%)"
  exit /b 0
) else (
  echo PhysX System Software payload is not cached; skipping modern PhysX step.
)

echo PhysX runtime pack finished.
echo Logs:
echo   %PREFIX_PACK_LOG_ROOT%\physx-legacy-9.13.0604.log
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%META_LOG%" "PhysX runtime pack installed"
exit /b 0
