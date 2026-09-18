package com.expensetracker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal RFC 4180 style CSV reading and writing for single-line records.
 * Fields containing commas or quotes are wrapped in quotes, and embedded quotes are doubled.
 */
public final class CsvUtil {

    private static final String FORMULA_PREFIXES = "=+-@";

    private CsvUtil() {
    }

    public static String toLine(List<String> fields) {
        return fields.stream().map(CsvUtil::escape).collect(Collectors.joining(","));
    }

    static String escape(String field) {
        String value = field == null ? "" : field;
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    /**
     * Splits one CSV line into fields.
     *
     * @throws IllegalArgumentException if a quoted field is never closed
     */
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    boolean escapedQuote = i + 1 < line.length() && line.charAt(i + 1) == '"';
                    if (escapedQuote) {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("unterminated quoted field");
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * Prevents CSV/formula injection: spreadsheet programs execute cells that start with
     * =, +, - or @, so such values are prefixed with an apostrophe in exported files.
     */
    public static String neutralizeFormula(String value) {
        if (!value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }
}
