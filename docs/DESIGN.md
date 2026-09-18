# Design Document: Expense Tracker

This document describes the architecture, the main design decisions and the UML diagrams of the
application. The diagrams are written in Mermaid, which GitHub renders directly in the browser.

## 1. System architecture

The application uses a **layered architecture**. Each layer only calls the layer below it, so
the console UI could be replaced (for example by a web UI) without changing business logic. The
CSV storage could likewise be replaced by a database without touching the services.

```mermaid
flowchart TB
    User(["User at the terminal"])

    subgraph Presentation["Presentation layer (ui)"]
        Menu["ConsoleMenu<br/>menus and workflows"]
        Input["InputReader<br/>prompting + re-asking on bad input"]
        Fmt["ConsoleFormatter<br/>tables and ASCII charts"]
    end

    subgraph Entry["Entry point"]
        App["App + AppOptions<br/>argument parsing, dependency wiring"]
    end

    subgraph Service["Service layer (business logic)"]
        ES["ExpenseService"]
        BS["BudgetService"]
        RS["ReportService"]
        RES["RecurringExpenseService"]
        IES["ImportExportService"]
    end

    subgraph Persistence["Persistence layer (repository)"]
        RepoI["Repository&lt;K,T&gt; interface"]
        Csv["CsvRepository&lt;K,T&gt;<br/>in-memory index + atomic file writes"]
        Mappers["ExpenseCsvMapper / BudgetCsvMapper /<br/>RecurringExpenseCsvMapper"]
    end

    subgraph Cross["Cross-cutting (util, exception, model)"]
        Val["Validator, Money, EnumParser, CsvUtil"]
        Log["AppLogger (java.util.logging)"]
        Model["Expense, Budget, RecurringExpense,<br/>Category, PaymentMethod, Frequency"]
    end

    Files[("data/*.csv<br/>data/logs/app0.log")]

    User --> Menu
    App --> Menu
    Menu --> Input
    Menu --> Fmt
    Menu --> ES & BS & RS & RES & IES
    RS --> ES
    RS --> BS
    BS --> ES
    RES --> ES
    IES --> ES
    ES & BS & RES --> RepoI
    RepoI -. implemented by .-> Csv
    Csv --> Mappers
    Csv --> Files
    Service -.-> Val
    Service -.-> Log
```

## 2. Process flow / workflow

```mermaid
flowchart TD
    Start(["java -jar expense-tracker.jar"]) --> Parse["Parse options"]
    Parse -->|invalid| Usage["Print error + usage, exit code 2"]
    Parse -->|--help / --version| Info["Print info and exit"]
    Parse --> Load["Configure logging, load CSV files<br/>(skip + log damaged rows)"]
    Load --> Demo{"--demo and no data?"}
    Demo -->|yes| Seed["Generate sample data"]
    Demo -->|no| Due
    Seed --> Due["Generate due recurring payments"]
    Due --> Cmd{"Command?"}
    Cmd -->|list / report| Print["Print table or report"] --> End(["Exit"])
    Cmd -->|menu| Menu["Show main menu"]
    Menu --> Choice{"User choice"}
    Choice -->|1-4| CRUD["Add / view / edit / delete expense"]
    Choice -->|5| Budget["Budget status / set / remove"]
    Choice -->|6| Report["Reports & insights"]
    Choice -->|7| Rec["Recurring expenses"]
    Choice -->|8| IO["Import / export"]
    Choice -->|0 or end of input| End
    CRUD --> Validate{"Input valid?"}
    Validate -->|no| Msg["Show reason, ask again"] --> CRUD
    Validate -->|yes| Save["Save to CSV (atomic)"] --> Alert["Show budget alerts if >= 80%"] --> Menu
    Budget --> Menu
    Report --> Menu
    Rec --> Menu
    IO --> Menu
```

## 3. Use case diagram

```mermaid
flowchart LR
    User(["User"])
    Clock(["System clock / start-up"])

    subgraph System["Expense Tracker"]
        UC1(["Record expense"])
        UC2(["View and search expenses"])
        UC3(["Edit expense"])
        UC4(["Delete expense"])
        UC5(["Manage budgets"])
        UC6(["Receive budget alert"])
        UC7(["View monthly report and insights"])
        UC8(["View trend / breakdown / top expenses"])
        UC9(["Manage recurring expenses"])
        UC10(["Generate due recurring expenses"])
        UC11(["Import expenses from CSV"])
        UC12(["Export expenses / report"])
    end

    User --- UC1 & UC2 & UC3 & UC4 & UC5 & UC7 & UC8 & UC9 & UC11 & UC12
    UC1 -. includes .-> UC6
    UC3 -. includes .-> UC6
    UC9 -. includes .-> UC10
    Clock --- UC10
```

## 4. Class diagram

```mermaid
classDiagram
    direction LR

    class Expense {
        <<record>>
        +int id
        +LocalDate date
        +BigDecimal amount
        +Category category
        +PaymentMethod paymentMethod
        +String description
        +month() YearMonth
    }
    class Budget {
        <<record>>
        +Category category
        +BigDecimal monthlyLimit
        +isOverall() boolean
        +key() String
    }
    class RecurringExpense {
        <<record>>
        +int id
        +Frequency frequency
        +LocalDate startDate
        +int occurrencesGenerated
        +boolean active
        +nextDueDate() LocalDate
    }
    class Category { <<enumeration>> }
    class PaymentMethod { <<enumeration>> }
    class Frequency {
        <<enumeration>>
        +occurrence(LocalDate start, int n) LocalDate
    }
    class Labeled {
        <<interface>>
        +label() String
    }

    class Repository~K,T~ {
        <<interface>>
        +findAll() List~T~
        +findById(K) Optional~T~
        +save(T) T
        +deleteById(K) boolean
        +count() int
    }
    class CsvRepository~K,T~ {
        -Path file
        -Map~K,T~ entities
        -persist()
        +skippedRows() int
    }
    class CsvMapper~T~ {
        <<interface>>
        +header() List~String~
        +toRow(T) List~String~
        +fromRow(List~String~) T
    }

    class ExpenseService {
        +add(...) Expense
        +update(id, ...) Expense
        +delete(id)
        +search(ExpenseFilter, SortOrder) List~Expense~
    }
    class BudgetService {
        +setBudget(Category, BigDecimal) Budget
        +statusFor(YearMonth) List~BudgetStatus~
        +alertsFor(Expense) List~String~
    }
    class ReportService {
        +monthlySummary(YearMonth) MonthlySummary
        +monthlyTrend(YearMonth, int) Map
        +insights(YearMonth) List~String~
    }
    class RecurringExpenseService {
        +create(...) RecurringExpense
        +processDue() List~Expense~
    }
    class ImportExportService {
        +exportExpenses(List, Path) int
        +importExpenses(Path) ImportResult
    }
    class ExpenseFilter {
        +test(Expense) boolean
    }
    class BudgetStatus {
        <<record>>
        +level() Level
        +percentUsed() BigDecimal
    }
    class MonthlySummary { <<record>> }

    class ConsoleMenu
    class InputReader
    class ConsoleFormatter

    Labeled <|.. Category
    Labeled <|.. PaymentMethod
    Labeled <|.. Frequency
    Repository <|.. CsvRepository
    CsvRepository o-- CsvMapper
    Expense --> Category
    Expense --> PaymentMethod
    Budget --> Category
    RecurringExpense --> Frequency

    ExpenseService --> Repository
    BudgetService --> Repository
    RecurringExpenseService --> Repository
    BudgetService --> ExpenseService
    ReportService --> ExpenseService
    ReportService --> BudgetService
    RecurringExpenseService --> ExpenseService
    ImportExportService --> ExpenseService
    ExpenseService ..> ExpenseFilter
    BudgetService ..> BudgetStatus
    ReportService ..> MonthlySummary

    ConsoleMenu --> ExpenseService
    ConsoleMenu --> BudgetService
    ConsoleMenu --> ReportService
    ConsoleMenu --> RecurringExpenseService
    ConsoleMenu --> ImportExportService
    ConsoleMenu --> InputReader
    ConsoleMenu ..> ConsoleFormatter
```

## 5. Sequence diagram: adding an expense

```mermaid
sequenceDiagram
    actor User
    participant Menu as ConsoleMenu
    participant In as InputReader
    participant ES as ExpenseService
    participant V as Validator
    participant Repo as CsvRepository
    participant FS as expenses.csv
    participant BS as BudgetService

    User->>Menu: choose "1. Add expense"
    Menu->>In: read amount, category, description, date, payment method
    loop until each answer is valid
        In->>User: prompt
        User-->>In: text
        In->>V: parse + validate
        V-->>In: value or ValidationException (message shown, ask again)
    end
    Menu->>ES: add(date, amount, category, method, description)
    ES->>V: validate all fields again (service is the source of truth)
    ES->>Repo: save(new Expense(nextId, ...))
    Repo->>FS: write expenses.csv.tmp, then atomic move over expenses.csv
    alt write fails
        Repo-->>ES: StorageException (in-memory change rolled back)
        ES-->>Menu: error shown, menu continues
    else success
        Repo-->>ES: saved expense
        ES-->>Menu: Expense #id
        Menu->>BS: alertsFor(expense)
        BS->>ES: search(this month)
        BS-->>Menu: alerts for budgets at >= 80%
        Menu-->>User: "Saved expense #id" + any [BUDGET ALERT]
    end
```

## 6. Sequence diagram: generating recurring expenses at start-up

```mermaid
sequenceDiagram
    participant App
    participant RES as RecurringExpenseService
    participant RR as Recurring repository
    participant ES as ExpenseService

    App->>RES: processDue()
    RES->>RR: findAll()
    loop each active schedule
        loop while nextDueDate <= today (max 400 per run)
            RES->>ES: add(nextDueDate, amount, ... "(recurring)")
            RES->>RR: save(schedule with occurrencesGenerated + 1)
        end
    end
    RES-->>App: list of generated expenses (shown as a start-up notice)
```

## 7. Storage design

### ER diagram

The data is stored in three CSV files. Categories, payment methods and frequencies are Java
enums, stored by name. They are shown as entities to make the relationships clear.

```mermaid
erDiagram
    CATEGORY ||--o{ EXPENSE : classifies
    CATEGORY ||--o| BUDGET : "limited by"
    CATEGORY ||--o{ RECURRING_EXPENSE : classifies
    PAYMENT_METHOD ||--o{ EXPENSE : "paid with"
    PAYMENT_METHOD ||--o{ RECURRING_EXPENSE : "paid with"
    RECURRING_EXPENSE ||--o{ EXPENSE : "generates"

    EXPENSE {
        int id PK
        date date
        decimal amount
        string category FK
        string payment_method FK
        string description
    }
    BUDGET {
        string scope PK "category name or OVERALL"
        decimal monthly_limit
    }
    RECURRING_EXPENSE {
        int id PK
        string description
        decimal amount
        string category FK
        string payment_method FK
        string frequency
        date start_date
        int occurrences
        boolean active
    }
    CATEGORY {
        string name PK
        string label
    }
    PAYMENT_METHOD {
        string name PK
        string label
    }
```

### Schema

| File | Columns | Key | Rules |
|---|---|---|---|
| `expenses.csv` | `id,date,amount,category,payment_method,description` | `id` (auto-increment) | amount > 0 and ≤ 10,000,000 with at most 2 decimals; date between 2000-01-01 and today; description 1–100 chars without line breaks |
| `budgets.csv` | `scope,monthly_limit` | `scope` | scope is `OVERALL` or a category name; at most one budget per scope |
| `recurring.csv` | `id,description,amount,category,payment_method,frequency,start_date,occurrences,active` | `id` | next due date = `frequency.occurrence(start_date, occurrences)` |

## 8. Design decisions and rationale

| Decision | Rationale |
|---|---|
| **Layered architecture + generic `Repository<K,T>`** | Business rules don't depend on how data is stored. One generic `CsvRepository` serves all three entity types via a pluggable `CsvMapper` (**Strategy pattern**). |
| **CSV instead of a database** | Zero setup for the user or evaluator, human-readable, opens in Excel. The repository interface keeps a later move to SQLite/JDBC cheap. |
| **Load everything into memory, write through on every change** | Personal expense data is small (thousands of rows), so this gives O(1) lookups and simple code. Writing a temp file then doing an atomic move means a crash never corrupts data. |
| **Immutable records for entities** | `Expense`, `Budget`, `RecurringExpense`, `BudgetStatus` and `MonthlySummary` are records. They can't be modified by accident, and equality works out of the box (useful in tests). |
| **`BigDecimal` for money** | `double` cannot represent 0.10 exactly, and rounding errors add up in totals. |
| **Validation in the service layer** | The UI validates for instant feedback, but the service validates again, so imports and recurring generation obey exactly the same rules. |
| **Injected `Clock`** | "Today" controls future-date checks, projections and recurring generation. A fixed clock makes these deterministic in tests. |
| **Recurring schedules store an occurrence counter, not a "next date"** | Dates are computed from the start date (`start.plusMonths(n)`), so the 31st never drifts to the 28th, and re-running generation is idempotent. |
| **Projection separates scheduled from day-to-day spending** | Rent paid on the 1st would otherwise be extrapolated as if it happened daily, grossly over-estimating month-end spending. |
| **Unchecked exception hierarchy** | `ValidationException`, `NotFoundException` and `StorageException` share `ExpenseTrackerException`, so the menu handles all expected errors in one `catch`, and unexpected ones are logged without crashing. |
| **Own tiny test framework** | `@Test` annotation + reflection runner means tests run with nothing but a JDK: no Maven, no internet. It also demonstrates annotations and reflection. |
| **Composition root in `App`** | All objects are created and wired in one place (manual dependency injection), keeping classes decoupled and easy to construct in tests. |

### Design patterns used

- **Repository**: `Repository<K,T>`, `CsvRepository`
- **Strategy**: `CsvMapper<T>` implementations; `SortOrder` enum holds a `Comparator`
- **Specification / Predicate**: `ExpenseFilter implements Predicate<Expense>`, built fluently
- **Dependency injection**: constructor injection of repositories, services and `Clock`
- **Template-like formatting helpers**: `ConsoleFormatter` returns strings, reused for the screen and for files
