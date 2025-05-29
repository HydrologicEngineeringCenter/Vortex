package mil.army.usace.hec.vortex.io;

import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Grid;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * The TemporalDataReader class provides functionality to read and process temporal
 * data from a given data source. It supports various operations like reading data
 * for a specific time range and getting grid definitions.
 */
public class TemporalDataReader implements AutoCloseable {
    private final DataReader dataReader;
    private final RecordIndexQuery recordIndexQuery;
    private final VortexGrid baseGrid;

    /* Constructor */
    private TemporalDataReader(DataReader dataReader) {
        this.dataReader = dataReader;
        this.baseGrid = Optional.ofNullable(dataReader.getDto(0))
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .orElse(null);

        if (baseGrid != null) {
            VortexDataType dataType = baseGrid.dataType();
            List<VortexDataInterval> timeIntervals = this.dataReader.getDataIntervals();
            recordIndexQuery = RecordIndexQuery.of(dataType, timeIntervals);
        } else {
            recordIndexQuery = RecordIndexQuery.of(VortexDataType.UNDEFINED, Collections.emptyList());
        }
    }

    /* Factory */
    public static TemporalDataReader create(DataReader dataReader) {
        return new TemporalDataReader(dataReader);
    }

    /**
     * Reads and returns an Optional containing a VortexGrid for the specified time range.
     * If no data is found for the specified time range, an empty Optional is returned.
     *
     * @param startTime The start time for the data reading.
     * @param endTime   The end time for the data reading.
     * @return An Optional containing a VortexGrid with data for the specified time range,
     * or an empty Optional if no data is found.
     */
    public Optional<VortexGrid> read(ZonedDateTime startTime, ZonedDateTime endTime) {
        if (baseGrid == null) {
            return Optional.empty();
        }

        // Special case
        if (!baseGrid.isTemporal() && dataReader.getDtoCount() == 1) {
            return Optional.of(baseGrid);
        }

        VortexDataType dataType = baseGrid.dataType();
        return Optional.ofNullable(read(dataType, startTime, endTime));
    }

    /**
     * Finds one {@link VortexGrid} whose valid time is closest to the specified {@code queryTime}.
     *
     * <p>Behavior:
     * <ul>
     *   <li><b>Point grids (start == end):</b> picks the grid with a timestamp closest to {@code queryTime}.</li>
     *   <li><b>Period grids (start < end):</b> picks the grid whose start time is nearest to {@code queryTime}.
     *       If two start times tie, selects the one with the finer resolution (smaller time step).</li>
     * </ul>
     *
     * <p><b>Tie-breakers & edge cases:</b>
     * <ul>
     *   <li><b>No grids available:</b> returns {@code Optional.empty()}.</li>
     *   <li><b>Exact match:</b> if {@code queryTime} equals a grid’s start or end time, that grid is returned immediately.</li>
     *   <li><b>Point grid tie:</b> if two point grids are equally distant (e.g. one hour before vs. one hour after), the earlier one wins.</li>
     *   <li><b>Time-zone handling:</b> all comparisons are done in UTC to avoid zone quirks.</li>
     * </ul>
     *
     * @param queryTime the {@link java.time.ZonedDateTime} to match against
     * @return an {@link Optional} containing the best-matched {@link VortexGrid},
     *         or {@code Optional.empty()} if no grids are available
     */
    public Optional<VortexGrid> readNearest(ZonedDateTime queryTime) {
        if (baseGrid == null) {
            return Optional.empty();
        }

        return recordIndexQuery.queryNearest(queryTime).stream()
                .map(dataReader::getDto)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .min(nearestComparator(queryTime));
    }

    private Comparator<VortexGrid> nearestComparator(ZonedDateTime queryTime) {
        return (g1, g2) -> {
            // 1) Compare absolute distance to startTime
            Duration dist1 = Duration.between(queryTime, g1.startTime()).abs();
            Duration dist2 = Duration.between(queryTime, g2.startTime()).abs();
            int comparatorValue = dist1.compareTo(dist2);
            if (comparatorValue != 0) {
                return comparatorValue;
            }

            // 2) If tied on start‐distance, compare record duration (point=zero)
            Duration dur1 = Duration.between(g1.startTime(), g1.endTime());
            Duration dur2 = Duration.between(g2.startTime(), g2.endTime());
            comparatorValue = dur1.compareTo(dur2);
            if (comparatorValue != 0) {
                return comparatorValue;
            }

            // 3) Final tie‐breaker: earlier startTime wins
            return g1.startTime().compareTo(g2.startTime());
        };
    }


    /**
     * Retrieves the minimum and maximum grid data within a specified time range.
     *
     * @param startTime The start time for the data range.
     * @param endTime   The end time for the data range.
     * @return An array of VortexGrid objects, where the first element is the minimum grid
     * and the second element is the maximum grid for the specified time range.
     * Returns an empty array if no data is found.
     */
    public VortexGrid[] getMinMaxGridData(ZonedDateTime startTime, ZonedDateTime endTime) {
        if (baseGrid == null) return new VortexGrid[0];
        VortexDataType dataType = baseGrid.dataType();
        return getMinMaxGridData(dataType, startTime, endTime);
    }

    /**
     * Retrieves the grid definition of the underlying data.
     * If no base grid is available, an empty Optional is returned.
     *
     * @return An Optional containing a Grid object representing the grid definition,
     * or an empty Optional if no base grid is available.
     */
    public Optional<Grid> getGridDefinition() {
        if (baseGrid == null) {
            return Optional.empty();
        }

        Grid gridDefinition = Grid.builder()
                .dx(baseGrid.dx())
                .dy(baseGrid.dy())
                .nx(baseGrid.nx())
                .ny(baseGrid.ny())
                .originX(baseGrid.originX())
                .originY(baseGrid.originY())
                .crs(baseGrid.wkt())
                .build();

        return Optional.of(gridDefinition);
    }

    /**
     * Retrieves the start time of the data records.
     * If no record is found, an empty Optional is returned.
     *
     * @return An Optional containing the ZonedDateTime representing the earliest start time
     * in the record list, or an empty Optional if no record is found.
     */
    public Optional<ZonedDateTime> getStartTime() {
        return Optional.ofNullable(recordIndexQuery.getEarliestStartTime());
    }

    /**
     * Retrieves the end time of the data records.
     * If no record is found, an empty Optional is returned.
     *
     * @return An Optional containing the ZonedDateTime representing the latest end time
     * in the record list, or an empty Optional if no record is found.
     */
    public Optional<ZonedDateTime> getEndTime() {
        return Optional.ofNullable(recordIndexQuery.getLatestEndTime());
    }

    /**
     * Retrieves the units string of the data.
     * If no base grid is available, an empty Optional is returned.
     *
     * @return An Optional containing a string representing the units of the data,
     * or an empty Optional if no base grid is available.
     */
    public Optional<String> getDataUnits() {
        return Optional.ofNullable(baseGrid).map(VortexGrid::units);
    }

    /* Read Methods */
    private VortexGrid read(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime == null || endTime == null) {
            return baseGrid;
        }

        return switch (dataType) {
            case ACCUMULATION -> readAccumulationData(startTime, endTime);
            case AVERAGE -> readAverageData(startTime, endTime);
            case INSTANTANEOUS -> readInstantaneousData(startTime, endTime);
            default -> null;
        };
    }

    private VortexGrid readAccumulationData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = getGridsForPeriod(startTime, endTime);
        float[] data = TemporalDataCalculator.calculateWeightedAccumulation(relevantGrids, startTime, endTime);
        return TemporalDataCalculator.buildGrid(baseGrid, startTime, endTime, data);
    }

    private VortexGrid readAverageData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = getGridsForPeriod(startTime, endTime);
        float[] data = TemporalDataCalculator.calculateWeightedAverage(relevantGrids, startTime, endTime);
        return TemporalDataCalculator.buildGrid(baseGrid, startTime, endTime, data);
    }

    private VortexGrid readInstantaneousData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = recordIndexQuery.query(startTime, endTime).stream()
                .map(dataReader::getDto)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .filter(grid -> grid.startTime() != null)
                .sorted(Comparator.comparing(VortexGrid::startTime))
                .toList();

        if (startTime.isEqual(endTime)) {
            float[] data = TemporalDataCalculator.calculatePointInstant(relevantGrids, startTime);
            return TemporalDataCalculator.buildGrid(baseGrid, startTime, startTime, data);
        } else {
            float[] data = TemporalDataCalculator.calculatePeriodInstant(relevantGrids, startTime, endTime);
            return TemporalDataCalculator.buildGrid(baseGrid, startTime, endTime, data);
        }
    }

    /* Get Min & Max Methods */
    private VortexGrid[] getMinMaxGridData(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        return switch (dataType) {
            case ACCUMULATION, AVERAGE -> getMinMaxForPeriodGrids(startTime, endTime);
            case INSTANTANEOUS -> getMinMaxForInstantaneousGrids(startTime, endTime);
            default -> null;
        };
    }

    private VortexGrid[] getMinMaxForPeriodGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<Integer> indices = new ArrayList<>(recordIndexQuery.query(startTime, endTime));
        List<VortexGrid> relevantGrids = indices.stream()
                .map(dataReader::getDto)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .toList();
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] getMinMaxForInstantaneousGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<Integer> indices = new ArrayList<>(recordIndexQuery.query(startTime, endTime));
        indices.remove(indices.size() - 1); // Drop last index

        List<VortexGrid> relevantGrids = indices.stream()
                .map(dataReader::getDto)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .filter(grid -> grid.startTime() != null)
                .filter(g -> g.startTime().toEpochSecond() >= startTime.toEpochSecond())
                .toList();
        return buildMinMaxGrids(relevantGrids, startTime, endTime);
    }

    private VortexGrid[] buildMinMaxGrids(List<VortexGrid> grids, ZonedDateTime startTime, ZonedDateTime endTime) {
        float[][] minMaxData;

        if (grids.isEmpty()) {
            minMaxData = new float[][]{new float[0], new float[0]};
        } else {
            minMaxData = TemporalDataCalculator.getMinMaxForGrids(grids);
        }

        float[] minData = minMaxData[0];
        float[] maxData = minMaxData[1];

        VortexGrid minGrid = TemporalDataCalculator.buildGrid(baseGrid, startTime, endTime, minData);
        VortexGrid maxGrid = TemporalDataCalculator.buildGrid(baseGrid, startTime, endTime, maxData);

        return new VortexGrid[]{minGrid, maxGrid};
    }

    private List<VortexGrid> getGridsForPeriod(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> relevantGrids = new ArrayList<>();
        VortexDataInterval coveredInterval = VortexDataInterval.UNDEFINED;

        List<Integer> indices = recordIndexQuery.query(startTime, endTime);
        List<VortexGrid> overlappingGrids = indices.stream()
                .map(dataReader::getDto)
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .sorted(gridPrioritizationComparator())
                .toList();

        for (VortexGrid grid : overlappingGrids) {
            VortexDataInterval gridInterval = VortexDataInterval.of(grid);
            if (!VortexDataInterval.isDefined(coveredInterval)) {
                relevantGrids.add(grid);
                coveredInterval = gridInterval;
            } else if (!coveredInterval.overlaps(gridInterval)) {
                // If gridInterval cover more than covered interval
                // Update covered interval end time
                relevantGrids.add(grid);
                coveredInterval = VortexDataInterval.of(coveredInterval.startTime(), gridInterval.endTime());
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
            Duration duration1 = VortexDataInterval.of(o1).getRecordDuration();
            Duration duration2 = VortexDataInterval.of(o2).getRecordDuration();
            return duration1.compareTo(duration2);
        };
    }

    private static Comparator<VortexGrid> prioritizeLessNoDataComparator() {
        return (o1, o2) -> {
            float[] o1DataArray = o1.data();
            float[] o2DataArray = o2.data();
            int o1Count = 0;
            int o2Count = 0;
            double noDataValue = o1.noDataValue();

            for (int i = 0; i < o1DataArray.length; i++) {
                float o1Data = o1DataArray[i];
                float o2Data = o2DataArray[i];

                if (Float.isNaN(o1Data) || o1Data == noDataValue || o1Data == Heclib.UNDEFINED_FLOAT) {
                    o1Count++;
                }

                if (Float.isNaN(o2Data) || o2Data == noDataValue || o2Data == Heclib.UNDEFINED_FLOAT) {
                    o2Count++;
                }
            }

            return Integer.compare(o1Count, o2Count);
        };
    }

    @Override
    public void close() throws Exception {
        dataReader.close();
    }
}
