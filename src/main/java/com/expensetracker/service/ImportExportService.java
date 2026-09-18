package com.expensetracker.service;

import com.expensetracker.exception.StorageException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.util.AppLogger;
import com.expensetracker.util.CsvUtil;
import com.expensetracker.util.EnumParser;
import com.expensetracker.util.Money;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Import/export module: moves expenses in and out of the application as CSV files
 * (e.g. to open them in a spreadsheet) and saves text reports.
 */
public class ImportExportService {

    /** Files larger than this are rejected to keep memory use bounded. */
    public static final long MAX_IMPORT_BYTES = 5L * 1024 * 1024;

    static final List<String> HEADER = List.of("date", "amount", "category", "payment_method", "description");

    private static final Logger LOG = AppLogger.get(ImportExportService.class);

    private final ExpenseService expenseService;

    public ImportExportService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /** Outcome of an import: how many rows were added and why the others were rejected. */
    public record ImportResult(int importedCount, List<String> errors) {
    }

    /** Writes the expenses as CSV. Descriptions are neutralised against spreadsheet formula injection. */
    public int exportExpenses(List<Expense> expenses, Path target) {
        List<String> lines = new ArrayList<>(expenses.size() + 1);
        lines.add(CsvUtil.toLine(HEADER));
        for (Expense e : expenses) {
            lines.add(CsvUtil.toLine(List.of(e.date().toString(), e.amount().toPlainString(), e.category().name(),
                    e.paymentMethod().name(), CsvUtil.neutralizeFormula(e.description()))));
        }
        write(target, lines);
        LOG.info(() -> "Exported " + expenses.size() + " expenses to " + target);
        return expenses.size();
    }

    /**
     * Reads expenses from a CSV file with the columns {@code date,amount,category,payment_method,description}.
     * Every row goes through the same validation as manual entry. Invalid rows are reported
     * with their line number and skipped; valid rows are still imported.
     */
    public ImportResult importExpenses(Path source) {
        requireReadableFile(source);
        int imported = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || (lineNumber == 1 && line.toLowerCase(Locale.ROOT).startsWith("date"))) {
                    continue;
                }
                try {
                    importRow(CsvUtil.parseLine(line));
                    imported++;
                } catch (ValidationException | DateTimeParseException | IllegalArgumentException e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new StorageException("Could not read " + source + ": " + e.getMessage(), e);
        }
        int importedCount = imported;
        LOG.info(() -> "Imported " + importedCount + " expenses from " + source + ", " + errors.size() + " rejected");
        return new ImportResult(imported, List.copyOf(errors));
    }

    public void writeText(Path target, String content) {
        write(target, List.of(content));
    }

    private void importRow(List<String> row) {
        if (row.size() != HEADER.size()) {
            throw new ValidationException("expected " + HEADER.size() + " columns but found " + row.size());
        }
        LocalDate date;
        try {
            date = LocalDate.parse(row.get(0).strip());
        } catch (DateTimeParseException e) {
            throw new ValidationException("invalid date '" + row.get(0).strip() + "' (use YYYY-MM-DD)");
        }
        expenseService.add(date,
                Money.parse(row.get(1)),
                EnumParser.parse(Category.class, row.get(2)),
                EnumParser.parse(PaymentMethod.class, row.get(3)),
                row.get(4));
    }

    private static void requireReadableFile(Path source) {
        if (!Files.isRegularFile(source)) {
            throw new ValidationException("File not found: " + source);
        }
        try {
            if (Files.size(source) > MAX_IMPORT_BYTES) {
                throw new ValidationException("File is larger than " + MAX_IMPORT_BYTES / (1024 * 1024) + " MB");
            }
        } catch (IOException e) {
            throw new StorageException("Could not read " + source + ": " + e.getMessage(), e);
        }
    }

    private static void write(Path target, List<String> lines) {
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not write " + target + ": " + e.getMessage(), e);
        }
    }
}
