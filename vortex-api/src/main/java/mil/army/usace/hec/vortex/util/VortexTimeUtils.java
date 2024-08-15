package mil.army.usace.hec.vortex.util;

import mil.army.usace.hec.vortex.temporal.VortexTimeRecord;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class VortexTimeUtils {
    public static final ZoneId BASE_ZONE_ID = ZoneId.of("UTC");
    public static final ZonedDateTime BASE_TIME = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, BASE_ZONE_ID);
    private static final long BASE_TIME_EPOCH_SECONDS = BASE_TIME.toEpochSecond();

    public static long getNumDurationsFromBaseTime(ZonedDateTime dateTime, VortexTimeRecord timeRecord) {
        long zDateTimeEpochSeconds = dateTime.withZoneSameInstant(BASE_ZONE_ID).toEpochSecond();
        long durationBetweenSeconds = zDateTimeEpochSeconds - BASE_TIME_EPOCH_SECONDS;
        long divisorSeconds = getBaseDuration(timeRecord).toSeconds();
        return durationBetweenSeconds / divisorSeconds;
    }

    private static Duration getBaseDuration(VortexTimeRecord timeRecord) {
        Duration interval = timeRecord.getRecordDuration();
        return interval.isZero() ? Duration.ofMinutes(1) : interval;
    }
}