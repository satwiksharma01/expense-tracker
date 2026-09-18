package com.expensetracker.exception;

/**
 * Base type for all application errors. Unchecked so that the service API stays clean;
 * the console layer catches it in one place and shows the message to the user.
 */
public class ExpenseTrackerException extends RuntimeException {

    public ExpenseTrackerException(String message) {
        super(message);
    }

    public ExpenseTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}
