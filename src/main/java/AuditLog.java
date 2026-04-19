import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class AuditLog {
    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    private final CopyOnWriteArrayList<AuditEntry> entries;
    private final BlockingQueue<AuditEntry> queue;
    private final Thread consumerThread;
    private volatile boolean running;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLog() {
        this.entries = new CopyOnWriteArrayList<>();
        this.queue = new LinkedBlockingQueue<>();
        this.running = true;
        this.consumerThread = new Thread(this::processQueue, "AuditLog-Consumer");
        this.consumerThread.setDaemon(true);
        this.consumerThread.start();
    }

    private void processQueue() {
        while (running || !queue.isEmpty()) {
            try {
                AuditEntry entry = queue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    entries.add(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
        queue.offer(entry);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.performer().equalsIgnoreCase(performer)) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<AuditEntry> getByAction(String action) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.action().equalsIgnoreCase(action)) {
                result.add(entry);
            }
        }
        return result;
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Лог аудита пуст.");
            return;
        }
        System.out.println("=== ЛОГ АУДИТА ===");
        for (AuditEntry entry : entries) {
            System.out.printf("[%s] %s | Исполнитель: %s | Объект: %s | Детали: %s%n",
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details());
        }
        System.out.println("==================");
    }

    public void saveToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (AuditEntry entry : entries) {
                writer.printf("%s|%s|%s|%s|%s%n",
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details());
            }
            System.out.println("Лог сохранён в файл: " + filename);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении лога: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        consumerThread.interrupt();
        try {
            consumerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
