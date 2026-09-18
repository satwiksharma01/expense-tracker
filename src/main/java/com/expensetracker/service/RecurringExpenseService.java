package com.expensetracker.service;

import com.expensetracker.exception.NotFoundException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Frequency;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.RecurringExpense;
import com.expensetracker.repository.Repository;
import com.expensetracker.util.AppLogger;
import com.expensetracker.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Recurring expense module: schedules such as rent or subscriptions that turn into real
 * expenses automatically when they fall due.
 */
public class RecurringExpenseService {

    public static final String GENERATED_SUFFIX = " (recurring)";

    /** Upper bound per schedule per run, so a daily schedule started years ago cannot flood the data file. */
    static final int MAX_OCCURRENCES_PER_RUN = 400;

    private static final Logger LOG = AppLogger.get(RecurringExpenseService.class);

    private final Repository<Integer, RecurringExpense> repository;
    private final ExpenseService expenseService;
    private final AtomicInteger lastId;

    public RecurringExpenseService(Repository<Integer, RecurringExpense> repository, ExpenseService expenseService) {
        this.repository = repository;
        this.expenseService = expenseService;
        this.lastId = new AtomicInteger(repository.findAll().stream().mapToInt(RecurringExpense::id).max().orElse(0));
    }

    public RecurringExpense create(String description, BigDecimal amount, Category category,
                                   PaymentMethod paymentMethod, Frequency frequency, LocalDate startDate) {
        String validDescription = Validator.requireValidDescription(description,
                Validator.MAX_DESCRIPTION_LENGTH - GENERATED_SUFFIX.length());
        BigDecimal validAmount = Validator.requireValidAmount(amount);
        Validator.requirePresent(category, "Category");
        Validator.requirePresent(paymentMethod, "Payment method");
        Validator.requirePresent(frequency, "Frequency");
        Validator.requirePresent(startDate, "Start date");
        if (startDate.isBefore(Validator.EARLIEST_DATE)) {
            throw new ValidationException("Start date must be on or after " + Validator.EARLIEST_DATE);
        }
        RecurringExpense created = repository.save(new RecurringExpense(lastId.incrementAndGet(), validDescription,
                validAmount, category, paymentMethod, frequency, startDate, 0, true));
        LOG.info(() -> "Created recurring expense #" + created.id() + " " + created.frequency());
        return created;
    }

    public List<RecurringExpense> list() {
        return repository.findAll().stream().sorted(Comparator.comparing(RecurringExpense::id)).toList();
    }

    public RecurringExpense setActive(int id, boolean active) {
        RecurringExpense existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No recurring expense with ID " + id));
        return repository.save(existing.withActive(active));
    }

    public void delete(int id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("No recurring expense with ID " + id);
        }
    }

    /**
     * Creates an expense for every occurrence that is due on or before today, including
     * occurrences missed while the application was not running. The occurrence counter is
     * saved after each generated expense, so calling this twice never creates duplicates.
     *
     * @return the expenses that were created
     */
    public List<Expense> processDue() {
        LocalDate today = expenseService.today();
        List<Expense> created = new ArrayList<>();
        for (RecurringExpense schedule : list()) {
            if (!schedule.active()) {
                continue;
            }
            RecurringExpense current = schedule;
            int generatedThisRun = 0;
            while (!current.nextDueDate().isAfter(today) && generatedThisRun < MAX_OCCURRENCES_PER_RUN) {
                created.add(expenseService.add(current.nextDueDate(), current.amount(), current.category(),
                        current.paymentMethod(), current.description() + GENERATED_SUFFIX));
                current = repository.save(current.withOccurrencesGenerated(current.occurrencesGenerated() + 1));
                generatedThisRun++;
            }
        }
        if (!created.isEmpty()) {
            LOG.info(() -> "Generated " + created.size() + " recurring expense(s)");
        }
        return created;
    }
}
