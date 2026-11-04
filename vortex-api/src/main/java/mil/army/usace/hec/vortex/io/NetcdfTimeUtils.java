package mil.army.usace.hec.vortex.io;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

class NetcdfTimeUtils {
    private static final ZoneId ORIGIN_ZONE_ID = ZoneId.of("UTC");
    static final ZonedDateTime ORIGIN_TIME = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ORIGIN_ZONE_ID);

    static long getNumIntervalsFromBaseTime(ZonedDateTime dateTime, ChronoUnit intervalUnit) {
        return intervalUnit.between(ORIGIN_TIME, dateTime);
    }

    /** The inverse of {@link #getNumIntervalsFromBaseTime}. */
    static ZonedDateTime addToBaseTime(long intervals, ChronoUnit intervalUnit) {
        return ORIGIN_TIME.plus(intervals, intervalUnit);
    }

    /**
     * The unit a CF time variable is encoded with, read from the units attribute written by
     * {@link VortexGridCollection#getTimeUnits()} ("MINUTES since 1900-01-01 00:00:00 +0000").
     *
     * @throws IllegalArgumentException if the attribute does not lead with a {@link ChronoUnit} name
     */
    static ChronoUnit getDeclaredTimeUnit(String units) {
        String attribute = units == null ? "" : units.trim();
        int sinceIndex = attribute.toLowerCase().indexOf(" since ");
        String unitToken = sinceIndex < 0 ? attribute : attribute.substring(0, sinceIndex);
        return ChronoUnit.valueOf(unitToken.trim().toUpperCase());
    }

    static ChronoUnit getDeltaTimeUnit(Duration duration) {
        // http://cfconventions.org/cf-conventions/cf-conventions.html#time-coordinate
        if (duration.toDays() > 0) {
            return ChronoUnit.DAYS;
        } else if (duration.toHours() > 0) {
            return ChronoUnit.HOURS;
        } else {
            return ChronoUnit.MINUTES;
        }
    }
}