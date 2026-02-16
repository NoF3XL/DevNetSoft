import java.util.regex.*;

public record Permission(String name, String resource, String description) {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Z_]+$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    public Permission{
        name = normalizeName(name);
        resource = normalizeResource(resource);
        description = normalizeDescription(description);
    }

    public static Permission of(String name, String resource, String description) {
        return new Permission(name, resource, description);
    }

    public String format() {
        return String.format("%s on %s: %s text", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        if (namePattern == null && resourcePattern == null) {
            return true;
        }
        boolean nameMatches = namePattern == null ||
                name.contains(namePattern) ||
                name.matches(namePattern);
        boolean resourceMatches = resourcePattern == null ||
                resource.contains(resourcePattern) ||
                resource.matches(resourcePattern);
        return nameMatches && resourceMatches;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name не может быть null");
        }
        String normalized = name.trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Name не может быть пустым");
        }
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Name должен содержать только буквы (в верхнем регистре) и без пробелов и может быть нижние подчеркивания"
            );
        }
        return normalized;
    }

    private static String normalizeResource(String resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource не может быть null");
        }
        String normalized = resource.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Resource не может быть пустым");
        }
        if (!RESOURCE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Resource должен содержать только буквы, цифры и может быть нижние подчеркивания"
            );
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            throw new IllegalArgumentException("Description не может быть null");
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Description не может быть пустым");
        }
        return normalized;
    }

    public static void main(String[] args) {
        try {
            Permission perm1 = Permission.of("read", "USERS", "Разрешение на чтение пользователей");
            System.out.println("Успешно создано:" + perm1.format());
            System.out.println("name нормализован:'" + perm1.name() + "'");
            System.out.println(" resource нормализован:'" + perm1.resource() + "'");
            System.out.println(" description нормализовано:'" + perm1.description() + "'");
            System.out.println();
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка:" + e.getMessage() + "\n");
        }
        try {
            Permission perm2 = new Permission("WRITE", "reports", "Разрешение на запись в отчёты");
            Permission perm3 = Permission.of("DELETE", "settings", "Разрешение на удаление настроек");

            System.out.println("Создано через конструктор:" + perm2.format());
            System.out.println("Создано через of():" + perm3.format());
            System.out.println();
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка:" + e.getMessage() + "\n");
        }
        try {
            Permission perm4 = Permission.of("READ WRITE", "users", "Описание");
            System.out.println("Успешно создано:" + perm4.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с именем 'READ WRITE':");
            System.out.println("  " + e.getMessage() + "\n");
        }
        try {
            Permission perm5 = Permission.of("READ@USER", "users", "Описание");
            System.out.println("Успешно создано:" + perm5.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с именем 'READ@USER':");
            System.out.println("  " + e.getMessage() + "\n");
        }
        try {
            Permission perm6 = Permission.of("READ", "user accounts", "Описание");
            System.out.println("Успешно создано:" + perm6.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с ресурсом 'user accounts':");
            System.out.println("  " + e.getMessage() + "\n");
        }
        try {
            Permission perm7 = Permission.of("READ", "users", "");
            System.out.println("Успешно создано:" + perm7.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с пустым описанием:");
            System.out.println("  " + e.getMessage() + "\n");
        }
        Permission testPerm = Permission.of("READ_USERS", "user_profiles", "Чтение профилей пользователей");

        System.out.println("Право: " + testPerm.format());
        System.out.println("matches('READ', 'user') -> " + testPerm.matches("READ", "user"));
        System.out.println("matches('WRITE', 'user') -> " + testPerm.matches("WRITE", "user"));
        System.out.println("matches('READ', 'admin') -> " + testPerm.matches("READ", "admin"));
        System.out.println("matches('READ_.*', '.*profiles') -> " + testPerm.matches("READ_.*", ".*profiles"));
        System.out.println("matches(null, 'user') -> " + testPerm.matches(null, "user"));
        System.out.println("matches('READ', null) -> " + testPerm.matches("READ", null));
        System.out.println("matches(null, null) -> " + testPerm.matches(null, null));
        System.out.println();

        Permission permRead = Permission.of("READ", "users", "Чтение пользователей");
        Permission permWrite = Permission.of("WRITE", "users", "Запись пользователей");
        Permission permDelete = Permission.of("DELETE", "reports", "Удаление отчётов");

        System.out.println("Тестирование поиска по шаблонам:");
        System.out.println("Права для поиска:");
        System.out.println("  1. " + permRead.format());
        System.out.println("  2. " + permWrite.format());
        System.out.println("  3. " + permDelete.format());
        System.out.println();

        System.out.println("Поиск по шаблону name='READ', resource='users':");
        System.out.println("  permRead.matches: " + permRead.matches("READ", "users"));
        System.out.println("  permWrite.matches: " + permWrite.matches("READ", "users"));
        System.out.println("  permDelete.matches: " + permDelete.matches("READ", "users"));
        System.out.println();

        System.out.println("Поиск по шаблону name='.*TE$', resource='users' (заканчивается на TE):");
        System.out.println("  permRead.matches: " + permRead.matches(".*TE$", "users"));
        System.out.println("  permWrite.matches: " + permWrite.matches(".*TE$", "users"));
        System.out.println("  permDelete.matches: " + permDelete.matches(".*TE$", "users"));
    }
}
