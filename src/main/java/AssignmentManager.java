import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignmentsById;
    private final UserManager userManager;
    private final RoleManager roleManager;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.assignmentsById = new ConcurrentHashMap<>();
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment не может быть null");

        User user = assignment.user();
        Role role = assignment.role();

        if (!userManager.exists(user.username())) {
            throw new IllegalArgumentException("User '" + user.username() + "' не существует");
        }

        if (!roleManager.findByName(role.getName()).isPresent()) {
            throw new IllegalArgumentException("Role '" + role.getName() + "' не существует");
        }

        synchronized (assignmentsById) {
            if (assignmentsById.containsKey(assignment.assignmentId())) {
                throw new IllegalArgumentException("Assignment с ID '" + assignment.assignmentId() + "' уже существует");
            }

            boolean hasActiveAssignment = assignmentsById.values().stream()
                    .filter(a -> a.user().equals(user) && a.role().equals(role))
                    .anyMatch(RoleAssignment::isActive);

            if (hasActiveAssignment) {
                throw new IllegalStateException("User '" + user.username() + "' уже есть активное назначение для роли '" + role.getName() + "'");
            }

            assignmentsById.put(assignment.assignmentId(), assignment);
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment не может быть null");
        return assignmentsById.remove(assignment.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        Objects.requireNonNull(id, "ID не может быть null");
        return Optional.ofNullable(assignmentsById.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return List.copyOf(assignmentsById.values());
    }

    @Override
    public int count() {
        return assignmentsById.size();
    }

    @Override
    public void clear() {
        assignmentsById.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        Objects.requireNonNull(user, "User не может быть null");
        return assignmentsById.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        Objects.requireNonNull(role, "Role не может быть null");
        return assignmentsById.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        return assignmentsById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        Objects.requireNonNull(sorter, "Sorter не может быть null");

        return assignmentsById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignmentsById.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignmentsById.values().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        Objects.requireNonNull(user, "User не может быть null");
        Objects.requireNonNull(role, "Role не может быть null");

        return assignmentsById.values().stream()
                .filter(a -> a.user().equals(user) && a.role().equals(role))
                .anyMatch(RoleAssignment::isActive);
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        Objects.requireNonNull(user, "User не может быть null");
        Objects.requireNonNull(permissionName, "Permission name не может быть null");
        Objects.requireNonNull(resource, "Resource не может быть null");

        return getUserPermissions(user).stream()
                .anyMatch(p -> p.matches(permissionName, resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        Objects.requireNonNull(user, "User не может быть null");

        return assignmentsById.values().stream()
                .filter(a -> a.user().equals(user) && a.isActive())
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        Objects.requireNonNull(assignmentId, "Assignment ID не может быть null");

        synchronized (assignmentsById) {
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment с ID '" + assignmentId + "' не найден");
            }

            if (assignment.assignmentType().equals("PERMANENT")) {
                PermanentAssignment permAssignment = (PermanentAssignment) assignment;
                permAssignment.revoke();
            } else {
                assignmentsById.remove(assignmentId);
            }
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        Objects.requireNonNull(assignmentId, "Assignment ID не может быть null");
        Objects.requireNonNull(newExpirationDate, "Новый срок годности не может быть null");

        synchronized (assignmentsById) {
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment с ID '" + assignmentId + "' не найден");
            }

            if (!assignment.assignmentType().equals("TEMPORARY")) {
                throw new IllegalArgumentException("Assignment не является временным");
            }

            TemporaryAssignment tempAssignment = (TemporaryAssignment) assignment;
            tempAssignment.extend(newExpirationDate);
        }
    }

    public List<RoleAssignment> findAssignmentsByUserAndRole(User user, Role role) {
        Objects.requireNonNull(user, "User не может быть null");
        Objects.requireNonNull(role, "Role не может быть null");

        return assignmentsById.values().stream()
                .filter(a -> a.user().equals(user) && a.role().equals(role))
                .collect(Collectors.toList());
    }

    public Map<User, List<Role>> getUserRolesMap() {
        synchronized (assignmentsById) {
            Map<User, List<Role>> result = new HashMap<>();

            for (RoleAssignment assignment : getActiveAssignments()) {
                result.computeIfAbsent(assignment.user(), k -> new ArrayList<>())
                        .add(assignment.role());
            }

            return result;
        }
    }

    public List<RoleAssignment> getAssignmentsExpiringBefore(String date) {
        synchronized (assignmentsById) {
            Objects.requireNonNull(date, "Date cannot be null");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime filterDate;
            try {
                filterDate = LocalDateTime.parse(date, formatter);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Date must be in format: yyyy-MM-dd HH:mm", e);
            }
            return assignmentsById.values().stream()
                    .filter(a -> a.assignmentType().equals("TEMPORARY"))
                    .filter(a -> {
                        TemporaryAssignment temp = (TemporaryAssignment) a;
                        try {
                            LocalDateTime expiresAt = LocalDateTime.parse(temp.getExpiresAt(), formatter);
                            return expiresAt.isBefore(filterDate);
                        } catch (DateTimeParseException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    public boolean removeByUser(User user) {
        Objects.requireNonNull(user, "User не может быть null");

        synchronized (assignmentsById) {
            List<String> toRemove = assignmentsById.values().stream()
                    .filter(a -> a.user().equals(user))
                    .map(RoleAssignment::assignmentId)
                    .collect(Collectors.toList());

            toRemove.forEach(assignmentsById::remove);
            return !toRemove.isEmpty();
        }
    }

    public boolean removeByRole(Role role) {
        Objects.requireNonNull(role, "Role  null");

        synchronized (assignmentsById) {
            List<String> toRemove = assignmentsById.values().stream()
                    .filter(a -> a.role().equals(role))
                    .map(RoleAssignment::assignmentId)
                    .collect(Collectors.toList());

            toRemove.forEach(assignmentsById::remove);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentManager that = (AssignmentManager) o;
        return Objects.equals(assignmentsById, that.assignmentsById);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentsById);
    }

    @Override
    public String toString() {
        return String.format("AssignmentManager{assignments=%d, active=%d}",
                count(), getActiveAssignments().size());
    }
}