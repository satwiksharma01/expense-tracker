@echo off
rem Compiles the application and packages it as expense-tracker.jar. Requires JDK 17 or newer.
setlocal
cd /d "%~dp0"

if exist out\classes rmdir /s /q out\classes
mkdir out\classes
javac --release 17 -encoding UTF-8 -d out\classes -sourcepath src\main\java src\main\java\com\expensetracker\App.java || exit /b 1
jar --create --file expense-tracker.jar --main-class com.expensetracker.App -C out\classes . || exit /b 1
echo Built expense-tracker.jar
