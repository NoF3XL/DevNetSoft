import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {

    protected final String assignmentId;
    protected final User user;
    protected final Role role;
    protected final AssignmentMetadata metadata;

    private static final DateTimeFormatter SUMMARY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        this.assignmentId = generateAssignmentId();
        this.user = validateUser(user);
        this.role = validateRole(role);
        this.metadata = validateMetadata(metadata);
    }

    public AbstractRoleAssignment(String assignmentId, User user, Role role, AssignmentMetadata metadata) {
        this.assignmentId = validateAssignmentId(assignmentId);
        this.user = validateUser(user);
        this.role = validateRole(role);
        this.metadata = validateMetadata(metadata);
    }

    private String validateAssignmentId(String assignmentId) {
        if (assignmentId == null || assignmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID не может быть пустым");
        }
        return assignmentId;
    }

    private User validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User не может быть null");
        }
        return user;
    }

    private Role validateRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role не может быть null");
        }
        return role;
    }

    private AssignmentMetadata validateMetadata(AssignmentMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata не может быть null");
        }
        return metadata;
    }

    protected String generateAssignmentId() {
        return "assign_" + UUID.randomUUID().toString();
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    protected String getFormattedAssignedAt() {
        try {
            LocalDateTime dateTime = metadata.getAssignedAtAsDateTime();
            return dateTime.format(SUMMARY_DATE_FORMATTER);
        } catch (Exception e) {
            return metadata.assignedAt();
        }
    }

    protected String getReasonString() {
        return metadata.getReason()
                .map(reason -> "Reason: " + reason)
                .orElse("");
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] %s assigned to %s by %s at %s",
                assignmentType(),
                role().getName(),
                user().username(),
                metadata().assignedBy(),
                getFormattedAssignedAt()));

        String reason = getReasonString();
        if (!reason.isEmpty()) {
            sb.append("\n").append(reason);
        }
        sb.append("\nStatus: ").append(isActive() ? "ACTIVE" : "INACTIVE");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', user=%s, role=%s, type=%s}",
                getClass().getSimpleName(),
                assignmentId,
                user.username(),
                role().getName(),
                assignmentType());
    }
}