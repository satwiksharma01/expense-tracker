package com.expensetracker.service;

import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.testing.Test;
import com.expensetracker.testing.TestSupport;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertThrows;
import static com.expensetracker.testing.Assertions.assertTrue;

public class ImportExportServiceTest {

    private static final LocalDate TODAY = TestSupport.TODAY;

    private final Path dir = TestSupport.tempDir();
    private final ExpenseService source = TestSupport.expenseService();
    private final ImportExportService exporter = new ImportExportService(source);

    @Test
    public void exportThenImportRestoresTheSameExpenses() {
        source.add(TODAY, new BigDecimal("250.50"), Category.FOOD, PaymentMethod.UPI, "Dinner, with friends");
        source.add(TODAY.minusDays(1), new BigDecimal("40"), Category.TRANSPORT, PaymentMethod.CASH, "Auto");
        Path file = dir.resolve("export.csv");
        assertEquals(2, exporter.exportExpenses(source.search(ExpenseFilter.all(), SortOrder.DATE_ASC), file));

        ExpenseService target = TestSupport.expenseService();
        ImportExportService.ImportResult result = new ImportExportService(target).importExpenses(file);
        assertEquals(2, result.importedCount());
        assertEquals(List.of(), result.errors());
        assertEquals(contentOf(source), contentOf(target));
    }

    /** Everything except the id, which is reassigned on import. */
    private static List<List<Object>> contentOf(ExpenseService service) {
        return service.search(ExpenseFilter.all(), SortOrder.DATE_ASC).stream()
                .map(e -> List.<Object>of(e.date(), e.amount(), e.category(), e.paymentMethod(), e.description()))
                .toList();
    }

    @Test
    public void importSkipsInvalidRowsAndReportsLineNumbers() throws IOException {
        Path file = dir.resolve("import.csv");
        Files.write(file, List.of(
                "date,amount,category,payment_method,description",
                "2026-09-01,120,Food,UPI,Lunch",
                "2026-09-02,abc,FOOD,UPI,Bad amount",
                "2099-01-01,10,FOOD,UPI,Future date",
                "2026-09-03,10,Groceries,UPI,Unknown category",
                "2026-09-04,10,FOOD,UPI"));

        ExpenseService target = TestSupport.expenseService();
        ImportExportService.ImportResult result = new ImportExportService(target).importExpenses(file);
        assertEquals(1, result.importedCount());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Line 3:"), "got: " + result.errors().get(0));
        assertEquals(1, target.count());
    }

    @Test
    public void exportNeutralizesFormulaInjection() throws IOException {
        source.add(TODAY, BigDecimal.TEN, Category.OTHER, PaymentMethod.CASH, "=HYPERLINK(\"http://x\")");
        Path file = dir.resolve("export.csv");
        exporter.exportExpenses(source.search(ExpenseFilter.all(), SortOrder.DATE_ASC), file);
        assertTrue(Files.readString(file).contains("'=HYPERLINK"), "formula should be prefixed with an apostrophe");
    }

    @Test
    public void missingImportFileIsRejected() {
        assertThrows(ValidationException.class, () -> exporter.importExpenses(dir.resolve("nope.csv")));
    }
}
