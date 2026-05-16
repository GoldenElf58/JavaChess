package utils;

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
        return time(getElapsedTimeNanos());
    }

    public static String time(long time, int precision) {
        String fmt = "%,." + precision + "g";
        if (time < 1_000)
            return String.format("%s ns", String.format(fmt, (double) time));
        if (time < 1_000_000)
            return String.format("%s µs", String.format(fmt, time / 1_000.0));
        if (time < 1_000_000_000)
            return String.format("%s ms", String.format(fmt, time / 1_000_000.0));
        return String.format("%s s", String.format(fmt, time / 1_000_000_000.0));
    }

    public static String time(long time) {
        return time(time, 5);
    }
}
