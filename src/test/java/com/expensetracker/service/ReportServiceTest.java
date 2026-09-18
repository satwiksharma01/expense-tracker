package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.testing.Test;
import com.expensetracker.testing.TestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static com.expensetracker.testing.Assertions.assertAmount;
import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertTrue;

public class ReportServiceTest {

    private static final LocalDate TENTH_OF_SEPTEMBER = LocalDate.of(2026, 9, 10);
    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);

    private final ExpenseService expenses = TestSupport.expenseService(TENTH_OF_SEPTEMBER);
    private final BudgetService budgets = TestSupport.budgetService(expenses);
    private final ReportService reports = new ReportService(expenses, budgets);

    private void spend(LocalDate date, String amount, Category category) {
        expenses.add(date, new BigDecimal(amount), category, PaymentMethod.UPI, "Test " + category);
    }

    @Test
    public void monthlySummaryTotalsAndRanksCategories() {
        spend(LocalDate.of(2026, 9, 1), "300", Category.FOOD);
        spend(LocalDate.of(2026, 9, 2), "200", Category.FOOD);
        spend(LocalDate.of(2026, 9, 3), "100", Category.TRANSPORT);
        spend(LocalDate.of(2026, 8, 31), "999", Category.TRANSPORT);

        MonthlySummary summary = reports.monthlySummary(SEPTEMBER);
        assertAmount("600", summary.total());
        assertEquals(3, summary.expenseCount());
        Map.Entry<Category, BigDecimal> first = summary.byCategory().entrySet().iterator().next();
        assertEquals(Category.FOOD, first.getKey());
        assertAmount("500", first.getValue());
        assertAmount("300", summary.largest().orElseThrow().amount());
    }

    @Test
    public void projectsCurrentMonthLinearly() {
        spend(LocalDate.of(2026, 9, 5), "1000", Category.FOOD);
        MonthlySummary summary = reports.monthlySummary(SEPTEMBER);
        assertAmount("100", summary.dailyAverage());
        assertAmount("3000", summary.projectedTotal());
    }

    @Test
    public void recurringPaymentsAreNotScaledInProjection() {
        spend(LocalDate.of(2026, 9, 5), "1000", Category.FOOD);
        expenses.add(LocalDate.of(2026, 9, 1), new BigDecimal("8000"), Category.RENT, PaymentMethod.NET_BANKING,
                "Rent" + RecurringExpenseService.GENERATED_SUFFIX);
        assertAmount("11000", reports.monthlySummary(SEPTEMBER).projectedTotal());
    }

    @Test
    public void pastMonthProjectionEqualsActualTotal() {
        spend(LocalDate.of(2026, 8, 5), "1000", Category.FOOD);
        MonthlySummary august = reports.monthlySummary(YearMonth.of(2026, 8));
        assertAmount("1000", august.projectedTotal());
        assertAmount("32.26", august.dailyAverage());
    }

    @Test
    public void trendIncludesMonthsWithoutSpending() {
        spend(LocalDate.of(2026, 7, 15), "700", Category.FOOD);
        spend(LocalDate.of(2026, 9, 1), "900", Category.FOOD);

        Map<YearMonth, BigDecimal> trend = reports.monthlyTrend(SEPTEMBER, 3);
        assertEquals(List.of(YearMonth.of(2026, 7), YearMonth.of(2026, 8), SEPTEMBER), List.copyOf(trend.keySet()));
        assertAmount("700", trend.get(YearMonth.of(2026, 7)));
        assertAmount("0", trend.get(YearMonth.of(2026, 8)));
        assertAmount("900", trend.get(SEPTEMBER));
    }

    @Test
    public void insightsCompareWithPreviousMonth() {
        spend(LocalDate.of(2026, 8, 10), "1000", Category.FOOD);
        spend(LocalDate.of(2026, 9, 2), "1000", Category.FOOD);
        spend(LocalDate.of(2026, 9, 3), "500", Category.SHOPPING);

        List<String> insights = reports.insights(SEPTEMBER);
        assertTrue(insights.get(0).startsWith("Spending is up 50.0%"), "got: " + insights.get(0));
        assertTrue(insights.stream().anyMatch(s -> s.startsWith("Shopping grew the most")), "got: " + insights);
    }

    @Test
    public void insightsWarnWhenPaceBreaksOverallBudget() {
        budgets.setBudget(null, new BigDecimal("2000"));
        spend(LocalDate.of(2026, 9, 5), "1000", Category.FOOD);
        assertTrue(reports.insights(SEPTEMBER).stream().anyMatch(s -> s.contains("overshoot")),
                "projected 3000 against a 2000 budget should warn");
    }

    @Test
    public void percentChangeIsEmptyWithoutPreviousSpending() {
        assertTrue(ReportService.percentChange(BigDecimal.ZERO, BigDecimal.TEN).isEmpty(), "no baseline");
        assertAmount("-25.0", ReportService.percentChange(new BigDecimal("200"), new BigDecimal("150")).orElseThrow());
    }
}
