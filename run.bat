@echo off
rem Builds the jar if needed, then starts the application. Arguments are passed through,
rem e.g.  run.bat --demo   or   run.bat report 2026-09
setlocal
cd /d "%~dp0"

if not exist expense-tracker.jar (
    call build.bat || exit /b 1
)
java -jar expense-tracker.jar %*
