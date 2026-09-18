# Problem Statement: Expense Tracker

## Problem statement

Students and young professionals often have little idea where their money goes. They make many
small payments every day (canteen food, auto rides, recharges, subscriptions) spread across cash,
UPI and cards, so no single statement shows the whole picture. Fixed costs like rent quietly
repeat every month. By the time they notice that a month has gone badly, it is too late to adjust.

Spreadsheets need discipline and formulas. Banking apps only see one account. Most budgeting apps
need an account, an internet connection and share personal financial data with a third party.

**The problem:** there is no simple, private and offline tool that records expenses from every
payment method in one place, shows spending against a budget *while the month is still in
progress*, and turns the raw numbers into something a person can act on.

## Objectives

1. Let a user record, correct and search expenses quickly from the command line.
2. Let a user set monthly budgets and warn them as soon as spending approaches or exceeds a limit.
3. Automate repeating payments so fixed costs are never forgotten or entered twice.
4. Summarise spending as reports, charts, projections and plain-language insights.
5. Keep all data local, in open CSV files the user owns, with no installation beyond Java.
6. Apply core Java concepts in a real application: OOP, collections, generics, streams, exceptions,
   file I/O, `java.time`, enums/records, annotations and reflection. Keep it layered and testable.

## Scope

**In scope**

- Single-user, offline console application (interactive menu plus one-shot `list`/`report` commands)
- CRUD, search, filter and sort of expenses
- Monthly budgets (overall and per category) with threshold alerts
- Recurring expense schedules (daily, weekly, monthly, yearly) with automatic catch-up
- Monthly reports, trends, category breakdowns, month-end projection and insights
- CSV import/export and text report export
- Validation, error recovery, logging and automated unit tests

**Out of scope** (possible future work)

- Multiple users, authentication and cloud sync
- Graphical or web interface
- Automatic import from bank or UPI statements in their native formats
- Multiple currencies and exchange rates
- Income tracking and savings goals

## Target users

- **College students** managing a monthly allowance or stipend who want to stop running out of
  money before the month ends
- **Young working professionals** living on their own who pay rent, bills and subscriptions and
  want to see spending patterns
- **Privacy-conscious users** who do not want to link bank accounts to a third-party app
- **Developers and terminal users** who prefer fast keyboard-driven tools and plain-text data

## High-level features

| # | Feature | Description |
|---|---|---|
| 1 | Expense management | Add, list, edit and delete expenses; filter by date range, category, payment method, amount and keyword; sort by date, amount or category |
| 2 | Budgets & alerts | Monthly limits overall or per category; status table with progress bars; alerts at 80% and above 100% |
| 3 | Reports & insights | Monthly summary, category and payment-method charts, multi-month trend, top expenses, month-end projection and automatic insights |
| 4 | Recurring expenses | Schedules that generate expenses when due, catch up on missed periods and never duplicate |
| 5 | Import / export | CSV export and import with per-row validation; monthly report saved as text |
| 6 | Reliable local storage | Human-readable CSV files, atomic writes, recovery from damaged rows, rotating log file |

## Functional requirements

| ID | Requirement |
|---|---|
| FR1 | The user can add an expense with amount, category, date, payment method and description. |
| FR2 | The user can list all expenses, list the current month, and search using any combination of filters. |
| FR3 | The user can edit any field of an existing expense and delete an expense after confirmation. |
| FR4 | The user can set, change and remove a monthly budget for overall spending or for a category. |
| FR5 | The system shows each budget's limit, spent amount, remaining amount, percentage used and status. |
| FR6 | The system shows an alert when an added or edited expense takes a budget to 80% or more. |
| FR7 | The system produces a monthly report with totals, averages, projection, charts, budgets and insights. |
| FR8 | The user can define recurring expenses; due occurrences are added automatically and exactly once. |
| FR9 | The user can export expenses to CSV, import expenses from CSV, and save a report as a text file. |
| FR10 | The system can be used non-interactively via `list` and `report` commands. |

## Non-functional requirements

| ID | Category | Requirement and how it is met |
|---|---|---|
| NFR1 | **Reliability** | No data loss on crash: every change is written to a temporary file that atomically replaces the data file. If a write fails, the in-memory change is rolled back. Damaged rows are skipped and logged instead of stopping start-up. |
| NFR2 | **Usability** | Numbered menus; choices accepted as number, name or label; sensible defaults on Enter; flexible date and amount formats; every invalid input explained and re-prompted. |
| NFR3 | **Performance** | In-memory index gives O(1) lookup by id. Filtering, sorting and reports are single stream passes over the data. All 52 tests run in well under a second. |
| NFR4 | **Security & data integrity** | Data stays on the user's machine. All input is validated (positive amounts ≤ Rs. 1 crore, max 2 decimals, no future dates, no control characters). Exports are protected against CSV formula injection. Import size is capped at 5 MB. `BigDecimal` avoids floating-point rounding errors. |
| NFR5 | **Maintainability** | Layered architecture (UI → service → repository → model). Services depend on a generic `Repository` interface. The clock is injected. The code is organised into small single-purpose classes in 7 packages. |
| NFR6 | **Logging & monitoring** | `java.util.logging` writes to a rotating log file (1 MB × 3), recording every create/update/delete, import/export, skipped row and unexpected error. |
| NFR7 | **Portability** | Pure Java 17 with no external libraries. Runs identically on Windows, macOS and Linux. Build and run scripts provided for both shells. |
| NFR8 | **Testability** | 52 automated unit tests, run with one command. A fixed clock makes date-dependent logic deterministic. |
