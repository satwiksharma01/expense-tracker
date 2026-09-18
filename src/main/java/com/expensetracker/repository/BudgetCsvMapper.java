package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;

import java.math.BigDecimal;
import java.util.List;

/**
 * CSV layout: {@code scope,monthly_limit} where scope is a category name or OVERALL.
 */
public class BudgetCsvMapper implements CsvMapper<Budget> {

    private static final List<String> HEADER = List.of("scope", "monthly_limit");

    @Override
    public List<String> header() {
        return HEADER;
    }

    @Override
    public List<String> toRow(Budget budget) {
        return List.of(budget.key(), budget.monthlyLimit().toPlainString());
    }

    @Override
    public Budget fromRow(List<String> row) {
        CsvMapper.requireColumns(row, HEADER.size());
        String scope = row.get(0).strip();
        BigDecimal limit = new BigDecimal(row.get(1).strip());
        return Budget.OVERALL_KEY.equals(scope)
                ? Budget.overall(limit)
                : Budget.forCategory(Category.valueOf(scope), limit);
    }
}
