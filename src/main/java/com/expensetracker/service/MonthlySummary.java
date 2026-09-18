package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;

/**
 * Aggregated figures for one calendar month.
 *
 * @param dailyAverage   total divided by the days elapsed in the month (all days for past months)
 * @param projectedTotal expected month-end total at the current pace; equals total for past months
 * @param byCategory     totals per category, largest first
 * @param byPaymentMethod totals per payment method, largest first
 * @param largestExpense the single biggest expense, or {@code null} if there were none
 */
public record MonthlySummary(YearMonth month,
                             BigDecimal total,
                             int expenseCount,
                             BigDecimal dailyAverage,
                             BigDecimal projectedTotal,
                             Map<Category, BigDecimal> byCategory,
                             Map<PaymentMethod, BigDecimal> byPaymentMethod,
                             Expense largestExpense) {

    public Optional<Expense> largest() {
        return Optional.ofNullable(largestExpense);
    }

    public boolean isEmpty() {
        return expenseCount == 0;
    }
}
