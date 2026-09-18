package com.expensetracker.model;

/**
 * How an expense was paid for.
 */
public enum PaymentMethod implements Labeled {
    CASH("Cash"),
    UPI("UPI"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    NET_BANKING("Net Banking"),
    WALLET("Wallet");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
