import java.util.*;
import java.io.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Отчем по пользователям\n");
        sb.append(String.format("%-20s %-30s %-20s %s\n", "Пользователь", "Полное имя", "Email", "Роли"));

        List<User> users = userManager.findAllSorted(Comparator.comparing(User::username));
        for (User user : users) {
            List<RoleAssignment> assignments = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
            String roles = assignments.stream()
                    .map(a -> a.role().getName())
                    .collect(Collectors.joining(", "));
            if (roles.isEmpty()) {
                roles = "(нет ролей)";
            }
            sb.append(String.format("%-20s %-30s %-20s %s\n",
                    user.username(),
                    user.fullName(),
                    user.email(),
                    roles));
        }
        sb.append(String.format("Всего пользователей: %d\n", users.size()));
        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Отчет по ролям\n");
        sb.append(String.format("%-20s %-40s %-15s %s\n", "Роль", "Описание", "Прав", "Пользователей"));

        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .collect(Collectors.toList());
        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
            int userCount = (int) assignments.stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .count();
            sb.append(String.format("%-20s %-40s %-15d %d\n",
                    role.getName(),
                    role.getDescription(),
                    role.getPermissionCount(),
                    userCount));
        }
        sb.append(String.format("Всего ролей: %d\n", roles.size()));
        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Отчет по правам\n");

        List<User> users = userManager.findAllSorted(Comparator.comparing(User::username));
        Set<String> allResources = new TreeSet<>();
        Map<User, Map<String, Set<String>>> matrix = new HashMap<>();

        for (User user : users) {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            Map<String, Set<String>> perResource = new HashMap<>();
            for (Permission perm : permissions) {
                String resource = perm.resource();
                allResources.add(resource);
                perResource.computeIfAbsent(resource, k -> new HashSet<>()).add(perm.name());
            }
            matrix.put(user, perResource);
        }

        sb.append(String.format("%-20s", "Пользователь"));
        for (String resource : allResources) {
            sb.append(String.format(" %-20s", resource));
        }
        sb.append("\n");

        for (User user : users) {
            sb.append(String.format("%-20s", user.username()));
            Map<String, Set<String>> perResource = matrix.get(user);
            for (String resource : allResources) {
                Set<String> perms = perResource.get(resource);
                String cell = perms == null || perms.isEmpty() ? "-" :
                        perms.stream().sorted().collect(Collectors.joining(", "));
                sb.append(String.format(" %-20s", cell));
            }
            sb.append("\n");
        }
        sb.append(String.format("Всего пользователей: %d, всего ресурсов: %d\n", users.size(), allResources.size()));
        return sb.toString();
    }

    public String generateUserReportParallel(UserManager userManager, AssignmentManager assignmentManager) {
        List<User> users = userManager.findAllSorted(Comparator.comparing(User::username));
        String header = "Отчем по пользователям\n" +
                String.format("%-20s %-30s %-20s %s\n", "Пользователь", "Полное имя", "Email", "Роли");
        String body = users.parallelStream()
                .map(user -> {
                    List<RoleAssignment> assignments = assignmentManager.findByUser(user).stream()
                            .filter(RoleAssignment::isActive)
                            .collect(Collectors.toList());
                    String roles = assignments.stream()
                            .map(a -> a.role().getName())
                            .collect(Collectors.joining(", "));
                    if (roles.isEmpty()) {
                        roles = "(нет ролей)";
                    }
                    return String.format("%-20s %-30s %-20s %s",
                            user.username(),
                            user.fullName(),
                            user.email(),
                            roles);
                })
                .collect(Collectors.joining("\n"));
        return header + body + String.format("\nВсего пользователей: %d\n", users.size());
    }

    public String generatePermissionMatrixParallel(UserManager userManager, AssignmentManager assignmentManager) {
        List<User> users = userManager.findAllSorted(Comparator.comparing(User::username));
        Set<String> allResources = new ConcurrentSkipListSet<>();
        Map<User, Map<String, Set<String>>> matrix = new ConcurrentHashMap<>();

        users.parallelStream().forEach(user -> {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            Map<String, Set<String>> perResource = new ConcurrentHashMap<>();
            for (Permission perm : permissions) {
                String resource = perm.resource();
                allResources.add(resource);
                perResource.computeIfAbsent(resource, k -> ConcurrentHashMap.newKeySet()).add(perm.name());
            }
            matrix.put(user, perResource);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("Отчет по правам\n");
        sb.append(String.format("%-20s", "Пользователь"));
        for (String resource : allResources) {
            sb.append(String.format(" %-20s", resource));
        }
        sb.append("\n");

        users.forEach(user -> {
            sb.append(String.format("%-20s", user.username()));
            Map<String, Set<String>> perResource = matrix.get(user);
            for (String resource : allResources) {
                Set<String> perms = perResource != null ? perResource.get(resource) : null;
                String cell = perms == null || perms.isEmpty() ? "-" :
                        perms.stream().sorted().collect(Collectors.joining(", "));
                sb.append(String.format(" %-20s", cell));
            }
            sb.append("\n");
        });
        sb.append(String.format("Всего пользователей: %d, всего ресурсов: %d\n", users.size(), allResources.size()));
        return sb.toString();
    }

    public String generateRoleReportParallel(RoleManager roleManager, AssignmentManager assignmentManager) {
        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .collect(Collectors.toList());
        String header = "Отчет по ролям\n" +
                String.format("%-20s %-40s %-15s %s\n", "Роль", "Описание", "Прав", "Пользователей");
        String body = roles.parallelStream()
                .map(role -> {
                    List<RoleAssignment> assignments = assignmentManager.findByRole(role).stream()
                            .filter(RoleAssignment::isActive)
                            .collect(Collectors.toList());
                    int userCount = (int) assignments.stream()
                            .map(a -> a.user().username())
                            .distinct()
                            .count();
                    return String.format("%-20s %-40s %-15d %d",
                            role.getName(),
                            role.getDescription(),
                            role.getPermissionCount(),
                            userCount);
                })
                .collect(Collectors.joining("\n"));
        return header + body + String.format("\nВсего ролей: %d\n", roles.size());
    }

    public void exportToFile(String report, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(report);
        }
    }
}
