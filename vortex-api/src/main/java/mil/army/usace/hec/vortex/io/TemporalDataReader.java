package mil.army.usace.hec.vortex.io;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TemporalDataReader {
    private static final Logger logger = Logger.getLogger(TemporalDataReader.class.getName());

    private final BufferedDataReader reader;

    private final TreeRangeMap<Long, VortexGrid> intervalDataTree = TreeRangeMap.create();
    private final TreeMap<Long, VortexGrid> instantDataTree = new TreeMap<>();

    public TemporalDataReader(String pathToFile, String pathToData) {
        this(new BufferedDataReader(pathToFile, pathToData));
    }

    public TemporalDataReader(BufferedDataReader reader) {
        this.reader = reader;
        initDataTrees();
    }

    private void initDataTrees() {
        List<VortexData> dataList = reader.getVortexDataList();

        for (VortexData x : dataList) {
            if (x instanceof VortexGrid vortexGrid) {
                long startTime = vortexGrid.startTime().toEpochSecond();
                long endTime = vortexGrid.endTime().toEpochSecond();
                // Interval Data Tree
                intervalDataTree.put(Range.closed(startTime, endTime), vortexGrid);
                // Instant Data Tree
                if (vortexGrid.startTime().isEqual(vortexGrid.endTime())) {
                    long time = vortexGrid.startTime().toEpochSecond();
                    instantDataTree.put(time, vortexGrid);
                }
            }
        }
    }

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
        return readPeriodInstantData(startTime, endTime);
    }

    private VortexGrid readPeriodInstantData(ZonedDateTime startTime, ZonedDateTime endTime) {
        long start = startTime.toEpochSecond();
        long end = endTime.toEpochSecond();

        List<VortexGrid> overlappedGrids = instantDataTree.subMap(start, true, end, true)
                .values()
                .stream()
                .toList();
        if (overlappedGrids.isEmpty()) return null;

        float[][] dataArrays = overlappedGrids.stream()
                .map(VortexGrid::data)
                .toArray(float[][]::new);

        VortexGrid representativeGrid = overlappedGrids.get(0);
        float[] weightedAverageData = calculateWeightedAverage(dataArrays);

        return buildGrid(representativeGrid, startTime, endTime, weightedAverageData);
    }

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
    private List<VortexGrid> getOverlappedIntervalGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        long queryStart = startTime.toEpochSecond();
        long queryEnd = endTime.toEpochSecond();

        RangeMap<Long, VortexGrid> subRangeMap = intervalDataTree.subRangeMap(Range.closedOpen(queryStart, queryEnd));
        return subRangeMap.asMapOfRanges().values().stream().toList();
    }

    private List<VortexGrid> getOverlappedInstantGrids(ZonedDateTime time) {
        long timeEpoch = time.toEpochSecond();

        Map.Entry<Long, VortexGrid> floorEntry = instantDataTree.floorEntry(timeEpoch);
        Map.Entry<Long, VortexGrid> ceilingEntry = instantDataTree.ceilingEntry(timeEpoch);

        if (floorEntry == null || ceilingEntry == null) {
            logger.info("Unable to find overlapped instant grid(s)");
            return Collections.emptyList();
        }

        VortexGrid floorIndex = floorEntry.getValue();
        VortexGrid ceilingIndex = ceilingEntry.getValue();

        if (floorEntry.equals(ceilingEntry))
            return Collections.singletonList(floorIndex);

        return List.of(floorIndex, ceilingIndex);
    }

    private float[] calculateWeightedAverage(float[]... dataArrays) {
        int numDataArrays = dataArrays.length;
        if (numDataArrays == 0) return new float[0];

        // NOTE: Weighted Average: first and last data weight is 0.5
        int numData = dataArrays[0].length;
        float[] weightedAverage = new float[numData];

        for (int i = 0; i < numDataArrays; i++) {
            float[] data = dataArrays[i];
            float weight = (i == 0 || i == numDataArrays - 1) ? 0.5f : 1f;
            for (int j = 0; j < numData; j++) data[j] *= weight;
            for (int j = 0; j < numData; j++) weightedAverage[j] += data[j];
        }

        for (int i = 0; i < numData; i++) weightedAverage[i] /= (numDataArrays - 1);

        return weightedAverage;
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

    private double calculateFraction(VortexGrid grid, ZonedDateTime startTime, ZonedDateTime endTime) {
        double overlapStart = Math.max(startTime.toEpochSecond(), grid.startTime().toEpochSecond());
        double overlapEnd = Math.min(endTime.toEpochSecond(), grid.endTime().toEpochSecond());
        return (overlapEnd - overlapStart) / (grid.endTime().toEpochSecond() - grid.startTime().toEpochSecond());
    }
}
