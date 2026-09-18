package com.expensetracker.util;

import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Labeled;

import java.util.Locale;

/**
 * Turns user input into enum constants for any enum that implements {@link Labeled}.
 */
public final class EnumParser {

    private EnumParser() {
    }

    /**
     * Accepts the constant name ("CREDIT_CARD"), its label ("Credit Card") or its
     * 1-based position in the on-screen list ("4"). Matching ignores case.
     */
    public static <E extends Enum<E> & Labeled> E parse(Class<E> type, String input) {
        String typeName = type.getSimpleName().toLowerCase(Locale.ROOT);
        if (input == null || input.isBlank()) {
            throw new ValidationException("Please choose a " + typeName);
        }
        String text = input.strip();
        E[] constants = type.getEnumConstants();
        if (text.matches("\\d{1,3}")) {
            int index = Integer.parseInt(text) - 1;
            if (index >= 0 && index < constants.length) {
                return constants[index];
            }
        }
        String asConstantName = text.replace(' ', '_').replace('-', '_');
        for (E constant : constants) {
            if (constant.name().equalsIgnoreCase(asConstantName) || constant.label().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        throw new ValidationException("Unknown " + typeName + " '" + text + "'");
    }
}
