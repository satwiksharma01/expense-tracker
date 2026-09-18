package com.expensetracker.ui;

import com.expensetracker.exception.ExpenseTrackerException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Labeled;
import com.expensetracker.util.EnumParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.function.Function;

/**
 * Reads and validates console input. Every {@code read...} method keeps asking until the
 * answer is valid, printing the validation message after each bad attempt.
 */
public class InputReader {

    private static final DateTimeFormatter DAY_FIRST =
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final BufferedReader in;
    private final PrintStream out;

    public InputReader(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * @throws EndOfInputException when input has been closed
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new EndOfInputException();
            }
            return line.strip();
        } catch (IOException e) {
            throw new EndOfInputException();
        }
    }

    public <T> T read(String prompt, Function<String, T> parser) {
        while (true) {
            String line = readLine(prompt);
            try {
                return parser.apply(line);
            } catch (ExpenseTrackerException | IllegalArgumentException e) {
                out.println("  ! " + e.getMessage());
            }
        }
    }

    /** Like {@link #read}, but an empty answer returns {@code defaultValue}. */
    public <T> T readOrDefault(String prompt, T defaultValue, Function<String, T> parser) {
        return read(prompt, text -> text.isEmpty() ? defaultValue : parser.apply(text));
    }

    public int readInt(String prompt, int min, int max) {
        return read(prompt, text -> parseIntInRange(text, min, max));
    }

    public boolean confirm(String question) {
        return read(question + " (y/n): ", text -> switch (text.toLowerCase(Locale.ROOT)) {
            case "y", "yes" -> true;
            case "n", "no" -> false;
            default -> throw new ValidationException("Please answer y or n");
        });
    }

    /** Shows a numbered list and returns the chosen constant; blank selects the default if there is one. */
    public <E extends Enum<E> & Labeled> E chooseEnum(String title, Class<E> type, E defaultValue) {
        printOptions(title, type);
        if (defaultValue == null) {
            return read("Choice: ", text -> EnumParser.parse(type, text));
        }
        return readOrDefault("Choice [" + defaultValue.label() + "]: ", defaultValue,
                text -> EnumParser.parse(type, text));
    }

    /** Like {@link #chooseEnum} but blank means "no preference" and returns {@code null}. */
    public <E extends Enum<E> & Labeled> E chooseOptionalEnum(String title, Class<E> type) {
        printOptions(title, type);
        return readOrDefault("Choice [any]: ", null, text -> EnumParser.parse(type, text));
    }

    private <E extends Enum<E> & Labeled> void printOptions(String title, Class<E> type) {
        out.println(title);
        E[] values = type.getEnumConstants();
        for (int i = 0; i < values.length; i++) {
            out.printf("  %2d. %s%n", i + 1, values[i].label());
        }
    }

    /** Accepts "today", "yesterday", ISO dates (2026-09-18) and day-first dates (18-09-2026 or 18/9/2026). */
    public static LocalDate parseDate(String text, LocalDate today) {
        String value = text.strip().toLowerCase(Locale.ROOT);
        if (value.equals("today")) {
            return today;
        }
        if (value.equals("yesterday")) {
            return today.minusDays(1);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through to the day-first format
        }
        try {
            return LocalDate.parse(value.replace('/', '-'), DAY_FIRST);
        } catch (DateTimeParseException e) {
            throw new ValidationException("'" + text.strip() + "' is not a valid date. Use YYYY-MM-DD, DD-MM-YYYY or 'today'");
        }
    }

    public static YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new ValidationException("'" + text.strip() + "' is not a valid month. Use YYYY-MM, e.g. 2026-09");
        }
    }

    public static int parseIntInRange(String text, int min, int max) {
        int value;
        try {
            value = Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            throw new ValidationException("Please enter a whole number");
        }
        if (value < min || value > max) {
            throw new ValidationException("Please enter a number between " + min + " and " + max);
        }
        return value;
    }
}
