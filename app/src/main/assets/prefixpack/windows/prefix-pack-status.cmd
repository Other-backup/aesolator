@echo off
setlocal EnableExtensions

if not defined PREFIX_PACK_SCRIPT_DIR set "PREFIX_PACK_SCRIPT_DIR=%~dp0"

call "%PREFIX_PACK_SCRIPT_DIR%prefix-pack-common.cmd" :init_env
set "ROOT=%PREFIX_PACK_ROOT%"
set "CACHE=%PREFIX_PACK_CACHE_ROOT%"

echo Prefix Pack Toolkit Root:
echo   %ROOT%
echo.
echo Backend Cache Root:
echo   %CACHE%
echo.
echo Installer Cache Root:
echo   %PREFIX_PACK_INSTALLER_CACHE%
echo.
echo Stage Root:
echo   %PREFIX_PACK_STAGE_ROOT%
echo.
echo Save Root:
echo   %PREFIX_PACK_SAVE_ROOT%
echo.
echo Log Root:
echo   %PREFIX_PACK_LOG_ROOT%
echo.
echo State Root:
echo   %PREFIX_PACK_STATE_ROOT%
echo.

if exist "%ROOT%\VERSION" (
  echo Toolkit Version:
  type "%ROOT%\VERSION"
) else (
  echo Toolkit Version:
  echo   missing
)

echo.
echo Cache Status:
echo.

for /f "usebackq eol=# tokens=1-9 delims=	" %%A in ("%ROOT%\catalog.tsv") do (
  call :check_entry "%%~A" "%%~B" "%%~C" "%%~D" "%%~E" "%%~F" "%%~G" "%%~H" "%%~I"
)

echo.
echo Install States:
for %%F in ("%PREFIX_PACK_STATE_ROOT%\*.properties") do (
  if exist "%%~fF" (
    echo.
    echo   [state] %%~nxF
    type "%%~fF"
  )
)

echo.
echo Raw Catalog:
type "%ROOT%\catalog.tsv"
echo.
exit /b 0

:check_entry
if exist "%CACHE%\%~2" (
  for %%F in ("%CACHE%\%~2") do (
    if %%~zF GTR 0 (
      set "BACKEND_STATE=present"
    ) else (
      set "BACKEND_STATE=empty"
    )
  )
) else (
  set "BACKEND_STATE=missing"
)
if exist "%PREFIX_PACK_INSTALLER_CACHE%\%~2" (
  for %%F in ("%PREFIX_PACK_INSTALLER_CACHE%\%~2") do (
    if %%~zF GTR 0 (
      set "INSTALLER_STATE=present"
    ) else (
      set "INSTALLER_STATE=empty"
    )
  )
) else (
  set "INSTALLER_STATE=missing"
)
echo   [%BACKEND_STATE%] %~1
echo     file: %~2
echo     backend cache: %CACHE%\%~2 [%BACKEND_STATE%]
echo     installer cache: %PREFIX_PACK_INSTALLER_CACHE%\%~2 [%INSTALLER_STATE%]
echo     install: %~8
echo     download: %~4
echo     source page: %~7
exit /b 0
