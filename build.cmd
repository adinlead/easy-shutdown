@echo off
rem EasyShutdown Windows build script: compile and package fat-jar
rem Artifact: target\EasyShutdown-1.0-SNAPSHOT.jar (double-click to run, requires JRE 17+)

setlocal
cd /d "%~dp0"

rem --- locate java: PATH first, then JAVA_HOME fallback ---
where java >nul 2>nul
if not errorlevel 1 goto java-ok
if not defined JAVA_HOME goto no-java
if not exist "%JAVA_HOME%\bin\java.exe" goto no-java
set "PATH=%JAVA_HOME%\bin;%PATH%"

:java-ok
echo [1/2] Cleaning previous build...
call mvnw.cmd -q clean
if errorlevel 1 exit /b 1

echo [2/2] Compiling and packaging...
call mvnw.cmd -q -DskipTests package
if errorlevel 1 (
    echo [ERROR] Build failed.
    exit /b 1
)

echo.
echo [OK] Build succeeded: target\EasyShutdown-1.0-SNAPSHOT.jar
endlocal
exit /b 0

:no-java
echo [ERROR] java not found in PATH and JAVA_HOME is not set correctly.
echo         Install JDK 17+ first.
exit /b 1
