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

/**
 * The TemporalDataReader class provides functionality to read and process temporal
 * data from a given data source. It supports various operations like reading data
 * for a specific time range and getting grid definitions.
 */
public class TemporalDataReader {
    private static final Logger logger = Logger.getLogger(TemporalDataReader.class.getName());

    private final BufferedDataReader bufferedReader;
    public final List<VortexTimeRecord> recordList;
    private final TreeMap<Long, Integer> instantDataTree = new TreeMap<>();

    /**
     * Constructs a TemporalDataReader using file paths for the data file and data.
     * @param pathToFile Path to the data file.
     * @param pathToData Path to the data.
     */
    public TemporalDataReader(String pathToFile, String pathToData) {
        this(new BufferedDataReader(pathToFile, pathToData));
    }

    /**
     * Constructs a TemporalDataReader using an instance of BufferedDataReader.
     * @param bufferedReader Instance of BufferedDataReader to read data.
     */
    public TemporalDataReader(BufferedDataReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.recordList = bufferedReader.getTimeRecords();
        initDataTree();
    }

    /**
     * Reads and returns a VortexGrid for the specified time range.
     * @param startTime The start time for the data reading.
     * @param endTime   The end time for the data reading.
     * @return A VortexGrid containing data for the specified time range.
     *         Returns null if no data is found.
     */
    public VortexGrid read(ZonedDateTime startTime, ZonedDateTime endTime) {
        VortexGrid baseGrid = bufferedReader.getBaseGrid();
        if (baseGrid == null) return null;
        VortexDataType dataType = baseGrid.dataType();
        return read(dataType, startTime, endTime);
    }

    /**
     * Retrieves the minimum and maximum grid data within a specified time range.
     * @param startTime The start time for the data range.
     * @param endTime   The end time for the data range.
     * @return An array of VortexGrid objects, where the first element is the minimum grid
     *         and the second element is the maximum grid for the specified time range.
     *         Returns an empty array if no data is found.
     */
    public VortexGrid[] getMinMaxGridData(ZonedDateTime startTime, ZonedDateTime endTime) {
        VortexGrid baseGrid = bufferedReader.getBaseGrid();
        if (baseGrid == null) return new VortexGrid[0];
        VortexDataType dataType = baseGrid.dataType();
        return getMinMaxGridData(dataType, startTime, endTime);
    }

    /**
     * Gets the grid definition of the underlying data.
     * @return A Grid object representing the grid definition. Returns null if no base grid is available.
     */
    public Grid getGridDefinition() {
        VortexGrid baseGrid = bufferedReader.getBaseGrid();
        if (baseGrid == null) return null;

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

    /**
     * Returns the file path to the data source.
     * @return A string representing the path to the data file.
     */
    public String getPathToFile() {
        return bufferedReader.getPathToFile();
    }

    /**
     * Retrieves the start time of the data records.
     * @return The ZonedDateTime representing the earliest start time in the record list.
     *         Returns null if no record is found.
     */
    public ZonedDateTime getStartTime() {
        if (recordList.isEmpty()) return null;
        VortexTimeRecord timeRecord = recordList.get(0);
        if (timeRecord == null) return null;
        return timeRecord.startTime();
    }

    /**
     * Retrieves the end time of the data records.
     * @return The ZonedDateTime representing the latest end time in the record list.
     *         Returns null if no record is found.
     */
    public ZonedDateTime getEndTime() {
        int lastIndex = recordList.size() - 1;
        VortexTimeRecord timeRecord = recordList.get(lastIndex);
        if (timeRecord == null) return null;
        return timeRecord.endTime();
    }

    /* Read Methods */
    private VortexGrid read(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime == null || endTime == null) return null;
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
    private void initDataTree() {
        for (int i = 0; i < recordList.size(); i++) {
            VortexTimeRecord timeRecord = recordList.get(i);
            if (timeRecord == null) continue;
            long startTime = timeRecord.startTime().toEpochSecond();
            long endTime = timeRecord.endTime().toEpochSecond();
            if (startTime == endTime) instantDataTree.put(startTime, i);
        }
    }

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
