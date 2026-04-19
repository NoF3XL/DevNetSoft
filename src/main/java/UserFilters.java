import java.util.Objects;

public final class UserFilters {

    private UserFilters() {
    }

    public static UserFilter byUsername(String username) {
        Objects.requireNonNull(username, "Username не может быть null");
        return user -> user.username().equals(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        Objects.requireNonNull(substring, "Подстрока не может быть null");
        String lowerSubstring = substring.toLowerCase();
        return user -> user.username().toLowerCase().contains(lowerSubstring);
    }

    public static UserFilter byEmail(String email) {
        Objects.requireNonNull(email, "Email не может быть null");
        return user -> user.email().equals(email);
    }

    public static UserFilter byEmailDomain(String domain) {
        Objects.requireNonNull(domain, "Домен не может быть null");
        if (!domain.startsWith("@")) {
            domain = "@" + domain;
        }
        String finalDomain = domain.toLowerCase();
        return user -> user.email().toLowerCase().endsWith(finalDomain);
    }

    public static UserFilter byFullNameContains(String substring) {
        Objects.requireNonNull(substring, "Подстрока не может быть null");
        String lowerSubstring = substring.toLowerCase();
        return user -> user.fullName().toLowerCase().contains(lowerSubstring);
    }
}