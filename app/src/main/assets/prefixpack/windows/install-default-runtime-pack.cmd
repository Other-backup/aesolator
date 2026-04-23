@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=default"
set "LOG_FILE=%PREFIX_PACK_LOG_ROOT%\default-runtime-pack.log"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%LOG_FILE%" "Installing core prefix pack stack"
> "%LOG_FILE%" echo Installing default runtime pack...

call "%PREFIX_PACK_SCRIPT_DIR%install-vcrun-full.cmd"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "VC runtime stack failed"
  exit /b %RC%
)

call "%PREFIX_PACK_SCRIPT_DIR%install-wine-web-stack.cmd"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "Wine web stack failed"
  exit /b %RC%
)

set "HAS_DOTNET="
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "dotnetfx35.exe" >nul 2>&1
if not errorlevel 1 set "HAS_DOTNET=1"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "dotNetFx40_Full_x86_x64.exe" >nul 2>&1
if not errorlevel 1 set "HAS_DOTNET=1"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "ndp48-x86-x64-allos-enu.exe" >nul 2>&1
if not errorlevel 1 set "HAS_DOTNET=1"
if defined HAS_DOTNET (
  echo .NET Framework payloads are cached but skipped in the default pack because they require an interactive GUI lane.
  >> "%LOG_FILE%" echo [.NET Framework] payloads are cached. Use the dedicated Prefix Pack lane to dispatch the staged GUI installer into the live prefix.
) else (
  echo .NET Framework payloads are not cached; skipping optional .NET Framework step.
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "directx_Jun2010_redist.exe" >nul 2>&1
if not errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%install-directx-jun2010.cmd"
  set "RC=%ERRORLEVEL%"
  if not "%RC%"=="0" (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "DirectX June 2010 step failed"
    exit /b %RC%
  )
) else (
  echo DirectX June 2010 payload is not cached; skipping optional legacy DirectX step.
)

set "HAS_XNA="
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "xnafx31_redist.msi" >nul 2>&1
if not errorlevel 1 set "HAS_XNA=1"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "xnafx40_redist.msi" >nul 2>&1
if not errorlevel 1 set "HAS_XNA=1"
if defined HAS_XNA (
  call "%PREFIX_PACK_SCRIPT_DIR%install-xna-framework.cmd"
  set "RC=%ERRORLEVEL%"
  if not "%RC%"=="0" (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "XNA step failed"
    exit /b %RC%
  )
) else (
  echo XNA Framework payload is not cached; skipping optional XNA step.
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "oalinst.zip" >nul 2>&1
if not errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%install-openal.cmd"
  set "RC=%ERRORLEVEL%"
  if not "%RC%"=="0" (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "OpenAL step failed"
    exit /b %RC%
  )
) else (
  echo OpenAL payload is not cached; skipping optional OpenAL step.
)

set "HAS_PHYSX="
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "PhysX_9.21.0713_SystemSoftware.exe" >nul 2>&1
if not errorlevel 1 set "HAS_PHYSX=1"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "PhysX-9.13.0604-SystemSoftware-Legacy.msi" >nul 2>&1
if not errorlevel 1 set "HAS_PHYSX=1"
if defined HAS_PHYSX (
  echo PhysX payloads are cached but skipped in the default pack because the modern setup lane is interactive.
  >> "%LOG_FILE%" echo [PhysX] payloads are cached. Use the dedicated Prefix Pack lane to dispatch the staged GUI installer into the live prefix.
) else (
  echo PhysX payloads are not cached; skipping optional PhysX step.
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "DXSDK_Jun10.exe" >nul 2>&1
if not errorlevel 1 (
  echo DirectX SDK tools payload is cached but skipped in the default pack because it is an interactive diagnostics lane.
  >> "%LOG_FILE%" echo [DirectX SDK tools] payload is cached. Use the dedicated Prefix Pack lane to dispatch the staged GUI installer into the live prefix.
) else (
  echo DirectX SDK tools payload is not cached; skipping optional diagnostics lane.
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "glview6499-setup.exe" >nul 2>&1
if not errorlevel 1 (
  echo GLview payload is cached but skipped in the default pack because it is an interactive diagnostics lane.
  >> "%LOG_FILE%" echo [GLview] payload is cached. Use the dedicated Prefix Pack lane to dispatch the staged GUI installer into the live prefix.
) else (
  echo GLview payload is not cached; skipping optional diagnostics lane.
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "LAVFilters-0.81-Installer.exe" >nul 2>&1
if not errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%install-lavfilters.cmd"
  set "RC=%ERRORLEVEL%"
  if not "%RC%"=="0" (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%LOG_FILE%" "LAVFilters step failed"
    exit /b %RC%
  )
) else (
  echo LAVFilters payload is not cached; skipping optional media stack step.
)

echo Default runtime pack finished.
echo Save root:
echo   %PREFIX_PACK_SAVE_ROOT%
echo Log root:
echo   %PREFIX_PACK_LOG_ROOT%
echo Installer cache:
echo   %PREFIX_PACK_INSTALLER_CACHE%
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%LOG_FILE%" "Default runtime pack finished. Interactive GUI lanes remain available as dedicated Prefix Pack installs."
exit /b 0
