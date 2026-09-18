package com.expensetracker.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A monthly spending limit, either for one category or for all spending combined.
 *
 * @param category     the category the limit applies to, or {@code null} for the overall budget
 * @param monthlyLimit the maximum amount the user wants to spend per calendar month
 */
public record Budget(Category category, BigDecimal monthlyLimit) {

    public static final String OVERALL_KEY = "OVERALL";

    public Budget {
        Objects.requireNonNull(monthlyLimit, "monthlyLimit");
    }

    public static Budget overall(BigDecimal limit) {
        return new Budget(null, limit);
    }

    public static Budget forCategory(Category category, BigDecimal limit) {
        return new Budget(Objects.requireNonNull(category, "category"), limit);
    }

    /** Storage key: the category name, or {@value #OVERALL_KEY} for the overall budget. */
    public static String keyFor(Category category) {
        return category == null ? OVERALL_KEY : category.name();
    }

    public boolean isOverall() {
        return category == null;
    }

    public String key() {
        return keyFor(category);
    }

    public String scopeLabel() {
        return isOverall() ? "Overall" : category.label();
    }
}
