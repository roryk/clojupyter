@echo off
set KDIR=%PREFIX%\share\jupyter\kernels\%PKG_NAME%-%PKG_VERSION%
set LIBDIR=%LIBRARY_PREFIX%\%PKG_NAME%-%PKG_VERSION%

mkdir %KDIR%
if %errorlevel% neq 0 (
   echo Mkdir of kernel dir failed with exit code %errorlevel%
   exit /b %errorlevel%
)

mkdir %LIBDIR%
if %errorlevel% neq 0 (
   echo Mkdir of lib dir failed with exit code %errorlevel%
   exit /b %errorlevel%
)

copy /B %SRC_DIR%\%PKG_NAME%-%PKG_VERSION%.jar %LIBDIR%
if %errorlevel% neq 0 (
   echo Jarfile copy failed with exit code %errorlevel%
   exit /b %errorlevel%
)

xcopy /E /I %SRC_DIR%\lib %LIBDIR%\lib
if %errorlevel% neq 0 (
   echo Lib dir copy failed with exit code  %errorlevel%
   exit /b %errorlevel%
)

copy /B %SRC_DIR%\logo-64x64.png %KDIR%
if %errorlevel% neq 0 (
   echo Logofile copy failed with exit code  %errorlevel%
   exit /b %errorlevel%
)

powershell "%RECIPE_DIR%\..\bin\kernel.json.ps1 -identity %PGK_NAME%-%PKG_VERSION% ..\..\..\..\..\Library | Out-File %KDIR%\kernel.json"
if %errorlevel% neq 0 (
   echo kernel.json failed with exit code  %errorlevel%
   exit /b %errorlevel%
)
