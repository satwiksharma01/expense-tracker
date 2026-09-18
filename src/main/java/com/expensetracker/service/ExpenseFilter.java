package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Search criteria for expenses, built fluently. Criteria left unset match everything.
 * <pre>{@code
 * ExpenseFilter.all().category(Category.FOOD).minAmount(new BigDecimal("100")).keyword("pizza")
 * }</pre>
 */
public final class ExpenseFilter implements Predicate<Expense> {

    private LocalDate from;
    private LocalDate to;
    private Category category;
    private PaymentMethod paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String keyword;

    private ExpenseFilter() {
    }

    public static ExpenseFilter all() {
        return new ExpenseFilter();
    }

    public static ExpenseFilter forMonth(YearMonth month) {
        return all().between(month.atDay(1), month.atEndOfMonth());
    }

    public ExpenseFilter between(LocalDate fromInclusive, LocalDate toInclusive) {
        this.from = fromInclusive;
        this.to = toInclusive;
        return this;
    }

    public ExpenseFilter category(Category value) {
        this.category = value;
        return this;
    }

    public ExpenseFilter paymentMethod(PaymentMethod value) {
        this.paymentMethod = value;
        return this;
    }

    public ExpenseFilter minAmount(BigDecimal value) {
        this.minAmount = value;
        return this;
    }

    public ExpenseFilter maxAmount(BigDecimal value) {
        this.maxAmount = value;
        return this;
    }

    public ExpenseFilter keyword(String value) {
        this.keyword = value == null || value.isBlank() ? null : value.strip().toLowerCase(Locale.ROOT);
        return this;
    }

    @Override
    public boolean test(Expense e) {
        return (from == null || !e.date().isBefore(from))
                && (to == null || !e.date().isAfter(to))
                && (category == null || e.category() == category)
                && (paymentMethod == null || e.paymentMethod() == paymentMethod)
                && (minAmount == null || e.amount().compareTo(minAmount) >= 0)
                && (maxAmount == null || e.amount().compareTo(maxAmount) <= 0)
                && (keyword == null || e.description().toLowerCase(Locale.ROOT).contains(keyword));
    }

    /** Human-readable summary of the active criteria, e.g. for table captions. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        if (from != null) {
            parts.add("from " + from);
        }
        if (to != null) {
            parts.add("to " + to);
        }
        if (category != null) {
            parts.add("category " + category.label());
        }
        if (paymentMethod != null) {
            parts.add("paid by " + paymentMethod.label());
        }
        if (minAmount != null) {
            parts.add("amount >= " + minAmount.toPlainString());
        }
        if (maxAmount != null) {
            parts.add("amount <= " + maxAmount.toPlainString());
        }
        if (keyword != null) {
            parts.add("matching '" + keyword + "'");
        }
        return parts.isEmpty() ? "all expenses" : String.join(", ", parts);
    }
}
