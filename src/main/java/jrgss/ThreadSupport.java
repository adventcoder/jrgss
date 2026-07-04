package jrgss;

public interface ThreadSupport {
    @FunctionalInterface
    public static interface InterruptibleRunnable {
        public void run() throws InterruptedException;
    }

    public static void runUninterruptibly(InterruptibleRunnable runnable) {
        boolean interrupted = false;
        while (true) {
            try {
                runnable.run();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    public static void sleep(long timeNanos) throws InterruptedException {
        Thread.sleep(timeNanos / 1000_000L, (int) (timeNanos % 1000_000L));
    }
}
