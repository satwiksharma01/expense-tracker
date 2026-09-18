package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Labeled;

import java.util.Comparator;

/**
 * Orderings available when listing expenses. Ties are broken by id so output is stable.
 */
public enum SortOrder implements Labeled {
    DATE_DESC("Newest first",
            Comparator.comparing(Expense::date).thenComparing(Expense::id).reversed()),
    DATE_ASC("Oldest first",
            Comparator.comparing(Expense::date).thenComparing(Expense::id)),
    AMOUNT_DESC("Highest amount first",
            Comparator.comparing(Expense::amount).thenComparing(Expense::id).reversed()),
    AMOUNT_ASC("Lowest amount first",
            Comparator.comparing(Expense::amount).thenComparing(Expense::id)),
    CATEGORY("By category",
            Comparator.comparing(Expense::category).thenComparing(Expense::date).thenComparing(Expense::id));

    private final String label;
    private final Comparator<Expense> comparator;

    SortOrder(String label, Comparator<Expense> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    @Override
    public String label() {
        return label;
    }

    public Comparator<Expense> comparator() {
        return comparator;
    }
}
