package com.expensetracker.exception;

/**
 * Thrown when user-supplied data breaks a business rule (negative amount, future date, ...).
 */
public class ValidationException extends ExpenseTrackerException {

    public ValidationException(String message) {
        super(message);
    }
}
