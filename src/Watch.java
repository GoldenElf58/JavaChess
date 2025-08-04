public class Watch {
    private long startTime;
    private long endTime;
    private boolean running = false;

    public void start() {
        startTime = System.nanoTime();
        running = true;
    }

    public void stop() {
        endTime = System.nanoTime();
        running = false;
    }

    public long getElapsedTimeNanos() {
        return running ? System.nanoTime() - startTime : endTime - startTime;
    }

    public long getElapsedTimeMicros() {
        return getElapsedTimeNanos() / 1_000;
    }

    public long getElapsedTimeMillis() {
        return getElapsedTimeNanos() / 1_000_000;
    }

    public long getElapsedTimeSeconds() {
        return getElapsedTimeNanos() / 1_000_000_000;
    }
}
