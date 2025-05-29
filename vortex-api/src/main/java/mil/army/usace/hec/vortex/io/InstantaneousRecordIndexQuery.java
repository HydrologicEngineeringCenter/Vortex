package mil.army.usace.hec.vortex.io;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

final class InstantaneousRecordIndexQuery implements RecordIndexQuery {
    private static final Logger logger = Logger.getLogger(InstantaneousRecordIndexQuery.class.getName());
    private final NavigableMap<ZonedDateTime, Integer> instantaneousDataTree;

    private InstantaneousRecordIndexQuery(List<VortexDataInterval> recordList) {
        this.instantaneousDataTree = initInstantaneousDataTree(recordList);
    }

    static InstantaneousRecordIndexQuery from(List<VortexDataInterval> recordList) {
        return new InstantaneousRecordIndexQuery(recordList);
    }

    /* Query */
    @Override
    public List<Integer> query(ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime.isEqual(endTime)) {
            return queryPoint(instantaneousDataTree, startTime);
        } else {
            return queryPeriod(instantaneousDataTree, startTime, endTime);
        }
    }

    @Override
    public List<Integer> queryNearest(ZonedDateTime queryTime) {
        Map.Entry<ZonedDateTime, Integer> floorEntry = instantaneousDataTree.floorEntry(queryTime);
        Map.Entry<ZonedDateTime, Integer> ceilingEntry = instantaneousDataTree.ceilingEntry(queryTime);
        return Stream.of(floorEntry, ceilingEntry)
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .toList();
    }

    @Override
    public ZonedDateTime getEarliestStartTime() {
        return !instantaneousDataTree.isEmpty() ? instantaneousDataTree.firstKey() : null;
    }

    @Override
    public ZonedDateTime getLatestEndTime() {
        return !instantaneousDataTree.isEmpty() ? instantaneousDataTree.lastKey() : null;
    }

    private static List<Integer> queryPoint(NavigableMap<ZonedDateTime, Integer> instantaneousDataTree, ZonedDateTime time) {
        Map.Entry<ZonedDateTime, Integer> floorEntry = instantaneousDataTree.floorEntry(time);
        Map.Entry<ZonedDateTime, Integer> ceilingEntry = instantaneousDataTree.ceilingEntry(time);

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

    private static List<Integer> queryPeriod(NavigableMap<ZonedDateTime, Integer> instantaneousDataTree, ZonedDateTime start, ZonedDateTime end) {
        ZonedDateTime adjustedStart = Optional.ofNullable(instantaneousDataTree.floorEntry(start))
                .map(Map.Entry::getKey)
                .orElse(start);

        ZonedDateTime adjustedEnd = Optional.ofNullable(instantaneousDataTree.ceilingEntry(end))
                .map(Map.Entry::getKey)
                .orElse(end);

        boolean inclusiveStart = true;
        boolean inclusiveEnd = true;
        SortedMap<ZonedDateTime, Integer> subMap = instantaneousDataTree.subMap(adjustedStart, inclusiveStart, adjustedEnd, inclusiveEnd);

        if (subMap == null || subMap.isEmpty()) {
            logger.info("Unable to find overlapped grid(s) for period instant data");
            return Collections.emptyList();
        }

        return subMap.values().stream().toList();
    }

    /* Helpers */
    private static NavigableMap<ZonedDateTime, Integer> initInstantaneousDataTree(List<VortexDataInterval> recordList) {
        TreeMap<ZonedDateTime, Integer> treeMap = new TreeMap<>();

        for (int i = 0; i < recordList.size(); i++) {
            VortexDataInterval timeRecord = recordList.get(i);
            boolean isUndefined = !VortexDataInterval.isDefined(timeRecord);

            if (isUndefined || !timeRecord.isInstantaneous()) {
                continue;
            }

            treeMap.put(timeRecord.startTime(), i);
        }

        return Collections.unmodifiableNavigableMap(treeMap);
    }
}
