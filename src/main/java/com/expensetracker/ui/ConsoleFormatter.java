package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Labeled;
import com.expensetracker.model.RecurringExpense;
import com.expensetracker.service.BudgetStatus;
import com.expensetracker.service.MonthlySummary;
import com.expensetracker.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the text tables and ASCII charts shown in the console. Methods return strings
 * instead of printing, so the same output can be written to a report file or checked in tests.
 */
public final class ConsoleFormatter {

    private static final int BAR_WIDTH = 30;
    private static final String NL = System.lineSeparator();
    private static final DateTimeFormatter MONTH_NAME = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);

    private ConsoleFormatter() {
    }

    public static String expenseTable(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return "  (no expenses found)" + NL;
        }
        String row = "  %-5s %-10s %-17s %-12s %17s  %s" + NL;
        String rule = "  " + "-".repeat(100) + NL;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(row, "ID", "Date", "Category", "Paid by", "Amount", "Description")).append(rule);
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : expenses) {
            sb.append(String.format(row, e.id(), e.date(), e.category().label(), e.paymentMethod().label(),
                    Money.format(e.amount()), truncate(e.description(), 42)));
            total = total.add(e.amount());
        }
        sb.append(rule).append(String.format("  %d expense(s), total %s%n", expenses.size(), Money.format(total)));
        return sb.toString();
    }

    public static String budgetTable(List<BudgetStatus> statuses) {
        if (statuses.isEmpty()) {
            return "  (no budgets set - use the Budgets menu to add one)" + NL;
        }
        String row = "  %-18s %17s %17s %17s  %-22s %s" + NL;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(row, "Budget", "Limit", "Spent", "Remaining", "Used", "Status"));
        for (BudgetStatus s : statuses) {
            String used = "[" + bar(s.spent(), s.budget().monthlyLimit(), 12, true) + "] " + s.percentUsed() + "%";
            sb.append(String.format(row, s.budget().scopeLabel(), Money.format(s.budget().monthlyLimit()),
                    Money.format(s.spent()), Money.format(s.remaining()), used, s.level()));
        }
        return sb.toString();
    }

    public static String monthlyReport(MonthlySummary s, List<BudgetStatus> budgets, List<String> insights) {
        StringBuilder sb = new StringBuilder();
        String title = "Monthly report - " + s.month().format(MONTH_NAME);
        sb.append(NL).append("  ").append(title).append(NL).append("  ").append("=".repeat(title.length())).append(NL);
        sb.append(String.format("  %-22s %s%n", "Total spent:", Money.format(s.total())));
        sb.append(String.format("  %-22s %d%n", "Transactions:", s.expenseCount()));
        sb.append(String.format("  %-22s %s%n", "Daily average:", Money.format(s.dailyAverage())));
        sb.append(String.format("  %-22s %s%n", "Projected month-end:", Money.format(s.projectedTotal())));
        s.largest().ifPresent(e -> sb.append(String.format("  %-22s %s - %s (%s)%n", "Largest expense:",
                Money.format(e.amount()), e.description(), e.date())));

        sb.append(NL).append("  Spending by category").append(NL);
        sb.append(shareChart(s.byCategory(), s.total()));
        sb.append(NL).append("  Spending by payment method").append(NL);
        sb.append(shareChart(s.byPaymentMethod(), s.total()));
        sb.append(NL).append("  Budgets").append(NL).append(budgetTable(budgets));
        sb.append(NL).append("  Insights").append(NL);
        for (String insight : insights) {
            sb.append("   * ").append(insight).append(NL);
        }
        return sb.toString();
    }

    public static String trendChart(Map<YearMonth, BigDecimal> totals) {
        BigDecimal max = totals.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<YearMonth, BigDecimal> entry : totals.entrySet()) {
            sb.append(String.format("  %-9s %-" + BAR_WIDTH + "s %s%n",
                    entry.getKey().format(DateTimeFormatter.ofPattern("MMM uuuu", Locale.ENGLISH)),
                    bar(entry.getValue(), max, BAR_WIDTH, false), Money.format(entry.getValue())));
        }
        return sb.toString();
    }

    /** One bar per key, scaled to the largest value, with its share of {@code total}. */
    public static <E extends Enum<E>> String shareChart(Map<E, BigDecimal> values, BigDecimal total) {
        if (values.isEmpty()) {
            return "  (nothing to show)" + NL;
        }
        BigDecimal max = Collections.max(values.values());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<E, BigDecimal> entry : values.entrySet()) {
            sb.append(String.format("  %-17s %-" + BAR_WIDTH + "s %17s %6s%%%n", labelOf(entry.getKey()),
                    bar(entry.getValue(), max, BAR_WIDTH, false), Money.format(entry.getValue()),
                    Money.percent(entry.getValue(), total)));
        }
        return sb.toString();
    }

    public static String recurringTable(List<RecurringExpense> schedules) {
        if (schedules.isEmpty()) {
            return "  (no recurring expenses yet)" + NL;
        }
        String row = "  %-4s %-30s %17s %-9s %-17s %-11s %s" + NL;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(row, "ID", "Description", "Amount", "Every", "Category", "Next due", "Status"));
        for (RecurringExpense r : schedules) {
            sb.append(String.format(row, r.id(), truncate(r.description(), 30), Money.format(r.amount()),
                    r.frequency().label(), r.category().label(), r.nextDueDate(), r.active() ? "active" : "paused"));
        }
        return sb.toString();
    }

    public static String expenseDetails(Expense e) {
        return String.format("  #%d  %s  %s  %s  %s  \"%s\"%n", e.id(), e.date(), Money.format(e.amount()),
                e.category().label(), e.paymentMethod().label(), e.description());
    }

    /**
     * Draws a bar of '#' proportional to value / max. With {@code capped} the bar never
     * exceeds the width, and a trailing '!' marks values above max (an exceeded budget).
     */
    static String bar(BigDecimal value, BigDecimal max, int width, boolean capped) {
        if (max.signum() <= 0 || value.signum() <= 0) {
            return capped ? ".".repeat(width) : "";
        }
        int filled = value.multiply(BigDecimal.valueOf(width)).divide(max, 0, RoundingMode.HALF_UP).intValue();
        filled = Math.max(1, filled);
        if (!capped) {
            return "#".repeat(Math.min(filled, width));
        }
        if (filled > width) {
            return "#".repeat(width - 1) + "!";
        }
        return "#".repeat(filled) + ".".repeat(width - filled);
    }

    private static String labelOf(Enum<?> value) {
        return value instanceof Labeled labeled ? labeled.label() : value.name();
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
