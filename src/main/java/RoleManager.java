import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById;
    private final Map<String, Role> rolesByName;
    private AssignmentManager assignmentManager;

    public RoleManager(AssignmentManager assignmentManager) {
        this.rolesById = new ConcurrentHashMap<>();
        this.rolesByName = new ConcurrentHashMap<>();
        this.assignmentManager = assignmentManager;
    }

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        Objects.requireNonNull(role, "Role не может быть null");

        synchronized (rolesById) {
            if (rolesById.containsKey(role.getId())) {
                throw new IllegalArgumentException("Role с ID '" + role.getId() + "' уже существует");
            }

            if (rolesByName.containsKey(role.getName())) {
                throw new IllegalArgumentException("Role с именем '" + role.getName() + "' уже существует");
            }

            rolesById.put(role.getId(), role);
            rolesByName.put(role.getName(), role);
        }
    }

    @Override
    public boolean remove(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        synchronized (rolesById) {
            if (assignmentManager == null) {
                throw new IllegalStateException("AssignmentManager not initialized");
            }
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            if (!assignments.isEmpty()) {
                throw new IllegalStateException("Cannot delete role '" + role.getName() +
                        "' because it is assigned to " + assignments.size() + " user(s)");
            }

            Role removed = rolesById.remove(role.getId());
            if (removed != null) {
                rolesByName.remove(removed.getName());
                return true;
            }
            return false;
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        Objects.requireNonNull(id, "ID не может быть null");
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return List.copyOf(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        synchronized (rolesById) {
            rolesById.clear();
            rolesByName.clear();
        }
    }

    public Optional<Role> findByName(String name) {
        Objects.requireNonNull(name, "Name не может быть null");
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        return rolesById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        return rolesById.values().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        Objects.requireNonNull(sorter, "Sorter не может быть null");

        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        Objects.requireNonNull(name, "Name не может бытьnull");
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Objects.requireNonNull(roleName, "Role name не может быть null");
        Objects.requireNonNull(permission, "Permission не может быть null");

        synchronized (rolesById) {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role с именем '" + roleName + "' не найден");
            }

            role.addPermission(permission);
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Objects.requireNonNull(roleName, "Role name не может быть null");
        Objects.requireNonNull(permission, "Permission не может быть null");

        synchronized (rolesById) {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role с именем '" + roleName + "' не найден");
            }

            role.removePermission(permission);
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        Objects.requireNonNull(permissionName, "Permission  не может быть null");
        Objects.requireNonNull(resource, "Resource не может быть null");

        String normalizedName = permissionName.toUpperCase();
        String normalizedResource = resource.toLowerCase();

        return rolesById.values().stream()
                .filter(role -> role.hasPermission(normalizedName, normalizedResource))
                .collect(Collectors.toList());
    }

    public List<Role> findRolesWithPermission(Permission permission) {
        Objects.requireNonNull(permission, "Permission не может быть null");

        return rolesById.values().stream()
                .filter(role -> role.hasPermission(permission))
                .collect(Collectors.toList());
    }

    public Set<String> getAllRoleNames() {
        return Set.copyOf(rolesByName.keySet());
    }

    public boolean removeByName(String name) {
        Objects.requireNonNull(name, "Name не может быть null");

        synchronized (rolesById) {
            Role role = rolesByName.get(name);
            if (role == null) {
                return false;
            }
            rolesById.remove(role.getId());
            rolesByName.remove(name);
            return true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleManager that = (RoleManager) o;
        return Objects.equals(rolesById, that.rolesById);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolesById);
    }

    @Override
    public String toString() {
        return String.format("RoleManager{roles=%d}", count());
    }
}