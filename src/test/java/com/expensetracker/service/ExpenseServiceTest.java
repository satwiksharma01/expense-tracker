package com.expensetracker.service;

import com.expensetracker.exception.NotFoundException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.repository.CsvRepository;
import com.expensetracker.repository.ExpenseCsvMapper;
import com.expensetracker.testing.Test;
import com.expensetracker.testing.TestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.expensetracker.testing.Assertions.assertAmount;
import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertThrows;

public class ExpenseServiceTest {

    private static final LocalDate TODAY = TestSupport.TODAY;

    private final ExpenseService service = TestSupport.expenseService();

    private Expense add(String amount, Category category, String description) {
        return service.add(TODAY, new BigDecimal(amount), category, PaymentMethod.UPI, description);
    }

    @Test
    public void addAssignsSequentialIds() {
        assertEquals(1, add("100", Category.FOOD, "Lunch").id());
        assertEquals(2, add("50", Category.TRANSPORT, "Bus").id());
        assertEquals(2, service.count());
    }

    @Test
    public void addNormalizesAmountAndTrimsDescription() {
        Expense saved = service.add(TODAY, new BigDecimal("99.5"), Category.FOOD, PaymentMethod.CASH, "  Snacks ");
        assertEquals("99.50", saved.amount().toPlainString());
        assertEquals("Snacks", saved.description());
    }

    @Test
    public void invalidInputIsRejectedWithoutUsingAnId() {
        assertThrows(ValidationException.class, () -> add("-10", Category.FOOD, "Refund?"));
        assertThrows(ValidationException.class, () -> add("10", Category.FOOD, ""));
        assertThrows(ValidationException.class, () -> add("10", null, "No category"));
        assertThrows(ValidationException.class,
                () -> service.add(TODAY.plusDays(1), BigDecimal.TEN, Category.FOOD, PaymentMethod.UPI, "Tomorrow"));
        assertEquals(1, add("10", Category.FOOD, "Valid").id());
    }

    @Test
    public void idsContinueAfterExistingData() {
        CsvRepository<Integer, Expense> repository = CsvRepository.inMemory(new ExpenseCsvMapper(), Expense::id);
        repository.save(new Expense(7, TODAY, BigDecimal.TEN, Category.FOOD, PaymentMethod.UPI, "Old"));
        ExpenseService reopened = new ExpenseService(repository, TestSupport.clockAt(TODAY));
        assertEquals(8, reopened.add(TODAY, BigDecimal.ONE, Category.FOOD, PaymentMethod.UPI, "New").id());
    }

    @Test
    public void updateReplacesAllFields() {
        Expense original = add("100", Category.FOOD, "Lunch");
        service.update(original.id(), TODAY.minusDays(3), new BigDecimal("120"), Category.TRAVEL,
                PaymentMethod.CREDIT_CARD, "Train snacks");

        Expense updated = service.getById(original.id());
        assertEquals(TODAY.minusDays(3), updated.date());
        assertAmount("120", updated.amount());
        assertEquals(Category.TRAVEL, updated.category());
        assertEquals(PaymentMethod.CREDIT_CARD, updated.paymentMethod());
        assertEquals("Train snacks", updated.description());
        assertEquals(1, service.count());
    }

    @Test
    public void unknownIdsThrowNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(42));
        assertThrows(NotFoundException.class, () -> service.delete(42));
        assertThrows(NotFoundException.class, () -> service.update(42, TODAY, BigDecimal.TEN, Category.FOOD,
                PaymentMethod.UPI, "Nothing here"));
    }

    @Test
    public void deleteRemovesExpense() {
        Expense saved = add("100", Category.FOOD, "Lunch");
        service.delete(saved.id());
        assertEquals(0, service.count());
    }

    @Test
    public void filterCombinesCriteria() {
        add("450", Category.FOOD, "Pizza night");
        add("80", Category.FOOD, "Pizza slice");
        add("500", Category.SHOPPING, "Pizza cutter");
        add("300", Category.FOOD, "Thali");

        ExpenseFilter filter = ExpenseFilter.all().category(Category.FOOD)
                .minAmount(new BigDecimal("100")).keyword("PIZZA");
        List<Expense> result = service.search(filter, SortOrder.DATE_ASC);
        assertEquals(1, result.size());
        assertEquals("Pizza night", result.get(0).description());
    }

    @Test
    public void filterByDateRangeIsInclusive() {
        service.add(TODAY.minusDays(10), BigDecimal.TEN, Category.FOOD, PaymentMethod.UPI, "Too early");
        service.add(TODAY.minusDays(5), BigDecimal.TEN, Category.FOOD, PaymentMethod.UPI, "Start");
        service.add(TODAY, BigDecimal.TEN, Category.FOOD, PaymentMethod.UPI, "End");

        List<Expense> result = service.search(ExpenseFilter.all().between(TODAY.minusDays(5), TODAY), SortOrder.DATE_ASC);
        assertEquals(List.of("Start", "End"), result.stream().map(Expense::description).toList());
    }

    @Test
    public void sortsByAmountDescending() {
        add("50", Category.FOOD, "Small");
        add("500", Category.FOOD, "Large");
        add("150", Category.FOOD, "Medium");
        List<String> order = service.search(ExpenseFilter.all(), SortOrder.AMOUNT_DESC).stream()
                .map(Expense::description).toList();
        assertEquals(List.of("Large", "Medium", "Small"), order);
    }
}
