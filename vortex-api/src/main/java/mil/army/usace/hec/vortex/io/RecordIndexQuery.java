package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexDataType;

import java.time.ZonedDateTime;
import java.util.List;

interface RecordIndexQuery {
    List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime);

    /**
     * Returns the index or indices of the record(s) whose timestamp or interval lies closest to a given instant.
     * <p>
     * For point‐in‐time (instantaneous) data, this will return:
     * <ul>
     *   <li>the index of the data point at or immediately before {@code queryTime}, and/or</li>
     *   <li>the index of the data point at or immediately after {@code queryTime}.</li>
     * </ul>
     * If the query exactly matches one timestamp, only that single index is returned.
     * <p>
     * For period‐based data, this returns the index or indices of the interval(s) whose span is nearest
     * to the supplied instant (for example, the interval that contains or borders the query time).
     *
     * @param queryTime
     *            the moment in time to search for; must not be {@code null}
     * @return a list of zero, one, or two record indices that are closest in time to {@code queryTime};
     *         an empty list if no suitable record is found
     * @throws NullPointerException
     *             if {@code queryTime} is {@code null}
     */
    List<Integer> queryNearest(ZonedDateTime queryTime);

    ZonedDateTime getEarliestStartTime();

    ZonedDateTime getLatestEndTime();

    static RecordIndexQuery of(VortexDataType dataType, List<VortexDataInterval> indexedRecords) {
        return switch (dataType) {
            case AVERAGE, ACCUMULATION -> PeriodRecordIndexQuery.from(indexedRecords);
            case INSTANTANEOUS -> InstantaneousRecordIndexQuery.from(indexedRecords);
            default -> new NoRecordIndexQuery();
        };
    }
}
