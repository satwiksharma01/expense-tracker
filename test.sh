#!/usr/bin/env sh
# Compiles the application together with the unit tests and runs them.
# Exit code is 0 when every test passes, 1 otherwise.
set -e
cd "$(dirname "$0")"

rm -rf out/test-classes
mkdir -p out/test-classes
javac --release 17 -encoding UTF-8 -d out/test-classes \
      -sourcepath "src/main/java:src/test/java" src/test/java/com/expensetracker/testing/TestRunner.java
java -cp out/test-classes com.expensetracker.testing.TestRunner
