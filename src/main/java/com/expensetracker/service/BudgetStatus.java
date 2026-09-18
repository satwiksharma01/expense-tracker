package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.util.Money;

import java.math.BigDecimal;

/**
 * How much of a budget has been used in a given month.
 */
public record BudgetStatus(Budget budget, BigDecimal spent) {

    /** Percentage of the limit at which the user gets a warning. */
    public static final BigDecimal WARNING_PERCENT = new BigDecimal("80");

    public enum Level { OK, WARNING, EXCEEDED }

    public BigDecimal remaining() {
        return budget.monthlyLimit().subtract(spent);
    }

    public BigDecimal percentUsed() {
        return Money.percent(spent, budget.monthlyLimit());
    }

    public Level level() {
        if (spent.compareTo(budget.monthlyLimit()) > 0) {
            return Level.EXCEEDED;
        }
        if (percentUsed().compareTo(WARNING_PERCENT) >= 0) {
            return Level.WARNING;
        }
        return Level.OK;
    }

    public String message() {
        String scope = budget.scopeLabel() + " budget";
        return switch (level()) {
            case EXCEEDED -> scope + " EXCEEDED by " + Money.format(spent.subtract(budget.monthlyLimit()))
                    + " (" + percentUsed() + "% used)";
            case WARNING -> scope + " at " + percentUsed() + "%, only " + Money.format(remaining()) + " left";
            case OK -> scope + " on track: " + Money.format(remaining()) + " left";
        };
    }
}
