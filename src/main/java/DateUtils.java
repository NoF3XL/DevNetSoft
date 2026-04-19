import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        LocalDate d1 = LocalDate.parse(date1);
        LocalDate d2 = LocalDate.parse(date2);
        return d1.isBefore(d2);
    }

    public static boolean isAfter(String date1, String date2) {
        LocalDate d1 = LocalDate.parse(date1);
        LocalDate d2 = LocalDate.parse(date2);
        return d1.isAfter(d2);
    }

    public static String addDays(String date, int days) {
        LocalDate d = LocalDate.parse(date);
        LocalDate result = d.plusDays(days);
        return result.format(DATE_FORMATTER);
    }

    public static String formatRelativeTime(String date) {
        LocalDate target = LocalDate.parse(date);
        LocalDate today = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(today, target);

        if (diff == 0) {
            return "today";
        } else if (diff > 0) {
            if (diff <= 30) {
                return "in " + diff + " days";
            } else {
                return target.format(DATE_FORMATTER);
            }
        } else {
            long absDiff = Math.abs(diff);
            if (absDiff <= 30) {
                return absDiff + " days ago";
            } else {
                return target.format(DATE_FORMATTER);
            }
        }
    }
}
