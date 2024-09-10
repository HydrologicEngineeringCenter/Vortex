package mil.army.usace.hec.vortex.temporal;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

class NoRecordIndexQuery implements RecordIndexQuery {
    @Override
    public List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime) {
        return Collections.emptyList();
    }

    @Override
    public ZonedDateTime getEarliestStartTime() {
        return null;
    }

    @Override
    public ZonedDateTime getLatestEndTime() {
        return null;
    }
}
