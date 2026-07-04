package jrgss;

public interface ThreadSupport {
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

    public static void sleep(long time) throws InterruptedException {
        Thread.sleep(time / 1000_000L, (int) (time % 1000_000L));
    }
}
