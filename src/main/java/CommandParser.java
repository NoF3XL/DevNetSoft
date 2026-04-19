import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands;
    private final Map<String, String> commandDescriptions;

    public CommandParser() {
        this.commands = new HashMap<>();
        this.commandDescriptions = new HashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command command = commands.get(commandName);
        if (command != null) {
            command.execute(scanner, system);
        } else {
            System.out.println("Ошибка: команда '" + commandName + "' не найдена.");
            System.out.println("Введите 'help' для просмотра доступных команд.");
        }
    }

    public void printHelp() {
        System.out.println("Доступные команды:");
        for (Map.Entry<String, String> entry : commandDescriptions.entrySet()) {
            System.out.println("  " + entry.getKey() + " — " + entry.getValue());
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        String[] parts = input.trim().split("\\s+");
        String commandName = parts[0];
        
        executeCommand(commandName, scanner, system);
    }
}
