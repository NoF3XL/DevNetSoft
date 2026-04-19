import java.util.Scanner;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

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
        parser.registerCommand("report-users", "вывести/сохранить отчёт по пользователям", CommandRegistry::reportUsers);
        parser.registerCommand("report-users-async", "запуск генерации отчёта по пользователям в отдельном потоке", CommandRegistry::reportUsersAsync);
        parser.registerCommand("report-roles", "отчёт по ролям", CommandRegistry::reportRoles);
        parser.registerCommand("report-roles-async", "запуск генерации отчёта по ролям в отдельном потоке", CommandRegistry::reportRolesAsync);
        parser.registerCommand("report-matrix", "отчет по правам", CommandRegistry::reportMatrix);
        parser.registerCommand("report-matrix-async", "запуск генерации отчёта по правам в отдельном потоке", CommandRegistry::reportMatrixAsync);
        parser.registerCommand("save-async", "сохранение данных в файл в фоне", CommandRegistry::saveAsync);
        parser.registerCommand("audit-log", "просмотр лога аудита", CommandRegistry::auditLog);
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
        System.out.println(ConsoleUtils.formatHeader("Создание нового пользователя"));
        String username = ConsoleUtils.promptString(scanner, "Введите username: ", true);
        String fullName = ConsoleUtils.promptString(scanner, "Введите полное имя: ", true);
        String email = ConsoleUtils.promptString(scanner, "Введите email: ", true);
        
        try {
            User user = User.create(username, fullName, email);
            system.getUserManager().add(user);
            System.out.println(ConsoleUtils.formatSuccess("Пользователь '" + username + "' успешно создан."));
            system.getAuditLog().log("USER_CREATE", system.getCurrentUser(), username,
                "Пользователь создан: " + fullName + " <" + email + ">");
        } catch (IllegalArgumentException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка создания пользователя: " + e.getMessage()));
        }
    }
    
    private static void userView(Scanner scanner, RBACSystem system) {
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя: ", true);
        User user = system.getUserManager().findByUsername(username).get();
        System.out.println(ConsoleUtils.formatHeader("Информация о пользователе"));
        System.out.println("Username: " + user.username());
        System.out.println("Полное имя: " + user.fullName());
        System.out.println("Email: " + user.email());
    }
    
    private static void userUpdate(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Обновление данных пользователя"));
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя для обновления: ", true);
        if (!system.getUserManager().exists(username)) {
            System.out.println(ConsoleUtils.formatError("Пользователь '" + username + "' не найден."));
            return;
        }
        String newFullName = ConsoleUtils.promptString(scanner, "Введите новое полное имя (оставьте пустым, чтобы оставить текущее): ", false);
        String newEmail = ConsoleUtils.promptString(scanner, "Введите новый email (оставьте пустым, чтобы оставить текущее): ", false);
        
        User current = system.getUserManager().findByUsername(username).get();
        if (newFullName.isEmpty()) {
            newFullName = current.fullName();
        }
        if (newEmail.isEmpty()) {
            newEmail = current.email();
        }
        
        try {
            system.getUserManager().update(username, newFullName, newEmail);
            System.out.println(ConsoleUtils.formatSuccess("Данные пользователя '" + username + "' успешно обновлены."));
        } catch (IllegalArgumentException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка обновления: " + e.getMessage()));
        }
    }
    
    private static void userDelete(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Удаление пользователя"));
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя для удаления: ", true);
        if (!system.getUserManager().exists(username)) {
            System.out.println(ConsoleUtils.formatError("Пользователь '" + username + "' не найден."));
            return;
        }
        boolean removed = system.getUserManager().removeByUsername(username);
        if (removed) {
            System.out.println(ConsoleUtils.formatSuccess("Пользователь '" + username + "' успешно удален."));
            system.getAuditLog().log("USER_DELETE", system.getCurrentUser(), username, "Пользователь удален");
        } else {
            System.out.println(ConsoleUtils.formatError("Не удалось удалить пользователя."));
        }
    }
    
    private static void userSearch(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Поиск пользователей"));
        System.out.println("Выберите тип фильтра:");
        System.out.println("1. По username (содержит)");
        System.out.println("2. По email (содержит)");
        System.out.println("3. По домену email");
        System.out.println("4. По полному имени (содержит)");
        int choice = ConsoleUtils.promptInt(scanner, "Выберите (1-4): ", 1, 4);
        UserFilter filter = null;
        switch (choice) {
            case 1:
                String usernameSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в username: ", true);
                filter = UserFilters.byUsernameContains(usernameSub);
                break;
            case 2:
                String emailSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в email: ", true);
                filter = user -> user.email().toLowerCase().contains(emailSub.toLowerCase());
                break;
            case 3:
                String domain = ConsoleUtils.promptString(scanner, "Введите домен (например 'example.com'): ", true);
                filter = UserFilters.byEmailDomain(domain);
                break;
            case 4:
                String fullNameSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в полном имени: ", true);
                filter = UserFilters.byFullNameContains(fullNameSub);
                break;
            default:
                System.out.println(ConsoleUtils.formatError("Неверный выбор. Поиск отменен."));
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
        System.out.println(ConsoleUtils.formatHeader("Создание новой роли"));
        String name = ConsoleUtils.promptString(scanner, "Введите название роли: ", true);
        String description = ConsoleUtils.promptString(scanner, "Введите описание роли: ", true);
        
        try {
            Role role = new Role(name, description);
            system.getRoleManager().add(role);
            System.out.println(ConsoleUtils.formatSuccess("Роль '" + name + "' успешно создана."));
            system.getAuditLog().log("ROLE_CREATE", system.getCurrentUser(), name, "Роль создана: " + description);
        } catch (IllegalArgumentException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка создания роли: " + e.getMessage()));
        }
    }
    
    private static void roleView(Scanner scanner, RBACSystem system) {
        String name = ConsoleUtils.promptString(scanner, "Введите название роли: ", true);
        Role role = system.getRoleManager().findByName(name).get();
        System.out.println(ConsoleUtils.formatHeader("Информация о роли"));
        System.out.println(role.format());
    }
    
    private static void roleUpdate(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Обновление роли"));
        String name = ConsoleUtils.promptString(scanner, "Введите название роли для обновления: ", true);
        Role role = system.getRoleManager().findByName(name).get();
        String newName = ConsoleUtils.promptString(scanner, "Введите новое название (оставьте пустым, чтобы оставить текущее): ", false);
        String newDescription = ConsoleUtils.promptString(scanner, "Введите новое описание (оставьте пустым, чтобы оставить текущее): ", false);
        
        if (!newDescription.isEmpty()) {
            role.setDescription(newDescription);
        }
        System.out.println(ConsoleUtils.formatSuccess("Роль обновлена."));
    }
    
    private static void roleDelete(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Удаление роли"));
        String name = ConsoleUtils.promptString(scanner, "Введите название роли для удаления: ", true);
        Role role = system.getRoleManager().findByName(name).get();
        List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
        if (!assignments.isEmpty()) {
            System.out.println(ConsoleUtils.formatError("Предупреждение: роль назначена " + assignments.size() + " пользователям"));
            System.out.println("Пользователи с этой ролью:");
            for (RoleAssignment assignment : assignments) {
                System.out.println(" - " + assignment.user().username());
            }
        }
        try {
            boolean removed = system.getRoleManager().remove(role);
            if (removed) {
                System.out.println(ConsoleUtils.formatSuccess("Роль '" + name + "' успешно удалена."));
                system.getAuditLog().log("ROLE_DELETE", system.getCurrentUser(), name, "Роль удалена");
            } else {
                System.out.println(ConsoleUtils.formatError("Не удалось удалить роль."));
            }
        } catch (IllegalStateException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка удаления: " + e.getMessage()));
        }
    }
    
    private static void roleAddPermission(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Добавление права к роли"));
        String roleName = ConsoleUtils.promptString(scanner, "Введите название роли: ", true);
        if (!system.getRoleManager().exists(roleName)) {
            System.out.println(ConsoleUtils.formatError("Роль '" + roleName + "' не найдена."));
            return;
        }
        String permName = ConsoleUtils.promptString(scanner, "Введите название права: ", true);
        String resource = ConsoleUtils.promptString(scanner, "Введите ресурс: ", true);
        String description = ConsoleUtils.promptString(scanner, "Введите описание права: ", true);
        
        Permission permission = new Permission(permName, resource, description);
        try {
            system.getRoleManager().addPermissionToRole(roleName, permission);
            System.out.println(ConsoleUtils.formatSuccess("Право добавлено к роли '" + roleName + "'"));
        } catch (IllegalArgumentException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка добавления права: " + e.getMessage()));
        }
    }
    
    private static void roleRemovePermission(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Удаление права из роли"));
        String roleName = ConsoleUtils.promptString(scanner, "Введите название роли: ", true);
        Role role = system.getRoleManager().findByName(roleName).get();
        Set<Permission> permissions = role.getPermissions();
        if (permissions.isEmpty()) {
            System.out.println(ConsoleUtils.formatError("У роли нет прав."));
            return;
        }
        System.out.println("Список прав роли:");
        int index = 1;
        List<Permission> permList = new ArrayList<>(permissions);
        for (Permission p : permList) {
            System.out.printf("%d. %s (%s) - %s%n", index++, p.name(), p.resource(), p.description());
        }
        int choice = ConsoleUtils.promptInt(scanner, "Введите номер права для удаления: ", 1, permList.size());
        Permission toRemove = permList.get(choice - 1);
        system.getRoleManager().removePermissionFromRole(roleName, toRemove);
        System.out.println(ConsoleUtils.formatSuccess("Право удалено."));
    }
    
    private static void roleSearch(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Поиск ролей"));
        System.out.println("Выберите тип фильтра:");
        System.out.println("1. По названию (содержит)");
        System.out.println("2. По наличию конкретного права");
        System.out.println("3. По минимальному количеству прав");
        int choice = ConsoleUtils.promptInt(scanner, "Выберите (1-3): ", 1, 3);
        RoleFilter filter = null;
        switch (choice) {
            case 1:
                String substring = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в названии: ", true);
                filter = RoleFilters.byNameContains(substring);
                break;
            case 2:
                String permName = ConsoleUtils.promptString(scanner, "Введите название права: ", true);
                String resource = ConsoleUtils.promptString(scanner, "Введите ресурс: ", true);
                filter = RoleFilters.hasPermission(permName, resource);
                break;
            case 3:
                int n = ConsoleUtils.promptInt(scanner, "Введите минимальное количество прав: ", 0, Integer.MAX_VALUE);
                filter = RoleFilters.hasAtLeastNPermissions(n);
                break;
            default:
                System.out.println(ConsoleUtils.formatError("Неверный выбор. Поиск отменен."));
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
        System.out.println(ConsoleUtils.formatHeader("Назначение роли пользователю"));
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя: ", true);
        User user = system.getUserManager().findByUsername(username).get();
        List<Role> roles = system.getRoleManager().findAll();
        if (roles.isEmpty()) {
            System.out.println(ConsoleUtils.formatError("Нет доступных ролей."));
            return;
        }
        System.out.println("Доступные роли:");
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            System.out.printf("%d. %s (%s)%n", i + 1, role.getName(), role.getDescription());
        }
        int choice = ConsoleUtils.promptInt(scanner, "Выберите номер роли: ", 1, roles.size());
        Role role = roles.get(choice - 1);
        
        boolean isTemporary = ConsoleUtils.promptYesNo(scanner, "Тип назначения - временное? (да/нет)");
        
        String expiresAt = null;
        if (isTemporary) {
            expiresAt = ConsoleUtils.promptString(scanner, "Введите дату истечения (формат: ГГГГ-ММ-ДД ЧЧ:ММ): ", true);
        }
        
        String reason = ConsoleUtils.promptString(scanner, "Причина назначения (оставьте пустым для 'Не указана'): ", false);
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
        
        try {
            system.getAssignmentManager().add(assignment);
            System.out.println(ConsoleUtils.formatSuccess("Роль '" + role.getName() + "' успешно назначена пользователю '" + username + "'"));
            system.getAuditLog().log("ROLE_ASSIGN", system.getCurrentUser(), username + " -> " + role.getName(),
                "Тип: " + (isTemporary ? "временное" : "постоянное") + ", причина: " + reason);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка назначения: " + e.getMessage()));
        }
    }
    
    private static void revokeRole(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Отзыв роли у пользователя"));
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя: ", true);
        User user = system.getUserManager().findByUsername(username).get();
        
        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
        List<RoleAssignment> activeAssignments = assignments.stream()
                .filter(RoleAssignment::isActive)
                .collect(java.util.stream.Collectors.toList());
        if (activeAssignments.isEmpty()) {
            System.out.println(ConsoleUtils.formatError("У пользователя нет активных назначений."));
            return;
        }
        System.out.println("Активные назначения пользователя:");
        for (int i = 0; i < activeAssignments.size(); i++) {
            RoleAssignment a = activeAssignments.get(i);
            System.out.printf("%d. Роль: %s, Тип: %s, Назначено: %s%n",
                    i + 1, a.role().getName(), a.assignmentType(), a.metadata().assignedAt());
        }
        int choice = ConsoleUtils.promptInt(scanner, "Выберите номер назначения для отзыва: ", 1, activeAssignments.size());
        RoleAssignment assignment = activeAssignments.get(choice - 1);
        try {
            system.getAssignmentManager().revokeAssignment(assignment.assignmentId());
            System.out.println(ConsoleUtils.formatSuccess("Назначение отозвано."));
            system.getAuditLog().log("ROLE_REVOKE", system.getCurrentUser(),
                assignment.user().username() + " -> " + assignment.role().getName(),
                "Назначение отозвано");
        } catch (IllegalArgumentException e) {
            System.out.println(ConsoleUtils.formatError("Ошибка отзыва: " + e.getMessage()));
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
        System.out.println(ConsoleUtils.formatHeader("Поиск назначений"));
        System.out.println("Выберите тип фильтра:");
        System.out.println("1. По пользователю");
        System.out.println("2. По роли");
        System.out.println("3. По типу (постоянное/временное)");
        System.out.println("4. По статусу (активное/неактивное)");
        System.out.println("5. Назначённые после даты");
        System.out.println("6. Истекающие до даты");
        int choice = ConsoleUtils.promptInt(scanner, "Выберите (1-6): ", 1, 6);
        AssignmentFilter filter = null;
        switch (choice) {
            case 1:
                String username = ConsoleUtils.promptString(scanner, "Введите username: ", true);
                filter = AssignmentFilters.byUsername(username);
                break;
            case 2:
                String roleName = ConsoleUtils.promptString(scanner, "Введите название роли: ", true);
                filter = AssignmentFilters.byRoleName(roleName);
                break;
            case 3:
                String type = ConsoleUtils.promptString(scanner, "Введите тип (PERMANENT/TEMPORARY): ", true);
                filter = AssignmentFilters.byType(type);
                break;
            case 4:
                boolean active = ConsoleUtils.promptYesNo(scanner, "Активные?");
                filter = active ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                break;
            case 5:
                String dateAfter = ConsoleUtils.promptString(scanner, "Введите дату (формат: ГГГГ-ММ-ДДТЧЧ:ММ:СС): ", true);
                filter = AssignmentFilters.assignedAfter(dateAfter);
                break;
            case 6:
                String dateBefore = ConsoleUtils.promptString(scanner, "Введите дату (формат: ГГГГ-ММ-ДДТЧЧ:ММ:СС): ", true);
                filter = AssignmentFilters.expiringBefore(dateBefore);
                break;
            default:
                System.out.println(ConsoleUtils.formatError("Неверный выбор. Поиск отменен."));
                return;
        }
        List<RoleAssignment> results = system.getAssignmentManager().findByFilter(filter);
        printAssignmentTable(results);
    }
    
    private static void permissionsUser(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Права пользователя"));
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя: ", true);
        User user = system.getUserManager().findByUsername(username).get();
        Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
        if (permissions.isEmpty()) {
            System.out.println(ConsoleUtils.formatError("У пользователя нет прав."));
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
        System.out.println(ConsoleUtils.formatHeader("Проверка права пользователя"));
        String username = ConsoleUtils.promptString(scanner, "Введите username пользователя: ", true);
        User user = system.getUserManager().findByUsername(username).get();
        String permName = ConsoleUtils.promptString(scanner, "Введите название права: ", true);
        String resource = ConsoleUtils.promptString(scanner, "Введите ресурс: ", true);
        
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
            System.out.println(ConsoleUtils.formatSuccess("У пользователя есть право '" + permName + "' на ресурс '" + resource + "'."));
        } else {
            System.out.println(ConsoleUtils.formatError("У пользователя НЕТ права '" + permName + "' на ресурс '" + resource + "'."));
        }
    }
    
    private static void help(Scanner scanner, RBACSystem system) {
        system.commandParser.printHelp();
    }
    
    private static void stats(Scanner scanner, RBACSystem system) {
        System.out.println(system.generateStatistics());
    }
    
    private static void reportUsers(Scanner scanner, RBACSystem system) {
        ReportGenerator generator = new ReportGenerator();
        String report = generator.generateUserReport(system.getUserManager(), system.getAssignmentManager());
        System.out.println(report);
        askExport(scanner, generator, report, "user-report.txt");
    }
    
    private static void reportRoles(Scanner scanner, RBACSystem system) {
        ReportGenerator generator = new ReportGenerator();
        String report = generator.generateRoleReport(system.getRoleManager(), system.getAssignmentManager());
        System.out.println(report);
        askExport(scanner, generator, report, "role-report.txt");
    }
    
    private static void reportMatrix(Scanner scanner, RBACSystem system) {
        ReportGenerator generator = new ReportGenerator();
        String report = generator.generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());
        System.out.println(report);
        askExport(scanner, generator, report, "permission-matrix.txt");
    }
    
    private static void askExport(Scanner scanner, ReportGenerator generator, String report, String defaultFilename) {
        String filename = ConsoleUtils.promptString(scanner, "Введите имя файла (по умолчанию " + defaultFilename + "): ", false);
        if (filename.isEmpty()) {
            filename = defaultFilename;
        }
        try {
            generator.exportToFile(report, filename);
            System.out.println(ConsoleUtils.formatSuccess("Отчёт сохранён в файл: " + filename));
        } catch (Exception e) {
            System.out.println(ConsoleUtils.formatError("Ошибка при сохранении файла: " + e.getMessage()));
        }
    }
    
    private static void auditLog(Scanner scanner, RBACSystem system) {
        system.getAuditLog().printLog();
    }

    private static void reportUsersAsync(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Запуск генерации отчёта по пользователям в фоновом режиме..."));
        system.getExecutorService().submit(() -> {
            try {
                ReportGenerator generator = new ReportGenerator();
                String report = generator.generateUserReportParallel(system.getUserManager(), system.getAssignmentManager());
                String filename = "user-report-async.txt";
                generator.exportToFile(report, filename);
                System.out.println(ConsoleUtils.formatSuccess("Асинхронный отчёт по пользователям сохранён в файл: " + filename));
                system.getAuditLog().log("REPORT_USERS_ASYNC", system.getCurrentUser(), "file", "Отчёт сохранён в " + filename);
            } catch (Exception e) {
                System.err.println(ConsoleUtils.formatError("Ошибка при генерации отчёта: " + e.getMessage()));
            }
        });
    }

    private static void reportRolesAsync(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Запуск генерации отчёта по ролям в фоновом режиме..."));
        system.getExecutorService().submit(() -> {
            try {
                ReportGenerator generator = new ReportGenerator();
                String report = generator.generateRoleReportParallel(system.getRoleManager(), system.getAssignmentManager());
                String filename = "role-report-async.txt";
                generator.exportToFile(report, filename);
                System.out.println(ConsoleUtils.formatSuccess("Асинхронный отчёт по ролям сохранён в файл: " + filename));
                system.getAuditLog().log("REPORT_ROLES_ASYNC", system.getCurrentUser(), "file", "Отчёт сохранён в " + filename);
            } catch (Exception e) {
                System.err.println(ConsoleUtils.formatError("Ошибка при генерации отчёта: " + e.getMessage()));
            }
        });
    }

    private static void reportMatrixAsync(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Запуск генерации матрицы прав в фоновом режиме..."));
        system.getExecutorService().submit(() -> {
            try {
                ReportGenerator generator = new ReportGenerator();
                String report = generator.generatePermissionMatrixParallel(system.getUserManager(), system.getAssignmentManager());
                String filename = "matrix-report-async.txt";
                generator.exportToFile(report, filename);
                System.out.println(ConsoleUtils.formatSuccess("Асинхронная матрица прав сохранён в файл: " + filename));
                system.getAuditLog().log("REPORT_MATRIX_ASYNC", system.getCurrentUser(), "file", "Отчёт сохранён в " + filename);
            } catch (Exception e) {
                System.err.println(ConsoleUtils.formatError("Ошибка при генерации отчёта: " + e.getMessage()));
            }
        });
    }

    private static void saveAsync(Scanner scanner, RBACSystem system) {
        System.out.println(ConsoleUtils.formatHeader("Запуск фонового сохранения данных системы..."));
        system.getExecutorService().submit(() -> {
            try {
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                saveUsersToFile(system.getUserManager(), "users_" + timestamp + ".dat");
                saveRolesToFile(system.getRoleManager(), "roles_" + timestamp + ".dat");
                saveAssignmentsToFile(system.getAssignmentManager(), "assignments_" + timestamp + ".dat");
                System.out.println(ConsoleUtils.formatSuccess("Данные системы сохранены в файлы с временной меткой " + timestamp));
                system.getAuditLog().log("SAVE_ASYNC", system.getCurrentUser(), "system", "Данные сохранены");
            } catch (Exception e) {
                System.err.println(ConsoleUtils.formatError("Ошибка при сохранении данных: " + e.getMessage()));
            }
        });
    }

    private static void saveUsersToFile(UserManager userManager, String filename) throws IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {
            for (User user : userManager.findAll()) {
                writer.printf("%s|%s|%s%n", user.username(), user.fullName(), user.email());
            }
        }
    }

    private static void saveRolesToFile(RoleManager roleManager, String filename) throws IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {
            for (Role role : roleManager.findAll()) {
                writer.printf("%s|%s|%d%n", role.getName(), role.getDescription(), role.getPermissionCount());
            }
        }
    }

    private static void saveAssignmentsToFile(AssignmentManager assignmentManager, String filename) throws IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {
            for (RoleAssignment assignment : assignmentManager.findAll()) {
                writer.printf("%s|%s|%s|%s|%s%n",
                        assignment.assignmentId(),
                        assignment.user().username(),
                        assignment.role().getName(),
                        assignment.assignmentType(),
                        assignment.isActive() ? "ACTIVE" : "INACTIVE");
            }
        }
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
