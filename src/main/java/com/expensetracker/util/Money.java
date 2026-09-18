package com.expensetracker.util;

import com.expensetracker.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helpers for monetary values. All amounts are {@link BigDecimal} with two decimal places;
 * {@code double} is never used for money because it cannot represent values like 0.1 exactly.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final String CURRENCY = "Rs.";

    /** Plain decimal numbers only: rejects exponents such as "1e9" and other surprises. */
    private static final Pattern NUMBER = Pattern.compile("-?\\d{1,12}(\\.\\d{1,10})?");

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Parses user input such as "250", "1,250.50", "Rs. 99" or "₹ 40".
     *
     * @throws ValidationException if the text is not a plain decimal number
     */
    public static BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Amount is required");
        }
        String cleaned = text.strip()
                .replace(",", "")
                .replace("₹", "")
                .replaceFirst("(?i)^rs\\.?", "")
                .strip();
        if (!NUMBER.matcher(cleaned).matches()) {
            throw new ValidationException("'" + text.strip() + "' is not a valid amount");
        }
        return new BigDecimal(cleaned);
    }

    public static String format(BigDecimal value) {
        return String.format(Locale.ENGLISH, "%s %,.2f", CURRENCY, value);
    }

    public static BigDecimal sum(Collection<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Returns part / whole * 100 rounded to one decimal, or zero when whole is zero. */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }
}
