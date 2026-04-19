import java.util.*;
import java.util.UUID;

public class Role {
    private final String id;
    private final String name;
    private String description;
    private final Set<Permission> permissions;

    private static String generateUUID() {
        return "role_" + UUID.randomUUID().toString();
    }

    private static String validateName(String name) {
        ValidationUtils.requireNonEmpty(name, "Имя роли");
        return name.trim();
    }

    private static String validateDescription(String description) {
        ValidationUtils.requireNonEmpty(description, "Описание");
        return description.trim();
    }

    public Role(String name, String description) {
        this.id = generateUUID();
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.permissions = new HashSet<>();
    }

    public Role(String id, String name, String description) {
        ValidationUtils.requireNonEmpty(id, "ID");
        this.id = id;
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.permissions = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = validateDescription(description);
    }

    public void addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission не может быть null");
        }
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission не может быть null");
        }
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permission != null && permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return false;
        }

        String trimmedName = permissionName.trim();
        String normalizedResource = resource.trim().toLowerCase();

        return permissions.stream()
                .anyMatch(p -> p.name().equals(trimmedName) &&
                        p.resource().equals(normalizedResource));
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(new HashSet<>(permissions));
    }

    public int getPermissionCount() {
        return permissions.size();
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description.isEmpty() ? "No description" : description));

        if (permissions.isEmpty()) {
            sb.append("Permissions: none");
        } else {
            sb.append(String.format("Permissions (%d):\n", permissions.size()));
            permissions.stream()
                    .sorted(Comparator.comparing(Permission::name)
                            .thenComparing(Permission::resource))
                    .forEach(p -> sb.append(" - ").append(p.format()).append("\n"));
        }

        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Role{id='%s', name='%s', description='%s', permissions=%d}",
                id, name, description, permissions.size());
    }
}