package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexDataType;

import java.time.ZonedDateTime;
import java.util.List;

interface RecordIndexQuery {
    List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime);

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
