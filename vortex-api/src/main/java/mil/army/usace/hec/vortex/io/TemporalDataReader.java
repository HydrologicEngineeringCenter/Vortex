package mil.army.usace.hec.vortex.io;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TemporalDataReader {
    private static final Logger logger = Logger.getLogger(TemporalDataReader.class.getName());

    private final BufferedDataReader reader;

    private final TreeRangeMap<Long, Integer> intervalDataTree = TreeRangeMap.create();
    private final TreeMap<Long, Integer> instantDataTree = new TreeMap<>();

    public TemporalDataReader(String pathToFile, String pathToData) {
        this(new BufferedDataReader(pathToFile, pathToData));
    }

    public TemporalDataReader(BufferedDataReader reader) {
        this.reader = reader;
        initDataTrees();
    }

    private void initDataTrees() {
        // Note: Making sure we never store the entire dtos in memory
        IntStream.range(0, reader.getCount()).forEach(i -> {
            VortexGrid vortexGrid = reader.get(i);
            long startTime = vortexGrid.startTime().toEpochSecond();
            long endTime = vortexGrid.endTime().toEpochSecond();
            // Interval Data Tree
            intervalDataTree.put(Range.closed(startTime, endTime), i);
            // Instant Data Tree
            if (vortexGrid.startTime().isEqual(vortexGrid.endTime())) {
                long time = vortexGrid.startTime().toEpochSecond();
                instantDataTree.put(time, i);
            }
        });
    }

    private List<VortexGrid> getOverlappedIntervalGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        long queryStart = startTime.toEpochSecond();
        long queryEnd = endTime.toEpochSecond();

        RangeMap<Long, Integer> subRangeMap = intervalDataTree.subRangeMap(Range.closedOpen(queryStart, queryEnd));
        Collection<Integer> overlappingGrids = subRangeMap.asMapOfRanges().values();
        return overlappingGrids.stream()
                .map(reader::get)
                .toList();
    }

    private List<VortexGrid> getOverlappedInstantGrids(ZonedDateTime time) {
        long timeEpoch = time.toEpochSecond();

        Map.Entry<Long, Integer> floorEntry = instantDataTree.floorEntry(timeEpoch);
        Map.Entry<Long, Integer> ceilingEntry = instantDataTree.ceilingEntry(timeEpoch);

        if (floorEntry == null || ceilingEntry == null) {
            logger.warning("Unable to find overlapped instant grid(s)");
            return Collections.emptyList();
        }

        Integer floorIndex = floorEntry.getValue();
        Integer ceilingIndex = ceilingEntry.getValue();

        if (floorEntry.equals(ceilingEntry))
            return Collections.singletonList(reader.get(floorIndex));

        return List.of(reader.get(floorIndex), reader.get(ceilingIndex));
    }

    /**
     * Reads a VortexGrid from the data source, with the type of data
     * determined by the VortexDataType. The VortexGrid will cover the time interval
     * from startTime to endTime.
     *
     * <ul>
     *   <li>If the data type is ACCUMULATION, it reads accumulation data.
     *   <li>If the data type is AVERAGE, it reads average data.
     *   <li>If the data type is INSTANTANEOUS, it reads instantaneous data for the start time only.
     * </ul>
     *
     * If the data type is not recognized, or if the data for the specified time
     * interval is not found, the method returns null.
     *
     * @param startTime The start of the time interval for which to read data.
     * @param endTime The end of the time interval for which to read data.
     * @return A VortexGrid covering the specified time interval, or null if the
     * data type is not recognized or data is not found.
     */
    public VortexGrid read(ZonedDateTime startTime, ZonedDateTime endTime) {
        VortexDataType dataType = reader.getType();
        return read(dataType, startTime, endTime);
    }

    public VortexGrid read(VortexDataType dataType, ZonedDateTime startTime, ZonedDateTime endTime) {
        return switch (dataType) {
            case ACCUMULATION -> readAccumulationData(startTime, endTime);
            case AVERAGE -> readAverageData(startTime, endTime);
            case INSTANTANEOUS -> readInstantaneousData(startTime, endTime);
            default -> null;
        };
    }

    /**
     * Mathematically, the accumulation data (for each point in the grid) is calculated as follows:
     * <pre>
     *     accumulationData[i] += fraction * gridData[i]
     * </pre>
     * Where:
     * <ul>
     *   <li>'i' is the index of the point in the grid.
     *   <li>fraction is the time fraction that the current grid contributes.
     *   <li>gridData[i] is the data point at index i in the current grid.
     * </ul>
     * @param startTime the start of the time period
     * @param endTime the end of the time period
     * @return a VortexGrid that represents the accumulated data of all overlapped grids
     *         for the given time period, or null if no overlapped grids were found.
     */
    private VortexGrid readAccumulationData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> overlappedGrids = getOverlappedIntervalGrids(startTime, endTime);
        if (overlappedGrids.isEmpty()) return null;

        VortexGrid firstGrid = overlappedGrids.get(0);
        float[] accumulationData = new float[firstGrid.data().length];

        for (VortexGrid grid : overlappedGrids) {
            double fraction = calculateFraction(grid, startTime, endTime);
            float[] gridData = grid.data();
            IntStream.range(0, accumulationData.length).forEach(i -> accumulationData[i] += fraction * gridData[i]);
        }

        return buildGrid(firstGrid, startTime, endTime, accumulationData);
    }

    /**
     * The function calculates the average data by summing up the values of overlapped grids and dividing the sum by the
     * number of overlapped grids. Mathematically, the calculation for each location `i` can be represented as:
     * <pre>
     *     averageData[i] = (grid1Data[i] + grid2Data[i] + ... + gridNData[i]) / N
     * </pre>
     * Where:
     * <ul>
     *   <li>`averageData[i]` represents the computed average data for location `i`
     *   <li>`grid1Data[i]...gridNData[i]` are the data values of the overlapped grids at location `i`
     *   <li>`N` is the number of overlapped grids.
     * </ul>
     * @param startTime The start of the time interval for which to read and compute the average data.
     * @param endTime The end of the time interval for which to read and compute the average data.
     * @return A VortexGrid object representing the average data for the specified time interval, or null if no overlapped grids are found.
     */
    private VortexGrid readAverageData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> overlappedGrids = getOverlappedIntervalGrids(startTime, endTime);
        if (overlappedGrids.isEmpty()) return null;

        VortexGrid firstGrid = overlappedGrids.get(0);
        float[] averageData = new float[firstGrid.data().length];

        for (VortexGrid grid : overlappedGrids) {
            float[] gridData = grid.data();
            IntStream.range(0, averageData.length).forEach(i -> averageData[i] += gridData[i]);
        }

        IntStream.range(0, averageData.length).forEach(i -> averageData[i] /= overlappedGrids.size());

        return buildGrid(firstGrid, startTime, endTime, averageData);
    }

    private VortexGrid readInstantaneousData(ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime.isEqual(endTime))
            return readInstantaneousData(startTime);
        // Instantaneous Data for Period
        ZonedDateTime midTime = calculateMidTime(startTime, endTime);
        VortexGrid grid = readInstantaneousData(midTime);
        return grid != null ? buildGrid(grid, startTime, endTime, grid.data()) : null;
    }

    /**
     * The function calculates the instantaneous data by
     * <pre>
     *     ratio = (targetTimeEpoch - nearestBeforeEpoch) / (nearestAfterEpoch - nearestBeforeEpoch);
     *     resultData[i] = beforeData[i] + (afterData[i] - beforeData[i]) * ratio
     * </pre>
     * Where:
     * <ul>
     *   <li>`ratio` is a measure of how far time is between nearestBeforeTime and nearestAfterTime.
     *   <li>`grid1Data[i]...gridNData[i]` are the data values of the overlapped grids at location `i`
     *   <li>`N` is the number of overlapped grids.
     * </ul>
     * @param time The instantaneous time which to read and compute the instantaneous data.
     * @return A VortexGrid object representing the instantaneous data for the specified time, or null if no data is found.
     */
    private VortexGrid readInstantaneousData(ZonedDateTime time) {
        List<VortexGrid> gridList = getOverlappedInstantGrids(time);

        if (gridList.isEmpty()) return null;
        if (gridList.size() == 1) return gridList.get(0); // Found grid that matches the exact time

        // Calculating instantaneous data using dataLeft and dataRight
        VortexGrid nearestBefore = gridList.get(0);
        VortexGrid nearestAfter = gridList.get(1);

        long nearestBeforeEpoch = nearestBefore.startTime().toEpochSecond();
        long nearestAfterEpoch = nearestAfter.startTime().toEpochSecond();
        long targetTimeEpoch = time.toEpochSecond();

        double ratio = (double) (targetTimeEpoch - nearestBeforeEpoch) / (nearestAfterEpoch - nearestBeforeEpoch);

        float[] beforeData = nearestBefore.data();
        float[] afterData = nearestAfter.data();
        float[] resultData = new float[beforeData.length];

        for (int i = 0; i < beforeData.length; i++) {
            resultData[i] = (float) (beforeData[i] + (afterData[i] - beforeData[i]) * ratio);
        }

        return buildGrid(nearestBefore, time, time, resultData);
    }

    /* Helpers */
    private VortexGrid buildGrid(VortexGrid grid, ZonedDateTime startTime, ZonedDateTime endTime, float[] data) {
        return VortexGrid.builder()
                .dx(grid.dx())
                .dy(grid.dy())
                .nx(grid.nx())
                .ny(grid.ny())
                .originX(grid.originX())
                .originY(grid.originY() + grid.dy() * grid.ny())
                .wkt(grid.wkt())
                .data(data)
                .noDataValue(grid.noDataValue())
                .units(grid.units())
                .fileName(grid.fileName()) // Check
                .shortName(grid.shortName())
                .fullName(grid.fullName())
                .description(grid.description())
                .startTime(startTime)
                .endTime(endTime)
                .interval(Duration.between(startTime, endTime))
                .dataType(grid.dataType())
                .build();
    }

    /**
     * Calculates the proportion of a VortexGrid's period that overlaps with a given time range.
     * <p>
     * The function first determines the overlap of the VortexGrid's period with the target time range.
     * Then, it calculates the proportion that this overlap represents.
     *
     * @param grid The VortexGrid object.
     * @param startTime The start of the target time range.
     * @param endTime The end of the target time range.
     * @return The proportion of the VortexGrid's period that overlaps with the target time range.
     */
    private double calculateFraction(VortexGrid grid, ZonedDateTime startTime, ZonedDateTime endTime) {
        double overlapStart = Math.max(startTime.toEpochSecond(), grid.startTime().toEpochSecond());
        double overlapEnd = Math.min(endTime.toEpochSecond(), grid.endTime().toEpochSecond());
        return (overlapEnd - overlapStart) / (grid.endTime().toEpochSecond() - grid.startTime().toEpochSecond());
    }

    private ZonedDateTime calculateMidTime(ZonedDateTime startTime, ZonedDateTime endTime) {
        long startSeconds = startTime.toEpochSecond();
        long endSeconds = endTime.toEpochSecond();
        long midSeconds = (startSeconds + endSeconds) / 2;
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(midSeconds), startTime.getZone());
    }
}
