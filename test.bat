@echo off
rem Compiles the application together with the unit tests and runs them.
rem Exit code is 0 when every test passes, 1 otherwise.
setlocal
cd /d "%~dp0"

if exist out\test-classes rmdir /s /q out\test-classes
mkdir out\test-classes
javac --release 17 -encoding UTF-8 -d out\test-classes -sourcepath "src\main\java;src\test\java" src\test\java\com\expensetracker\testing\TestRunner.java || exit /b 1
java -cp out\test-classes com.expensetracker.testing.TestRunner
