import java.util.regex.*;

public record User(String username, String fullName, String email) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s]+@[^\\s]+\\.[^\\s]+$");

    public User {
        validateUsername(username);
        validateFullName(fullName);
        validateEmail(email);
    }

    public static User create(String username, String fullName, String email) {
        return new User(username, fullName, email);
    }

    public String format() {
        return String.format("%s %s %s", username, fullName, email);
    }

    private static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException();
        }
    }

    private static void validateFullName(String fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException();
        }
    }

    private static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Неправильный email");
        }
    }

    public static void main(String[] args) {
        try {
            User user1 = User.create("john_doe", "John Doe", "john.doe@email.com");
            System.out.format("Успешно создан:  " + user1.format() + "\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании user");
        }
        try {
            User user2 = User.create(null, "Test User", "test@email.com");
            System.out.println("Успешно создан: " + user2.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с null username");
        }
        try {
            User user3 = User.create("jo", "Jane Smith", "jane@email.com");
            System.out.println("Успешно создан: " + user3.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с коротким username");
        }
        try {
            User user4 = User.create("test_user", "", "test@email.com");
            System.out.println("Успешно создан: " + user4.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с пустым fullName");
        }
        try {
            User user5 = User.create("jane_doe", "Jane Doe", "jane.email.com");
            System.out.println("Успешно создан: " + user5.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка при создании с email без @");
        }
        User userA = User.create("alice", "Alice Wonderland", "alice@email.com");
        User userB = User.create("alice", "Alice Wonderland", "alice@email.com");
        User userC = User.create("bob", "Bob Builder", "bob@email.com");
        System.out.println("userA.equals(userB): " + userA.equals(userB));
        System.out.println("userA.equals(userC): " + userA.equals(userC));
        System.out.println("userA.hashCode(): " + userA.hashCode());
        System.out.println("userB.hashCode(): " + userB.hashCode());
        System.out.println("userC.hashCode(): " + userC.hashCode());
    }
}