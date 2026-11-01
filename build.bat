@echo off
REM Butuh JDK 25 + Gradle 9.5.1+. Pemakaian: build.bat [mc_version] [fabric_api_version]
if "%~2"=="" (gradle build) else (gradle build -Pminecraft_version=%1 -Pfabric_api_version=%2)
echo Selesai. Jar ada di build\libs\ (ambil yang tanpa -sources).
pause
