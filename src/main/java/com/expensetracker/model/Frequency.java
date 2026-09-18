package com.expensetracker.model;

import java.time.LocalDate;

/**
 * How often a recurring expense repeats.
 */
public enum Frequency implements Labeled {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    private final String label;

    Frequency(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /**
     * Returns the date of the n-th occurrence (0 = the start date itself).
     * Occurrences are always computed from the start date rather than from the previous
     * occurrence, so a schedule starting on the 31st does not drift to the 28th after February.
     */
    public LocalDate occurrence(LocalDate start, int n) {
        return switch (this) {
            case DAILY -> start.plusDays(n);
            case WEEKLY -> start.plusWeeks(n);
            case MONTHLY -> start.plusMonths(n);
            case YEARLY -> start.plusYears(n);
        };
    }
}
