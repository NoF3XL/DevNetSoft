import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {
    // ANSI escape codes for colors (optional)
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_UNDERLINE = "\u001B[4m";

    private static final boolean ANSI_SUPPORTED = detectAnsiSupport();

    private static boolean detectAnsiSupport() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        try {
            return System.console() != null && !System.getProperty("os.name").toLowerCase().contains("win");
        } catch (SecurityException e) {
            return false;
        }
    }

    private static String colorize(String text, String ansiCode) {
        return ANSI_SUPPORTED ? ansiCode + text + ANSI_RESET : text;
    }

    // Formatting helpers
    public static String formatSuccess(String text) {
        return colorize(text, ANSI_GREEN + ANSI_BOLD);
    }

    public static String formatError(String text) {
        return colorize(text, ANSI_RED + ANSI_BOLD);
    }

    public static String formatPrompt(String text) {
        return colorize(text, ANSI_CYAN);
    }

    public static String formatHeader(String text) {
        return colorize(text, ANSI_BLUE + ANSI_BOLD + ANSI_UNDERLINE);
    }

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(formatPrompt(message));
            String input = scanner.nextLine().trim();
            if (!required || !input.isEmpty()) {
                return input;
            }
            System.out.println(formatError("Значение не может быть пустым. Пожалуйста, повторите ввод."));
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(formatPrompt(message));
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println(formatError("Число должно быть в диапазоне от " + min + " до " + max + "."));
            } catch (NumberFormatException e) {
                System.out.println(formatError("Некорректный ввод. Пожалуйста, введите целое число."));
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(formatPrompt(message + " (да/нет): "));
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("да") || input.equals("д") || input.equals("yes") || input.equals("y")) {
                return true;
            }
            if (input.equals("нет") || input.equals("н") || input.equals("no") || input.equals("n")) {
                return false;
            }
            System.out.println(formatError("Пожалуйста, введите 'да' или 'нет'."));
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Список опций пуст.");
        }
        System.out.println(formatHeader("Доступные варианты:"));
        for (int i = 0; i < options.size(); i++) {
            System.out.println(formatPrompt("  " + (i + 1) + ". " + options.get(i).toString()));
        }
        int choice = promptInt(scanner, message, 1, options.size());
        return options.get(choice - 1);
    }
}
