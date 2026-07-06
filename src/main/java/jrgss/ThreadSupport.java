package jrgss;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThreadSupport {
    @FunctionalInterface
    public static interface Runnable {
        public void run() throws InterruptedException;
    }

    public static void runUninterruptibly(Runnable runnable) {
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
