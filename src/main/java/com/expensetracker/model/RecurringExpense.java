package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A schedule that produces an {@link Expense} automatically, e.g. monthly rent or a subscription.
 *
 * @param occurrencesGenerated how many expenses have already been created from this schedule;
 *                             the next due date is derived from it, which makes generation idempotent
 */
public record RecurringExpense(int id,
                               String description,
                               BigDecimal amount,
                               Category category,
                               PaymentMethod paymentMethod,
                               Frequency frequency,
                               LocalDate startDate,
                               int occurrencesGenerated,
                               boolean active) {

    public RecurringExpense {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(paymentMethod, "paymentMethod");
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(startDate, "startDate");
        if (occurrencesGenerated < 0) {
            throw new IllegalArgumentException("occurrencesGenerated must not be negative");
        }
    }

    public LocalDate nextDueDate() {
        return frequency.occurrence(startDate, occurrencesGenerated);
    }

    public RecurringExpense withOccurrencesGenerated(int count) {
        return new RecurringExpense(id, description, amount, category, paymentMethod, frequency,
                startDate, count, active);
    }

    public RecurringExpense withActive(boolean isActive) {
        return new RecurringExpense(id, description, amount, category, paymentMethod, frequency,
                startDate, occurrencesGenerated, isActive);
    }
}
