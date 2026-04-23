@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=xna"
set "XNA31_PAGE=https://www.microsoft.com/en-us/download/details.aspx?id=15163"
set "XNA40_PAGE=https://www.microsoft.com/en-us/download/details.aspx?id=27598"
set "MONO_PAGE=https://dl.winehq.org/wine/wine-mono/11.0.0/"
set "META_LOG=%PREFIX_PACK_LOG_ROOT%\xna-framework-stack.log"
set "XNA31_LOG=%PREFIX_PACK_LOG_ROOT%\xna-framework-3.1.log"
set "XNA40_LOG=%PREFIX_PACK_LOG_ROOT%\xna-framework-4.0-refresh.log"
set "MONO_LOG=%PREFIX_PACK_LOG_ROOT%\xna-wine-mono-prereq.log"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "" "Follow the XNA installer window if it opens, or inspect the state and logs under AePrefixPack save_data."
> "%META_LOG%" echo XNA runtime stack donor-backed lane
>> "%META_LOG%" echo note=Ajay Prefix treats XNA as Wine Mono dependent, not as a direct .NET Framework 4 redirect.
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_wine_mono_runtime
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "wine-mono-11.0.0-x86.msi"
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "%META_LOG%" "XNA needs Wine Mono in the current prefix, but no Wine Mono offline MSI is staged"
    echo Missing Wine Mono payload:
    echo   %PREFIX_PACK_INSTALLER_CACHE%\wine-mono-11.0.0-x86.msi
    echo Source page:
    echo   %MONO_PAGE%
    exit /b 1
  )
  set "WINE_MONO=%PREFIX_PACK_ACTIVE_PAYLOAD%"
  >> "%META_LOG%" echo [prereq] Wine Mono proof is missing, so the XNA lane repairs the Wine Mono runtime first.
  set "PREFIX_PACK_PRIMARY_PAYLOAD=%WINE_MONO%"
  set "PREFIX_PACK_NEXT_ACTION=Inspect the Wine Mono prerequisite log if the XNA lane stalls before the XNA MSI step."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "%WINE_MONO%" "%PREFIX_PACK_NEXT_ACTION%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%META_LOG%" "Installing Wine Mono prerequisite for the XNA runtime stack"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "Wine Mono x86 prerequisite" "%WINE_MONO%" "/qn" "%MONO_LOG%" "Installing Wine Mono prerequisite for XNA"
  set "RC=%ERRORLEVEL%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%MONO_LOG%" "Wine Mono prerequisite for XNA failed"
    exit /b %RC%
  )
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_wine_mono_runtime
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" interactive "%RC%" "%MONO_LOG%" "Wine Mono prerequisite finished, but the live prefix still does not expose Wine Mono proof for the XNA lane"
    exit /b 0
  )
  >> "%META_LOG%" echo [prereq] Wine Mono proof is now visible. Continuing with the XNA installers.
)
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_disabled_managed_overrides
if not errorlevel 1 (
  >> "%META_LOG%" echo [repair] Managed runtime overrides are disabled in the live prefix. Restoring builtin Wine Mono overrides before XNA MSI work.
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :ensure_wine_mono_builtin_overrides
  if not errorlevel 1 (
    >> "%META_LOG%" echo [repair] Wine Mono builtin overrides restored successfully.
  ) else (
    >> "%META_LOG%" echo [repair] Wine Mono builtin override repair was unavailable. The XNA lane may still fail on managed helper execution.
  )
)

set "XNA31="
set "XNA40="
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "xnafx31_redist.msi"
if not errorlevel 1 set "XNA31=%PREFIX_PACK_ACTIVE_PAYLOAD%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "xnafx40_redist.msi"
if not errorlevel 1 set "XNA40=%PREFIX_PACK_ACTIVE_PAYLOAD%"

if not defined XNA31 if not defined XNA40 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing XNA Framework payload"
  echo Missing XNA Framework payloads:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\xnafx31_redist.msi
  echo   %PREFIX_PACK_INSTALLER_CACHE%\xnafx40_redist.msi
  echo Source pages:
  echo   %XNA31_PAGE%
  echo   %XNA40_PAGE%
  exit /b 1
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%META_LOG%" "Installing XNA runtime stack"
>> "%META_LOG%" echo [stack] Installing XNA runtime stack...

if defined XNA31 (
  echo Installing XNA Framework 3.1...
  set "PREFIX_PACK_PRIMARY_PAYLOAD=%XNA31%"
  set "PREFIX_PACK_NEXT_ACTION=Inspect the XNA 3.1 MSI log if setup fails."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "%XNA31%" "%PREFIX_PACK_NEXT_ACTION%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "XNA Framework 3.1" "%XNA31%" "/qn /norestart" "%XNA31_LOG%" "Installing XNA Framework 3.1"
  set "RC=%ERRORLEVEL%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_xna31_runtime
    if not errorlevel 1 (
      >> "%META_LOG%" echo [proof] XNA Framework 3.1 proof is already visible in the prefix despite rc=%RC%. Continuing to the next XNA step.
    ) else (
      call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%XNA31_LOG%" "XNA Framework 3.1 failed"
      exit /b %RC%
    )
  )
)

if defined XNA40 (
  echo Installing XNA Framework 4.0 Refresh...
  set "PREFIX_PACK_PRIMARY_PAYLOAD=%XNA40%"
  set "PREFIX_PACK_NEXT_ACTION=Inspect the XNA 4.0 Refresh MSI log if setup fails."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "%XNA40%" "%PREFIX_PACK_NEXT_ACTION%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :install_msi "%TARGET%" "XNA Framework 4.0 Refresh" "%XNA40%" "/qn /norestart" "%XNA40_LOG%" "Installing XNA Framework 4.0 Refresh"
  set "RC=%ERRORLEVEL%"
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
  if errorlevel 1 (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_xna40_runtime
    if not errorlevel 1 (
      >> "%META_LOG%" echo [proof] XNA Framework 4.0 Refresh proof is already visible in the prefix despite rc=%RC%. Treating the refresh step as successful.
    ) else (
      call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%XNA40_LOG%" "XNA Framework 4.0 Refresh failed"
      exit /b %RC%
    )
  )
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%META_LOG%" "XNA runtime stack installed"
exit /b 0
