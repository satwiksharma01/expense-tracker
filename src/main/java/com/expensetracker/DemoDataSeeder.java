package com.expensetracker;

import com.expensetracker.model.Category;
import com.expensetracker.model.Frequency;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.RecurringExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Fills an empty data folder with realistic sample data (about three months of a student's
 * spending, a few budgets and recurring payments) so every feature can be tried immediately.
 * A fixed random seed keeps the generated data the same on every run.
 */
public final class DemoDataSeeder {

    private record Template(String description, Category category, PaymentMethod method, int min, int max) {
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("Canteen lunch", Category.FOOD, PaymentMethod.UPI, 60, 180),
            new Template("Groceries", Category.FOOD, PaymentMethod.UPI, 300, 1100),
            new Template("Dinner with friends", Category.FOOD, PaymentMethod.CREDIT_CARD, 400, 1400),
            new Template("Coffee and snacks", Category.FOOD, PaymentMethod.UPI, 40, 160),
            new Template("Auto rickshaw", Category.TRANSPORT, PaymentMethod.CASH, 30, 150),
            new Template("Metro card recharge", Category.TRANSPORT, PaymentMethod.UPI, 200, 500),
            new Template("Cab to railway station", Category.TRANSPORT, PaymentMethod.UPI, 150, 450),
            new Template("Movie tickets", Category.ENTERTAINMENT, PaymentMethod.DEBIT_CARD, 200, 600),
            new Template("Stationery", Category.EDUCATION, PaymentMethod.CASH, 50, 300),
            new Template("Reference book", Category.EDUCATION, PaymentMethod.CREDIT_CARD, 300, 900),
            new Template("Pharmacy", Category.HEALTH, PaymentMethod.UPI, 80, 600),
            new Template("Clothes", Category.SHOPPING, PaymentMethod.CREDIT_CARD, 400, 1500));

    private DemoDataSeeder() {
    }

    /** @return the number of one-off expenses created (recurring payments are generated separately) */
    public static int seed(ExpenseService expenses, BudgetService budgets, RecurringExpenseService recurring) {
        LocalDate today = expenses.today();
        LocalDate start = today.minusMonths(2).withDayOfMonth(1);
        Random random = new Random(2026L);
        int created = 0;
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            int expensesToday = random.nextInt(3);
            for (int i = 0; i < expensesToday; i++) {
                Template t = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
                int amount = t.min() + random.nextInt(t.max() - t.min() + 1);
                expenses.add(day, BigDecimal.valueOf(amount), t.category(), t.method(), t.description());
                created++;
            }
        }

        budgets.setBudget(null, new BigDecimal("25000"));
        budgets.setBudget(Category.FOOD, new BigDecimal("6000"));
        budgets.setBudget(Category.TRANSPORT, new BigDecimal("2000"));
        budgets.setBudget(Category.ENTERTAINMENT, new BigDecimal("1200"));
        budgets.setBudget(Category.SHOPPING, new BigDecimal("2500"));

        recurring.create("Hostel rent", new BigDecimal("8000"), Category.RENT,
                PaymentMethod.NET_BANKING, Frequency.MONTHLY, start);
        recurring.create("Music streaming plan", new BigDecimal("119"), Category.ENTERTAINMENT,
                PaymentMethod.UPI, Frequency.MONTHLY, start.plusDays(4));
        recurring.create("Mobile recharge", new BigDecimal("299"), Category.UTILITIES,
                PaymentMethod.UPI, Frequency.MONTHLY, start.plusDays(9));
        return created;
    }
}
