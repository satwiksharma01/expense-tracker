# Contributing

Use JDK 17 or newer; no dependency manager or external test library is needed. Read the [design document](docs/DESIGN.md) for the service/storage boundaries.

Run `sh test.sh` and `sh build.sh` on Linux/macOS, or `test.bat` and `build.bat` on Windows. Then run `java -jar expense-tracker.jar --help`. CI checks Java 17 and 21.

For bugs, add a regression case under `src/test/java/com/expensetracker/` and use the existing annotation-based runner. Date-sensitive tests should use a fixed clock; storage tests should use a temporary directory. Check the existing runner when adding a new test class: it maintains an explicit class list.

Explain the problem, behavior change and checks performed in your pull request. Preserve CSV compatibility, decimal money handling, atomic write behavior and recurring-payment idempotency. Avoid committing real expense data, exports or logs. Contributions use MIT.
