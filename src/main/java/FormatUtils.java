import java.util.ArrayList;
import java.util.List;

public class FormatUtils {

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("Заголовки не могут быть пустыми");
        }
        int colCount = headers.length;
        for (String[] row : rows) {
            if (row == null || row.length != colCount) {
                throw new IllegalArgumentException(
                    "Каждая строка должна содержать ровно " + colCount + " столбцов"
                );
            }
        }

        int[] colWidths = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            int max = headers[i] != null ? headers[i].length() : 0;
            for (String[] row : rows) {
                if (row[i] != null && row[i].length() > max) {
                    max = row[i].length();
                }
            }
            colWidths[i] = max;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(createHorizontalBorder(colWidths)).append("\n");

        sb.append("|");
        for (int i = 0; i < colCount; i++) {
            sb.append(" ").append(padRight(headers[i] != null ? headers[i] : "", colWidths[i])).append(" |");
        }
        sb.append("\n");

        sb.append(createHorizontalBorder(colWidths)).append("\n");

        for (String[] row : rows) {
            sb.append("|");
            for (int i = 0; i < colCount; i++) {
                sb.append(" ").append(padRight(row[i] != null ? row[i] : "", colWidths[i])).append(" |");
            }
            sb.append("\n");
        }

        sb.append(createHorizontalBorder(colWidths));

        return sb.toString();
    }

    public static String formatBox(String text) {
        if (text == null) text = "";
        String[] lines = text.split("\n");
        int maxLen = 0;
        for (String line : lines) {
            if (line.length() > maxLen) {
                maxLen = line.length();
            }
        }
        int boxWidth = maxLen + 4;
        StringBuilder sb = new StringBuilder();
        sb.append("+").append(repeatChar('-', boxWidth - 2)).append("+\n");
        for (String line : lines) {
            sb.append("| ").append(padRight(line, maxLen)).append(" |\n");
        }
        sb.append("+").append(repeatChar('-', boxWidth - 2)).append("+");
        return sb.toString();
    }

    public static String formatHeader(String text) {
        if (text == null) text = "";
        StringBuilder sb = new StringBuilder();
        sb.append(text).append("\n");
        sb.append(repeatChar('=', text.length()));
        return sb.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (maxLength <= 3) {
            return repeatChar('.', maxLength);
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() > length) {
            return text.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() > length) {
            return text.substring(0, length);
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - text.length()) {
            sb.append(' ');
        }
        sb.append(text);
        return sb.toString();
    }

    private static String createHorizontalBorder(int[] colWidths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : colWidths) {
            sb.append(repeatChar('-', w + 2)).append("+");
        }
        return sb.toString();
    }

    private static String repeatChar(char ch, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
