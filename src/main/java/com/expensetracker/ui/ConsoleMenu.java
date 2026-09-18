package com.expensetracker.ui;

import com.expensetracker.exception.ExpenseTrackerException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Frequency;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.RecurringExpense;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.ExpenseFilter;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ImportExportService;
import com.expensetracker.service.MonthlySummary;
import com.expensetracker.service.RecurringExpenseService;
import com.expensetracker.service.ReportService;
import com.expensetracker.service.SortOrder;
import com.expensetracker.util.AppLogger;
import com.expensetracker.util.Money;
import com.expensetracker.util.Validator;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interactive, menu-driven front end. It only talks to the service layer; all business
 * rules live in the services, so this class is limited to prompting and printing.
 */
public class ConsoleMenu {

    private static final Logger LOG = AppLogger.get(ConsoleMenu.class);

    private final ExpenseService expenses;
    private final BudgetService budgets;
    private final ReportService reports;
    private final RecurringExpenseService recurring;
    private final ImportExportService importExport;
    private final InputReader input;
    private final PrintStream out;
    private final Path exportDir;

    public ConsoleMenu(ExpenseService expenses, BudgetService budgets, ReportService reports,
                       RecurringExpenseService recurring, ImportExportService importExport,
                       InputReader input, PrintStream out, Path exportDir) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.reports = reports;
        this.recurring = recurring;
        this.importExport = importExport;
        this.input = input;
        this.out = out;
        this.exportDir = exportDir;
    }

    /** Runs the main loop until the user chooses Exit or input ends. */
    public void run(List<String> startupNotices) {
        out.println();
        out.println("  +--------------------------------------+");
        out.println("  |   EXPENSE TRACKER - personal finance |");
        out.println("  +--------------------------------------+");
        startupNotices.forEach(notice -> out.println("  > " + notice));
        try {
            boolean running = true;
            while (running) {
                printMainMenu();
                running = dispatch(input.readInt("Select an option: ", 0, 8));
            }
            out.println("Goodbye! All changes have been saved.");
        } catch (EndOfInputException e) {
            out.println();
            out.println("Input closed - exiting. All changes have been saved.");
        }
    }

    private void printMainMenu() {
        out.println();
        out.println("=== Main menu ===   (" + expenses.count() + " expenses recorded, today is " + today() + ")");
        out.println("  1. Add expense");
        out.println("  2. View / search expenses");
        out.println("  3. Edit expense");
        out.println("  4. Delete expense");
        out.println("  5. Budgets");
        out.println("  6. Reports & insights");
        out.println("  7. Recurring expenses");
        out.println("  8. Import / export");
        out.println("  0. Exit");
    }

    /** @return {@code false} when the user asked to exit */
    private boolean dispatch(int choice) {
        try {
            switch (choice) {
                case 1 -> addExpense();
                case 2 -> viewExpenses();
                case 3 -> editExpense();
                case 4 -> deleteExpense();
                case 5 -> budgetMenu();
                case 6 -> reportMenu();
                case 7 -> recurringMenu();
                case 8 -> importExportMenu();
                default -> {
                    return false;
                }
            }
        } catch (EndOfInputException e) {
            throw e;
        } catch (ExpenseTrackerException e) {
            out.println("  ! " + e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error in menu option " + choice, e);
            out.println("  ! Unexpected error: " + e.getMessage() + " (details in the log file)");
        }
        return true;
    }

    // ---- 1-4: expense management -------------------------------------------------------

    private void addExpense() {
        out.println();
        out.println("-- Add expense --");
        BigDecimal amount = readAmount("Amount (Rs.): ");
        Category category = input.chooseEnum("Category:", Category.class, null);
        String description = input.read("Description: ", Validator::requireValidDescription);
        LocalDate date = input.readOrDefault("Date (YYYY-MM-DD, DD-MM-YYYY, 'yesterday') [today]: ", today(),
                this::parseExpenseDate);
        PaymentMethod method = input.chooseEnum("Paid by:", PaymentMethod.class, PaymentMethod.UPI);

        Expense saved = expenses.add(date, amount, category, method, description);
        out.println("  Saved expense #" + saved.id() + ": " + Money.format(saved.amount())
                + " for " + saved.category().label() + " on " + saved.date());
        printBudgetAlerts(saved);
    }

    private void viewExpenses() {
        out.println();
        out.println("-- View expenses --");
        out.println("  1. All expenses (newest first)");
        out.println("  2. This month");
        out.println("  3. Search with filters");
        out.println("  0. Back");
        switch (input.readInt("Select: ", 0, 3)) {
            case 1 -> out.print(ConsoleFormatter.expenseTable(expenses.search(ExpenseFilter.all(), SortOrder.DATE_DESC)));
            case 2 -> out.print(ConsoleFormatter.expenseTable(
                    expenses.search(ExpenseFilter.forMonth(YearMonth.from(today())), SortOrder.DATE_DESC)));
            case 3 -> searchExpenses();
            default -> {
                // back to main menu
            }
        }
    }

    private void searchExpenses() {
        out.println("Leave any field blank to skip it.");
        LocalDate from = input.readOrDefault("From date: ", null, text -> InputReader.parseDate(text, today()));
        LocalDate to = input.readOrDefault("To date: ", null, text -> InputReader.parseDate(text, today()));
        Category category = input.chooseOptionalEnum("Category:", Category.class);
        PaymentMethod method = input.chooseOptionalEnum("Paid by:", PaymentMethod.class);
        BigDecimal min = input.readOrDefault("Minimum amount: ", null, Money::parse);
        BigDecimal max = input.readOrDefault("Maximum amount: ", null, Money::parse);
        String keyword = input.readLine("Description contains: ");
        SortOrder order = input.chooseEnum("Sort by:", SortOrder.class, SortOrder.DATE_DESC);

        ExpenseFilter filter = ExpenseFilter.all().between(from, to).category(category).paymentMethod(method)
                .minAmount(min).maxAmount(max).keyword(keyword);
        out.println("Showing " + filter.describe() + ":");
        out.print(ConsoleFormatter.expenseTable(expenses.search(filter, order)));
    }

    private void editExpense() {
        Expense current = expenses.getById(input.readInt("ID of the expense to edit: ", 1, Integer.MAX_VALUE));
        out.print(ConsoleFormatter.expenseDetails(current));
        out.println("Press Enter to keep the value shown in brackets.");
        BigDecimal amount = input.readOrDefault("Amount [" + current.amount().toPlainString() + "]: ",
                current.amount(), text -> Validator.requireValidAmount(Money.parse(text)));
        Category category = input.chooseEnum("Category:", Category.class, current.category());
        String description = input.readOrDefault("Description [" + current.description() + "]: ",
                current.description(), Validator::requireValidDescription);
        LocalDate date = input.readOrDefault("Date [" + current.date() + "]: ", current.date(), this::parseExpenseDate);
        PaymentMethod method = input.chooseEnum("Paid by:", PaymentMethod.class, current.paymentMethod());

        Expense updated = expenses.update(current.id(), date, amount, category, method, description);
        out.println("  Updated:");
        out.print(ConsoleFormatter.expenseDetails(updated));
        printBudgetAlerts(updated);
    }

    private void deleteExpense() {
        Expense target = expenses.getById(input.readInt("ID of the expense to delete: ", 1, Integer.MAX_VALUE));
        out.print(ConsoleFormatter.expenseDetails(target));
        if (input.confirm("Delete this expense?")) {
            expenses.delete(target.id());
            out.println("  Deleted expense #" + target.id());
        } else {
            out.println("  Nothing deleted.");
        }
    }

    // ---- 5: budgets ----------------------------------------------------------------------

    private void budgetMenu() {
        out.println();
        out.println("-- Budgets --");
        out.println("  1. Budget status for this month");
        out.println("  2. Set or change a budget");
        out.println("  3. Remove a budget");
        out.println("  0. Back");
        switch (input.readInt("Select: ", 0, 3)) {
            case 1 -> {
                YearMonth month = YearMonth.from(today());
                out.println("Budget status for " + month + ":");
                out.print(ConsoleFormatter.budgetTable(budgets.statusFor(month)));
            }
            case 2 -> {
                Category scope = readBudgetScope();
                BigDecimal limit = input.read("Monthly limit (Rs.): ",
                        text -> Validator.requireValidLimit(Money.parse(text)));
                budgets.setBudget(scope, limit);
                out.println("  Budget saved: " + (scope == null ? "Overall" : scope.label()) + " = "
                        + Money.format(limit) + " per month");
            }
            case 3 -> {
                Category scope = readBudgetScope();
                budgets.removeBudget(scope);
                out.println("  Budget removed.");
            }
            default -> {
                // back to main menu
            }
        }
    }

    /** @return the chosen category, or {@code null} for the overall budget */
    private Category readBudgetScope() {
        out.println("Apply to:");
        out.println("  1. Overall spending (all categories)");
        out.println("  2. A single category");
        return input.readInt("Select: ", 1, 2) == 1 ? null
                : input.chooseEnum("Category:", Category.class, null);
    }

    private void printBudgetAlerts(Expense expense) {
        budgets.alertsFor(expense).forEach(alert -> out.println("  [BUDGET ALERT] " + alert));
    }

    // ---- 6: reports ----------------------------------------------------------------------

    private void reportMenu() {
        out.println();
        out.println("-- Reports & insights --");
        out.println("  1. Monthly report (summary, charts, budgets, insights)");
        out.println("  2. Spending trend over recent months");
        out.println("  3. Category breakdown for a date range");
        out.println("  4. Top 5 expenses of a month");
        out.println("  0. Back");
        switch (input.readInt("Select: ", 0, 4)) {
            case 1 -> out.print(monthlyReportText(readMonth()));
            case 2 -> {
                int months = input.readOrDefault("How many months? [6]: ", 6,
                        text -> InputReader.parseIntInRange(text, 1, 24));
                out.println();
                out.print(ConsoleFormatter.trendChart(reports.monthlyTrend(YearMonth.from(today()), months)));
            }
            case 3 -> {
                LocalDate from = input.readOrDefault("From date [first day of this month]: ",
                        today().withDayOfMonth(1), text -> InputReader.parseDate(text, today()));
                LocalDate to = input.readOrDefault("To date [today]: ", today(),
                        text -> InputReader.parseDate(text, today()));
                var breakdown = reports.categoryBreakdown(ExpenseFilter.all().between(from, to));
                out.println("Spending from " + from + " to " + to + ":");
                out.print(ConsoleFormatter.shareChart(breakdown, Money.sum(breakdown.values())));
            }
            case 4 -> {
                YearMonth month = readMonth();
                out.print(ConsoleFormatter.expenseTable(reports.topExpenses(ExpenseFilter.forMonth(month), 5)));
            }
            default -> {
                // back to main menu
            }
        }
    }

    private String monthlyReportText(YearMonth month) {
        MonthlySummary summary = reports.monthlySummary(month);
        return ConsoleFormatter.monthlyReport(summary, budgets.statusFor(month), reports.insights(month));
    }

    // ---- 7: recurring expenses -----------------------------------------------------------

    private void recurringMenu() {
        out.println();
        out.println("-- Recurring expenses --");
        out.println("  1. List recurring expenses");
        out.println("  2. Add a recurring expense");
        out.println("  3. Pause or resume one");
        out.println("  4. Delete one");
        out.println("  0. Back");
        switch (input.readInt("Select: ", 0, 4)) {
            case 1 -> out.print(ConsoleFormatter.recurringTable(recurring.list()));
            case 2 -> addRecurring();
            case 3 -> {
                out.print(ConsoleFormatter.recurringTable(recurring.list()));
                int id = input.readInt("ID: ", 1, Integer.MAX_VALUE);
                boolean resume = input.confirm("Resume it? (answer n to pause)");
                RecurringExpense changed = recurring.setActive(id, resume);
                out.println("  '" + changed.description() + "' is now " + (changed.active() ? "active" : "paused"));
                if (resume) {
                    reportGenerated(recurring.processDue());
                }
            }
            case 4 -> {
                int id = input.readInt("ID of the recurring expense to delete: ", 1, Integer.MAX_VALUE);
                if (input.confirm("Delete it? Expenses it already created are kept.")) {
                    recurring.delete(id);
                    out.println("  Deleted.");
                }
            }
            default -> {
                // back to main menu
            }
        }
    }

    private void addRecurring() {
        String description = input.read("Description (e.g. Hostel rent): ", text -> Validator.requireValidDescription(
                text, Validator.MAX_DESCRIPTION_LENGTH - RecurringExpenseService.GENERATED_SUFFIX.length()));
        BigDecimal amount = readAmount("Amount (Rs.): ");
        Category category = input.chooseEnum("Category:", Category.class, null);
        PaymentMethod method = input.chooseEnum("Paid by:", PaymentMethod.class, PaymentMethod.UPI);
        Frequency frequency = input.chooseEnum("Repeats:", Frequency.class, Frequency.MONTHLY);
        LocalDate start = input.readOrDefault("First payment date [today]: ", today(),
                text -> InputReader.parseDate(text, today()));
        RecurringExpense created = recurring.create(description, amount, category, method, frequency, start);
        out.println("  Created recurring expense #" + created.id() + ", next due " + created.nextDueDate());
        reportGenerated(recurring.processDue());
    }

    private void reportGenerated(List<Expense> generated) {
        if (!generated.isEmpty()) {
            out.println("  " + generated.size() + " due payment(s) were added as expenses:");
            out.print(ConsoleFormatter.expenseTable(generated));
        }
    }

    // ---- 8: import / export --------------------------------------------------------------

    private void importExportMenu() {
        out.println();
        out.println("-- Import / export --");
        out.println("  1. Export expenses to CSV");
        out.println("  2. Import expenses from CSV");
        out.println("  3. Save a monthly report as a text file");
        out.println("  0. Back");
        switch (input.readInt("Select: ", 0, 3)) {
            case 1 -> {
                YearMonth month = input.readOrDefault("Month to export (YYYY-MM) [all]: ", null, InputReader::parseMonth);
                ExpenseFilter filter = month == null ? ExpenseFilter.all() : ExpenseFilter.forMonth(month);
                Path target = readPath("expenses-" + (month == null ? "all" : month) + ".csv");
                int count = importExport.exportExpenses(expenses.search(filter, SortOrder.DATE_ASC), target);
                out.println("  Exported " + count + " expense(s) to " + target.toAbsolutePath());
            }
            case 2 -> {
                out.println("Expected columns: date,amount,category,payment_method,description");
                Path source = input.read("CSV file to import: ", Path::of);
                ImportExportService.ImportResult result = importExport.importExpenses(source);
                out.println("  Imported " + result.importedCount() + " expense(s).");
                if (!result.errors().isEmpty()) {
                    out.println("  " + result.errors().size() + " row(s) were skipped:");
                    result.errors().forEach(error -> out.println("    - " + error));
                }
            }
            case 3 -> {
                YearMonth month = readMonth();
                Path target = readPath("report-" + month + ".txt");
                importExport.writeText(target, monthlyReportText(month));
                out.println("  Report saved to " + target.toAbsolutePath());
            }
            default -> {
                // back to main menu
            }
        }
    }

    // ---- helpers -------------------------------------------------------------------------

    private LocalDate today() {
        return expenses.today();
    }

    private LocalDate parseExpenseDate(String text) {
        return Validator.requireValidExpenseDate(InputReader.parseDate(text, today()), today());
    }

    private BigDecimal readAmount(String prompt) {
        return input.read(prompt, text -> Validator.requireValidAmount(Money.parse(text)));
    }

    private YearMonth readMonth() {
        YearMonth current = YearMonth.from(today());
        return input.readOrDefault("Month (YYYY-MM) [" + current + "]: ", current, InputReader::parseMonth);
    }

    private Path readPath(String defaultFileName) {
        Path defaultPath = exportDir.resolve(defaultFileName);
        return input.readOrDefault("Save to [" + defaultPath + "]: ", defaultPath, Path::of);
    }
}
