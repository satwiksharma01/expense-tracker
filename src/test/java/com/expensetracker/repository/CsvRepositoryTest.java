package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
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
import static com.expensetracker.testing.Assertions.assertFalse;
import static com.expensetracker.testing.Assertions.assertTrue;

public class CsvRepositoryTest {

    private final Path file = TestSupport.tempDir().resolve("expenses.csv");

    private CsvRepository<Integer, Expense> open() {
        return new CsvRepository<>(file, new ExpenseCsvMapper(), Expense::id);
    }

    private static Expense expense(int id, String description) {
        return new Expense(id, LocalDate.of(2026, 9, id), new BigDecimal("250.00"), Category.FOOD,
                PaymentMethod.UPI, description);
    }

    @Test
    public void missingFileStartsEmpty() {
        assertEquals(0, open().count());
    }

    @Test
    public void persistsAndReloadsFromDisk() {
        CsvRepository<Integer, Expense> repository = open();
        repository.save(expense(1, "Lunch"));
        repository.save(expense(2, "Bus pass"));

        CsvRepository<Integer, Expense> reloaded = open();
        assertEquals(2, reloaded.count());
        assertEquals(expense(2, "Bus pass"), reloaded.findById(2).orElseThrow());
    }

    @Test
    public void descriptionWithCommasAndQuotesSurvivesReload() {
        Expense tricky = expense(1, "Dinner, drinks and \"dessert\"");
        open().save(tricky);
        assertEquals(tricky, open().findById(1).orElseThrow());
    }

    @Test
    public void saveWithExistingIdReplacesEntity() {
        CsvRepository<Integer, Expense> repository = open();
        repository.save(expense(1, "Old"));
        repository.save(expense(1, "New"));
        assertEquals(1, open().count());
        assertEquals("New", open().findById(1).orElseThrow().description());
    }

    @Test
    public void deleteIsPersisted() {
        CsvRepository<Integer, Expense> repository = open();
        repository.save(expense(1, "Lunch"));
        assertTrue(repository.deleteById(1), "delete should report success");
        assertFalse(repository.deleteById(1), "second delete should find nothing");
        assertEquals(0, open().count());
    }

    @Test
    public void skipsCorruptRowsButLoadsValidOnes() throws IOException {
        Files.write(file, List.of(
                "id,date,amount,category,payment_method,description",
                "1,2026-09-01,250.00,FOOD,UPI,Lunch",
                "this line is garbage",
                "2,not-a-date,10.00,FOOD,UPI,Bad date",
                "3,2026-09-03,99.00,UNKNOWN_CATEGORY,UPI,Bad category",
                "4,2026-09-04,40.00,TRANSPORT,CASH,Auto"));

        CsvRepository<Integer, Expense> repository = open();
        assertEquals(2, repository.count());
        assertEquals(3, repository.skippedRows());
        assertTrue(repository.findById(4).isPresent(), "valid row after corrupt ones should load");
    }
}
