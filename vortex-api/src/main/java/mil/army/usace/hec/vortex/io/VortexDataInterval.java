package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

record VortexDataInterval(ZonedDateTime startTime, ZonedDateTime endTime) implements Interval {
    static final VortexDataInterval UNDEFINED = VortexDataInterval.of(null, null);

    static VortexDataInterval of(ZonedDateTime startTime, ZonedDateTime endTime) {
        return new VortexDataInterval(startTime, endTime);
    }

    static VortexDataInterval of(String dssPathname) {
        String[] parts = getDssParts(dssPathname);

        String dPart = parts[3];
        String ePart = parts[4].isEmpty() ? dPart : parts[4];

        ZonedDateTime startTime = TimeConverter.toZonedDateTime(dPart);
        ZonedDateTime endTime = TimeConverter.toZonedDateTime(ePart);
        return VortexDataInterval.of(startTime, endTime);
    }

    static VortexDataInterval of(VortexData vortexData) {
        ZonedDateTime start = vortexData.startTime();
        ZonedDateTime end = vortexData.endTime();
        return VortexDataInterval.of(start, end);
    }

    static boolean isDefined(VortexDataInterval timeRecord) {
        return timeRecord != null && timeRecord.startTime != null && timeRecord.endTime != null;
    }

    Duration getRecordDuration() {
        return isDefined(this) ? Duration.between(this.startTime, this.endTime) : Duration.ZERO;
    }

    double getPercentOverlapped(VortexDataInterval other) {
        long overlapDuration = getOverlapDuration(other).toSeconds();
        long recordDuration = getRecordDuration().toSeconds();
        return (double) overlapDuration / recordDuration;
    }

    Duration getOverlapDuration(VortexDataInterval other) {
        long recordStart = startTime.toEpochSecond();
        long recordEnd = endTime.toEpochSecond();

        long overlapStart = Math.max(other.startTime().toEpochSecond(), recordStart);
        long overlapEnd = Math.min(other.endTime().toEpochSecond(), recordEnd);
        long overlapDuration = overlapEnd > overlapStart ? overlapEnd - overlapStart : 0;

        return Duration.ofSeconds(overlapDuration);
    }

    boolean isInstantaneous() {
        return startTime.isEqual(endTime);
    }

    /* Interval */
    @Override
    public long startEpochSecond() {
        return startTime.toEpochSecond();
    }

    @Override
    public long endEpochSecond() {
        return endTime.toEpochSecond();
    }

    /* Override equals and hashCode to be the same for time with the same offset but different IDs */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VortexDataInterval that)) {
            return false;
        }

        return startTime.toEpochSecond() == that.startTime.toEpochSecond() &&
                endTime.toEpochSecond() == that.endTime.toEpochSecond() &&
                startTime.getOffset().equals(that.startTime.getOffset()) &&
                endTime.getOffset().equals(that.endTime.getOffset());
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(startTime.toEpochSecond());
        result = 31 * result + startTime.getOffset().hashCode();
        result = 31 * result + Long.hashCode(endTime.toEpochSecond());
        result = 31 * result + endTime.getOffset().hashCode();
        return result;
    }

    /* Helpers */
    private static String[] getDssParts(String pathname) {
        String trimmedPathname = Optional.ofNullable(pathname)
                .map(String::trim)
                .filter(p -> p.startsWith("/") && p.endsWith("/"))
                .map(p -> p.substring(1, p.length() - 1))
                .orElse("");

        if (trimmedPathname.isEmpty()) {
            return new String[0];
        }

        String[] parts = Arrays.stream(trimmedPathname.split("/", -1))
                .map(String::trim)
                .toArray(String[]::new);

        // There should be exactly 6 parts
        return parts.length == 6 ? parts : new String[0];
    }
}
