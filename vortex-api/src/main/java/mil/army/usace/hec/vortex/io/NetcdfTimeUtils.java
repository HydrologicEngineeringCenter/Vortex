package mil.army.usace.hec.vortex.io;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

class NetcdfTimeUtils {
    private static final ZoneId ORIGIN_ZONE_ID = ZoneId.of("UTC");
    static final ZonedDateTime ORIGIN_TIME = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ORIGIN_ZONE_ID);

    static long getNumIntervalsFromBaseTime(ZonedDateTime dateTime, ChronoUnit intervalUnit) {
        return intervalUnit.between(ORIGIN_TIME, dateTime);
    }

    static ChronoUnit getMinimumDeltaTimeUnit(List<VortexDataInterval> timeRecords) {
        ChronoUnit minimum = ChronoUnit.DAYS;
        for (VortexDataInterval timeRecord : timeRecords) {
            ChronoUnit unit = getDeltaTimeUnit(timeRecord.getRecordDuration());
            if (unit.getDuration().toSeconds() < minimum.getDuration().toSeconds()) {
                minimum = unit;
            }
        }
        return minimum;
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