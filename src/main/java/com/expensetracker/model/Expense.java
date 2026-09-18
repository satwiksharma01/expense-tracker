package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * A single recorded expense. Immutable: an update produces a new instance with the same id.
 * Business rules (positive amount, no future dates, ...) are enforced by the service layer;
 * the record itself only guarantees that no field is null.
 */
public record Expense(int id,
                      LocalDate date,
                      BigDecimal amount,
                      Category category,
                      PaymentMethod paymentMethod,
                      String description) {

    public Expense {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(paymentMethod, "paymentMethod");
        description = description == null ? "" : description.strip();
    }

    public YearMonth month() {
        return YearMonth.from(date);
    }
}
