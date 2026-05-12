public class Watch {
    private long startTime;
    private boolean running = false;
    private long totalTime = 0;

    public void start() {
        startTime = System.nanoTime();
        running = true;
    }

    public void stop() {
        totalTime += System.nanoTime() - startTime;
        running = false;
    }

    public void reset() {
        totalTime = 0;
        running = false;
    }

    public long getElapsedTimeNanos() {
        return running ? System.nanoTime() - startTime : totalTime;
    }

    public long getElapsedTimeMillis() {
        return getElapsedTimeNanos() / 1_000_000;
    }

    public long getElapsedTimeSeconds() {
        return getElapsedTimeNanos() / 1_000_000_000;
    }

    @Override
    public String toString() {
        return Main.time(getElapsedTimeNanos());
    }
}
