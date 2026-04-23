@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env
set "ROOT=%PREFIX_PACK_ROOT%"
set "CACHE=%PREFIX_PACK_CACHE_ROOT%"

if "%~1"=="" goto :help
if /I "%~1"=="help" goto :help
if /I "%~1"=="status" goto :status
if /I "%~1"=="links" goto :links
if /I "%~1"=="install" goto :install

echo Unknown prefix-pack command: %~1
echo.
goto :help

:status
call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-status.cmd"
exit /b %ERRORLEVEL%

:links
set "FILTER=%~2"
set "MATCHED="
for /f "usebackq eol=# tokens=1-9 delims=	" %%A in ("%ROOT%\catalog.tsv") do (
  if not defined FILTER (
    call :print_links "%%~A" "%%~B" "%%~C" "%%~D" "%%~E" "%%~F" "%%~G" "%%~H" "%%~I"
  ) else (
    if /I "%%~A"=="%FILTER%" call :print_links "%%~A" "%%~B" "%%~C" "%%~D" "%%~E" "%%~F" "%%~G" "%%~H" "%%~I"
  )
)
if defined FILTER if not defined MATCHED (
  echo No catalog entry matched: %FILTER%
  exit /b 1
)
exit /b 0

:install
if "%~2"=="" goto :help

if /I "%~2"=="default" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-default-runtime-pack.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="vcrun_full" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-vcrun-full.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="vcpp_aio" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-vcpp-aio.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="vcpp" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-vcpp-aio.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="vcrun" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-vcrun-full.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="vcrun6" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-vcrun6.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="vcrun6sp6" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-vcrun6.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="wine_web_stack" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-wine-web-stack.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="wine_mono_11_0_0" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-wine-web-stack.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="wine_gecko_2_47_4_x86" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-wine-web-stack.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="wine_gecko_2_47_4_x86_64" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-wine-web-stack.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="dotnet_framework" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-dotnet-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="dotnet" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-dotnet-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="dotnetfx35sp1" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-dotnet-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="dotnetfx40_full" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-dotnet-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="dotnetfx48" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-dotnet-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="directx_jun2010" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-directx-jun2010.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="directx" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-directx-jun2010.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="xnafx40_refresh" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-xna-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="xnafx31_refresh" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-xna-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="xna" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-xna-framework.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="physx" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-physx-runtime.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="physx_runtime" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-physx-runtime.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="physx_system_9_21_0713" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-physx-runtime.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="physx_legacy_9_13_0604" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-physx-runtime.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="lavfilters" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-lavfilters.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="lavfilters_0_81" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-lavfilters.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="openal" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-openal.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="openal_1_1" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-openal.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="legacy_dx_sdk" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-directx-sdk-tools.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="dxsdk_jun10" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-directx-sdk-tools.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="graphics_diag" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-glview.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="glview" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-glview.cmd"
  exit /b %ERRORLEVEL%
)
if /I "%~2"=="glview_6499" (
  call "%PREFIX_PACK_SCRIPT_DIR%install-glview.cmd"
  exit /b %ERRORLEVEL%
)

echo Unknown install target: %~2
echo.
goto :help

:print_links
set "MATCHED=1"
echo id=%~1
echo   file=%~2
echo   mode=%~3
echo   install_group=%~5
echo   source_label=%~6
echo   source_page=%~7
echo   install_cmd=%~8
echo   download_url=%~4
echo   backend_cache=%CACHE%\%~2
echo   installer_cache=%PREFIX_PACK_INSTALLER_CACHE%\%~2
echo.
exit /b 0

:help
echo Prefix Pack Loader
echo.
echo Usage:
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd status
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd links [entry_id]
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install default
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install vcrun_full
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install vcpp_aio
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install vcrun6
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install wine_web_stack
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install dotnet_framework
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install directx_jun2010
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install xna
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install physx
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install lavfilters
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install openal
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install legacy_dx_sdk
echo   Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd install glview
echo.
echo Installer cache:
echo   %PREFIX_PACK_INSTALLER_CACHE%
echo Stage root:
echo   %PREFIX_PACK_STAGE_ROOT%
echo.
echo Save root:
echo   %PREFIX_PACK_SAVE_ROOT%
echo Log root:
echo   %PREFIX_PACK_LOG_ROOT%
echo State root:
echo   %PREFIX_PACK_STATE_ROOT%
exit /b 1
