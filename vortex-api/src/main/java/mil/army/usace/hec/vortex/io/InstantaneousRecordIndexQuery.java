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

        int undefinedCount = 0;
        int spanningCount = 0;

        for (int i = 0; i < recordList.size(); i++) {
            VortexDataInterval timeRecord = recordList.get(i);

            if (!VortexDataInterval.isDefined(timeRecord)) {
                undefinedCount++;
                continue;
            }

            if (!timeRecord.isInstantaneous()) {
                spanningCount++;
                continue;
            }

            treeMap.put(timeRecord.startTime(), i);
        }

        logSkippedRecords(recordList.size(), undefinedCount, spanningCount);

        return Collections.unmodifiableNavigableMap(treeMap);
    }

    /**
     * A record that spans a period cannot be indexed as an instant, so it is dropped. That is a
     * classification defect — the data was typed INSTANTANEOUS but its start and end times differ — and
     * dropping every record leaves the reader with no time range at all. Report it rather than let the
     * caller discover it as an empty read.
     */
    private static void logSkippedRecords(int total, int undefinedCount, int spanningCount) {
        if (spanningCount > 0) {
            logger.warning(() -> "Skipped " + spanningCount + " of " + total + " instantaneous records "
                    + "with differing start and end times. Data typed as instantaneous must not span a "
                    + "period; check the source's cell_methods and time bounds.");
        }

        if (undefinedCount > 0) {
            logger.info(() -> "Skipped " + undefinedCount + " of " + total + " instantaneous records with undefined times.");
        }
    }
}
