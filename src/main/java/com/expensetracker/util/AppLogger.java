package com.expensetracker.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configures java.util.logging for the application. Log records go to a rotating file
 * (data/logs/app0.log) so they never clutter the interactive console.
 */
public final class AppLogger {

    /** Held in a static field: LogManager only keeps weak references to loggers. */
    private static final Logger ROOT = Logger.getLogger("com.expensetracker");

    private static final int MAX_BYTES_PER_FILE = 1_000_000;
    private static final int FILE_COUNT = 3;

    private AppLogger() {
    }

    public static Logger get(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    public static void configure(Path logDirectory) {
        ROOT.setUseParentHandlers(false);
        ROOT.setLevel(Level.INFO);
        try {
            Files.createDirectories(logDirectory);
            String pattern = logDirectory.resolve("app%g.log").toAbsolutePath().toString();
            FileHandler handler = new FileHandler(pattern, MAX_BYTES_PER_FILE, FILE_COUNT, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new LineFormatter());
            ROOT.addHandler(handler);
        } catch (IOException e) {
            ROOT.setUseParentHandlers(true);
            ROOT.setLevel(Level.WARNING);
            System.err.println("Warning: file logging disabled (" + e.getMessage() + ")");
        }
    }

    /** Turns logging off completely; used by the test runner. */
    public static void disable() {
        ROOT.setLevel(Level.OFF);
    }

    private static final class LineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String line = String.format("%1$tF %1$tT %2$-7s %3$s - %4$s%n",
                    record.getMillis(), record.getLevel().getName(),
                    record.getLoggerName().replace("com.expensetracker.", ""),
                    formatMessage(record));
            if (record.getThrown() != null) {
                line += "        caused by: " + record.getThrown() + System.lineSeparator();
            }
            return line;
        }
    }
}
