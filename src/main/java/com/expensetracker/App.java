package com.expensetracker;

import com.expensetracker.exception.ExpenseTrackerException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.model.RecurringExpense;
import com.expensetracker.repository.BudgetCsvMapper;
import com.expensetracker.repository.CsvRepository;
import com.expensetracker.repository.ExpenseCsvMapper;
import com.expensetracker.repository.RecurringExpenseCsvMapper;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.ExpenseFilter;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ImportExportService;
import com.expensetracker.service.RecurringExpenseService;
import com.expensetracker.service.ReportService;
import com.expensetracker.service.SortOrder;
import com.expensetracker.ui.ConsoleFormatter;
import com.expensetracker.ui.ConsoleMenu;
import com.expensetracker.ui.InputReader;
import com.expensetracker.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Entry point and composition root: parses arguments, wires repositories and services
 * together (manual dependency injection) and starts either the menu or a one-shot command.
 */
public final class App {

    public static final String VERSION = "1.0.0";

    private final AppOptions options;
    private final PrintStream out = System.out;

    private App(AppOptions options) {
        this.options = options;
    }

    public static void main(String[] args) {
        AppOptions options;
        try {
            options = AppOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.print(AppOptions.USAGE);
            System.exit(2);
            return;
        }
        if (options.help()) {
            System.out.print(AppOptions.USAGE);
            return;
        }
        if (options.version()) {
            System.out.println("Expense Tracker " + VERSION);
            return;
        }
        try {
            new App(options).run();
        } catch (ExpenseTrackerException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        Path dataDir = options.dataDir();
        AppLogger.configure(dataDir.resolve("logs"));
        Logger log = AppLogger.get(App.class);
        log.info(() -> "Starting Expense Tracker " + VERSION + ", data folder " + dataDir.toAbsolutePath());

        Clock clock = Clock.systemDefaultZone();
        CsvRepository<Integer, Expense> expenseRepository =
                new CsvRepository<>(dataDir.resolve("expenses.csv"), new ExpenseCsvMapper(), Expense::id);
        CsvRepository<String, Budget> budgetRepository =
                new CsvRepository<>(dataDir.resolve("budgets.csv"), new BudgetCsvMapper(), Budget::key);
        CsvRepository<Integer, RecurringExpense> recurringRepository =
                new CsvRepository<>(dataDir.resolve("recurring.csv"), new RecurringExpenseCsvMapper(), RecurringExpense::id);

        ExpenseService expenseService = new ExpenseService(expenseRepository, clock);
        BudgetService budgetService = new BudgetService(budgetRepository, expenseService);
        ReportService reportService = new ReportService(expenseService, budgetService);
        RecurringExpenseService recurringService = new RecurringExpenseService(recurringRepository, expenseService);
        ImportExportService importExportService = new ImportExportService(expenseService);

        List<String> notices = new ArrayList<>();
        int skipped = expenseRepository.skippedRows() + budgetRepository.skippedRows() + recurringRepository.skippedRows();
        if (skipped > 0) {
            notices.add(skipped + " damaged row(s) in the data files were skipped (see " + dataDir.resolve("logs") + ")");
        }
        if (options.demo() && expenseService.count() == 0) {
            int created = DemoDataSeeder.seed(expenseService, budgetService, recurringService);
            notices.add("Demo data loaded: " + created + " sample expenses, 5 budgets and 3 recurring payments.");
        }
        List<Expense> generated = recurringService.processDue();
        if (!generated.isEmpty()) {
            notices.add(generated.size() + " recurring payment(s) that fell due were added as expenses.");
        }

        switch (options.command()) {
            case "list" -> {
                notices.forEach(n -> out.println("> " + n));
                ExpenseFilter filter = options.commandArgument() == null ? ExpenseFilter.all()
                        : ExpenseFilter.forMonth(InputReader.parseMonth(options.commandArgument()));
                out.print(ConsoleFormatter.expenseTable(expenseService.search(filter, SortOrder.DATE_ASC)));
            }
            case "report" -> {
                notices.forEach(n -> out.println("> " + n));
                YearMonth month = options.commandArgument() == null ? YearMonth.now(clock)
                        : InputReader.parseMonth(options.commandArgument());
                out.print(ConsoleFormatter.monthlyReport(reportService.monthlySummary(month),
                        budgetService.statusFor(month), reportService.insights(month)));
            }
            default -> {
                InputReader input = new InputReader(new BufferedReader(new InputStreamReader(System.in)), out);
                new ConsoleMenu(expenseService, budgetService, reportService, recurringService,
                        importExportService, input, out, dataDir.resolve("exports")).run(notices);
            }
        }
        log.info("Shutting down");
    }
}
