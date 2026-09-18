package com.expensetracker.model;

/**
 * Spending categories an expense can belong to.
 */
public enum Category implements Labeled {
    FOOD("Food & Dining"),
    TRANSPORT("Transport"),
    RENT("Rent & Housing"),
    UTILITIES("Utilities & Bills"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    EDUCATION("Education"),
    TRAVEL("Travel"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
