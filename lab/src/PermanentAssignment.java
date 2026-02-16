import java.time.LocalDateTime;
import java.util.Objects;

public class PermanentAssignment extends AbstractRoleAssignment {

    private boolean revoked;
    private LocalDateTime revokedAt;
    private String revokedBy;
    private String revokeReason;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
        this.revoked = false;
        this.revokedAt = null;
        this.revokedBy = null;
        this.revokeReason = null;
    }

    public PermanentAssignment(String assignmentId, User user, Role role,
                               AssignmentMetadata metadata, boolean revoked) {
        super(assignmentId, user, role, metadata);
        this.revoked = revoked;
        this.revokedAt = null;
        this.revokedBy = null;
        this.revokeReason = null;
    }

    public PermanentAssignment(String assignmentId, User user, Role role,
                               AssignmentMetadata metadata, boolean revoked,
                               LocalDateTime revokedAt, String revokedBy, String revokeReason) {
        super(assignmentId, user, role, metadata);
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
        this.revokeReason = revokeReason;
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        revoke(null, null);
    }

    public void revoke(String revokedBy, String revokeReason) {
        if (this.revoked) {
            throw new IllegalStateException("Назначение уже было отозвано");
        }
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = revokedBy;
        this.revokeReason = revokeReason;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public boolean hasRevokeInfo() {
        return revoked && revokedAt != null;
    }

    public String getRevokeInfo() {
        if (!hasRevokeInfo()) {
            return "Назначение не отозвано";
        }
        StringBuilder sb = new StringBuilder("Отозвано: ");
        sb.append(revokedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (revokedBy != null) {
            sb.append(" кем: ").append(revokedBy);
        }
        if (revokeReason != null) {
            sb.append(" причина: ").append(revokeReason);
        }

        return sb.toString();
    }

    public void restore() {
        if (!this.revoked) {
            throw new IllegalStateException("Назначение не было отозвано");
        }
        this.revoked = false;
        this.revokedAt = null;
        this.revokedBy = null;
        this.revokeReason = null;
    }

    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder(super.summary());
        if (revoked) {
            sb.append("\n").append(getRevokeInfo());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PermanentAssignment that = (PermanentAssignment) o;
        return revoked == that.revoked &&
                Objects.equals(revokedAt, that.revokedAt) &&
                Objects.equals(revokedBy, that.revokedBy) &&
                Objects.equals(revokeReason, that.revokeReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), revoked, revokedAt, revokedBy, revokeReason);
    }

    @Override
    public String toString() {
        return String.format("PermanentAssignment{id='%s', user=%s, role=%s, revoked=%s%s}",
                assignmentId(),
                user().username(),
                role().getName(),
                revoked,
                revoked ? ", revokedAt=" + revokedAt : "");
    }
}