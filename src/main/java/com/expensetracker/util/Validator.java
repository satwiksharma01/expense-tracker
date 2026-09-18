package com.expensetracker.util;

import com.expensetracker.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Central place for the business rules applied to user input. Every method either returns
 * a cleaned-up value or throws a {@link ValidationException} with a message meant for the user.
 */
public final class Validator {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");
    public static final int MAX_DESCRIPTION_LENGTH = 100;
    public static final LocalDate EARLIEST_DATE = LocalDate.of(2000, 1, 1);

    private Validator() {
    }

    public static BigDecimal requireValidAmount(BigDecimal amount) {
        return requirePositiveMoney(amount, "Amount");
    }

    public static BigDecimal requireValidLimit(BigDecimal limit) {
        return requirePositiveMoney(limit, "Budget limit");
    }

    private static BigDecimal requirePositiveMoney(BigDecimal value, String field) {
        requirePresent(value, field);
        if (value.signum() <= 0) {
            throw new ValidationException(field + " must be greater than zero");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException(field + " must not exceed " + Money.format(MAX_AMOUNT));
        }
        if (value.stripTrailingZeros().scale() > Money.SCALE) {
            throw new ValidationException(field + " can have at most " + Money.SCALE + " decimal places");
        }
        return Money.normalize(value);
    }

    public static String requireValidDescription(String description) {
        return requireValidDescription(description, MAX_DESCRIPTION_LENGTH);
    }

    public static String requireValidDescription(String description, int maxLength) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description is required");
        }
        String trimmed = description.strip();
        if (trimmed.length() > maxLength) {
            throw new ValidationException("Description must be at most " + maxLength + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ValidationException("Description must not contain line breaks or control characters");
        }
        return trimmed;
    }

    /** Expenses are recorded after they happen, so dates in the future are rejected. */
    public static LocalDate requireValidExpenseDate(LocalDate date, LocalDate today) {
        requirePresent(date, "Date");
        if (date.isBefore(EARLIEST_DATE)) {
            throw new ValidationException("Date must be on or after " + EARLIEST_DATE);
        }
        if (date.isAfter(today)) {
            throw new ValidationException("Expense date " + date + " is in the future");
        }
        return date;
    }

    public static <T> T requirePresent(T value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
        return value;
    }
}
