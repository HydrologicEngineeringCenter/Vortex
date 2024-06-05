package mil.army.usace.hec.vortex.io;

import hec.heclib.util.Heclib;
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
    private final List<VortexTimeRecord> recordList;
    private final NavigableMap<Long, Integer> instantDataTree;

    /**
     * Constructs a TemporalDataReader using file paths for the data file and data.
     * @param pathToFile Path to the data file.
     * @param pathToData Path to the data.
     */
    public TemporalDataReader(String pathToFile, String pathToData) {
        this(new BufferedDataReader(pathToFile, pathToData));
    }

    public TemporalDataReader(DataReader dataReader) {
        this(new BufferedDataReader(dataReader));
    }

    /**
     * Constructs a TemporalDataReader using an instance of BufferedDataReader.
     * @param bufferedReader Instance of BufferedDataReader to read data.
     */
    public TemporalDataReader(BufferedDataReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.recordList = initRecordList(this.bufferedReader);
        this.instantDataTree = initInstantDataTree(this.recordList);
    }

    /* Init */
    private static List<VortexTimeRecord> initRecordList(BufferedDataReader bufferedReader) {
        return List.copyOf(bufferedReader.getTimeRecords());
    }

    private static NavigableMap<Long, Integer> initInstantDataTree(List<VortexTimeRecord> recordList) {
        TreeMap<Long, Integer> treeMap = new TreeMap<>();

        for (int i = 0; i < recordList.size(); i++) {
            VortexTimeRecord timeRecord = recordList.get(i);
            if (timeRecord == null || !timeRecord.startTime().isEqual(timeRecord.endTime())) {
                continue;
            }
            treeMap.put(timeRecord.startTime().toEpochSecond(), i);
        }

        return Collections.unmodifiableNavigableMap(treeMap);
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
        // Special case
        if (!baseGrid.hasTime() && bufferedReader.getCount() == 1) return baseGrid;

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
        if (baseGrid == null) {
            return null;
        }

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

    public VortexGrid getBaseGrid() {
        return bufferedReader.getBaseGrid();
    }

    /**
     * Returns the file path to the data source.
     * @return A string representing the path to the data file.
     */
    public String getPathToFile() {
        return bufferedReader.getPathToFile();
    }

    /**
     * Returns the path to the data within the data source.
     * @return A string representing the path to the data file.
     */
    public String getPathToData() {
        return bufferedReader.getPathToData();
    }

    /**
     * Retrieves the start time of the data records.
     * @return The ZonedDateTime representing the earliest start time in the record list.
     *         Returns null if no record is found.
     */
    public ZonedDateTime getStartTime() {
        return Optional.ofNullable(recordList)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(VortexTimeRecord::startTime)
                .orElse(null);
    }

    /**
     * Retrieves the end time of the data records.
     * @return The ZonedDateTime representing the latest end time in the record list.
     *         Returns null if no record is found.
     */
    public ZonedDateTime getEndTime() {
        return Optional.ofNullable(recordList)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(recordList.size() - 1))
                .map(VortexTimeRecord::endTime)
                .orElse(null);
    }

    /**
     * Retrieves the units string of the data.
     * @return A string representing the units of the data.
     */
    public String getDataUnits() {
        return bufferedReader.getBaseGrid().units();
    }

    /* Read Methods */
    private VortexGrid read(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime == null && endTime == null) {
            return bufferedReader.getBaseGrid();
        }

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
        List<VortexGrid> relevantGrids = getGridsWithinTime(startTime, endTime);
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] getMinMaxForInstantGrids(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = queryPeriodInstantData(startTime, endTime, true, false).stream()
                .filter(g -> g.startTime().toEpochSecond() >= startTime)
                .toList();
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] buildMinMaxGrids(List<VortexGrid> grids, long startTime, long endTime) {
        float[][] minMaxData;

        if (grids.isEmpty()) {
            minMaxData = new float[][] {new float[0], new float[0]};
        } else {
            minMaxData = GridDataProcessor.getMinMaxForGrids(grids);
        }

        float[] minData = minMaxData[0];
        float[] maxData = minMaxData[1];

        VortexGrid minGrid = GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, minData);
        VortexGrid maxGrid = GridDataProcessor.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, maxData);

        return new VortexGrid[] {minGrid, maxGrid};
    }

    /* Helpers */
    private List<VortexGrid> getGridsWithinTime(long startTimeInclusive, long endTimeInclusive) {
        List<VortexGrid> grids = new ArrayList<>();

        for (int i = 0; i < recordList.size(); i++) {
            VortexTimeRecord timeRecord = recordList.get(i);
            long recordStart = timeRecord.startTime().toEpochSecond();
            long recordEnd = timeRecord.endTime().toEpochSecond();
            if (startTimeInclusive <= recordEnd && endTimeInclusive >= recordStart) {
                VortexGrid grid = bufferedReader.get(i);
                grids.add(grid);
            }
        }

        return grids;
    }

    private List<VortexGrid> getRelevantGrids(long startTime, long endTime) {
        List<VortexGrid> relevantGrids = new ArrayList<>();
        long coveredUntil = startTime;

        List<VortexGrid> overlappingGrids = getOverlappedIndices(recordList, startTime, endTime).stream()
                .map(bufferedReader::get)
                .sorted(Comparator.comparing(VortexGrid::endTime)
                        .thenComparing(prioritizeHigherResolutionDataComparator())
                        .thenComparing(prioritizeLessNoDataComparator())
                )
                .toList();

        for (VortexGrid grid : overlappingGrids) {
            VortexTimeRecord timeRecord = VortexTimeRecord.of(grid);
            long recordStart = timeRecord.startTime().toEpochSecond();
            long recordEnd = timeRecord.endTime().toEpochSecond();

            boolean isRelevant = recordEnd > coveredUntil && recordStart <= coveredUntil;

            if (isRelevant) {
                relevantGrids.add(grid);
                coveredUntil = recordEnd;
            }

            if (coveredUntil >= endTime) {
                break;
            }
        }

        return relevantGrids;
    }

    private static List<Integer> getOverlappedIndices(List<VortexTimeRecord> recordList, long startTime, long endTime) {
        return IntStream.range(0, recordList.size())
                .filter(i -> recordList.get(i).hasOverlap(startTime, endTime))
                .boxed()
                .toList();
    }

    private static Comparator<VortexGrid> prioritizeHigherResolutionDataComparator() {
        return (o1, o2) -> {
            Duration duration1 = VortexTimeRecord.of(o1).getRecordDuration();
            Duration duration2 = VortexTimeRecord.of(o2).getRecordDuration();
            return duration1.compareTo(duration2);
        };
    }

    private static Comparator<VortexGrid> prioritizeLessNoDataComparator() {
        return (o1, o2) -> {
            float[] o1DataArray = o1.data();
            float[] o2DataArray = o2.data();
            int o1Count = 0;
            int o2Count = 0;

            for (int i = 0; i < o1DataArray.length; i++) {
                float o1Data = o1DataArray[i];
                float o2Data = o2DataArray[i];

                if (Float.isNaN(o1Data) || o1.isNoDataValue(o1Data) || o1Data == Heclib.UNDEFINED_FLOAT) {
                    o1Count++;
                }

                if (Float.isNaN(o2Data) || o1.isNoDataValue(o2Data) || o2Data == Heclib.UNDEFINED_FLOAT) {
                    o2Count++;
                }
            }

            return Integer.compare(o1Count, o2Count);
        };
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
