package mil.army.usace.hec.vortex;

import hec.heclib.dss.DSSPathname;
import hec.heclib.util.HecTime;
import mil.army.usace.hec.vortex.util.TimeConverter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

public record VortexTimeRecord(ZonedDateTime startTime, ZonedDateTime endTime) {
    private static final Logger logger = Logger.getLogger(VortexTimeRecord.class.getName());
    public static final VortexTimeRecord UNDEFINED = new VortexTimeRecord(null, null);

    public static VortexTimeRecord of(DSSPathname dssPathname) {
        HecTime hecStart = new HecTime(dssPathname.dPart());
        HecTime hecEnd = new HecTime(dssPathname.ePart());

        if (!hecStart.isDefined()) {
            logger.warning("No start time found");
            return null;
        }

        if (!hecEnd.isDefined())
            hecEnd = hecStart;

        ZonedDateTime start = TimeConverter.toZonedDateTime(hecStart);
        ZonedDateTime end = TimeConverter.toZonedDateTime(hecEnd);
        return new VortexTimeRecord(start, end);
    }

    public static VortexTimeRecord of(VortexData vortexData) {
        ZonedDateTime start = vortexData.startTime();
        ZonedDateTime end = vortexData.endTime();
        return new VortexTimeRecord(start, end);
    }

    public static boolean isUndefined(VortexTimeRecord vortexTimeRecord) {
        return vortexTimeRecord == null || vortexTimeRecord.equals(UNDEFINED);
    }

    public Duration getRecordDuration() {
        return this.startTime != null && this.endTime != null ? Duration.between(this.startTime, this.endTime) : Duration.ZERO;
    }

    public double getPercentOverlapped(long start, long end) {
        long overlapDuration = getOverlapDuration(start, end).toSeconds();
        long recordDuration = getRecordDuration().toSeconds();
        return (double) overlapDuration / recordDuration;
    }

    public Duration getOverlapDuration(long start, long end) {
        long recordStart = startTime.toEpochSecond();
        long recordEnd = endTime.toEpochSecond();

        long overlapStart = Math.max(start, recordStart);
        long overlapEnd = Math.min(end, recordEnd);
        long overlapDuration =  overlapEnd > overlapStart ? overlapEnd - overlapStart : 0;

        return Duration.ofSeconds(overlapDuration);
    }

    public boolean hasOverlap(long start, long end) {
        return getOverlapDuration(start, end).toSeconds() > 0;
    }
}
