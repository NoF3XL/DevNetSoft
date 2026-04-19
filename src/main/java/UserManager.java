import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {

    private final Map<String, User> usersByUsername;

    public UserManager() {
        this.usersByUsername = new HashMap<>();
    }

    @Override
    public void add(User user) {
        Objects.requireNonNull(user, "Пользователь не может быть null");

        String username = user.username();
        if (usersByUsername.containsKey(username)) {
            throw new IllegalArgumentException("Пользователь с именем пользователя '" + username + "' уже существует");
        }

        usersByUsername.put(username, user);
    }

    @Override
    public boolean remove(User user) {
        Objects.requireNonNull(user, "Пользователь не может быть null");
        return usersByUsername.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        Objects.requireNonNull(id, "ID не может быть null");
        return Optional.ofNullable(usersByUsername.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(usersByUsername.values());
    }

    @Override
    public int count() {
        return usersByUsername.size();
    }

    @Override
    public void clear() {
        usersByUsername.clear();
    }

    public Optional<User> findByUsername(String username) {
        Objects.requireNonNull(username, "Username не может быть null");
        return Optional.ofNullable(usersByUsername.get(username));
    }

    public Optional<User> findByEmail(String email) {
        Objects.requireNonNull(email, "Email не может быть null");
        return usersByUsername.values().stream()
                .filter(user -> user.email().equals(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        return usersByUsername.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        return usersByUsername.values().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Objects.requireNonNull(filter, "Filter не может быть null");
        Objects.requireNonNull(sorter, "Sorter не может быть null");

        return usersByUsername.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        Objects.requireNonNull(username, "Username не может быть null");
        return usersByUsername.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        Objects.requireNonNull(username, "Username не может быть null");
        Objects.requireNonNull(newFullName, "Full name не может быть null");
        Objects.requireNonNull(newEmail, "Email не может быть null");
        User existingUser = usersByUsername.get(username);
        if (existingUser == null) {
            throw new IllegalArgumentException("Пользователь с именем пользователя '" + username + "' не найдено");
        }
        User updatedUser = User.create(username, newFullName, newEmail);
        usersByUsername.put(username, updatedUser);
    }

    public boolean removeByUsername(String username) {
        Objects.requireNonNull(username, "Username не может быть null");
        return usersByUsername.remove(username) != null;
    }

    public List<User> findAllSorted(Comparator<User> sorter) {
        Objects.requireNonNull(sorter, "Sorter не может быть null");
        return usersByUsername.values().stream()
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public Set<String> getAllUsernames() {
        return Set.copyOf(usersByUsername.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return Objects.equals(usersByUsername, that.usersByUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usersByUsername);
    }

    @Override
    public String toString() {
        return String.format("UserManager{users=%d}", count());
    }
}