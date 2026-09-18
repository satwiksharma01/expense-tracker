package com.expensetracker.service;

import com.expensetracker.exception.NotFoundException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.Repository;
import com.expensetracker.util.AppLogger;
import com.expensetracker.util.Money;
import com.expensetracker.util.Validator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Budget module: monthly limits per category or overall, and alerts when they are approached.
 */
public class BudgetService {

    private static final Logger LOG = AppLogger.get(BudgetService.class);

    /** Overall budget first, then categories in their declared order. */
    private static final Comparator<Budget> DISPLAY_ORDER = Comparator.comparing(
            (Budget b) -> b.isOverall() ? -1 : b.category().ordinal());

    private final Repository<String, Budget> repository;
    private final ExpenseService expenseService;

    public BudgetService(Repository<String, Budget> repository, ExpenseService expenseService) {
        this.repository = repository;
        this.expenseService = expenseService;
    }

    /**
     * Creates or replaces a budget.
     *
     * @param category the category to limit, or {@code null} for the overall budget
     */
    public Budget setBudget(Category category, BigDecimal monthlyLimit) {
        BigDecimal limit = Validator.requireValidLimit(monthlyLimit);
        Budget budget = category == null ? Budget.overall(limit) : Budget.forCategory(category, limit);
        repository.save(budget);
        LOG.info(() -> "Budget set: " + budget.key() + " = " + limit);
        return budget;
    }

    public void removeBudget(Category category) {
        String key = Budget.keyFor(category);
        if (!repository.deleteById(key)) {
            throw new NotFoundException("No budget set for " + (category == null ? "overall spending" : category.label()));
        }
        LOG.info(() -> "Budget removed: " + key);
    }

    public List<Budget> list() {
        return repository.findAll().stream().sorted(DISPLAY_ORDER).toList();
    }

    public List<BudgetStatus> statusFor(YearMonth month) {
        List<Expense> expenses = expenseService.search(ExpenseFilter.forMonth(month), SortOrder.DATE_ASC);
        Map<Category, BigDecimal> spentByCategory = expenses.stream().collect(Collectors.groupingBy(
                Expense::category,
                () -> new EnumMap<>(Category.class),
                Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)));
        BigDecimal total = Money.sum(spentByCategory.values());
        return list().stream()
                .map(b -> new BudgetStatus(b, b.isOverall() ? total
                        : spentByCategory.getOrDefault(b.category(), BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Budget warnings relevant to a newly recorded expense: its category budget and the
     * overall budget, but only when they are at or above the warning threshold.
     */
    public List<String> alertsFor(Expense expense) {
        return statusFor(expense.month()).stream()
                .filter(s -> s.budget().isOverall() || s.budget().category() == expense.category())
                .filter(s -> s.level() != BudgetStatus.Level.OK)
                .map(BudgetStatus::message)
                .toList();
    }
}
