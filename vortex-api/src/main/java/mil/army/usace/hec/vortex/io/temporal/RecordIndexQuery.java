package mil.army.usace.hec.vortex.io.temporal;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexTimeRecord;

import java.time.ZonedDateTime;
import java.util.List;

public interface RecordIndexQuery {
    List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime);
    ZonedDateTime getEarliestStartTime();
    ZonedDateTime getLatestEndTime();

    static RecordIndexQuery of(VortexDataType dataType, List<VortexTimeRecord> indexedRecords) {
        return switch (dataType) {
            case AVERAGE, ACCUMULATION -> new PeriodRecordIndexQuery(indexedRecords);
            case INSTANTANEOUS -> new InstantRecordIndexQuery(indexedRecords);
            default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
        };
    }
}
