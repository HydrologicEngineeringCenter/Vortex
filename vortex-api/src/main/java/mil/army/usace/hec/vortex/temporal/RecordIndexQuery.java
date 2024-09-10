package mil.army.usace.hec.vortex.temporal;

import mil.army.usace.hec.vortex.VortexDataType;

import java.time.ZonedDateTime;
import java.util.List;

interface RecordIndexQuery {
    List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime);
    ZonedDateTime getEarliestStartTime();
    ZonedDateTime getLatestEndTime();

    static RecordIndexQuery of(VortexDataType dataType, List<VortexTimeRecord> indexedRecords) {
        return switch (dataType) {
            case AVERAGE, ACCUMULATION -> new PeriodRecordIndexQuery(indexedRecords);
            case INSTANTANEOUS -> new InstantRecordIndexQuery(indexedRecords);
            default -> new NoRecordIndexQuery();
        };
    }
}
