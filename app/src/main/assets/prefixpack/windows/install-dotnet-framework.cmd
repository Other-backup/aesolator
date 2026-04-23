@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env

set "TARGET=dotnet_framework"
set "DOTNET35_PAGE=https://dotnet.microsoft.com/en-us/download/dotnet-framework/net35-sp1"
set "DOTNET40_PAGE=https://dotnet.microsoft.com/en-us/download/dotnet-framework/net40"
set "DOTNET48_PAGE=https://dotnet.microsoft.com/en-us/download/dotnet-framework/net48"
set "META_LOG=%PREFIX_PACK_LOG_ROOT%\dotnet-framework-stack.log"
set "AUDIT_LOG=%PREFIX_PACK_LOG_ROOT%\dotnet-framework-registry-audit.log"
set "DOTNET35_LOG=%PREFIX_PACK_LOG_ROOT%\dotnet-framework-3.5sp1.log"
set "DOTNET40_LOG=%PREFIX_PACK_LOG_ROOT%\dotnet-framework-4.0-full.log"
set "DOTNET48_LOG=%PREFIX_PACK_LOG_ROOT%\dotnet-framework-4.8.log"

set "DOTNET35="
set "DOTNET40="
set "DOTNET48="
set "GUI_PAYLOAD="
set "GUI_LABEL="
set "GUI_LOG="
set "REQUESTING_TARGET=%PREFIX_PACK_REQUESTING_TARGET%"
set "NEEDS_LEGACY=1"
set "NEEDS_DOTNET4=1"
set "LEGACY_READY="
set "DOTNET4_READY="
set "USE_MONO_GUARD="
set "MANAGED_CONTRACT_READY="

if /I "%REQUESTING_TARGET%"=="legacy_dx_sdk" set "NEEDS_DOTNET4="

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "" "Follow the .NET Framework installer window. If nothing appears, inspect the audit, bootstrap and lane logs under AePrefixPack save_data."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :normalize_dotnet_install_roots

> "%AUDIT_LOG%" echo Auditing .NET Framework 4.x registry proof for the current live prefix...
for %%K in (
  "HKLM\Software\Microsoft\.NETFramework"
  "HKLM\Software\Wow6432Node\Microsoft\.NETFramework"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v2.0.50727"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v2.0.50727"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v3.5"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v3.5"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Client"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Full"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Client"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Full"
) do (
  >> "%AUDIT_LOG%" echo [pre] %%~K
  reg query %%K >> "%AUDIT_LOG%" 2>&1
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "dotnetfx35.exe"
if not errorlevel 1 set "DOTNET35=%PREFIX_PACK_ACTIVE_PAYLOAD%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "dotNetFx40_Full_x86_x64.exe"
if not errorlevel 1 set "DOTNET40=%PREFIX_PACK_ACTIVE_PAYLOAD%"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :resolve_payload "ndp48-x86-x64-allos-enu.exe"
if not errorlevel 1 set "DOTNET48=%PREFIX_PACK_ACTIVE_PAYLOAD%"

if not defined DOTNET35 if not defined DOTNET40 if not defined DOTNET48 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "" "Missing .NET Framework payloads"
  echo Missing .NET Framework payloads:
  echo   %PREFIX_PACK_INSTALLER_CACHE%\dotnetfx35.exe
  echo   %PREFIX_PACK_INSTALLER_CACHE%\dotNetFx40_Full_x86_x64.exe
  echo   %PREFIX_PACK_INSTALLER_CACHE%\ndp48-x86-x64-allos-enu.exe
  echo Source pages:
  echo   %DOTNET35_PAGE%
  echo   %DOTNET40_PAGE%
  echo   %DOTNET48_PAGE%
  exit /b 1
)

> "%META_LOG%" echo Preparing .NET Framework stack...
if not "%REQUESTING_TARGET%"=="" >> "%META_LOG%" echo [requester] %REQUESTING_TARGET%

if defined DOTNET35 (
  echo Legacy .NET Framework 3.5 SP1 payload is cached.
  >> "%META_LOG%" echo [.NET 3.5 SP1] %DOTNET35%
  >> "%META_LOG%" echo [.NET 3.5 SP1] this payload repairs the legacy 2.0/3.0/3.5 family that older installers such as DirectX SDK June 2010 still pull in.
)

if defined DOTNET40 (
  >> "%META_LOG%" echo [.NET 4.0 Full] %DOTNET40%
) else if defined DOTNET48 (
  >> "%META_LOG%" echo [.NET 4.8] %DOTNET48%
)

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_dotnet_legacy_runtime
if not errorlevel 1 set "LEGACY_READY=1"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_dotnet4_runtime
if not errorlevel 1 set "DOTNET4_READY=1"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_disabled_managed_overrides
if not errorlevel 1 (
  >> "%AUDIT_LOG%" echo [pre] Managed runtime overrides are disabled. Repairing the Wine Mono execution contract before trusting the current .NET proof.
  >> "%META_LOG%" echo [guard] Managed runtime overrides are currently disabled, so registry proof alone is not enough to satisfy the .NET lane.
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :ensure_wine_mono_builtin_overrides
  if not errorlevel 1 (
    set "MANAGED_CONTRACT_READY=1"
    >> "%AUDIT_LOG%" echo [repair] Rebuilt mscoree/mscoreei/mscorlib/mscorwks overrides to builtin for the live Wine Mono runtime.
    >> "%META_LOG%" echo [repair] Repaired the live Wine Mono execution contract by restoring builtin managed DLL overrides.
  ) else (
    >> "%AUDIT_LOG%" echo [repair] No Wine Mono runtime was available to rebuild the managed execution contract automatically.
    >> "%META_LOG%" echo [guard] Managed runtime overrides are disabled and Wine Mono repair was unavailable, so the lane keeps treating the current prefix as incomplete.
  )
) else (
  set "MANAGED_CONTRACT_READY=1"
)

if not defined LEGACY_READY (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :repair_dotnet_legacy_registry_from_files
  if not errorlevel 1 (
    set "LEGACY_READY=1"
    >> "%META_LOG%" echo [repair] Rebuilt the legacy .NET 2.0/3.5 registry proof from existing CLR files already present in the prefix.
  )
)

if defined NEEDS_DOTNET4 if not defined DOTNET4_READY (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :repair_dotnet4_registry_from_files
  if not errorlevel 1 (
    set "DOTNET4_READY=1"
    >> "%META_LOG%" echo [repair] Rebuilt the .NET 4.x registry proof from existing v4.0.30319 CLR files already present in the prefix.
  )
)

if not defined MANAGED_CONTRACT_READY (
  if defined LEGACY_READY set "LEGACY_READY="
  if defined DOTNET4_READY set "DOTNET4_READY="
)

if defined LEGACY_READY if not defined NEEDS_DOTNET4 goto :proof_ready
if defined LEGACY_READY if defined DOTNET4_READY goto :proof_ready

if not defined LEGACY_READY (
  if defined DOTNET35 (
    set "GUI_PAYLOAD=%DOTNET35%"
    set "GUI_LABEL=.NET Framework 3.5 SP1"
    set "GUI_LOG=%DOTNET35_LOG%"
    >> "%META_LOG%" echo [strategy] Repair the legacy .NET 2.0/3.0/3.5 family first because the current prefix still lacks trustworthy proof for those runtimes.
  ) else (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "%META_LOG%" "Legacy .NET 2.0/3.5 proof is missing and no .NET 3.5 SP1 offline installer is staged"
    exit /b 1
  )
) else if defined NEEDS_DOTNET4 if not defined DOTNET4_READY (
  if defined DOTNET40 (
    set "GUI_PAYLOAD=%DOTNET40%"
    set "GUI_LABEL=.NET Framework 4.0 Full"
    set "GUI_LOG=%DOTNET40_LOG%"
    >> "%META_LOG%" echo [strategy] Legacy proof is ready. Repair the v4.0.30319 branch next for classic managed launchers that still depend on .NET 4.x.
  ) else if defined DOTNET48 (
    set "GUI_PAYLOAD=%DOTNET48%"
    set "GUI_LABEL=.NET Framework 4.8"
    set "GUI_LOG=%DOTNET48_LOG%"
    >> "%META_LOG%" echo [strategy] .NET 4.0 Full is not cached, so the lane falls back to 4.8 to restore v4 runtime coverage.
  ) else (
    call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" missing_payload 1 "%META_LOG%" "The current prefix already has legacy .NET proof, but no launchable .NET 4.x offline installer is staged"
    exit /b 1
  )
)

if not defined GUI_PAYLOAD goto :proof_ready

>> "%META_LOG%" echo [GUI payload] %GUI_PAYLOAD%
if defined DOTNET40 if defined DOTNET48 >> "%META_LOG%" echo [note] .NET 4.8 remains staged in C:\AePrefixPack\cache as an optional follow-up after the core v4 CLR path is repaired.
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "%GUI_PAYLOAD%" "Follow the .NET Framework installer window. If nothing appears, inspect the bootstrap and lane logs under AePrefixPack save_data."
set "PREFIX_PACK_PRIMARY_PAYLOAD=%GUI_PAYLOAD%"
set "PREFIX_PACK_NEXT_ACTION=Follow the .NET Framework installer window. If nothing appears, inspect the bootstrap and lane logs under AePrefixPack save_data."
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" running 0 "%META_LOG%" "Preparing staged .NET Framework installer hand-off"
if defined DOTNET35 if /I "%GUI_PAYLOAD%"=="%DOTNET35%" set "USE_MONO_GUARD=1"
if defined USE_MONO_GUARD (
  >> "%META_LOG%" echo [guard] Legacy compatibility guard enabled for the .NET 3.5 SP1 branch.
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :capture_managed_override_snapshot
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_managed_override_state disabled
  >> "%META_LOG%" echo [guard] Temporarily disabled mscoree/mscoreei/mscorlib/mscorwks overrides before the legacy installer launch.
)
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :launch_exe "%TARGET%" "%GUI_LABEL%" "%GUI_PAYLOAD%" "" "%GUI_LOG%" "Launching staged .NET Framework GUI installer"
set "RC=%ERRORLEVEL%"
if defined USE_MONO_GUARD (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :restore_managed_override_snapshot
  >> "%META_LOG%" echo [guard] Restored managed runtime DLL overrides after the legacy installer finished.
)
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :is_nonfatal_installer_rc "%RC%"
if errorlevel 1 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" failed "%RC%" "%GUI_LOG%" ".NET Framework GUI installer dispatch failed"
  exit /b %RC%
)
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_dotnet_legacy_runtime
if not errorlevel 1 set "LEGACY_READY=1"
if defined NEEDS_DOTNET4 (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_dotnet4_runtime
  if not errorlevel 1 set "DOTNET4_READY=1"
)
if not defined LEGACY_READY (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :repair_dotnet_legacy_registry_from_files
  if not errorlevel 1 (
    set "LEGACY_READY=1"
    >> "%META_LOG%" echo [repair] Rebuilt the legacy .NET registry proof after the GUI installer completed.
  )
)
if defined NEEDS_DOTNET4 if not defined DOTNET4_READY (
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :repair_dotnet4_registry_from_files
  if not errorlevel 1 (
    set "DOTNET4_READY=1"
    >> "%META_LOG%" echo [repair] Rebuilt the .NET 4.x registry proof after the GUI installer completed.
  )
)
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :has_disabled_managed_overrides
if not errorlevel 1 (
  set "LEGACY_READY="
  set "DOTNET4_READY="
  >> "%META_LOG%" echo [guard] The GUI installer returned, but managed runtime overrides remain disabled, so the .NET execution contract is still incomplete.
) else (
  set "MANAGED_CONTRACT_READY=1"
)
if defined LEGACY_READY if not defined NEEDS_DOTNET4 goto :proof_ready
if defined LEGACY_READY if defined DOTNET4_READY goto :proof_ready
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" interactive "%RC%" "%GUI_LOG%" "%GUI_LABEL% finished, but the required .NET proof is still incomplete. Inspect the audit and lane logs before retrying dependent installers."
exit /b 0

:proof_ready
if defined NEEDS_DOTNET4 (
  >> "%META_LOG%" echo [ready] The current prefix already reports the required legacy and v4 .NET proof, and the managed runtime execution contract is no longer disabled.
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "C:\windows\Microsoft.NET\Framework\v4.0.30319\mscorlib.dll" "Rerun the dependent installer. The live prefix now reports both .NET proof and a working managed runtime dispatch contract."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%META_LOG%" "The current prefix now reports both legacy and .NET 4.x proof without disabled managed overrides. No GUI installer launch is required."
) else (
  >> "%META_LOG%" echo [ready] The current prefix already reports the required legacy .NET 2.0/3.5 family proof, and the managed runtime execution contract is no longer disabled.
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :set_lane_context "%TARGET%" "C:\windows\Microsoft.NET\Framework\v2.0.50727\mscorlib.dll" "Rerun the dependent installer. The live prefix now reports the required legacy .NET proof and a working managed runtime dispatch contract."
  call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :mark_state "%TARGET%" success 0 "%META_LOG%" "The current prefix now reports the required legacy .NET 2.0/3.5 family proof without disabled managed overrides. No GUI installer launch is required."
)
>> "%AUDIT_LOG%" echo [result] Final .NET registry audit after proof or repair.
for %%K in (
  "HKLM\Software\Microsoft\.NETFramework"
  "HKLM\Software\Wow6432Node\Microsoft\.NETFramework"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v2.0.50727"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v2.0.50727"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v3.5"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v3.5"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Client"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Full"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Client"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Full"
) do (
  >> "%AUDIT_LOG%" echo [post] %%~K
  reg query %%K >> "%AUDIT_LOG%" 2>&1
)
exit /b 0
