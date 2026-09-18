package com.expensetracker.util;

import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.testing.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.expensetracker.testing.Assertions.assertAmount;
import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertThrows;

public class ValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);

    @Test
    public void rejectsZeroNegativeAndExcessiveAmounts() {
        assertThrows(ValidationException.class, () -> Validator.requireValidAmount(BigDecimal.ZERO));
        assertThrows(ValidationException.class, () -> Validator.requireValidAmount(new BigDecimal("-5")));
        assertThrows(ValidationException.class, () -> Validator.requireValidAmount(new BigDecimal("10000000.01")));
        assertThrows(ValidationException.class, () -> Validator.requireValidAmount(null));
    }

    @Test
    public void rejectsMoreThanTwoDecimalPlaces() {
        assertThrows(ValidationException.class, () -> Validator.requireValidAmount(new BigDecimal("10.555")));
        assertAmount("10.5", Validator.requireValidAmount(new BigDecimal("10.500")));
    }

    @Test
    public void normalizesAmountsToTwoDecimals() {
        assertEquals("12.50", Validator.requireValidAmount(new BigDecimal("12.5")).toPlainString());
    }

    @Test
    public void rejectsFutureAndVeryOldDates() {
        assertThrows(ValidationException.class, () -> Validator.requireValidExpenseDate(TODAY.plusDays(1), TODAY));
        assertThrows(ValidationException.class,
                () -> Validator.requireValidExpenseDate(LocalDate.of(1999, 12, 31), TODAY));
        assertEquals(TODAY, Validator.requireValidExpenseDate(TODAY, TODAY));
    }

    @Test
    public void rejectsBlankLongAndMultilineDescriptions() {
        assertThrows(ValidationException.class, () -> Validator.requireValidDescription("   "));
        assertThrows(ValidationException.class, () -> Validator.requireValidDescription("x".repeat(101)));
        assertThrows(ValidationException.class, () -> Validator.requireValidDescription("line one\nline two"));
        assertEquals("Lunch", Validator.requireValidDescription("  Lunch  "));
    }

    @Test
    public void moneyParserAcceptsCommonFormats() {
        assertAmount("1250.50", Money.parse("1,250.50"));
        assertAmount("99", Money.parse("Rs. 99"));
        assertAmount("40", Money.parse("₹40"));
        assertAmount("5", Money.parse("rs 5"));
    }

    @Test
    public void moneyParserRejectsNonNumbers() {
        assertThrows(ValidationException.class, () -> Money.parse("abc"));
        assertThrows(ValidationException.class, () -> Money.parse("1e9"));
        assertThrows(ValidationException.class, () -> Money.parse("12.3.4"));
        assertThrows(ValidationException.class, () -> Money.parse(""));
    }

    @Test
    public void enumParserAcceptsNameLabelAndPosition() {
        assertEquals(Category.FOOD, EnumParser.parse(Category.class, "food"));
        assertEquals(Category.FOOD, EnumParser.parse(Category.class, "Food & Dining"));
        assertEquals(Category.TRANSPORT, EnumParser.parse(Category.class, "2"));
        assertEquals(PaymentMethod.CREDIT_CARD, EnumParser.parse(PaymentMethod.class, "credit card"));
        assertThrows(ValidationException.class, () -> EnumParser.parse(Category.class, "99"));
        assertThrows(ValidationException.class, () -> EnumParser.parse(Category.class, "groceries"));
    }
}
