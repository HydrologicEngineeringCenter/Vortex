package mil.army.usace.hec.vortex;

import hec.heclib.dss.DSSPathname;
import hec.heclib.util.HecTime;
import mil.army.usace.hec.vortex.io.temporal.Interval;
import mil.army.usace.hec.vortex.util.TimeConverter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

public record VortexTimeRecord(ZonedDateTime startTime, ZonedDateTime endTime) implements Interval {
    private static final Logger logger = Logger.getLogger(VortexTimeRecord.class.getName());
    public static final VortexTimeRecord UNDEFINED = VortexTimeRecord.of(null, null);

    public static VortexTimeRecord of(ZonedDateTime startTime, ZonedDateTime endTime) {
        return new VortexTimeRecord(startTime, endTime);
    }

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
        return VortexTimeRecord.of(start, end);
    }

    public static VortexTimeRecord of(VortexData vortexData) {
        ZonedDateTime start = vortexData.startTime();
        ZonedDateTime end = vortexData.endTime();
        return VortexTimeRecord.of(start, end);
    }

    public static boolean isUndefined(VortexTimeRecord vortexTimeRecord) {
        return vortexTimeRecord == null || vortexTimeRecord.equals(UNDEFINED);
    }

    public Duration getRecordDuration() {
        return this.startTime != null && this.endTime != null ? Duration.between(this.startTime, this.endTime) : Duration.ZERO;
    }

    public double getPercentOverlapped(VortexTimeRecord other) {
        long overlapDuration = getOverlapDuration(other).toSeconds();
        long recordDuration = getRecordDuration().toSeconds();
        return (double) overlapDuration / recordDuration;
    }

    public Duration getOverlapDuration(VortexTimeRecord other) {
        long recordStart = startTime.toEpochSecond();
        long recordEnd = endTime.toEpochSecond();

        long overlapStart = Math.max(other.startTime().toEpochSecond(), recordStart);
        long overlapEnd = Math.min(other.endTime().toEpochSecond(), recordEnd);
        long overlapDuration =  overlapEnd > overlapStart ? overlapEnd - overlapStart : 0;

        return Duration.ofSeconds(overlapDuration);
    }

    public boolean isInstantaneous() {
        return startTime.isEqual(endTime);
    }

    /* Interval */
    @Override
    public long start() {
        return startTime.toEpochSecond();
    }

    @Override
    public long end() {
        return endTime.toEpochSecond();
    }
}