package com.expensetracker.ui;

/**
 * Signals that standard input was closed (Ctrl+D / Ctrl+Z, or the end of a piped script),
 * so the menu loop can shut down cleanly instead of spinning on empty reads.
 */
public class EndOfInputException extends RuntimeException {

    public EndOfInputException() {
        super("input closed");
    }
}
