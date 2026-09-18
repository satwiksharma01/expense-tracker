package com.expensetracker.testing;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.model.RecurringExpense;
import com.expensetracker.repository.BudgetCsvMapper;
import com.expensetracker.repository.CsvRepository;
import com.expensetracker.repository.ExpenseCsvMapper;
import com.expensetracker.repository.RecurringExpenseCsvMapper;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.ExpenseService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Factories shared by the tests: fixed clocks, in-memory repositories and temp folders.
 */
public final class TestSupport {

    /** "Today" for every test unless a test chooses otherwise: a Friday in mid-September. */
    public static final LocalDate TODAY = LocalDate.of(2026, 9, 18);

    private TestSupport() {
    }

    public static Clock clockAt(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    public static ExpenseService expenseService() {
        return expenseService(TODAY);
    }

    public static ExpenseService expenseService(LocalDate today) {
        return new ExpenseService(CsvRepository.inMemory(new ExpenseCsvMapper(), Expense::id), clockAt(today));
    }

    public static BudgetService budgetService(ExpenseService expenses) {
        return new BudgetService(CsvRepository.inMemory(new BudgetCsvMapper(), Budget::key), expenses);
    }

    public static CsvRepository<Integer, RecurringExpense> recurringRepository() {
        return CsvRepository.inMemory(new RecurringExpenseCsvMapper(), RecurringExpense::id);
    }

    public static Path tempDir() {
        try {
            Path dir = Files.createTempDirectory("expense-tracker-test");
            dir.toFile().deleteOnExit();
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
