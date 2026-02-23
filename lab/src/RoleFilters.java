import java.util.Objects;
import java.util.Set;

public final class RoleFilters {

    private RoleFilters() {

    }

    public static RoleFilter byName(String name) {
        Objects.requireNonNull(name, "Название роли не может быть null");
        return role -> role.getName().equals(name);
    }

    public static RoleFilter byNameContains(String substring) {
        Objects.requireNonNull(substring, "Подстрока не может быть null");
        String lowerSubstring = substring.toLowerCase();
        return role -> role.getName().toLowerCase().contains(lowerSubstring);
    }

    public static RoleFilter hasPermission(Permission permission) {
        Objects.requireNonNull(permission, "Право доступа не может быть null");
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String permissionName, String resource) {
        Objects.requireNonNull(permissionName, "Название права не может быть null");
        Objects.requireNonNull(resource, "Ресурс не может быть null");
        String normalizedName = permissionName.toUpperCase().trim();
        String normalizedResource = resource.toLowerCase().trim();
        return role -> role.hasPermission(normalizedName, normalizedResource);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Количество прав не может быть отрицательным: " + n);
        }
        return role -> {
            Set<Permission> permissions = role.getPermissions();
            return permissions.size() >= n;
        };
    }


    public static RoleFilter hasNoPermissions() {
        return role -> role.getPermissions().isEmpty();
    }

    public static RoleFilter hasAllPermissions(Permission... permissions) {
        Objects.requireNonNull(permissions, "Массив прав не может быть null");
        return role -> {
            Set<Permission> rolePermissions = role.getPermissions();
            for (Permission p : permissions) {
                if (!rolePermissions.contains(p)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static RoleFilter hasAnyPermission(Permission... permissions) {
        Objects.requireNonNull(permissions, "Массив прав не может быть null");
        if (permissions.length == 0) {
            return role -> false;
        }
        return role -> {
            Set<Permission> rolePermissions = role.getPermissions();
            for (Permission p : permissions) {
                if (rolePermissions.contains(p)) {
                    return true;
                }
            }
            return false;
        };
    }
    public static RoleFilter byDescriptionContains(String substring) {
        Objects.requireNonNull(substring, "Подстрока не может быть null");
        String lowerSubstring = substring.toLowerCase();
        return role -> role.getDescription().toLowerCase().contains(lowerSubstring);
    }
}