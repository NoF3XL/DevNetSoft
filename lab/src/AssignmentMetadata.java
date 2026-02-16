import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter SIMPLE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AssignmentMetadata {
        if (assignedBy == null || assignedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("assignedBy не может быть null или пустым");
        }

        if (assignedAt == null || assignedAt.trim().isEmpty()) {
            throw new IllegalArgumentException("assignedAt не может быть null или пустым");
        }

        assignedBy = assignedBy.trim();
        assignedAt = assignedAt.trim();

        if (reason != null) {
            reason = reason.trim();
            if (reason.isEmpty()) {
                reason = null;
            }
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String now = LocalDateTime.now().format(ISO_FORMATTER);
        return new AssignmentMetadata(assignedBy, now, reason);
    }

    public static AssignmentMetadata nowSimple(String assignedBy, String reason) {
        String now = LocalDateTime.now().format(SIMPLE_FORMATTER);
        return new AssignmentMetadata(assignedBy, now, reason);
    }

    public static AssignmentMetadata now(String assignedBy) {
        return now(assignedBy, null);
    }

    public LocalDateTime getAssignedAtAsDateTime() {
        try {
            // Пробуем распарсить ISO формат
            return LocalDateTime.parse(assignedAt, ISO_FORMATTER);
        } catch (Exception e) {
            try {
                // Пробуем распарсить простой формат
                return LocalDateTime.parse(assignedAt, SIMPLE_FORMATTER);
            } catch (Exception ex) {
                throw new IllegalStateException("Не удалось распарсить assignedAt: " + assignedAt);
            }
        }
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    public boolean hasReason() {
        return reason != null && !reason.isEmpty();
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Assigned by: %s", assignedBy));
        sb.append(String.format("\nAssigned at: %s", assignedAt));

        getReason().ifPresent(r -> sb.append(String.format("\nReason: %s", r)));

        return sb.toString();
    }

    public String formatShort() {
        if (hasReason()) {
            return String.format("%s @ %s (%s)", assignedBy, assignedAt, reason);
        } else {
            return String.format("%s @ %s", assignedBy, assignedAt);
        }
    }
}