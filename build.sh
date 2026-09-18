#!/usr/bin/env sh
# Compiles the application and packages it as expense-tracker.jar. Requires JDK 17 or newer.
set -e
cd "$(dirname "$0")"

rm -rf out/classes
mkdir -p out/classes
javac --release 17 -encoding UTF-8 -d out/classes \
      -sourcepath src/main/java src/main/java/com/expensetracker/App.java
jar --create --file expense-tracker.jar --main-class com.expensetracker.App -C out/classes .
echo "Built expense-tracker.jar"
