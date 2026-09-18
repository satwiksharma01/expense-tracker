package com.expensetracker.service;

import com.expensetracker.exception.NotFoundException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.repository.Repository;
import com.expensetracker.util.AppLogger;
import com.expensetracker.util.Validator;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Expense management module: create, read, update, delete and search expenses.
 * All input is validated here before it reaches the repository.
 */
public class ExpenseService {

    private static final Logger LOG = AppLogger.get(ExpenseService.class);

    private final Repository<Integer, Expense> repository;
    private final Clock clock;
    private final AtomicInteger lastId;

    /**
     * @param clock source of "today"; injected so tests can run against a fixed date
     */
    public ExpenseService(Repository<Integer, Expense> repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        int maxId = repository.findAll().stream().mapToInt(Expense::id).max().orElse(0);
        this.lastId = new AtomicInteger(maxId);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public Expense add(LocalDate date, BigDecimal amount, Category category,
                       PaymentMethod paymentMethod, String description) {
        Expense draft = validated(0, date, amount, category, paymentMethod, description);
        Expense saved = repository.save(new Expense(lastId.incrementAndGet(), draft.date(), draft.amount(),
                draft.category(), draft.paymentMethod(), draft.description()));
        LOG.info(() -> "Added expense #" + saved.id() + " " + saved.amount() + " " + saved.category());
        return saved;
    }

    public Expense update(int id, LocalDate date, BigDecimal amount, Category category,
                          PaymentMethod paymentMethod, String description) {
        getById(id);
        Expense updated = repository.save(validated(id, date, amount, category, paymentMethod, description));
        LOG.info(() -> "Updated expense #" + id);
        return updated;
    }

    public void delete(int id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("No expense with ID " + id);
        }
        LOG.info(() -> "Deleted expense #" + id);
    }

    public Expense getById(int id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("No expense with ID " + id));
    }

    public List<Expense> search(ExpenseFilter filter, SortOrder order) {
        return repository.findAll().stream()
                .filter(filter)
                .sorted(order.comparator())
                .toList();
    }

    public int count() {
        return repository.count();
    }

    private Expense validated(int id, LocalDate date, BigDecimal amount, Category category,
                              PaymentMethod paymentMethod, String description) {
        return new Expense(id,
                Validator.requireValidExpenseDate(date, today()),
                Validator.requireValidAmount(amount),
                Validator.requirePresent(category, "Category"),
                Validator.requirePresent(paymentMethod, "Payment method"),
                Validator.requireValidDescription(description));
    }
}
