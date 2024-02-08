package mil.army.usace.hec.vortex.util;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch {
    private Instant startTime;
    private Instant endTime;

    public void start() {
        if (startTime != null)
            throw new IllegalStateException("Timer has already started");

        startTime = Instant.now();
    }

    public void end() {
        if (endTime != null)
            throw new IllegalStateException("Timer has already ended");

        endTime = Instant.now();
    }

    private long getSeconds() {
        if (startTime == null)
            throw new IllegalStateException("Timer has not started");

        if (endTime == null)
            throw new IllegalStateException("Timer has not ended");

        return Duration.between(startTime, endTime).toSeconds();
    }

    @Override
    public String toString() {
        long seconds = getSeconds();
        return String.format("%d:%02d:%02d%n", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }
}
