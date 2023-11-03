package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexTimeRecord;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.math.GridDataProcessor;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TemporalDataReader {
    private static final Logger logger = Logger.getLogger(TemporalDataReader.class.getName());

    private final BufferedDataReader bufferedReader;
    public final List<VortexTimeRecord> recordList;
    private final TreeMap<Long, Integer> instantDataTree = new TreeMap<>();

    /* Constructors */
    public TemporalDataReader(String pathToFile, String pathToData) {
        this(new BufferedDataReader(pathToFile, pathToData));
    }

    public TemporalDataReader(BufferedDataReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.recordList = bufferedReader.getTimeRecords();
        initDataTree();
    }

    private void initDataTree() {
        List<VortexTimeRecord> timeRecords = bufferedReader.getTimeRecords();

        for (int i = 0; i < timeRecords.size(); i++) {
            VortexTimeRecord timeRecord = timeRecords.get(i);
            long startTime = timeRecord.startTime().toEpochSecond();
            long endTime = timeRecord.endTime().toEpochSecond();
            if (startTime == endTime) instantDataTree.put(startTime, i);
        }
    }

    /* Public API */
    public VortexGrid read(ZonedDateTime startTime, ZonedDateTime endTime) {
        VortexGrid baseGrid = bufferedReader.getBaseGrid();
        VortexDataType dataType = baseGrid.dataType();
        return read(dataType, startTime, endTime);
    }

    public VortexGrid[] getMinMaxGridData(ZonedDateTime startTime, ZonedDateTime endTime) {
        VortexGrid baseGrid = bufferedReader.getBaseGrid();
        VortexDataType dataType = baseGrid.dataType();
        return getMinMaxGridData(dataType, startTime, endTime);
    }

    public Grid getGridDefinition() {
        VortexGrid baseGrid = bufferedReader.getBaseGrid();

        return Grid.builder()
                .dx(baseGrid.dx())
                .dy(baseGrid.dy())
                .nx(baseGrid.nx())
                .ny(baseGrid.ny())
                .originX(baseGrid.originX())
                .originY(baseGrid.originY())
                .crs(baseGrid.wkt())
                .build();
    }

    public String getPathToFile() {
        return bufferedReader.getPathToFile();
    }

    /* Read Methods */
    private VortexGrid read(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        long start = startTime.toEpochSecond();
        long end = endTime.toEpochSecond();

        return switch (dataType) {
            case ACCUMULATION -> readAccumulationData(start, end);
            case AVERAGE -> readAverageData(start, end);
            case INSTANTANEOUS -> readInstantaneousData(start, end);
            default -> null;
        };
    }

    private VortexGrid readAccumulationData(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = getRelevantGrids(startTime, endTime);
        float[] data = GridDataProcessor.calculateWeightedAccumulation(relevantGrids, startTime, endTime);
        return GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, data);
    }

    private VortexGrid readAverageData(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = getRelevantGrids(startTime, endTime);
        float[] data = GridDataProcessor.calculateWeightedAverage(relevantGrids, startTime, endTime);
        return GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, data);
    }

    private VortexGrid readInstantaneousData(long startTime, long endTime) {
        return startTime == endTime ? readPointInstantData(startTime) : readPeriodInstantData(startTime, endTime);
    }

    private VortexGrid readPointInstantData(long time) {
        List<VortexGrid> relevantGrids = queryPointInstantData(time);
        float[] data = GridDataProcessor.calculatePointInstant(relevantGrids, time);
        return GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), time, time, data);
    }

    private VortexGrid readPeriodInstantData(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = queryPeriodInstantData(startTime, endTime, true, true);
        float[] data = GridDataProcessor.calculatePeriodInstant(relevantGrids, startTime, endTime);
        return GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, data);
    }

    /* Get Min & Max Methods */
    private VortexGrid[] getMinMaxGridData(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        long start = startTime.toEpochSecond();
        long end = endTime.toEpochSecond();

        return switch (dataType) {
            case ACCUMULATION, AVERAGE -> getMinMaxForPeriodGrids(start, end);
            case INSTANTANEOUS -> getMinMaxForInstantGrids(start, end);
            default -> null;
        };
    }

    private VortexGrid[] getMinMaxForPeriodGrids(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = getRelevantGrids(startTime, endTime);
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] getMinMaxForInstantGrids(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = queryPeriodInstantData(startTime, endTime, true, false).stream()
                .filter(g -> g.startTime().toEpochSecond() >= startTime)
                .toList();
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] buildMinMaxGrids(List<VortexGrid> grids, long startTime, long endTime) {
        float[][] minMaxData = GridDataProcessor.getMinMaxForGrids(grids);
        float[] minData = minMaxData[0];
        float[] maxData = minMaxData[1];

        VortexGrid minGrid = GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, minData);
        VortexGrid maxGrid = GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, maxData);

        return new VortexGrid[] {minGrid, maxGrid};
    }

    /* Helpers */
    private List<VortexGrid> getRelevantGrids(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = new ArrayList<>();
        long coveredUntil = startTime;

        List<Integer> overlappedAndSortedIndices = getOverlappedAndSortedIndices(startTime, endTime);

        for (int index : overlappedAndSortedIndices) {
            VortexTimeRecord timeRecord = recordList.get(index);
            long recordStart = timeRecord.startTime().toEpochSecond();
            long recordEnd = timeRecord.endTime().toEpochSecond();

            boolean isRelevant = recordEnd > coveredUntil && recordStart <= coveredUntil;

            if (isRelevant) {
                relevantGrids.add(bufferedReader.get(index));
                coveredUntil = recordEnd;
            }

            if (coveredUntil >= endTime) {
                break;
            }
        }

        return relevantGrids;
    }

    private List<Integer> getOverlappedAndSortedIndices(long startTime, long endTime) {
        return IntStream.range(0, recordList.size())
                .filter(i -> recordList.get(i).hasOverlap(startTime, endTime))
                .boxed()
                .sorted((i1, i2) -> {
                    Duration duration1 = recordList.get(i1).getRecordDuration();
                    Duration duration2 = recordList.get(i2).getRecordDuration();
                    return duration1.compareTo(duration2);
                })
                .toList();
    }

    private List<VortexGrid> queryPointInstantData(long time) {
        Map.Entry<Long, Integer> floorEntry = instantDataTree.floorEntry(time);
        Map.Entry<Long, Integer> ceilingEntry = instantDataTree.ceilingEntry(time);

        if (floorEntry == null || ceilingEntry == null) {
            logger.info("Unable to find overlapped instant grid(s)");
            return Collections.emptyList();
        }

        VortexGrid floorGrid = bufferedReader.get(floorEntry.getValue());
        VortexGrid ceilingGrid = bufferedReader.get(ceilingEntry.getValue());

        if (floorEntry.equals(ceilingEntry))
            return List.of(floorGrid);

        return List.of(floorGrid, ceilingGrid);
    }

    private List<VortexGrid> queryPeriodInstantData(long start, long end, boolean startInclusive, boolean endInclusive) {
        Long adjustedStart = Optional.ofNullable(instantDataTree.floorEntry(start))
                .map(Map.Entry::getKey)
                .orElse(start);

        Long adjustedEnd = Optional.ofNullable(instantDataTree.ceilingEntry(end))
                .map(Map.Entry::getKey)
                .orElse(end);

        SortedMap<Long, Integer> subMap = instantDataTree.subMap(adjustedStart, startInclusive, adjustedEnd, endInclusive);

        if (subMap == null || subMap.isEmpty()) {
            logger.info("Unable to find overlapped grid(s) for period instant data");
            return Collections.emptyList();
        }

        return subMap.values().stream()
                .map(bufferedReader::get)
                .sorted(Comparator.comparing(VortexGrid::startTime))
                .toList();
    }
}
