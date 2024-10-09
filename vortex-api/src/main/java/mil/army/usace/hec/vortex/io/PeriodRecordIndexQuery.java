package mil.army.usace.hec.vortex.io;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PeriodRecordIndexQuery implements RecordIndexQuery {
    private final Map<VortexDataInterval, Integer> originalIndexMap;
    private final IntervalTree<VortexDataInterval> intervalTree;

    private PeriodRecordIndexQuery(List<VortexDataInterval> recordList) {
        this.originalIndexMap = initOriginalIndexMap(recordList);
        this.intervalTree = IntervalTree.from(recordList);
    }

    static PeriodRecordIndexQuery from(List<VortexDataInterval> recordList) {
        return new PeriodRecordIndexQuery(recordList);
    }

    /* Query */
    @Override
    public List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexDataInterval> overlappers = intervalTree.findOverlaps(VortexDataInterval.of(startTime, endTime));
        return overlappers.stream().map(originalIndexMap::get).toList();
    }

    @Override
    public ZonedDateTime getEarliestStartTime() {
        return Optional.ofNullable(intervalTree.findMinimum())
                .map(VortexDataInterval::startTime)
                .orElse(null);
    }

    @Override
    public ZonedDateTime getLatestEndTime() {
        return Optional.ofNullable(intervalTree.findMaximum())
                .map(VortexDataInterval::endTime)
                .orElse(null);
    }

    private static Map<VortexDataInterval, Integer> initOriginalIndexMap(List<VortexDataInterval> recordList) {
        Map<VortexDataInterval, Integer> indexMap = new HashMap<>();

        for (int i = 0; i < recordList.size(); i++) {
            VortexDataInterval timeRecord = recordList.get(i);
            indexMap.put(timeRecord, i);
        }

        return indexMap;
    }
}
