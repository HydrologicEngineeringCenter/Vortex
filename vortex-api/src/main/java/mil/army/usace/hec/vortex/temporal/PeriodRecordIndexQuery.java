package mil.army.usace.hec.vortex.temporal;

import java.time.ZonedDateTime;
import java.util.*;

final class PeriodRecordIndexQuery implements RecordIndexQuery {
    private final Map<VortexTimeRecord, Integer> originalIndexMap;
    private final StaticIntervalTree<VortexTimeRecord> intervalTree;

    PeriodRecordIndexQuery(List<VortexTimeRecord> recordList) {
        this.originalIndexMap = initOriginalIndexMap(recordList);
        this.intervalTree = new StaticIntervalTree<>(recordList);
    }

    /* Query */
    @Override
    public List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexTimeRecord> overlappers = intervalTree.findOverlaps(VortexTimeRecord.of(startTime, endTime));
        return overlappers.stream().map(originalIndexMap::get).toList();
    }

    @Override
    public ZonedDateTime getEarliestStartTime() {
        return Optional.ofNullable(intervalTree.findMinimum())
                .map(VortexTimeRecord::startTime)
                .orElse(null);
    }

    @Override
    public ZonedDateTime getLatestEndTime() {
        return Optional.ofNullable(intervalTree.findMaximum())
                .map(VortexTimeRecord::endTime)
                .orElse(null);
    }

    private static Map<VortexTimeRecord, Integer> initOriginalIndexMap(List<VortexTimeRecord> recordList) {
        Map<VortexTimeRecord, Integer> indexMap = new HashMap<>();

        for (int i = 0; i < recordList.size(); i++) {
            VortexTimeRecord timeRecord = recordList.get(i);
            indexMap.put(timeRecord, i);
        }

        return indexMap;
    }
}
