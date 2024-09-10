package mil.army.usace.hec.vortex.temporal;

import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.io.DataReader;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * The TemporalDataReader class provides functionality to read and process temporal
 * data from a given data source. It supports various operations like reading data
 * for a specific time range and getting grid definitions.
 */
public class TemporalDataReader {
    private final BufferedDataReader bufferedReader;
    private final RecordIndexQuery recordIndexQuery;

    /* Constructor */
    private TemporalDataReader(BufferedDataReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        VortexDataType dataType = Optional.ofNullable(bufferedReader.getBaseGrid())
                .map(VortexGrid::dataType)
                .orElse(VortexDataType.UNDEFINED);
        List<VortexTimeRecord> timeRecords = this.bufferedReader.getTimeRecords();
        this.recordIndexQuery = RecordIndexQuery.of(dataType, timeRecords);
    }

    /* Factory */
    public static TemporalDataReader create(DataReader dataReader) {
        BufferedDataReader bufferedDataReader = new BufferedDataReader(dataReader);
        return new TemporalDataReader(bufferedDataReader);
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

    /**
     * Retrieves the start time of the data records.
     * @return The ZonedDateTime representing the earliest start time in the record list.
     *         Returns null if no record is found.
     */
    public ZonedDateTime getStartTime() {
        return recordIndexQuery.getEarliestStartTime();
    }

    /**
     * Retrieves the end time of the data records.
     * @return The ZonedDateTime representing the latest end time in the record list.
     *         Returns null if no record is found.
     */
    public ZonedDateTime getEndTime() {
        return recordIndexQuery.getLatestEndTime();
    }

    /**
     * Retrieves the units string of the data.
     * @return A string representing the units of the data.
     */
    public String getDataUnits() {
        return Optional.ofNullable(bufferedReader.getBaseGrid())
                .map(VortexGrid::units)
                .orElse(null);
    }

    /* Read Methods */
    private VortexGrid read(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime == null || endTime == null) {
            return bufferedReader.getBaseGrid();
        }

        return switch (dataType) {
            case ACCUMULATION -> readAccumulationData(startTime, endTime);
            case AVERAGE -> readAverageData(startTime, endTime);
            case INSTANTANEOUS -> readInstantaneousDataWithRecordQuery(startTime, endTime);
            default -> null;
        };
    }

    private VortexGrid readAccumulationData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = getRelevantGrids(startTime, endTime);
        float[] data = TemporalDataCalculator.calculateWeightedAccumulation(relevantGrids, startTime, endTime);
        return TemporalDataCalculator.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, data);
    }

    private VortexGrid readAverageData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = getRelevantGrids(startTime, endTime);
        float[] data = TemporalDataCalculator.calculateWeightedAverage(relevantGrids, startTime, endTime);
        return TemporalDataCalculator.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, data);
    }

    private VortexGrid readInstantaneousDataWithRecordQuery(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = recordIndexQuery.query(startTime, endTime).stream()
                .map(bufferedReader::get)
                .sorted(Comparator.comparing(VortexGrid::startTime))
                .toList();

        if (startTime.isEqual(endTime)) {
            float[] data = TemporalDataCalculator.calculatePointInstant(relevantGrids, startTime);
            return TemporalDataCalculator.buildGrid(bufferedReader.getBaseGrid(), startTime, startTime, data);
        } else {
            float[] data = TemporalDataCalculator.calculatePeriodInstant(relevantGrids, startTime, endTime);
            return TemporalDataCalculator.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, data);
        }
    }

    /* Get Min & Max Methods */
    private VortexGrid[] getMinMaxGridData(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        return switch (dataType) {
            case ACCUMULATION, AVERAGE -> getMinMaxForPeriodGrids(startTime, endTime);
            case INSTANTANEOUS -> getMinMaxForInstantGridsWithRecordQuery(startTime, endTime);
            default -> null;
        };
    }

    private VortexGrid[] getMinMaxForPeriodGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<Integer> indices = new ArrayList<>(recordIndexQuery.query(startTime, endTime));
        List<VortexGrid> relevantGrids = indices.stream().map(bufferedReader::get).toList();
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] getMinMaxForInstantGridsWithRecordQuery(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<Integer> indices = new ArrayList<>(recordIndexQuery.query(startTime, endTime));
        indices.remove(indices.size() - 1); // Drop last index

        List<VortexGrid> relevantGrids = indices.stream()
                .map(bufferedReader::get)
                .filter(g -> g.startTime().toEpochSecond() >= startTime.toEpochSecond())
                .toList();
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] buildMinMaxGrids(List<VortexGrid> grids, ZonedDateTime startTime, ZonedDateTime endTime) {
        float[][] minMaxData;

        if (grids.isEmpty()) {
            minMaxData = new float[][] {new float[0], new float[0]};
        } else {
            minMaxData = TemporalDataCalculator.getMinMaxForGrids(grids);
        }

        float[] minData = minMaxData[0];
        float[] maxData = minMaxData[1];

        VortexGrid minGrid = TemporalDataCalculator.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, minData);
        VortexGrid maxGrid = TemporalDataCalculator.buildGrid(bufferedReader.getBaseGrid(), startTime, endTime, maxData);

        return new VortexGrid[] {minGrid, maxGrid};
    }

    private List<VortexGrid> getRelevantGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = new ArrayList<>();
        long coveredUntil = startTime.toEpochSecond();

        List<Integer> indices = recordIndexQuery.query(startTime, endTime);
        List<VortexGrid> overlappingGrids = indices.stream()
                .map(bufferedReader::get)
                .sorted(gridPrioritizationComparator())
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

            if (coveredUntil >= endTime.toEpochSecond()) {
                break;
            }
        }

        return relevantGrids;
    }

    /* Grid Selection/Prioritization Logic */
    private static Comparator<VortexGrid> gridPrioritizationComparator() {
        return Comparator.comparing(VortexGrid::endTime)
                .thenComparing(prioritizeHigherResolutionDataComparator())
                .thenComparing(prioritizeLessNoDataComparator());
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
}
