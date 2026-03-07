import java.util.Scanner;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CommandRegistry {
    
    public static void registerAllCommands(CommandParser parser) {
        // Команды управления пользователями
        parser.registerCommand("user-list", "вывести список всех пользователей", CommandRegistry::userList);
        parser.registerCommand("user-create", "создать нового пользователя", CommandRegistry::userCreate);
        parser.registerCommand("user-view", "просмотр информации о пользователе", CommandRegistry::userView);
        parser.registerCommand("user-update", "обновить данные пользователя", CommandRegistry::userUpdate);
        parser.registerCommand("user-delete", "удалить пользователя", CommandRegistry::userDelete);
        parser.registerCommand("user-search", "поиск пользователей по фильтрам", CommandRegistry::userSearch);
        
        // Команды управления ролями
        parser.registerCommand("role-list", "вывести список всех ролей", CommandRegistry::roleList);
        parser.registerCommand("role-create", "создать новую роль", CommandRegistry::roleCreate);
        parser.registerCommand("role-view", "просмотр роли", CommandRegistry::roleView);
        parser.registerCommand("role-update", "обновить роль (название/описание)", CommandRegistry::roleUpdate);
        parser.registerCommand("role-delete", "удалить роль", CommandRegistry::roleDelete);
        parser.registerCommand("role-add-permission", "добавить право к роли", CommandRegistry::roleAddPermission);
        parser.registerCommand("role-remove-permission", "удалить право из роли", CommandRegistry::roleRemovePermission);
        parser.registerCommand("role-search", "поиск ролей", CommandRegistry::roleSearch);
        
        // Команды управления назначениями
        parser.registerCommand("assign-role", "назначить роль пользователю", CommandRegistry::assignRole);
        parser.registerCommand("revoke-role", "отозвать роль у пользователя", CommandRegistry::revokeRole);
        parser.registerCommand("assignment-list", "список всех назначений", CommandRegistry::assignmentList);
        parser.registerCommand("assignment-list-user", "назначения конкретного пользователя", CommandRegistry::assignmentListUser);
        parser.registerCommand("assignment-list-role", "список пользователей с конкретной ролью", CommandRegistry::assignmentListRole);
        parser.registerCommand("assignment-active", "только активные назначения", CommandRegistry::assignmentActive);
        parser.registerCommand("assignment-expired", "истёкшие временные назначения", CommandRegistry::assignmentExpired);
        parser.registerCommand("assignment-extend", "продлить временное назначение", CommandRegistry::assignmentExtend);
        parser.registerCommand("assignment-search", "поиск назначений по фильтрам", CommandRegistry::assignmentSearch);
        
        // Команды просмотра прав
        parser.registerCommand("permissions-user", "все права конкретного пользователя", CommandRegistry::permissionsUser);
        parser.registerCommand("permissions-check", "проверить, есть ли у пользователя конкретное право", CommandRegistry::permissionsCheck);
        
        // Служебные команды
        parser.registerCommand("help", "справка по командам", CommandRegistry::help);
        parser.registerCommand("stats", "статистика системы", CommandRegistry::stats);
        parser.registerCommand("clear", "очистить экран", CommandRegistry::clear);
        parser.registerCommand("exit", "выход из программы", CommandRegistry::exit);
    }
    
    // Вспомогательные методы
    private static void printUserTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("Пользователи не найдены.");
            return;
        }
        for (User user : users) {
            System.out.printf("%-18s %-20s %-26s %n",
                    user.username(),
                    user.fullName(),
                    user.email());
        }
        System.out.println("Всего: " + users.size());
    }
    
    private static void userList(Scanner scanner, RBACSystem system) {
        UserManager userManager = system.getUserManager();
        List<User> users = userManager.findAll();
        printUserTable(users);
    }
    
    private static void userCreate(Scanner scanner, RBACSystem system) {
        System.out.println("Создание нового пользователя");
        System.out.print("Введите username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Введите полное имя: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Введите email: ");
        String email = scanner.nextLine().trim();
        
        try {
            User user = User.create(username, fullName, email);
            system.getUserManager().add(user);
            System.out.println("Пользователь '" + username + "' успешно создан.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка создания пользователя: " + e.getMessage());
        }
    }
    
    private static void userView(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя: ");
        String username = scanner.nextLine().trim();
        User user = system.getUserManager().findByUsername(username).get();
        System.out.println("Username: " + user.username());
        System.out.println("Полное имя: " + user.fullName());
        System.out.println("Email: " + user.email());
    }
    
    private static void userUpdate(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя для обновления: ");
        String username = scanner.nextLine().trim();
        if (!system.getUserManager().exists(username)) {
            System.out.println("Пользователь '" + username + "' не найден.");
            return;
        }
        System.out.print("Введите новое полное имя: ");
        String newFullName = scanner.nextLine().trim();
        System.out.print("Введите новый email: ");
        String newEmail = scanner.nextLine().trim();
        
        User current = system.getUserManager().findByUsername(username).get();
        if (newFullName.isEmpty()) {
            newFullName = current.fullName();
        }
        if (newEmail.isEmpty()) {
            newEmail = current.email();
        }
        
        try {
            system.getUserManager().update(username, newFullName, newEmail);
            System.out.println("Данные пользователя '" + username + "' успешно обновлены.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка обновления: " + e.getMessage());
        }
    }
    
    private static void userDelete(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя для удаления: ");
        String username = scanner.nextLine().trim();
        if (!system.getUserManager().exists(username)) {
            System.out.println("Пользователь '" + username + "' не найден.");
            return;
        }
        boolean removed = system.getUserManager().removeByUsername(username);
        if (removed) {
            System.out.println("Пользователь '" + username + "' успешно удален.");
        } else {
            System.out.println("Не удалось удалить пользователя.");
        }
    }
    
    private static void userSearch(Scanner scanner, RBACSystem system) {
        System.out.println("Выберите тип фильтра:");
        System.out.println("1. По username (содержит)");
        System.out.println("2. По email (содержит)");
        System.out.println("3. По домену email");
        System.out.println("4. По полному имени (содержит)");
        System.out.print("Выберите (1-4): ");
        String choice = scanner.nextLine().trim();
        UserFilter filter = null;
        switch (choice) {
            case "1":
                System.out.print("Введите подстроку для поиска в username: ");
                String usernameSub = scanner.nextLine().trim();
                filter = UserFilters.byUsernameContains(usernameSub);
                break;
            case "2":
                System.out.print("Введите подстроку для поиска в email: ");
                String emailSub = scanner.nextLine().trim();
                filter = user -> user.email().toLowerCase().contains(emailSub.toLowerCase());
                break;
            case "3":
                System.out.print("Введите домен (например 'example.com'): ");
                String domain = scanner.nextLine().trim();
                filter = UserFilters.byEmailDomain(domain);
                break;
            case "4":
                System.out.print("Введите подстроку для поиска в полном имени: ");
                String fullNameSub = scanner.nextLine().trim();
                filter = UserFilters.byFullNameContains(fullNameSub);
                break;
            default:
                System.out.println("Неверный выбор. Поиск отменен.");
                return;
        }
        List<User> results = system.getUserManager().findByFilter(filter);
        printUserTable(results);
    }
    
    private static void printRoleTable(List<Role> roles) {
        if (roles.isEmpty()) {
            System.out.println("Роли не найдены.");
            return;
        }
        for (Role role : roles) {
            System.out.printf("%-18s %-20s %-18d %n",
                    role.getName(),
                    role.getDescription(),
                    role.getPermissionCount());
        }
        System.out.println("Всего: " + roles.size());
    }
    
    private static void roleList(Scanner scanner, RBACSystem system) {
        List<Role> roles = system.getRoleManager().findAll();
        printRoleTable(roles);
    }
    
    private static void roleCreate(Scanner scanner, RBACSystem system) {
        System.out.println("Создание новой роли");
        System.out.print("Введите название роли: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите описание роли: ");
        String description = scanner.nextLine().trim();
        
        try {
            Role role = new Role(name, description);
            system.getRoleManager().add(role);
            System.out.println("Роль '" + name + "' успешно создана.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка создания роли: " + e.getMessage());
        }
    }
    
    private static void roleView(Scanner scanner, RBACSystem system) {
        System.out.print("Введите название роли: ");
        String name = scanner.nextLine().trim();
        Role role = system.getRoleManager().findByName(name).get();
        System.out.println(role.format());
    }
    
    private static void roleUpdate(Scanner scanner, RBACSystem system) {
        System.out.print("Введите название роли для обновления: ");
        String name = scanner.nextLine().trim();
        Role role = system.getRoleManager().findByName(name).get();
        System.out.print("Введите новое название: ");
        String newName = scanner.nextLine().trim();
        System.out.print("Введите новое описание: ");
        String newDescription = scanner.nextLine().trim();
        
        if (!newDescription.isEmpty()) {
            role.setDescription(newDescription);
        }
        System.out.println("Роль обновлена.");
    }
    
    private static void roleDelete(Scanner scanner, RBACSystem system) {
        System.out.print("Введите название роли для удаления: ");
        String name = scanner.nextLine().trim();
        Role role = system.getRoleManager().findByName(name).get();
        List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
        if (!assignments.isEmpty()) {
            System.out.println("Предупреждение: роль назначена " + assignments.size() + " пользователям");
            System.out.println("Пользователи с этой ролью:");
            for (RoleAssignment assignment : assignments) {
                System.out.println(" - " + assignment.user().username());
            }
        }
        try {
            boolean removed = system.getRoleManager().remove(role);
            if (removed) {
                System.out.println("Роль '" + name + "' успешно удалена.");
            } else {
                System.out.println("Не удалось удалить роль.");
            }
        } catch (IllegalStateException e) {
            System.out.println("Ошибка удаления: " + e.getMessage());
        }
    }
    
    private static void roleAddPermission(Scanner scanner, RBACSystem system) {
        System.out.print("Введите название роли: ");
        String roleName = scanner.nextLine().trim();
        if (!system.getRoleManager().exists(roleName)) {
            System.out.println("Роль '" + roleName + "' не найдена.");
            return;
        }
        System.out.print("Введите название права: ");
        String permName = scanner.nextLine().trim();
        System.out.print("Введите ресурс: ");
        String resource = scanner.nextLine().trim();
        System.out.print("Введите описание права: ");
        String description = scanner.nextLine().trim();
        
        Permission permission = new Permission(permName, resource, description);
        try {
            system.getRoleManager().addPermissionToRole(roleName, permission);
            System.out.println("Право добавлено к роли '" + roleName + "'");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка добавления права: " + e.getMessage());
        }
    }
    
    private static void roleRemovePermission(Scanner scanner, RBACSystem system) {
        System.out.print("Введите название роли: ");
        String roleName = scanner.nextLine().trim();
        Role role = system.getRoleManager().findByName(roleName).get();
        Set<Permission> permissions = role.getPermissions();
        if (permissions.isEmpty()) {
            System.out.println("У роли нет прав.");
            return;
        }
        System.out.println("Список прав роли:");
        int index = 1;
        List<Permission> permList = new ArrayList<>(permissions);
        for (Permission p : permList) {
            System.out.printf("%d. %s (%s) - %s%n", index++, p.name(), p.resource(), p.description());
        }
        System.out.print("Введите номер права для удаления: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice < 1 || choice > permList.size()) {
                System.out.println("Неверный номер.");
                return;
            }
            Permission toRemove = permList.get(choice - 1);
            system.getRoleManager().removePermissionFromRole(roleName, toRemove);
            System.out.println("Право удалено.");
        } catch (NumberFormatException e) {
            System.out.println("Некорректный ввод.");
        }
    }
    
    private static void roleSearch(Scanner scanner, RBACSystem system) {
        System.out.println("Выберите тип фильтра:");
        System.out.println("1. По названию (содержит)");
        System.out.println("2. По наличию конкретного права");
        System.out.println("3. По минимальному количеству прав");
        System.out.print("Выберите (1-3): ");
        String choice = scanner.nextLine().trim();
        RoleFilter filter = null;
        switch (choice) {
            case "1":
                System.out.print("Введите подстроку для поиска в названии: ");
                String substring = scanner.nextLine().trim();
                filter = RoleFilters.byNameContains(substring);
                break;
            case "2":
                System.out.print("Введите название права: ");
                String permName = scanner.nextLine().trim();
                System.out.print("Введите ресурс: ");
                String resource = scanner.nextLine().trim();
                filter = RoleFilters.hasPermission(permName, resource);
                break;
            case "3":
                System.out.print("Введите минимальное количество прав: ");
                try {
                    int n = Integer.parseInt(scanner.nextLine().trim());
                    filter = RoleFilters.hasAtLeastNPermissions(n);
                } catch (NumberFormatException e) {
                    System.out.println("Некорректное число.");
                    return;
                }
                break;
            default:
                System.out.println("Неверный выбор. Поиск отменен.");
                return;
        }
        List<Role> results = system.getRoleManager().findByFilter(filter);
        printRoleTable(results);
    }
    
    private static void printAssignmentTable(List<RoleAssignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("Назначения не найдены.");
            return;
        }
        for (RoleAssignment assignment : assignments) {
            System.out.printf("%-18s %-18s %-18s %-18s %-18s %n",
                    assignment.user().username(),
                    assignment.role().getName(),
                    assignment.assignmentType(),
                    assignment.isActive() ? "Активно" : "Неактивно",
                    assignment.metadata().assignedAt());
        }
        System.out.println("Всего: " + assignments.size());
    }
    
    private static void assignRole(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя: ");
        String username = scanner.nextLine().trim();
        User user = system.getUserManager().findByUsername(username).get();
        List<Role> roles = system.getRoleManager().findAll();
        if (roles.isEmpty()) {
            System.out.println("Нет доступных ролей.");
            return;
        }
        System.out.println("Доступные роли:");
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            System.out.printf("%d. %s (%s)%n", i + 1, role.getName(), role.getDescription());
        }
        System.out.print("Выберите номер роли: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice < 1 || choice > roles.size()) {
                System.out.println("Неверный номер.");
                return;
            }
            Role role = roles.get(choice - 1);
            
            System.out.print("Тип назначения (постоянное/временное) [п/в]: ");
            String typeInput = scanner.nextLine().trim();
            boolean isTemporary = typeInput.equalsIgnoreCase("в") || typeInput.equalsIgnoreCase("временное");
            
            String expiresAt = null;
            if (isTemporary) {
                System.out.print("Введите дату истечения (формат: ГГГГ-ММ-ДД ЧЧ:ММ): ");
                expiresAt = scanner.nextLine().trim();
            }
            
            System.out.print("Причина назначения: ");
            String reason = scanner.nextLine().trim();
            if (reason.isEmpty()) {
                reason = "Не указана";
            }
            
            AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);
            RoleAssignment assignment;
            if (isTemporary) {
                assignment = new TemporaryAssignment(user, role, metadata, expiresAt, false);
            } else {
                assignment = new PermanentAssignment(user, role, metadata);
            }
            
            system.getAssignmentManager().add(assignment);
            System.out.println("Роль '" + role.getName() + "' успешно назначена пользователю '" + username + "'");
        } catch (NumberFormatException e) {
            System.out.println("Некорректный ввод.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Ошибка назначения: " + e.getMessage());
        }
    }
    
    private static void revokeRole(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя: ");
        String username = scanner.nextLine().trim();
        User user = system.getUserManager().findByUsername(username).get();
        
        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
        List<RoleAssignment> activeAssignments = assignments.stream()
                .filter(RoleAssignment::isActive)
                .collect(java.util.stream.Collectors.toList());
        if (activeAssignments.isEmpty()) {
            System.out.println("У пользователя нет активных назначений.");
            return;
        }
        System.out.println("Активные назначения пользователя:");
        for (int i = 0; i < activeAssignments.size(); i++) {
            RoleAssignment a = activeAssignments.get(i);
            System.out.printf("%d. Роль: %s, Тип: %s, Назначено: %s%n",
                    i + 1, a.role().getName(), a.assignmentType(), a.metadata().assignedAt());
        }
        System.out.print("Выберите номер назначения для отзыва: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice < 1 || choice > activeAssignments.size()) {
                System.out.println("Неверный номер.");
                return;
            }
            RoleAssignment assignment = activeAssignments.get(choice - 1);
            system.getAssignmentManager().revokeAssignment(assignment.assignmentId());
            System.out.println("Назначение отозвано.");
        } catch (NumberFormatException e) {
            System.out.println("Некорректный ввод.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка отзыва: " + e.getMessage());
        }
    }
    
    private static void assignmentList(Scanner scanner, RBACSystem system) {
        List<RoleAssignment> assignments = system.getAssignmentManager().findAll();
        printAssignmentTable(assignments);
    }
    
    private static void assignmentListUser(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя: ");
        String username = scanner.nextLine().trim();
        User user = system.getUserManager().findByUsername(username).get();
        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
        printAssignmentTable(assignments);
    }
    
    private static void assignmentListRole(Scanner scanner, RBACSystem system) {
        System.out.print("Введите название роли: ");
        String roleName = scanner.nextLine().trim();
        Role role = system.getRoleManager().findByName(roleName).get();
        List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
        printAssignmentTable(assignments);
    }
    
    private static void assignmentActive(Scanner scanner, RBACSystem system) {
        List<RoleAssignment> assignments = system.getAssignmentManager().getActiveAssignments();
        printAssignmentTable(assignments);
    }
    
    private static void assignmentExpired(Scanner scanner, RBACSystem system) {
        List<RoleAssignment> assignments = system.getAssignmentManager().getExpiredAssignments();
        printAssignmentTable(assignments);
    }
    
    private static void assignmentExtend(Scanner scanner, RBACSystem system) {
        System.out.print("Введите assignment ID или username + role (через пробел): ");
        String input = scanner.nextLine().trim();
        String assignmentId = null;
        if (input.contains(" ")) {
            String[] parts = input.split("\\s+");
            if (parts.length >= 2) {
                String username = parts[0];
                String roleName = parts[1];
                User user = system.getUserManager().findByUsername(username).get();
                Role role = system.getRoleManager().findByName(roleName).get();
                List<RoleAssignment> assignments = system.getAssignmentManager()
                        .findAssignmentsByUserAndRole(user, role);
                if (assignments.isEmpty()) {
                    System.out.println("Назначение не найдено.");
                    return;
                }
                assignmentId = assignments.get(0).assignmentId();
            }
        } else {
            assignmentId = input;
        }
        if (assignmentId == null) {
            System.out.println("Не удалось определить назначение.");
            return;
        }
        System.out.print("Введите новую дату истечения (формат: ГГГГ-ММ-ДД ЧЧ:ММ): ");
        String newDate = scanner.nextLine().trim();
        try {
            system.getAssignmentManager().extendTemporaryAssignment(assignmentId, newDate);
            System.out.println("Назначение продлено.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка продления: " + e.getMessage());
        }
    }
    
    private static void assignmentSearch(Scanner scanner, RBACSystem system) {
        System.out.println("Выберите тип фильтра:");
        System.out.println("1. По пользователю");
        System.out.println("2. По роли");
        System.out.println("3. По типу (постоянное/временное)");
        System.out.println("4. По статусу (активное/неактивное)");
        System.out.println("5. Назначённые после даты");
        System.out.println("6. Истекающие до даты");
        System.out.print("Выберите (1-6): ");
        String choice = scanner.nextLine().trim();
        AssignmentFilter filter = null;
        switch (choice) {
            case "1":
                System.out.print("Введите username: ");
                String username = scanner.nextLine().trim();
                filter = AssignmentFilters.byUsername(username);
                break;
            case "2":
                System.out.print("Введите название роли: ");
                String roleName = scanner.nextLine().trim();
                filter = AssignmentFilters.byRoleName(roleName);
                break;
            case "3":
                System.out.print("Введите тип (PERMANENT/TEMPORARY): ");
                String type = scanner.nextLine().trim();
                filter = AssignmentFilters.byType(type);
                break;
            case "4":
                System.out.print("Активные? (да/нет): ");
                String active = scanner.nextLine().trim();
                filter = active.equalsIgnoreCase("да") ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                break;
            case "5":
                System.out.print("Введите дату (формат: ГГГГ-ММ-ДДТЧЧ:ММ:СС): ");
                String dateAfter = scanner.nextLine().trim();
                filter = AssignmentFilters.assignedAfter(dateAfter);
                break;
            case "6":
                System.out.print("Введите дату (формат: ГГГГ-ММ-ДДТЧЧ:ММ:СС): ");
                String dateBefore = scanner.nextLine().trim();
                filter = AssignmentFilters.expiringBefore(dateBefore);
                break;
            default:
                System.out.println("Неверный выбор. Поиск отменен.");
                return;
        }
        List<RoleAssignment> results = system.getAssignmentManager().findByFilter(filter);
        printAssignmentTable(results);
    }
    
    private static void permissionsUser(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя: ");
        String username = scanner.nextLine().trim();
        User user = system.getUserManager().findByUsername(username).get();
        Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
        if (permissions.isEmpty()) {
            System.out.println("У пользователя нет прав.");
            return;
        }
        Map<String, List<Permission>> byResource = new HashMap<>();
        for (Permission p : permissions) {
            byResource.computeIfAbsent(p.resource(), k -> new ArrayList<>()).add(p);
        }
        System.out.println("Права пользователя '" + username + "'");
        for (Map.Entry<String, List<Permission>> entry : byResource.entrySet()) {
            System.out.println("Ресурс: " + entry.getKey());
            for (Permission p : entry.getValue()) {
                System.out.println("  - " + p.name() + ": " + p.description());
            }
        }
        System.out.println("Всего прав: " + permissions.size());
    }
    
    private static void permissionsCheck(Scanner scanner, RBACSystem system) {
        System.out.print("Введите username пользователя: ");
        String username = scanner.nextLine().trim();
        User user = system.getUserManager().findByUsername(username).get();
        System.out.print("Введите название права: ");
        String permName = scanner.nextLine().trim();
        System.out.print("Введите ресурс: ");
        String resource = scanner.nextLine().trim();
        
        boolean hasPermission = system.getAssignmentManager().userHasPermission(user, permName, resource);
        if (hasPermission) {
            Set<Permission> userPerms = system.getAssignmentManager().getUserPermissions(user);
            Permission target = null;
            for (Permission p : userPerms) {
                if (p.matches(permName, resource)) {
                    target = p;
                    break;
                }
            }
            System.out.println("У пользователя есть право '" + permName + "' на ресурс '" + resource + "'.");
        } else {
            System.out.println("У пользователя НЕТ права '" + permName + "' на ресурс '" + resource + "'.");
        }
    }
    
    private static void help(Scanner scanner, RBACSystem system) {
        system.commandParser.printHelp();
    }
    
    private static void stats(Scanner scanner, RBACSystem system) {
        System.out.println(system.generateStatistics());
    }
    
    private static void clear(Scanner scanner, RBACSystem system) {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }
    
    private static void exit(Scanner scanner, RBACSystem system) {
        System.out.println("Выход из программы...");
        System.exit(0);
    }
}
