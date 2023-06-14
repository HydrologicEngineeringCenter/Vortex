package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TemporalDataReader {
    private static final Logger logger = Logger.getLogger(TemporalDataReader.class.getName());

    private final BufferedDataReader reader;
    private final String pathToFile;
    private final String pathToData;

    public TemporalDataReader(String pathToFile, String pathToData) {
        this(new BufferedDataReader(pathToFile, pathToData));
    }

    public TemporalDataReader(BufferedDataReader reader) {
        this.reader = reader;
        this.pathToFile = reader.getPathToFile();
        this.pathToData = reader.getPathToData();
    }

    /* Getters for pathToFile and pathToData */
    public String getPathToFile() {
        return pathToFile;
    }

    public String getPathToData() {
        return pathToData;
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
        switch (dataType) {
            case ACCUMULATION:
                return readAccumulationData(startTime, endTime);
            case AVERAGE:
                return readAverageData(startTime, endTime);
            case INSTANTANEOUS:
                return readInstantaneousData(startTime, endTime);
            default:
                return null;
        }
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
        List<VortexGrid> overlappedGrids = getGridsForTimePeriod(startTime, endTime);
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
        List<VortexGrid> overlappedGrids = getGridsForTimePeriod(startTime, endTime);
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
        List<VortexGrid> gridList = getGridsForInstant(time);

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
    private List<VortexGrid> getGridsForTimePeriod(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> gridList = new ArrayList<>();

        for (int i = 0; i < reader.getCount(); i++) {
            VortexGrid grid = reader.get(i);
            if (grid == null) continue;
            // Finding grids with time range that overlaps with the specified time range
            boolean sameTime = grid.startTime().isEqual(startTime) && grid.endTime().isEqual(endTime);
            boolean startTimeLteEndTime = grid.startTime().isBefore(endTime);
            boolean endTimeGteStartTime = grid.endTime().isAfter(startTime);
            if (sameTime) return List.of(grid);
            if (startTimeLteEndTime && endTimeGteStartTime) gridList.add(grid);
        }

        if (gridList.isEmpty()) logger.warning("No grids found for the specified time period");

        return gridList;
    }

    private List<VortexGrid> getGridsForInstant(ZonedDateTime instantTime) {
        // Assumed that all grids are of type Instantaneous (where startTime equals endTime)
        // Assumed that all grids are ordered from earliest to latest
        VortexGrid nearestBefore = null;
        VortexGrid nearestAfter = null;

        for (int i = 0; i < reader.getCount(); i++) {
            VortexGrid grid = reader.get(i);
            if (grid == null) continue;
            ZonedDateTime gridTime = grid.startTime();

            boolean isEqualToTargetTime = gridTime.isEqual(instantTime);
            boolean isNearestBefore = gridTime.isBefore(instantTime)
                    && (nearestBefore == null || gridTime.isAfter(nearestBefore.startTime()));
            boolean isNearestAfter = gridTime.isAfter(instantTime)
                    && (nearestAfter == null || gridTime.isBefore(nearestAfter.startTime()));

            if (isEqualToTargetTime) return List.of(grid);
            if (isNearestBefore) nearestBefore = grid;
            if (isNearestAfter) nearestAfter = grid;

            if (nearestBefore != null && nearestAfter != null)
                return List.of(nearestBefore, nearestAfter);
        }

        String message = "No overlapped grids found for time: " + instantTime.toString();
        logger.warning(message);
        return Collections.emptyList();
    }

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
