package com.expensetracker.exception;

/**
 * Thrown when an entity with the requested id does not exist.
 */
public class NotFoundException extends ExpenseTrackerException {

    public NotFoundException(String message) {
        super(message);
    }
}
