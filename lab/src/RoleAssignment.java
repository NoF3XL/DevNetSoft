import java.time.LocalDateTime;
import java.util.UUID;

public interface RoleAssignment {
    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();

    default String generateAssignmentId() {
        return "assign_" + UUID.randomUUID().toString();
    }
    default String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Assignment ID: %s\n", assignmentId()));
        sb.append(String.format("Type: %s\n", assignmentType()));
        sb.append(String.format("Status: %s\n", isActive() ? "ACTIVE" : "INACTIVE"));
        sb.append(String.format("User: %s\n", user().format()));
        sb.append(String.format("Role: %s [ID: %s]\n", role().getName(), role().getId()));
        sb.append("Assignment metadata:\n");
        sb.append(metadata().format().replaceAll("(?m)^", "  "));
        return sb.toString();
    }

    default boolean isTemporary() {
        return "TEMPORARY".equals(assignmentType());
    }

    default boolean isPermanent() {
        return "PERMANENT".equals(assignmentType());
    }
}