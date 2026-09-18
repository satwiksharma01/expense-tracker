package com.expensetracker.service;

import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Frequency;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.RecurringExpense;
import com.expensetracker.repository.CsvRepository;
import com.expensetracker.testing.Test;
import com.expensetracker.testing.TestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertThrows;
import static com.expensetracker.testing.Assertions.assertTrue;

public class RecurringExpenseServiceTest {

    private final CsvRepository<Integer, RecurringExpense> repository = TestSupport.recurringRepository();

    private static RecurringExpenseService serviceAt(LocalDate today, CsvRepository<Integer, RecurringExpense> repo) {
        return new RecurringExpenseService(repo, TestSupport.expenseService(today));
    }

    private static RecurringExpense rent(RecurringExpenseService service, LocalDate start) {
        return service.create("Rent", new BigDecimal("8000"), Category.RENT, PaymentMethod.NET_BANKING,
                Frequency.MONTHLY, start);
    }

    @Test
    public void generatesEveryMissedOccurrenceUpToToday() {
        RecurringExpenseService service = serviceAt(LocalDate.of(2026, 9, 18), repository);
        rent(service, LocalDate.of(2026, 6, 1));

        List<Expense> created = service.processDue();
        assertEquals(List.of(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 9, 1)), created.stream().map(Expense::date).toList());
        assertEquals(LocalDate.of(2026, 10, 1), service.list().get(0).nextDueDate());
    }

    @Test
    public void processingTwiceCreatesNoDuplicates() {
        RecurringExpenseService service = serviceAt(LocalDate.of(2026, 9, 18), repository);
        rent(service, LocalDate.of(2026, 9, 1));
        assertEquals(1, service.processDue().size());
        assertEquals(0, service.processDue().size());
    }

    @Test
    public void monthEndScheduleDoesNotDrift() {
        RecurringExpenseService service = serviceAt(LocalDate.of(2026, 4, 30), repository);
        rent(service, LocalDate.of(2026, 1, 31));
        List<LocalDate> dates = service.processDue().stream().map(Expense::date).toList();
        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30)), dates);
    }

    @Test
    public void pausedAndFutureSchedulesGenerateNothing() {
        RecurringExpenseService service = serviceAt(LocalDate.of(2026, 9, 18), repository);
        RecurringExpense paused = rent(service, LocalDate.of(2026, 8, 1));
        service.setActive(paused.id(), false);
        rent(service, LocalDate.of(2026, 10, 1));
        assertEquals(0, service.processDue().size());
    }

    @Test
    public void generatedExpensesAreMarkedAsRecurring() {
        RecurringExpenseService service = serviceAt(LocalDate.of(2026, 9, 18), repository);
        service.create("Gym", new BigDecimal("50"), Category.HEALTH, PaymentMethod.UPI,
                Frequency.WEEKLY, LocalDate.of(2026, 9, 4));
        List<Expense> created = service.processDue();
        assertEquals(3, created.size());
        assertTrue(created.stream().allMatch(e -> e.description().equals("Gym (recurring)")), "suffix expected");
    }

    @Test
    public void rejectsInvalidSchedules() {
        RecurringExpenseService service = serviceAt(LocalDate.of(2026, 9, 18), repository);
        assertThrows(ValidationException.class, () -> service.create("Rent", BigDecimal.ZERO, Category.RENT,
                PaymentMethod.UPI, Frequency.MONTHLY, LocalDate.of(2026, 9, 1)));
        assertThrows(ValidationException.class, () -> service.create("Rent", BigDecimal.TEN, Category.RENT,
                PaymentMethod.UPI, null, LocalDate.of(2026, 9, 1)));
    }
}
