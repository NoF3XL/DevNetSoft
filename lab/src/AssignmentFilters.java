import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class AssignmentFilters {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private AssignmentFilters() {
    }

    public static AssignmentFilter byUser(User user) {
        Objects.requireNonNull(user, "Пользователь не может быть null");
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        Objects.requireNonNull(username, "Имя пользователя не может быть null");
        return assignment -> assignment.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        Objects.requireNonNull(role, "Роль не может быть null");
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        Objects.requireNonNull(roleName, "Название роли не может быть null");
        return assignment -> assignment.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        Objects.requireNonNull(type, "Тип назначения не может быть null");
        String normalizedType = type.toUpperCase();
        if (!normalizedType.equals("PERMANENT") && !normalizedType.equals("TEMPORARY")) {
            throw new IllegalArgumentException("Тип должен быть PERMANENT или TEMPORARY");
        }
        return assignment -> assignment.assignmentType().equals(normalizedType);
    }

    public static AssignmentFilter assignedBy(String username) {
        Objects.requireNonNull(username, "Имя назначившего не может быть null");
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        Objects.requireNonNull(date, "Дата не может быть null");
        LocalDateTime filterDate = LocalDateTime.parse(date, DATE_FORMATTER);
        return assignment -> {
            LocalDateTime assignedDate = LocalDateTime.parse(assignment.metadata().assignedAt(), DATE_FORMATTER);
            return assignedDate.isAfter(filterDate);
        };
    }

    public static AssignmentFilter assignedBefore(String date) {
        Objects.requireNonNull(date, "Дата не может быть null");
        LocalDateTime filterDate = LocalDateTime.parse(date, DATE_FORMATTER);
        return assignment -> {
            LocalDateTime assignedDate = LocalDateTime.parse(assignment.metadata().assignedAt(), DATE_FORMATTER);
            return assignedDate.isBefore(filterDate);
        };
    }

    public static AssignmentFilter assignedBetween(String startDate, String endDate) {
        Objects.requireNonNull(startDate, "Начальная дата не может быть null");
        Objects.requireNonNull(endDate, "Конечная дата не может быть null");
        LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
        return assignment -> {
            LocalDateTime assignedDate = LocalDateTime.parse(assignment.metadata().assignedAt(), DATE_FORMATTER);
            return !assignedDate.isBefore(start) && !assignedDate.isAfter(end);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        Objects.requireNonNull(date, "Дата не может быть null");
        LocalDateTime filterDate = LocalDateTime.parse(date, DATE_FORMATTER);
        return assignment -> {
            if (!assignment.assignmentType().equals("TEMPORARY")) {
                return false;
            }
            TemporaryAssignment tempAssignment = (TemporaryAssignment) assignment;
            LocalDateTime expiresAt = LocalDateTime.parse(tempAssignment.getExpiresAt(), DATE_FORMATTER);
            return expiresAt.isBefore(filterDate);
        };
    }

    public static AssignmentFilter expiringAfter(String date) {
        Objects.requireNonNull(date, "Дата не может быть null");
        LocalDateTime filterDate = LocalDateTime.parse(date, DATE_FORMATTER);
        return assignment -> {
            if (!assignment.assignmentType().equals("TEMPORARY")) {
                return false;
            }
            TemporaryAssignment tempAssignment = (TemporaryAssignment) assignment;
            LocalDateTime expiresAt = LocalDateTime.parse(tempAssignment.getExpiresAt(), DATE_FORMATTER);
            return expiresAt.isAfter(filterDate);
        };
    }

    public static AssignmentFilter expiringBetween(String startDate, String endDate) {
        Objects.requireNonNull(startDate, "Начальная дата не может быть null");
        Objects.requireNonNull(endDate, "Конечная дата не может быть null");
        LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
        return assignment -> {
            if (!assignment.assignmentType().equals("TEMPORARY")) {
                return false;
            }
            TemporaryAssignment tempAssignment = (TemporaryAssignment) assignment;
            LocalDateTime expiresAt = LocalDateTime.parse(tempAssignment.getExpiresAt(), DATE_FORMATTER);
            return !expiresAt.isBefore(start) && !expiresAt.isAfter(end);
        };
    }

    public static AssignmentFilter withReason(String reasonSubstring) {
        Objects.requireNonNull(reasonSubstring, "Подстрока причины не может быть null");
        String lowerSubstring = reasonSubstring.toLowerCase();
        return assignment -> {
            String reason = assignment.metadata().reason();
            return reason != null && reason.toLowerCase().contains(lowerSubstring);
        };
    }

    public static AssignmentFilter withReason() {
        return assignment -> assignment.metadata().reason() != null;
    }

    public static AssignmentFilter withoutReason() {
        return assignment -> assignment.metadata().reason() == null;
    }

    public static AssignmentFilter autoRenewable() {
        return assignment -> {
            if (!assignment.assignmentType().equals("TEMPORARY")) {
                return false;
            }
            TemporaryAssignment tempAssignment = (TemporaryAssignment) assignment;
            return tempAssignment.isAutoRenew();
        };
    }

    public static AssignmentFilter revoked() {
        return assignment -> {
            if (!assignment.assignmentType().equals("PERMANENT")) {
                return false;
            }
            PermanentAssignment permAssignment = (PermanentAssignment) assignment;
            return permAssignment.isRevoked();
        };
    }

    public static AssignmentFilter notRevoked() {
        return assignment -> {
            if (!assignment.assignmentType().equals("PERMANENT")) {
                return true;
            }
            PermanentAssignment permAssignment = (PermanentAssignment) assignment;
            return !permAssignment.isRevoked();
        };
    }
}