package mil.army.usace.hec.vortex.temporal;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;

final class InstantRecordIndexQuery implements RecordIndexQuery {
    private static final Logger logger = Logger.getLogger(InstantRecordIndexQuery.class.getName());
    private final NavigableMap<ZonedDateTime, Integer> instantDataTree;

    InstantRecordIndexQuery(List<VortexTimeRecord> recordList) {
        this.instantDataTree = initInstantDataTree(recordList);
    }

    /* Query */
    @Override
    public List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime.isEqual(endTime)) {
            return queryPoint(instantDataTree, startTime);
        } else {
            return queryPeriod(instantDataTree, startTime, endTime);
        }
    }

    @Override
    public ZonedDateTime getEarliestStartTime() {
        return !instantDataTree.isEmpty() ? instantDataTree.firstKey() : null;
    }

    @Override
    public ZonedDateTime getLatestEndTime() {
        return !instantDataTree.isEmpty() ? instantDataTree.lastKey() : null;
    }

    private static List<Integer> queryPoint(NavigableMap<ZonedDateTime, Integer> instantDataTree, ZonedDateTime time) {
        Map.Entry<ZonedDateTime, Integer> floorEntry = instantDataTree.floorEntry(time);
        Map.Entry<ZonedDateTime, Integer> ceilingEntry = instantDataTree.ceilingEntry(time);

        if (floorEntry == null || ceilingEntry == null) {
            logger.info("Unable to find overlapped instant grid(s)");
            return Collections.emptyList();
        }

        int floor = floorEntry.getValue();
        int ceiling = ceilingEntry.getValue();

        if (floor == ceiling) {
            return Collections.singletonList(floor);
        } else {
            return List.of(floor, ceiling);
        }
    }

    private static List<Integer> queryPeriod(NavigableMap<ZonedDateTime, Integer> instantDataTree, ZonedDateTime start, ZonedDateTime end) {
        ZonedDateTime adjustedStart = Optional.ofNullable(instantDataTree.floorEntry(start))
                .map(Map.Entry::getKey)
                .orElse(start);

        ZonedDateTime adjustedEnd = Optional.ofNullable(instantDataTree.ceilingEntry(end))
                .map(Map.Entry::getKey)
                .orElse(end);

        boolean inclusiveStart = true;
        boolean inclusiveEnd = true;
        SortedMap<ZonedDateTime, Integer> subMap = instantDataTree.subMap(adjustedStart, inclusiveStart, adjustedEnd, inclusiveEnd);

        if (subMap == null || subMap.isEmpty()) {
            logger.info("Unable to find overlapped grid(s) for period instant data");
            return Collections.emptyList();
        }

        return subMap.values().stream().toList();
    }

    /* Helpers */
    private static NavigableMap<ZonedDateTime, Integer> initInstantDataTree(List<VortexTimeRecord> recordList) {
        TreeMap<ZonedDateTime, Integer> treeMap = new TreeMap<>();

        for (int i = 0; i < recordList.size(); i++) {
            VortexTimeRecord timeRecord = recordList.get(i);
            boolean isUndefined = !VortexTimeRecord.isDefined(timeRecord);

            if (isUndefined || !timeRecord.isInstantaneous()) {
                continue;
            }

            treeMap.put(timeRecord.startTime(), i);
        }

        return Collections.unmodifiableNavigableMap(treeMap);
    }
}
