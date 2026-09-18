package com.expensetracker.testing;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The assertions used by the test suite. A failed assertion throws {@link AssertionError}.
 */
public final class Assertions {

    /** A block of code expected to throw. */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    private Assertions() {
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    /** Compares numerically, so 5, 5.0 and 5.00 are equal. */
    public static void assertAmount(String expected, BigDecimal actual) {
        if (actual == null || new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("expected amount <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, ThrowingRunnable code) {
        try {
            code.run();
        } catch (Throwable thrown) {
            if (expectedType.isInstance(thrown)) {
                return expectedType.cast(thrown);
            }
            throw new AssertionError("expected " + expectedType.getSimpleName() + " but got " + thrown, thrown);
        }
        throw new AssertionError("expected " + expectedType.getSimpleName() + " to be thrown, but nothing was");
    }
}
