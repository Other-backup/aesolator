@echo off

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

if "%~1"=="" exit /b 0
goto %~1

:init_env
if not defined PREFIX_PACK_ROOT set "PREFIX_PACK_ROOT=Z:\opt\ae\prefix-pack"
if not defined PREFIX_PACK_CACHE_ROOT set "PREFIX_PACK_CACHE_ROOT=%PREFIX_PACK_ROOT%\cache"
if not defined PREFIX_PACK_INSTALLER_ROOT set "PREFIX_PACK_INSTALLER_ROOT=C:\AePrefixPack"
if not defined PREFIX_PACK_INSTALLER_CACHE set "PREFIX_PACK_INSTALLER_CACHE=%PREFIX_PACK_INSTALLER_ROOT%\cache"
if not defined PREFIX_PACK_STAGE_ROOT set "PREFIX_PACK_STAGE_ROOT=%PREFIX_PACK_INSTALLER_ROOT%\staging"
if not defined PREFIX_PACK_SAVE_ROOT set "PREFIX_PACK_SAVE_ROOT=%USERPROFILE%\Documents\AePrefixPack\save_data"
if not defined PREFIX_PACK_LOG_ROOT set "PREFIX_PACK_LOG_ROOT=%PREFIX_PACK_SAVE_ROOT%\logs"
if not defined PREFIX_PACK_STATE_ROOT set "PREFIX_PACK_STATE_ROOT=%PREFIX_PACK_SAVE_ROOT%\state"
if not defined PREFIX_PACK_CMD_EXE set "PREFIX_PACK_CMD_EXE=C:\windows\system32\cmd.exe"
if not defined PREFIX_PACK_WSCRIPT_EXE set "PREFIX_PACK_WSCRIPT_EXE=C:\windows\system32\wscript.exe"
if not defined PREFIX_PACK_MSIEXEC_EXE set "PREFIX_PACK_MSIEXEC_EXE=C:\windows\system32\msiexec.exe"

if not exist "%PREFIX_PACK_INSTALLER_ROOT%" mkdir "%PREFIX_PACK_INSTALLER_ROOT%"
if not exist "%PREFIX_PACK_INSTALLER_CACHE%" mkdir "%PREFIX_PACK_INSTALLER_CACHE%"
if not exist "%PREFIX_PACK_STAGE_ROOT%" mkdir "%PREFIX_PACK_STAGE_ROOT%"
if not exist "%USERPROFILE%\Documents\AePrefixPack" mkdir "%USERPROFILE%\Documents\AePrefixPack"
if not exist "%PREFIX_PACK_SAVE_ROOT%" mkdir "%PREFIX_PACK_SAVE_ROOT%"
if not exist "%PREFIX_PACK_LOG_ROOT%" mkdir "%PREFIX_PACK_LOG_ROOT%"
if not exist "%PREFIX_PACK_STATE_ROOT%" mkdir "%PREFIX_PACK_STATE_ROOT%"
exit /b 0

:file_nonempty
set "PP_FILE=%~2"
if "%PP_FILE%"=="" exit /b 1
if not exist "%PP_FILE%" exit /b 1
for %%Z in ("%PP_FILE%") do (
  if %%~zZ GTR 0 exit /b 0
)
exit /b 1

:is_nonfatal_installer_rc
set "PREFIX_PACK_RC_STATUS=failure"
set "PP_RC=%~2"
if "%PP_RC%"=="" exit /b 1
if "%PP_RC%"=="0" (
  set "PREFIX_PACK_RC_STATUS=success"
  exit /b 0
)
if "%PP_RC%"=="3010" (
  set "PREFIX_PACK_RC_STATUS=reboot_required"
  exit /b 0
)
if "%PP_RC%"=="1641" (
  set "PREFIX_PACK_RC_STATUS=reboot_initiated"
  exit /b 0
)
if "%PP_RC%"=="1638" (
  set "PREFIX_PACK_RC_STATUS=already_present"
  exit /b 0
)
exit /b 1

:mark_state
call "%~f0" :init_env
set "PP_TARGET=%~2"
set "PP_STATUS=%~3"
set "PP_EXIT_CODE=%~4"
set "PP_LOG_FILE=%~5"
set "PP_DETAIL=%~6"
set "PP_STATE_FILE=%PREFIX_PACK_STATE_ROOT%\%PP_TARGET%.properties"
set "PP_ESC_TARGET=%PP_TARGET:\=\\%"
set "PP_ESC_STATUS=%PP_STATUS:\=\\%"
set "PP_ESC_EXIT_CODE=%PP_EXIT_CODE:\=\\%"
set "PP_ESC_LOG_FILE=%PP_LOG_FILE:\=\\%"
set "PP_ESC_DETAIL=%PP_DETAIL:\=\\%"
set "PP_ESC_BACKEND_CACHE=%PREFIX_PACK_CACHE_ROOT:\=\\%"
set "PP_ESC_INSTALLER_CACHE=%PREFIX_PACK_INSTALLER_CACHE:\=\\%"
set "PP_ESC_STAGE_ROOT=%PREFIX_PACK_STAGE_ROOT:\=\\%"
set "PP_ESC_STATE_ROOT=%PREFIX_PACK_STATE_ROOT:\=\\%"
set "PP_ESC_LOG_ROOT=%PREFIX_PACK_LOG_ROOT:\=\\%"
set "PP_ESC_LAUNCHER_FILE=%PREFIX_PACK_LAUNCHER_FILE:\=\\%"
set "PP_ESC_PRIMARY_PAYLOAD=%PREFIX_PACK_PRIMARY_PAYLOAD:\=\\%"
set "PP_ESC_NEXT_ACTION=%PREFIX_PACK_NEXT_ACTION:\=\\%"
set "PP_ESC_REQUESTED_BY="
if defined PREFIX_PACK_REQUESTING_TARGET set "PP_ESC_REQUESTED_BY=%PREFIX_PACK_REQUESTING_TARGET:\=\\%"
(
  echo install_target=%PP_ESC_TARGET%
  echo status=%PP_ESC_STATUS%
  echo exit_code=%PP_ESC_EXIT_CODE%
  echo updated_at=%DATE% %TIME%
  echo log_file=%PP_ESC_LOG_FILE%
  echo detail=%PP_ESC_DETAIL%
  echo backend_cache=%PP_ESC_BACKEND_CACHE%
  echo installer_cache=%PP_ESC_INSTALLER_CACHE%
  echo stage_root=%PP_ESC_STAGE_ROOT%
  echo state_root=%PP_ESC_STATE_ROOT%
  echo log_root=%PP_ESC_LOG_ROOT%
  echo launcher_file=%PP_ESC_LAUNCHER_FILE%
  echo primary_payload=%PP_ESC_PRIMARY_PAYLOAD%
  echo next_action=%PP_ESC_NEXT_ACTION%
  echo requested_by=%PP_ESC_REQUESTED_BY%
) > "%PP_STATE_FILE%"
exit /b 0

:resolve_payload
call "%~f0" :init_env
set "PREFIX_PACK_ACTIVE_PAYLOAD="
set "PP_FILE_NAME=%~2"
if "%PP_FILE_NAME%"=="" exit /b 1
if exist "%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%" (
  call "%~f0" :file_nonempty "%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%"
  if not errorlevel 1 (
    set "PREFIX_PACK_ACTIVE_PAYLOAD=%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%"
    exit /b 0
  )
  del /f /q "%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%" >nul 2>&1
)
if exist "%PREFIX_PACK_CACHE_ROOT%\%PP_FILE_NAME%" (
  call "%~f0" :file_nonempty "%PREFIX_PACK_CACHE_ROOT%\%PP_FILE_NAME%"
  if not errorlevel 1 (
    copy /Y "%PREFIX_PACK_CACHE_ROOT%\%PP_FILE_NAME%" "%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%" >nul
    call "%~f0" :file_nonempty "%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%"
    if not errorlevel 1 (
      set "PREFIX_PACK_ACTIVE_PAYLOAD=%PREFIX_PACK_INSTALLER_CACHE%\%PP_FILE_NAME%"
      exit /b 0
    )
  )
)
exit /b 1

:resolve_extract_tool
call "%~f0" :init_env
set "PREFIX_PACK_EXTRACTOR="
if exist "Z:\opt\7-Zip\7z.exe" set "PREFIX_PACK_EXTRACTOR=Z:\opt\7-Zip\7z.exe"
if not defined PREFIX_PACK_EXTRACTOR if exist "Z:\opt\7-Zip\7za.exe" set "PREFIX_PACK_EXTRACTOR=Z:\opt\7-Zip\7za.exe"
if defined PREFIX_PACK_EXTRACTOR exit /b 0
exit /b 1

:set_lane_context
call "%~f0" :init_env
set "PP_TARGET=%~2"
set "PP_PRIMARY_PAYLOAD=%~3"
set "PP_NEXT_ACTION=%~4"
if "%PP_TARGET%"=="" exit /b 1
set "PREFIX_PACK_LAUNCHER_FILE=%PREFIX_PACK_STAGE_ROOT%\%PP_TARGET%\install-%PP_TARGET%.cmd"
set "PREFIX_PACK_PRIMARY_PAYLOAD=%PP_PRIMARY_PAYLOAD%"
if not "%PP_NEXT_ACTION%"=="" set "PREFIX_PACK_NEXT_ACTION=%PP_NEXT_ACTION%"
exit /b 0

:wait_short
if exist "C:\windows\system32\timeout.exe" (
  timeout.exe /t 1 /nobreak >nul 2>&1
) else (
  ping -n 2 127.0.0.1 >nul 2>&1
)
exit /b 0

:has_dotnet4_files
call "%~f0" :init_env
if not exist "C:\windows\Microsoft.NET\Framework\v4.0.30319\mscorlib.dll" exit /b 1
if not exist "C:\windows\Microsoft.NET\Framework64\v4.0.30319\mscorlib.dll" exit /b 1
exit /b 0

:has_dotnet_legacy_files
call "%~f0" :init_env
if not exist "C:\windows\Microsoft.NET\Framework\v2.0.50727\mscorlib.dll" exit /b 1
if not exist "C:\windows\Microsoft.NET\Framework\v2.0.50727\fusion.dll" exit /b 1
exit /b 0

:has_dotnet_legacy_runtime
call "%~f0" :init_env
reg query "HKLM\Software\Microsoft\NET Framework Setup\NDP\v3.5" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v3.5" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Microsoft\NET Framework Setup\NDP\v2.0.50727" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v2.0.50727" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
exit /b 1

:has_dotnet4_runtime
call "%~f0" :init_env
reg query "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Full" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Full" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Client" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Client" /v Install 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Microsoft\.NETFramework" /v InstallRoot 2>nul | find /I "Microsoft.NET\Framework" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\.NETFramework" /v InstallRoot 2>nul | find /I "Microsoft.NET\Framework" >nul
if not errorlevel 1 exit /b 0
exit /b 1

:has_wine_mono_runtime
call "%~f0" :init_env
if exist "C:\windows\mono\mono-2.0\bin\libmono-2.0-x86.dll" exit /b 0
if exist "C:\windows\mono\mono-2.0\lib\mono\4.8-api\mscorlib.dll" exit /b 0
if exist "C:\windows\mono\mono-2.0\lib\mono\4.7.1-api\mscorlib.dll" exit /b 0
if exist "C:\windows\mono\mono-2.0\lib\mono\4.6.1-api\mscorlib.dll" exit /b 0
exit /b 1

:has_disabled_managed_overrides
call "%~f0" :init_env
for %%K in (mscoree mscoreei mscorlib mscorwks) do (
  reg query "HKCU\Software\Wine\DllOverrides" /v "%%~K" 2>nul | find /I "disabled" >nul
  if not errorlevel 1 exit /b 0
)
exit /b 1

:normalize_dotnet_install_roots
call "%~f0" :init_env
reg add "HKLM\Software\Microsoft\.NETFramework" /v InstallRoot /t REG_SZ /d "C:\windows\Microsoft.NET\Framework\" /f >nul 2>&1
reg add "HKLM\Software\Wow6432Node\Microsoft\.NETFramework" /v InstallRoot /t REG_SZ /d "C:\windows\Microsoft.NET\Framework\" /f >nul 2>&1
exit /b 0

:ensure_wine_mono_builtin_overrides
call "%~f0" :init_env
call "%~f0" :has_wine_mono_runtime
if errorlevel 1 exit /b 1
call "%~f0" :normalize_dotnet_install_roots
for /L %%P in (1,1,2) do (
  for %%K in (mscoree mscoreei mscorlib mscorwks) do (
    reg add "HKCU\Software\Wine\DllOverrides" /v "%%~K" /t REG_SZ /d "builtin" /f >nul 2>&1
  )
  call "%~f0" :wait_short
)
call "%~f0" :has_disabled_managed_overrides
if not errorlevel 1 exit /b 1
exit /b 0

:has_xna31_runtime
call "%~f0" :init_env
if exist "C:\Program Files (x86)\Common Files\Microsoft Shared\XNA\Framework\v3.1\XnaNative.dll" exit /b 0
if exist "C:\Program Files\Common Files\Microsoft Shared\XNA\Framework\v3.1\XnaNative.dll" exit /b 0
reg query "HKLM\Software\Microsoft\XNA\Framework\v3.1" /v Installed 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\XNA\Framework\v3.1" /v Installed 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
exit /b 1

:has_xna40_runtime
call "%~f0" :init_env
if exist "C:\Program Files (x86)\Common Files\Microsoft Shared\XNA\Framework\v4.0\XnaNative.dll" exit /b 0
if exist "C:\Program Files\Common Files\Microsoft Shared\XNA\Framework\v4.0\XnaNative.dll" exit /b 0
reg query "HKLM\Software\Microsoft\XNA\Framework\v4.0" /v Refresh1Installed 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\XNA\Framework\v4.0" /v Refresh1Installed 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Microsoft\XNA\Framework\v4.0" /v Installed 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
reg query "HKLM\Software\Wow6432Node\Microsoft\XNA\Framework\v4.0" /v Installed 2>nul | find /I "0x1" >nul
if not errorlevel 1 exit /b 0
exit /b 1

:repair_dotnet4_registry_from_files
call "%~f0" :init_env
call "%~f0" :has_dotnet4_runtime
if not errorlevel 1 exit /b 0
call "%~f0" :has_dotnet4_files
if errorlevel 1 exit /b 1
call "%~f0" :normalize_dotnet_install_roots
for %%K in (
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Client"
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v4\Full"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Client"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v4\Full"
) do (
  reg add %%K /f >nul 2>&1
  reg add %%K /v Install /t REG_DWORD /d 1 /f >nul 2>&1
  reg add %%K /v Version /t REG_SZ /d "4.0.30319" /f >nul 2>&1
  reg add %%K /v SP /t REG_DWORD /d 0 /f >nul 2>&1
  reg add %%K /v Servicing /t REG_DWORD /d 0 /f >nul 2>&1
  reg add %%K /v InstallPath /t REG_SZ /d "C:\windows\Microsoft.NET\Framework\v4.0.30319\" /f >nul 2>&1
)
call "%~f0" :has_dotnet4_runtime
if not errorlevel 1 exit /b 0
exit /b 1

:repair_dotnet_legacy_registry_from_files
call "%~f0" :init_env
call "%~f0" :has_dotnet_legacy_runtime
if not errorlevel 1 exit /b 0
call "%~f0" :has_dotnet_legacy_files
if errorlevel 1 exit /b 1
call "%~f0" :normalize_dotnet_install_roots
for %%K in (
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v2.0.50727"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v2.0.50727"
) do (
  reg add %%K /f >nul 2>&1
  reg add %%K /v Install /t REG_DWORD /d 1 /f >nul 2>&1
  reg add %%K /v Version /t REG_SZ /d "2.0.50727.5420" /f >nul 2>&1
  reg add %%K /v SP /t REG_DWORD /d 2 /f >nul 2>&1
  reg add %%K /v InstallPath /t REG_SZ /d "C:\windows\Microsoft.NET\Framework\v2.0.50727\" /f >nul 2>&1
)
for %%K in (
  "HKLM\Software\Microsoft\NET Framework Setup\NDP\v3.5"
  "HKLM\Software\Wow6432Node\Microsoft\NET Framework Setup\NDP\v3.5"
) do (
  reg add %%K /f >nul 2>&1
  reg add %%K /v Install /t REG_DWORD /d 1 /f >nul 2>&1
  reg add %%K /v Version /t REG_SZ /d "3.5.30729.01" /f >nul 2>&1
  reg add %%K /v SP /t REG_DWORD /d 1 /f >nul 2>&1
  reg add %%K /v InstallPath /t REG_SZ /d "C:\windows\Microsoft.NET\Framework\v2.0.50727\" /f >nul 2>&1
)
call "%~f0" :has_dotnet_legacy_runtime
if not errorlevel 1 exit /b 0
exit /b 1

:read_dll_override
call "%~f0" :init_env
set "PP_OVERRIDE_KEY=%~2"
if "%PP_OVERRIDE_KEY%"=="" exit /b 1
if not "%~3"=="" set "%~3="
if not "%~4"=="" set "%~4="
for /f "skip=2 tokens=1,2,*" %%A in ('reg query "HKCU\Software\Wine\DllOverrides" /v "%PP_OVERRIDE_KEY%" 2^>nul') do (
  if /I "%%~A"=="%PP_OVERRIDE_KEY%" (
    if not "%~3"=="" set "%~3=1"
    if not "%~4"=="" set "%~4=%%~C"
  )
)
exit /b 0

:capture_managed_override_snapshot
call "%~f0" :init_env
call "%~f0" :read_dll_override "mscoree" PREFIX_PACK_MONO_MSCOREE_PRESENT PREFIX_PACK_MONO_MSCOREE_VALUE
call "%~f0" :read_dll_override "mscoreei" PREFIX_PACK_MONO_MSCOREEI_PRESENT PREFIX_PACK_MONO_MSCOREEI_VALUE
call "%~f0" :read_dll_override "mscorlib" PREFIX_PACK_MONO_MSCORLIB_PRESENT PREFIX_PACK_MONO_MSCORLIB_VALUE
call "%~f0" :read_dll_override "mscorwks" PREFIX_PACK_MONO_MSCORWKS_PRESENT PREFIX_PACK_MONO_MSCORWKS_VALUE
exit /b 0

:set_managed_override_state
call "%~f0" :init_env
set "PP_OVERRIDE_VALUE=%~2"
if "%PP_OVERRIDE_VALUE%"=="" exit /b 1
for /L %%P in (1,1,2) do (
  for %%K in (mscoree mscoreei mscorlib mscorwks) do (
    reg add "HKCU\Software\Wine\DllOverrides" /v "%%~K" /t REG_SZ /d "%PP_OVERRIDE_VALUE%" /f >nul 2>&1
  )
  call "%~f0" :wait_short
)
exit /b 0

:restore_managed_override_snapshot
call "%~f0" :init_env
call "%~f0" :restore_dll_override "mscoree" "%PREFIX_PACK_MONO_MSCOREE_PRESENT%" "%PREFIX_PACK_MONO_MSCOREE_VALUE%"
call "%~f0" :restore_dll_override "mscoreei" "%PREFIX_PACK_MONO_MSCOREEI_PRESENT%" "%PREFIX_PACK_MONO_MSCOREEI_VALUE%"
call "%~f0" :restore_dll_override "mscorlib" "%PREFIX_PACK_MONO_MSCORLIB_PRESENT%" "%PREFIX_PACK_MONO_MSCORLIB_VALUE%"
call "%~f0" :restore_dll_override "mscorwks" "%PREFIX_PACK_MONO_MSCORWKS_PRESENT%" "%PREFIX_PACK_MONO_MSCORWKS_VALUE%"
exit /b 0

:restore_dll_override
call "%~f0" :init_env
set "PP_OVERRIDE_KEY=%~2"
set "PP_OVERRIDE_PRESENT=%~3"
set "PP_OVERRIDE_VALUE=%~4"
if "%PP_OVERRIDE_KEY%"=="" exit /b 1
for /L %%P in (1,1,2) do (
  if /I "%PP_OVERRIDE_PRESENT%"=="1" (
    reg add "HKCU\Software\Wine\DllOverrides" /v "%PP_OVERRIDE_KEY%" /t REG_SZ /d "%PP_OVERRIDE_VALUE%" /f >nul 2>&1
  ) else (
    reg delete "HKCU\Software\Wine\DllOverrides" /v "%PP_OVERRIDE_KEY%" /f >nul 2>&1
  )
  call "%~f0" :wait_short
)
exit /b 0

:launch_exe
call "%~f0" :init_env
set "PP_TARGET=%~2"
set "PP_LABEL=%~3"
set "PP_FILE=%~4"
set "PP_ARGS=%~5"
set "PP_LOG=%~6"
set "PP_DETAIL=%~7"
if "%PP_FILE%"=="" exit /b 1
call "%~f0" :file_nonempty "%PP_FILE%"
if errorlevel 1 exit /b 1
if "%PP_LOG%"=="" exit /b 1
for %%I in ("%PP_FILE%") do set "PP_WORKDIR=%%~dpI"
for %%I in ("%PP_FILE%") do set "PP_EXT=%%~xI"
call "%~f0" :mark_state "%PP_TARGET%" interactive 0 "%PP_LOG%" "%PP_DETAIL%"
> "%PP_LOG%" echo [%DATE% %TIME%] launching %PP_LABEL%
>> "%PP_LOG%" echo file=%PP_FILE%
if not "%PP_ARGS%"=="" >> "%PP_LOG%" echo args=%PP_ARGS%
if defined PP_WORKDIR >> "%PP_LOG%" echo cwd=%PP_WORKDIR%
if defined PP_WORKDIR pushd "%PP_WORKDIR%"
if /I "%PP_EXT%"==".cmd" (
  if "%PP_ARGS%"=="" (
    call "%PP_FILE%" >> "%PP_LOG%" 2>&1
  ) else (
    call "%PP_FILE%" %PP_ARGS% >> "%PP_LOG%" 2>&1
  )
) else if /I "%PP_EXT%"==".bat" (
  if "%PP_ARGS%"=="" (
    call "%PP_FILE%" >> "%PP_LOG%" 2>&1
  ) else (
    call "%PP_FILE%" %PP_ARGS% >> "%PP_LOG%" 2>&1
  )
) else if /I "%PP_EXT%"==".vbs" (
  "%PREFIX_PACK_WSCRIPT_EXE%" "%PP_FILE%" >> "%PP_LOG%" 2>&1
) else if /I "%PP_EXT%"==".js" (
  "%PREFIX_PACK_WSCRIPT_EXE%" "%PP_FILE%" >> "%PP_LOG%" 2>&1
) else (
  >> "%PP_LOG%" echo launch_mode=direct_exec
  if "%PP_ARGS%"=="" (
    "%PP_FILE%" >> "%PP_LOG%" 2>&1
  ) else (
    "%PP_FILE%" %PP_ARGS% >> "%PP_LOG%" 2>&1
  )
)
set "PP_RC=%ERRORLEVEL%"
if defined PP_WORKDIR popd
>> "%PP_LOG%" echo exit_code=%PP_RC%
exit /b %PP_RC%

:spawn_exe
call "%~f0" :init_env
set "PP_TARGET=%~2"
set "PP_LABEL=%~3"
set "PP_FILE=%~4"
set "PP_ARGS=%~5"
set "PP_LOG=%~6"
set "PP_DETAIL=%~7"
if "%PP_FILE%"=="" exit /b 1
call "%~f0" :file_nonempty "%PP_FILE%"
if errorlevel 1 exit /b 1
if "%PP_LOG%"=="" exit /b 1
for %%I in ("%PP_FILE%") do set "PP_WORKDIR=%%~dpI"
for %%I in ("%PP_FILE%") do set "PP_EXT=%%~xI"
call "%~f0" :mark_state "%PP_TARGET%" queued 0 "%PP_LOG%" "%PP_DETAIL%"
> "%PP_LOG%" echo [%DATE% %TIME%] spawning %PP_LABEL%
>> "%PP_LOG%" echo file=%PP_FILE%
if not "%PP_ARGS%"=="" >> "%PP_LOG%" echo args=%PP_ARGS%
if defined PP_WORKDIR >> "%PP_LOG%" echo cwd=%PP_WORKDIR%
set "PP_SPAWN_COMMAND="
if /I "%PP_EXT%"==".vbs" (
  set "PP_SPAWN_COMMAND=""%PREFIX_PACK_WSCRIPT_EXE%"" ""%PP_FILE%"""
  if not "%PP_ARGS%"=="" set "PP_SPAWN_COMMAND=%PP_SPAWN_COMMAND% %PP_ARGS%"
) else if /I "%PP_EXT%"==".cmd" (
  set "PP_SPAWN_COMMAND=""%PREFIX_PACK_CMD_EXE%"" /c call ""%PP_FILE%"""
  if not "%PP_ARGS%"=="" set "PP_SPAWN_COMMAND=%PP_SPAWN_COMMAND% %PP_ARGS%"
) else if /I "%PP_EXT%"==".bat" (
  set "PP_SPAWN_COMMAND=""%PREFIX_PACK_CMD_EXE%"" /c call ""%PP_FILE%"""
  if not "%PP_ARGS%"=="" set "PP_SPAWN_COMMAND=%PP_SPAWN_COMMAND% %PP_ARGS%"
) else if /I "%PP_EXT%"==".js" (
  set "PP_SPAWN_COMMAND=""%PREFIX_PACK_WSCRIPT_EXE%"" ""%PP_FILE%"""
  if not "%PP_ARGS%"=="" set "PP_SPAWN_COMMAND=%PP_SPAWN_COMMAND% %PP_ARGS%"
) else if /I "%PP_EXT%"==".msi" (
  set "PP_SPAWN_COMMAND=""%PREFIX_PACK_MSIEXEC_EXE%"" /i ""%PP_FILE%"""
  if not "%PP_ARGS%"=="" set "PP_SPAWN_COMMAND=%PP_SPAWN_COMMAND% %PP_ARGS%"
) else (
  set "PP_SPAWN_COMMAND=""%PP_FILE%"""
  if not "%PP_ARGS%"=="" set "PP_SPAWN_COMMAND=%PP_SPAWN_COMMAND% %PP_ARGS%"
)
if "%PP_SPAWN_COMMAND%"=="" exit /b 1
set "PP_ESCAPED_SPAWN_COMMAND=%PP_SPAWN_COMMAND:"=""%"
set "PP_SPAWN_SCRIPT=%PREFIX_PACK_STAGE_ROOT%\spawn-%PP_TARGET%.vbs"
> "%PP_SPAWN_SCRIPT%" echo Option Explicit
>> "%PP_SPAWN_SCRIPT%" echo Dim shell
>> "%PP_SPAWN_SCRIPT%" echo Set shell = CreateObject("WScript.Shell")
if defined PP_WORKDIR (
  set "PP_ESCAPED_WORKDIR=%PP_WORKDIR:"=""%"
  >> "%PP_SPAWN_SCRIPT%" echo shell.CurrentDirectory = "%PP_ESCAPED_WORKDIR%"
)
>> "%PP_SPAWN_SCRIPT%" echo shell.Run "%PP_ESCAPED_SPAWN_COMMAND%", 0, False
"%PREFIX_PACK_WSCRIPT_EXE%" "%PP_SPAWN_SCRIPT%" >> "%PP_LOG%" 2>&1
set "PP_RC=%ERRORLEVEL%"
>> "%PP_LOG%" echo spawn_mode=wscript_shell_run
>> "%PP_LOG%" echo dispatch_rc=%PP_RC%
exit /b %PP_RC%

:install_msi
call "%~f0" :init_env
set "PP_TARGET=%~2"
set "PP_LABEL=%~3"
set "PP_FILE=%~4"
set "PP_ARGS=%~5"
set "PP_LOG=%~6"
set "PP_DETAIL=%~7"
if "%PP_FILE%"=="" exit /b 1
call "%~f0" :file_nonempty "%PP_FILE%"
if errorlevel 1 exit /b 1
if "%PP_LOG%"=="" exit /b 1
for %%I in ("%PP_FILE%") do set "PP_WORKDIR=%%~dpI"
call "%~f0" :mark_state "%PP_TARGET%" running 0 "%PP_LOG%" "%PP_DETAIL%"
> "%PP_LOG%" echo [%DATE% %TIME%] installing %PP_LABEL%
>> "%PP_LOG%" echo msi=%PP_FILE%
if not "%PP_ARGS%"=="" >> "%PP_LOG%" echo args=%PP_ARGS%
if defined PP_WORKDIR pushd "%PP_WORKDIR%"
if "%PP_ARGS%"=="" (
  "%PREFIX_PACK_MSIEXEC_EXE%" /i "%PP_FILE%" /l*v "%PP_LOG%"
) else (
  "%PREFIX_PACK_MSIEXEC_EXE%" /i "%PP_FILE%" %PP_ARGS% /l*v "%PP_LOG%"
)
set "PP_RC=%ERRORLEVEL%"
if defined PP_WORKDIR popd
exit /b %PP_RC%
