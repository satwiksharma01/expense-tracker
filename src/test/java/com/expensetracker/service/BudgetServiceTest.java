package com.expensetracker.service;

import com.expensetracker.exception.NotFoundException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.testing.Test;
import com.expensetracker.testing.TestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.expensetracker.testing.Assertions.assertAmount;
import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertThrows;
import static com.expensetracker.testing.Assertions.assertTrue;

public class BudgetServiceTest {

    private static final LocalDate TODAY = TestSupport.TODAY;
    private static final YearMonth MONTH = YearMonth.from(TODAY);

    private final ExpenseService expenses = TestSupport.expenseService();
    private final BudgetService budgets = TestSupport.budgetService(expenses);

    private Expense spend(String amount, Category category) {
        return expenses.add(TODAY, new BigDecimal(amount), category, PaymentMethod.UPI, "Test");
    }

    private BudgetStatus.Level levelOf(Category category) {
        return budgets.statusFor(MONTH).stream()
                .filter(s -> s.budget().category() == category)
                .findFirst().orElseThrow().level();
    }

    @Test
    public void statusMovesFromOkToWarningToExceeded() {
        budgets.setBudget(Category.FOOD, new BigDecimal("1000"));
        spend("500", Category.FOOD);
        assertEquals(BudgetStatus.Level.OK, levelOf(Category.FOOD));
        spend("300", Category.FOOD);
        assertEquals(BudgetStatus.Level.WARNING, levelOf(Category.FOOD));
        spend("300", Category.FOOD);
        assertEquals(BudgetStatus.Level.EXCEEDED, levelOf(Category.FOOD));
    }

    @Test
    public void overallBudgetCountsEveryCategory() {
        budgets.setBudget(null, new BigDecimal("1000"));
        spend("300", Category.FOOD);
        spend("200", Category.TRANSPORT);
        BudgetStatus overall = budgets.statusFor(MONTH).get(0);
        assertTrue(overall.budget().isOverall(), "overall budget should be listed first");
        assertAmount("500", overall.spent());
        assertAmount("500", overall.remaining());
        assertAmount("50.0", overall.percentUsed());
    }

    @Test
    public void onlyTheSelectedMonthCounts() {
        budgets.setBudget(Category.FOOD, new BigDecimal("1000"));
        expenses.add(TODAY.minusMonths(1), new BigDecimal("900"), Category.FOOD, PaymentMethod.UPI, "Last month");
        assertAmount("0", budgets.statusFor(MONTH).get(0).spent());
    }

    @Test
    public void alertsOnlyMentionAffectedBudgets() {
        budgets.setBudget(Category.FOOD, new BigDecimal("100"));
        budgets.setBudget(Category.TRANSPORT, new BigDecimal("100"));
        spend("150", Category.FOOD);

        List<String> alerts = budgets.alertsFor(spend("90", Category.TRANSPORT));
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).startsWith("Transport budget at 90.0%"), "unexpected alert: " + alerts.get(0));
    }

    @Test
    public void setBudgetReplacesExistingLimit() {
        budgets.setBudget(Category.FOOD, new BigDecimal("1000"));
        budgets.setBudget(Category.FOOD, new BigDecimal("2000"));
        assertEquals(1, budgets.list().size());
        assertAmount("2000", budgets.list().get(0).monthlyLimit());
    }

    @Test
    public void rejectsInvalidLimitsAndMissingBudgets() {
        assertThrows(ValidationException.class, () -> budgets.setBudget(Category.FOOD, BigDecimal.ZERO));
        assertThrows(NotFoundException.class, () -> budgets.removeBudget(Category.HEALTH));
    }
}
