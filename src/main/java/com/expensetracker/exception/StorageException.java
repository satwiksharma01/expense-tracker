package com.expensetracker.exception;

/**
 * Thrown when data cannot be read from or written to disk.
 */
public class StorageException extends ExpenseTrackerException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
