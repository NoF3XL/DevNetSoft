import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;

public class MultiThreadProgress {

    private static final int THREAD_COUNT = 5;      // Количество потоков
    private static final int PROGRESS_LENGTH = 30;  // Длина прогресс-бара
    private static final int MIN_DELAY_MS = 50;     // Минимальная задержка между обновлениями
    private static final int MAX_DELAY_MS = 150;    // Максимальная задержка между обновлениями

    private static final String RESET = "\u001B[0m";

    private static final AtomicBoolean running = new AtomicBoolean(true);

    static class ProgressTracker {
        private final int threadIndex;
        private final long threadId;
        private final int progressLength;
        private volatile int currentProgress = 0;
        private volatile boolean completed = false;
        private long startTime;
        private long endTime;

        public ProgressTracker(int threadIndex, long threadId, int progressLength) {
            this.threadIndex = threadIndex;
            this.threadId = threadId;
            this.progressLength = progressLength;
            this.startTime = System.currentTimeMillis();
        }

        public void updateProgress(int progress) {
            this.currentProgress = Math.min(progress, progressLength);
            if (progress >= progressLength && !completed) {
                completed = true;
                endTime = System.currentTimeMillis();
            }
        }

        public boolean isCompleted() {
            return completed;
        }

        public String getProgressBar() {
            StringBuilder bar = new StringBuilder("[");
            int filled = currentProgress;
            for (int i = 0; i < progressLength; i++) {
                if (i < filled) {
                    bar.append("=");
                } else if (i == filled) {
                    bar.append(">");
                } else {
                    bar.append(" ");
                }
            }
            bar.append("]");

            int percent = (currentProgress * 100) / progressLength;
            bar.append(String.format(" %3d%%", percent));

            return bar.toString();
        }

        public String getInfo() {
            String timeInfo = "";
            if (completed) {
                long duration = endTime - startTime;
                timeInfo = String.format(" - Завершён за %d мс", duration);
            }
            return String.format("Поток #%d (ID: %d)%s %s%s",
                    threadIndex + 1, threadId, RESET, getProgressBar(), timeInfo);
        }
    }

    static class CalculationTask implements Runnable {
        private final ProgressTracker tracker;
        private final Random random = new Random();

        public CalculationTask(ProgressTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public void run() {
            for (int progress = 1; progress <= tracker.progressLength; progress++) {
                if (!running.get()) break;

                tracker.updateProgress(progress);

                try {
                    int delay = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            tracker.updateProgress(tracker.progressLength);
        }
    }

    public static void main(String[] args) {
        System.out.println("Запуск многопоточного расчёта...\n");
        System.out.println("Количество потоков: " + THREAD_COUNT);
        System.out.println("Длина прогресс-бара: " + PROGRESS_LENGTH + " символов");
        System.out.println("Задержка между обновлениями: " + MIN_DELAY_MS + "-" + MAX_DELAY_MS + " мс\n");
        System.out.println("Нажмите Enter для остановки...\n");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        ProgressTracker[] trackers = new ProgressTracker[THREAD_COUNT];
        Future<?>[] futures = new Future<?>[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            ProgressTracker tracker = new ProgressTracker(threadIndex,
                    Thread.currentThread().threadId(), PROGRESS_LENGTH);
            trackers[threadIndex] = tracker;

            futures[threadIndex] = executor.submit(new CalculationTask(tracker));
        }

        Thread inputThread = new Thread(() -> {
            try {
                System.in.read();
                running.set(false);
                System.out.println("\n\nОстановка программы...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();

        boolean allCompleted = false;
        while (!allCompleted && running.get()) {
            clearConsole();

            System.out.println("=== Многопоточный расчёт в процессе ===\n");

            allCompleted = true;
            for (int i = 0; i < THREAD_COUNT; i++) {
                ProgressTracker tracker = trackers[i];
                if (tracker != null) {
                    System.out.println(tracker.getInfo());
                    if (!tracker.isCompleted()) {
                        allCompleted = false;
                    }
                }
            }

            System.out.println("\n=== Нажмите Enter для досрочной остановки ===");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        clearConsole();
        System.out.println("=== РЕЗУЛЬТАТЫ РАСЧЁТА ===\n");
        for (int i = 0; i < THREAD_COUNT; i++) {
            ProgressTracker tracker = trackers[i];
            if (tracker != null) {
                System.out.println(tracker.getInfo());
            }
        }

        System.out.println("\nПрограмма завершена.");
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}