import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidationUtils {
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";
    private static final String EMAIL_REGEX = "^[^\\s]+@[^\\s]+\\.[^\\s]+$";
    private static final String PERMISSION_NAME_REGEX = "^[A-Z_]+$";
    private static final String RESOURCE_NAME_REGEX = "^[a-z0-9_]+$";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return username.matches(USERNAME_REGEX);
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches(EMAIL_REGEX);
    }

    public static boolean isValidDate(String date) {
        if (date == null) {
            return false;
        }
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidDateTime(String dateTime) {
        if (dateTime == null) {
            return false;
        }
        try {
            LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidPermissionName(String permissionName) {
        if (permissionName == null) {
            return false;
        }
        return permissionName.matches(PERMISSION_NAME_REGEX);
    }

    public static boolean isValidResourceName(String resourceName) {
        if (resourceName == null) {
            return false;
        }
        return resourceName.matches(RESOURCE_NAME_REGEX);
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        String singleSpaced = trimmed.replaceAll("\\s+", " ");
        return singleSpaced.toLowerCase();
    }

    public static String normalizePermissionName(String permissionName) {
        if (permissionName == null) {
            return null;
        }
        return permissionName.trim().toUpperCase();
    }

    public static String normalizeResourceName(String resourceName) {
        if (resourceName == null) {
            return null;
        }
        return resourceName.trim().toLowerCase();
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Поле '%s' не может быть пустым", fieldName)
            );
        }
    }
}
