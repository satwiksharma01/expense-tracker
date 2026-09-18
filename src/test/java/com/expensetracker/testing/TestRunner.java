package com.expensetracker.testing;

import com.expensetracker.repository.CsvRepositoryTest;
import com.expensetracker.service.BudgetServiceTest;
import com.expensetracker.service.ExpenseServiceTest;
import com.expensetracker.service.ImportExportServiceTest;
import com.expensetracker.service.RecurringExpenseServiceTest;
import com.expensetracker.service.ReportServiceTest;
import com.expensetracker.util.CsvUtilTest;
import com.expensetracker.util.ValidatorTest;
import com.expensetracker.util.AppLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A small dependency-free test runner. It uses reflection to find every method annotated
 * with {@link Test} in the listed classes, runs each on a fresh instance and reports the
 * results. The exit code is 0 only if every test passed, so scripts and CI can rely on it.
 */
public final class TestRunner {

    private static final List<Class<?>> TEST_CLASSES = List.of(
            CsvUtilTest.class,
            ValidatorTest.class,
            CsvRepositoryTest.class,
            ExpenseServiceTest.class,
            BudgetServiceTest.class,
            ReportServiceTest.class,
            RecurringExpenseServiceTest.class,
            ImportExportServiceTest.class);

    private TestRunner() {
    }

    public static void main(String[] args) {
        AppLogger.disable();
        int passed = 0;
        List<String> failures = new ArrayList<>();
        long start = System.nanoTime();

        for (Class<?> testClass : TEST_CLASSES) {
            System.out.println(testClass.getSimpleName());
            Method[] methods = testClass.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (!method.isAnnotationPresent(Test.class)) {
                    continue;
                }
                String name = testClass.getSimpleName() + "." + method.getName();
                try {
                    method.invoke(testClass.getDeclaredConstructor().newInstance());
                    passed++;
                    System.out.println("  PASS  " + method.getName());
                } catch (InvocationTargetException e) {
                    failures.add(name + ": " + e.getCause());
                    System.out.println("  FAIL  " + method.getName() + " -> " + e.getCause());
                } catch (ReflectiveOperationException e) {
                    failures.add(name + ": could not run (" + e + ")");
                    System.out.println("  ERROR " + method.getName() + " -> " + e);
                }
            }
        }

        long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.println();
        System.out.printf("Tests run: %d, passed: %d, failed: %d (%d ms)%n",
                passed + failures.size(), passed, failures.size(), millis);
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            failures.forEach(f -> System.out.println("  - " + f));
            System.exit(1);
        }
    }
}
