package com.expensetracker;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Parsed command-line arguments.
 *
 * @param command         "menu" (interactive, default), "list" or "report"
 * @param commandArgument optional month (YYYY-MM) for the list and report commands
 */
public record AppOptions(Path dataDir, boolean demo, boolean help, boolean version,
                         String command, String commandArgument) {

    private static final Set<String> COMMANDS = Set.of("menu", "list", "report");

    public static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar expense-tracker.jar [options] [command [YYYY-MM]]",
            "",
            "Commands:",
            "  menu              Start the interactive menu (default)",
            "  list [YYYY-MM]    Print all expenses, or those of one month, and exit",
            "  report [YYYY-MM]  Print the monthly report (default: current month) and exit",
            "",
            "Options:",
            "  --data-dir <dir>  Folder for data files and logs (default: ./data)",
            "  --demo            Load sample data if no expenses exist yet",
            "  -h, --help        Show this help",
            "  -v, --version     Show the version",
            "");

    /**
     * @throws IllegalArgumentException for unknown options or commands
     */
    public static AppOptions parse(String[] args) {
        Path dataDir = Path.of("data");
        boolean demo = false;
        boolean help = false;
        boolean version = false;
        String command = null;
        String argument = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--data-dir" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--data-dir needs a folder name");
                    }
                    dataDir = Path.of(args[++i]);
                }
                case "--demo" -> demo = true;
                case "-h", "--help" -> help = true;
                case "-v", "--version" -> version = true;
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    if (command == null) {
                        command = arg.toLowerCase(Locale.ROOT);
                    } else if (argument == null) {
                        argument = arg;
                    } else {
                        throw new IllegalArgumentException("Unexpected argument: " + arg);
                    }
                }
            }
        }
        if (command == null) {
            command = "menu";
        }
        if (!COMMANDS.contains(command)) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
        return new AppOptions(dataDir, demo, help, version, command, argument);
    }
}
