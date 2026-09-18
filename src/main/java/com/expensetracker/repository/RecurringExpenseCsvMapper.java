package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.Frequency;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.RecurringExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CSV layout: {@code id,description,amount,category,payment_method,frequency,start_date,occurrences,active}.
 */
public class RecurringExpenseCsvMapper implements CsvMapper<RecurringExpense> {

    private static final List<String> HEADER = List.of("id", "description", "amount", "category",
            "payment_method", "frequency", "start_date", "occurrences", "active");

    @Override
    public List<String> header() {
        return HEADER;
    }

    @Override
    public List<String> toRow(RecurringExpense r) {
        return List.of(String.valueOf(r.id()), r.description(), r.amount().toPlainString(),
                r.category().name(), r.paymentMethod().name(), r.frequency().name(),
                r.startDate().toString(), String.valueOf(r.occurrencesGenerated()),
                String.valueOf(r.active()));
    }

    @Override
    public RecurringExpense fromRow(List<String> row) {
        CsvMapper.requireColumns(row, HEADER.size());
        return new RecurringExpense(
                Integer.parseInt(row.get(0).strip()),
                row.get(1),
                new BigDecimal(row.get(2).strip()),
                Category.valueOf(row.get(3).strip()),
                PaymentMethod.valueOf(row.get(4).strip()),
                Frequency.valueOf(row.get(5).strip()),
                LocalDate.parse(row.get(6).strip()),
                Integer.parseInt(row.get(7).strip()),
                Boolean.parseBoolean(row.get(8).strip()));
    }
}
