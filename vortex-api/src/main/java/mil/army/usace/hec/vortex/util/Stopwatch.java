package mil.army.usace.hec.vortex.util;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

public class Stopwatch {
    private static final Logger LOGGER = Logger.getLogger(Stopwatch.class.getName());

    private Instant startTime;
    private Instant endTime;

    public void start() {
        if (startTime != null)
            LOGGER.warning(() -> "Timer has already started");

        startTime = Instant.now();
    }

    public void end() {
        if (endTime != null)
            LOGGER.warning(() -> "Timer has already ended");

        endTime = Instant.now();
    }

    private long getSeconds() {
        if (startTime == null) {
            LOGGER.warning(() -> "Timer has not started");
            return 0;
        }

        if (endTime == null) {
            LOGGER.warning(() -> "Timer has not ended");
            return 0;
        }

        return Duration.between(startTime, endTime).toSeconds();
    }

    @Override
    public String toString() {
        long seconds = getSeconds();
        return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }
}
