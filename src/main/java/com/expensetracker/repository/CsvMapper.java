package com.expensetracker.repository;

import java.util.List;

/**
 * Converts an entity to and from one CSV row (strategy used by {@link CsvRepository}).
 */
public interface CsvMapper<T> {

    List<String> header();

    List<String> toRow(T entity);

    /**
     * @throws RuntimeException if the row is malformed; the repository skips such rows
     */
    T fromRow(List<String> row);

    /** Shared guard used by implementations before reading columns. */
    static void requireColumns(List<String> row, int expected) {
        if (row.size() != expected) {
            throw new IllegalArgumentException("expected " + expected + " columns but found " + row.size());
        }
    }
}
