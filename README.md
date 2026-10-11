# Expense Tracker (Java, command line)

[![CI](https://github.com/satwiksharma01/expense-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/satwiksharma01/expense-tracker/actions/workflows/ci.yml)

A personal finance manager that runs in the terminal. You record your daily expenses. The app then:

- tracks them against monthly budgets and warns you before you overspend
- turns recurring payments (rent, subscriptions) into expenses automatically
- produces monthly reports with ASCII charts, a month-end projection and plain-language insights

It is written in plain Java 17 with **no external libraries**, so the only thing you need is a JDK.

> Problem statement, scope and target users: [statement.md](statement.md)
> Architecture, UML diagrams and storage design: [docs/DESIGN.md](docs/DESIGN.md)

Contribution workflow: [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Features

| Module | What it does |
|---|---|
| **Expense management** | Add, view, edit and delete expenses (amount, category, date, payment method, description). Search by date range, category, payment method, amount range and keyword. Sort by date, amount or category. |
| **Budgets & alerts** | Set a monthly limit overall and/or per category. See a status table with progress bars. You get an alert when an expense takes a budget past 80% or over 100%. |
| **Reports & insights** | Monthly report: total, daily average, projected month-end total, largest expense, spending by category and payment method (bar charts), budget status. Plus a multi-month trend chart, a category breakdown for any date range, and the top expenses. It also generates insights automatically, e.g. *"Spending is up 12% vs last month"* or *"At this pace you will overshoot your budget by Rs. 1,150"*. |
| **Recurring expenses** | Daily/weekly/monthly/yearly schedules. Due payments are added on start-up, including any missed while the app was closed. Generation never creates duplicates, and month-end dates don't drift (31 Jan → 28 Feb → 31 Mar). Schedules can be paused and resumed. |
| **Import / export** | Export all expenses or one month to CSV (opens in Excel/Sheets). Import from CSV with row-by-row validation and line-numbered error messages. Save a monthly report as a text file. |
| **Command-line mode** | `list` and `report` commands print results and exit, so the app can be scripted without the menu. |

## Technologies used

- **Java 17** (records, enums, switch expressions, streams, generics, `java.time`, `BigDecimal`, `java.nio.file`, `java.util.logging`, annotations and reflection)
- **CSV files** for persistence, with no database server to install
- **Custom test runner** (annotation + reflection based), so tests run without JUnit or internet access
- **Git** for version control; shell (`.sh`) and batch (`.bat`) scripts for building

## Project structure

```
expense-tracker/
├── src/main/java/com/expensetracker/
│   ├── App.java                 entry point, wires everything together
│   ├── AppOptions.java          command-line argument parsing
│   ├── DemoDataSeeder.java      optional sample data (--demo)
│   ├── model/                   Expense, Budget, RecurringExpense, Category, PaymentMethod, Frequency
│   ├── repository/              Repository<K,T> interface, CsvRepository, CSV mappers
│   ├── service/                 ExpenseService, BudgetService, ReportService,
│   │                            RecurringExpenseService, ImportExportService, ExpenseFilter
│   ├── ui/                      ConsoleMenu, InputReader, ConsoleFormatter
│   ├── util/                    Validator, Money, CsvUtil, EnumParser, AppLogger
│   └── exception/               ValidationException, NotFoundException, StorageException
├── src/test/java/com/expensetracker/   52 unit tests + the test runner
├── samples/sample-import.csv    example file for the import feature
├── docs/DESIGN.md               architecture and UML diagrams
├── build.sh / build.bat         compile + package the jar
├── run.sh / run.bat             build if needed, then start the app
├── test.sh / test.bat           compile and run the unit tests
├── statement.md                 problem statement
└── README.md
```

---

## Setup

### 1. Install a JDK (version 17 or newer)

Check whether you already have one:

```bash
java -version
javac -version
```

Both commands must print version 17 or higher. If they don't, install a JDK:

- **Windows:** `winget install EclipseAdoptium.Temurin.21.JDK` (or the installer from https://adoptium.net)
- **macOS:** `brew install openjdk@21`
- **Ubuntu/Debian:** `sudo apt install openjdk-21-jdk`

Open a new terminal afterwards so `java` and `javac` are on your `PATH`.

### 2. Get the code

```bash
git clone https://github.com/satwiksharma01/expense-tracker.git
cd expense-tracker
```

There are no dependencies to download and no configuration files to edit.

### 3. Build

| Linux / macOS | Windows (Command Prompt or PowerShell) |
|---|---|
| `./build.sh` | `build.bat` (PowerShell: `.\build.bat`) |

This compiles the sources into `out/classes` and creates `expense-tracker.jar`.

<details>
<summary>Building without the scripts</summary>

```bash
javac --release 17 -d out/classes -sourcepath src/main/java src/main/java/com/expensetracker/App.java
jar --create --file expense-tracker.jar --main-class com.expensetracker.App -C out/classes .
```
</details>

### 4. Run

Start the interactive menu **with sample data** so every screen has something to show (recommended the first time):

```bash
java -jar expense-tracker.jar --demo
```

Or start with an empty tracker:

```bash
java -jar expense-tracker.jar
```

`./run.sh` / `run.bat` do the same thing and build the jar first if it is missing, e.g. `./run.sh --demo`.

---

## Usage

### Interactive menu

```
=== Main menu ===   (87 expenses recorded, today is 2026-09-18)
  1. Add expense
  2. View / search expenses
  3. Edit expense
  4. Delete expense
  5. Budgets
  6. Reports & insights
  7. Recurring expenses
  8. Import / export
  0. Exit
```

Type the number of an option and press Enter. Some tips:

- Choices can be typed as a number (`1`), a name (`food`) or a label (`Credit Card`).
- Press **Enter** on a prompt that shows `[brackets]` to accept the value in brackets. When editing, that keeps the current value.
- Dates accept `2026-09-18`, `18-09-2026`, `18/9/2026`, `today` or `yesterday`.
- Amounts accept `250`, `1,250.50` or `Rs. 99`.
- Invalid input is never saved. You get a message explaining why and are asked again.

Every change is saved to disk immediately, so there is no "save" step. Closing the terminal loses nothing.

### Command-line commands

```bash
java -jar expense-tracker.jar --help              # usage
java -jar expense-tracker.jar report              # this month's report, then exit
java -jar expense-tracker.jar report 2026-08      # report for August 2026
java -jar expense-tracker.jar list 2026-09        # table of September's expenses
java -jar expense-tracker.jar --data-dir ~/money  # keep data somewhere else
```

| Option | Meaning |
|---|---|
| `--data-dir <dir>` | Folder for data files and logs (default `./data`) |
| `--demo` | Load about 3 months of sample expenses, 5 budgets and 3 recurring payments if the tracker is empty |
| `-h`, `--help` | Show usage |
| `-v`, `--version` | Show version |

### Trying the import feature

Choose `8. Import / export` → `2. Import expenses from CSV` and enter `samples/sample-import.csv`. Five rows are imported. Two rows are rejected on purpose (a bad amount and an unknown category), and the app reports their line numbers.

### Where data is stored

```
data/
├── expenses.csv     id,date,amount,category,payment_method,description
├── budgets.csv      scope,monthly_limit
├── recurring.csv    id,description,amount,category,payment_method,frequency,start_date,occurrences,active
├── exports/         CSV exports and saved reports
└── logs/app0.log    application log (rotates at 1 MB, keeps 3 files)
```

The files are plain CSV and can be opened in any spreadsheet. If a row is damaged by hand-editing, it is skipped and logged, and the rest of the data still loads.

---

## Running the tests

| Linux / macOS | Windows |
|---|---|
| `./test.sh` | `test.bat` |

Expected output (abridged):

```
CsvUtilTest
  PASS  neutralizesSpreadsheetFormulas
  ...
RecurringExpenseServiceTest
  PASS  monthEndScheduleDoesNotDrift
  ...
Tests run: 52, passed: 52, failed: 0 (301 ms)
```

The script exits with code 0 when every test passes and 1 otherwise. The tests cover:

- validation rules
- CSV escaping and corrupt-file recovery
- CRUD and search
- budget thresholds
- report maths (totals, projections, trends, insights)
- recurring-payment generation (catch-up, idempotency, month-end dates, pausing)
- import/export round trips and formula-injection protection

They use a fixed clock, so results don't depend on the day they are run.

---

## Sample output

`java -jar expense-tracker.jar --demo report` (abridged):

```
  Monthly report - September 2026
  ===============================
  Total spent:           Rs. 15,694.00
  Transactions:          17
  Daily average:         Rs. 871.89
  Projected month-end:   Rs. 20,544.67
  Largest expense:       Rs. 8,000.00 - Hostel rent (recurring) (2026-09-01)

  Spending by category
  Rent & Housing    ##############################      Rs. 8,000.00   51.0%
  Food & Dining     ################                    Rs. 4,222.00   26.9%
  Education         #####                               Rs. 1,406.00    9.0%
  Entertainment     ####                                Rs. 1,020.00    6.5%

  Budgets
  Budget                         Limit             Spent         Remaining  Used                   Status
  Overall                Rs. 25,000.00     Rs. 15,694.00      Rs. 9,306.00  [########....] 62.8%   OK
  Food & Dining           Rs. 6,000.00      Rs. 4,222.00      Rs. 1,778.00  [########....] 70.4%   OK
  Entertainment           Rs. 1,200.00      Rs. 1,020.00        Rs. 180.00  [##########..] 85.0%   WARNING

  Insights
   * Spending is down 44.5% compared with 2026-08 (Rs. 28,262.00).
   * Rent & Housing is your biggest category at 51.0% of spending.
   * Education grew the most: +Rs. 138.00 vs last month.
   * Weekend spending: 9.5% of the month's total.
   * At this pace you will stay within your overall budget (Rs. 4,455.33 to spare).
```

(The demo data is generated relative to today's date, so your numbers will differ.)

## License

[MIT](LICENSE).

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `javac: command not found` / `'javac' is not recognized` | Install a JDK (not just a JRE) and open a new terminal. |
| `UnsupportedClassVersionError` | Your `java` is older than 17. Check `java -version`. |
| `./build.sh: Permission denied` | Run `chmod +x *.sh`, or use `sh build.sh`. |
| Demo data didn't load | `--demo` only seeds an empty tracker. Use a fresh folder: `--demo --data-dir demo-data`. |
