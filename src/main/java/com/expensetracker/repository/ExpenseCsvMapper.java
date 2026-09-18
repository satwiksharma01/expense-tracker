package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CSV layout: {@code id,date,amount,category,payment_method,description}.
 */
public class ExpenseCsvMapper implements CsvMapper<Expense> {

    private static final List<String> HEADER =
            List.of("id", "date", "amount", "category", "payment_method", "description");

    @Override
    public List<String> header() {
        return HEADER;
    }

    @Override
    public List<String> toRow(Expense e) {
        return List.of(String.valueOf(e.id()), e.date().toString(), e.amount().toPlainString(),
                e.category().name(), e.paymentMethod().name(), e.description());
    }

    @Override
    public Expense fromRow(List<String> row) {
        CsvMapper.requireColumns(row, HEADER.size());
        return new Expense(
                Integer.parseInt(row.get(0).strip()),
                LocalDate.parse(row.get(1).strip()),
                new BigDecimal(row.get(2).strip()),
                Category.valueOf(row.get(3).strip()),
                PaymentMethod.valueOf(row.get(4).strip()),
                row.get(5));
    }
}
