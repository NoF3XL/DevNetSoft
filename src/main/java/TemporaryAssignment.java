import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private String expiresAt;
    private boolean autoRenew;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = validateExpiresAt(expiresAt);
        this.autoRenew = autoRenew;
    }
    public TemporaryAssignment(String assignmentId, User user, Role role,
                               AssignmentMetadata metadata, String expiresAt,
                               boolean autoRenew) {
        super(assignmentId, user, role, metadata);
        this.expiresAt = validateExpiresAt(expiresAt);
        this.autoRenew = autoRenew;
    }
    private String validateExpiresAt(String expiresAt) {
        ValidationUtils.requireNonEmpty(expiresAt, "expiresAt");
        String trimmed = expiresAt.trim();
        if (!isValidDateFormat(trimmed)) {
            throw new IllegalArgumentException(
                    "expiresAt должен быть в формате yyyy-MM-dd или yyyy-MM-dd HH:mm"
            );
        }
        return trimmed;
    }

    private boolean isValidDateFormat(String dateStr) {
        if (dateStr.length() == 10) { // yyyy-MM-dd
            return ValidationUtils.isValidDate(dateStr);
        } else if (dateStr.length() == 16) { // yyyy-MM-dd HH:mm
            return ValidationUtils.isValidDateTime(dateStr);
        } else {
            return false;
        }
    }

    private LocalDateTime parseExpiresAt() {
        try {
            if (expiresAt.length() == 10) { // yyyy-MM-dd
                return LocalDateTime.parse(expiresAt + "T00:00:00");
            } else { // yyyy-MM-dd HH:mm
                return LocalDateTime.parse(expiresAt.replace(" ", "T") + ":00");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось распарсить expiresAt: " + expiresAt);
        }
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    public boolean isActive(LocalDateTime dateTime) {
        return !isExpired(dateTime);
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        return isExpired(LocalDateTime.now());
    }

    public boolean isExpired(LocalDateTime dateTime) {
        LocalDateTime expires = parseExpiresAt();
        return dateTime.isAfter(expires);
    }

    public void extend(String newExpirationDate) {
        String validatedNewDate = validateExpiresAt(newExpirationDate);
        LocalDateTime newDate = parseDate(validatedNewDate);
        LocalDateTime currentDate = parseExpiresAt();
        if (newDate.isBefore(currentDate)) {
            throw new IllegalArgumentException(
                    "Новая дата истечения не может быть раньше текущей"
            );
        }
        this.expiresAt = validatedNewDate;
    }

    public void extendDays(long days) {
        LocalDateTime current = parseExpiresAt();
        LocalDateTime newDate = current.plusDays(days);
        this.expiresAt = formatDateTime(newDate);
    }

    public void extendMonths(long months) {
        LocalDateTime current = parseExpiresAt();
        LocalDateTime newDate = current.plusMonths(months);
        this.expiresAt = formatDateTime(newDate);
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public String getTimeRemaining() {
        return getTimeRemaining(LocalDateTime.now());
    }

    public String getTimeRemaining(LocalDateTime dateTime) {
        if (isExpired(dateTime)) {
            return "Истекло";
        }
        LocalDateTime expires = parseExpiresAt();
        long days = ChronoUnit.DAYS.between(dateTime, expires);
        long hours = ChronoUnit.HOURS.between(dateTime, expires) % 24;
        long minutes = ChronoUnit.MINUTES.between(dateTime, expires) % 60;
        if (days > 0) {
            return String.format("%d дн %d ч %d мин", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d ч %d мин", hours, minutes);
        } else {
            return String.format("%d мин", minutes);
        }
    }

    public double getUsagePercentage() {
        return getUsagePercentage(LocalDateTime.now());
    }

    public double getUsagePercentage(LocalDateTime dateTime) {
        LocalDateTime created = metadata().getAssignedAtAsDateTime();
        LocalDateTime expires = parseExpiresAt();
        long totalDuration = ChronoUnit.MINUTES.between(created, expires);
        long usedDuration = ChronoUnit.MINUTES.between(created, dateTime);
        if (usedDuration < 0) return 0;
        if (usedDuration > totalDuration) return 100;
        return (usedDuration * 100.0) / totalDuration;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr.length() == 10) {
            return LocalDateTime.parse(dateStr + "T00:00:00");
        } else {
            return LocalDateTime.parse(dateStr.replace(" ", "T") + ":00");
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (expiresAt.length() == 10) {
            return dateTime.format(DATE_FORMATTER);
        } else {
            return dateTime.format(DATETIME_FORMATTER);
        }
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getExpiresAtAsDateTime() {
        return parseExpiresAt();
    }

    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder(super.summary());
        String status;
        if (isExpired()) {
            status = "EXPIRED";
        } else if (isActive()) {
            status = "ACTIVE";
        } else {
            status = "INACTIVE";
        }
        String baseSummary = String.format("[%s] %s assigned to %s by %s at %s",
                assignmentType(),
                role().getName(),
                user().username(),
                metadata().assignedBy(),
                getFormattedAssignedAt());

        sb = new StringBuilder(baseSummary);
        String reason = getReasonString();
        if (!reason.isEmpty()) {
            sb.append("\n").append(reason);
        }
        sb.append(String.format("\nExpires: %s", expiresAt));
        if (!isExpired()) {
            sb.append(String.format(" (remaining: %s)", getTimeRemaining()));
        }
        sb.append(String.format("\nAuto-renew: %s", autoRenew ? "ON" : "OFF"));
        sb.append(String.format("\nStatus: %s", status));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TemporaryAssignment that = (TemporaryAssignment) o;
        return autoRenew == that.autoRenew &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), expiresAt, autoRenew);
    }

    @Override
    public String toString() {
        return String.format("TemporaryAssignment{id='%s', user=%s, role=%s, expires=%s, autoRenew=%s}",
                assignmentId(),
                user().username(),
                role().getName(),
                expiresAt,
                autoRenew);
    }
}