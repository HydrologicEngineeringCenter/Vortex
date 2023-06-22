package mil.army.usace.hec.vortex.io;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class TemporalDataReader {
    private static final Logger logger = Logger.getLogger(TemporalDataReader.class.getName());

    private final TreeRangeMap<Long, VortexGrid> intervalDataTree = TreeRangeMap.create();
    private final TreeMap<Long, VortexGrid> instantDataTree = new TreeMap<>();
    private final VortexGrid baseGrid;

    /* Constructors */
    public TemporalDataReader(String pathToFile, String pathToData) {
        this(DataReader.builder()
                .path(pathToFile)
                .variable(pathToData)
                .build()
        );
    }

    public TemporalDataReader(DataReader dataReader) {

        // Initialize Data Tree
        VortexGrid firstGrid = null;
        for (VortexData vortexData : dataReader.getDtos()) {
            if (vortexData instanceof VortexGrid vortexGrid) {
                if (firstGrid == null) firstGrid = vortexGrid;

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

        this.baseGrid = firstGrid;
    }

    /* Public API */
    public VortexGrid read(ZonedDateTime startTime, ZonedDateTime endTime) {
        VortexDataType dataType = baseGrid.dataType();
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

    /* Read Methods */
    private VortexGrid readAccumulationData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> overlappedGrids = getOverlappedIntervalGrids(startTime, endTime);

        if (overlappedGrids.isEmpty()) {
            logger.info("Unable to find overlapped grid(s) for accumulation data");
            return null;
        }

        float[] accumulationData = new float[baseGrid.data().length];

        for (VortexGrid grid : overlappedGrids) {
            double overlapStart = Math.max(startTime.toEpochSecond(), grid.startTime().toEpochSecond());
            double overlapEnd = Math.min(endTime.toEpochSecond(), grid.endTime().toEpochSecond());
            double fraction = (overlapEnd - overlapStart) / (grid.endTime().toEpochSecond() - grid.startTime().toEpochSecond());

            float[] gridData = grid.data();
            for (int i = 0; i < accumulationData.length; i++) accumulationData[i] += fraction * gridData[i];
        }

        return buildGrid(startTime, endTime, accumulationData);
    }

    private VortexGrid readAverageData(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<VortexGrid> overlappedGrids = getOverlappedIntervalGrids(startTime, endTime);

        if (overlappedGrids.isEmpty()) {
            logger.info("Unable to find overlapped grid(s) for average data");
            return null;
        }

        float[] averageData = new float[baseGrid.data().length];

        for (VortexGrid grid : overlappedGrids) {
            float[] gridData = grid.data();
            for (int i = 0; i < averageData.length; i++) averageData[i] += gridData[i];
        }

        for (int i = 0; i < averageData.length; i++) averageData[i] /= overlappedGrids.size();

        return buildGrid(startTime, endTime, averageData);
    }

    private VortexGrid readInstantaneousData(ZonedDateTime startTime, ZonedDateTime endTime) {
        return startTime.isEqual(endTime)
                ? readPointInstantData(startTime)
                : readPeriodInstantData(startTime, endTime);
    }

    private VortexGrid readPointInstantData(ZonedDateTime time) {
        long timeEpoch = time.toEpochSecond();

        Map.Entry<Long, VortexGrid> floorEntry = instantDataTree.floorEntry(timeEpoch);
        Map.Entry<Long, VortexGrid> ceilingEntry = instantDataTree.ceilingEntry(timeEpoch);

        if (floorEntry == null || ceilingEntry == null) {
            logger.info("Unable to find overlapped instant grid(s)");
            return null;
        }

        VortexGrid floorGrid = floorEntry.getValue();
        VortexGrid ceilingGrid = ceilingEntry.getValue();

        if (floorEntry.equals(ceilingEntry))
            return floorGrid;

        long floorTime = floorGrid.startTime().toEpochSecond();
        long ceilingTime = ceilingGrid.startTime().toEpochSecond();

        double ratio = (double) (timeEpoch - floorTime) / (ceilingTime - floorTime);

        float[] floorData = floorGrid.data();
        float[] ceilingData = ceilingGrid.data();
        float[] resultData = new float[floorData.length];

        for (int i = 0; i < floorData.length; i++) {
            resultData[i] = (float) (floorData[i] + (ceilingData[i] - floorData[i]) * ratio);
        }

        return buildGrid(time, time, resultData);
    }

    private VortexGrid readPeriodInstantData(ZonedDateTime startTime, ZonedDateTime endTime) {
        long start = startTime.toEpochSecond();
        long end = endTime.toEpochSecond();

        List<VortexGrid> overlappedGrids = instantDataTree.subMap(start, true, end, true)
                .values()
                .stream()
                .toList();

        if (overlappedGrids.isEmpty()) {
            logger.info("Unable to find overlapped grid(s) for period instant data");
            return null;
        }

        float[][] dataArrays = overlappedGrids.stream()
                .map(VortexGrid::data)
                .toArray(float[][]::new);

        float[] weightedAverageData = calculateWeightedAverage(dataArrays);

        return buildGrid(startTime, endTime, weightedAverageData);
    }

    /* Helpers */
    private List<VortexGrid> getOverlappedIntervalGrids(ZonedDateTime startTime, ZonedDateTime endTime) {
        long queryStart = startTime.toEpochSecond();
        long queryEnd = endTime.toEpochSecond();

        RangeMap<Long, VortexGrid> subRangeMap = intervalDataTree.subRangeMap(Range.closedOpen(queryStart, queryEnd));
        return subRangeMap.asMapOfRanges().values().stream().toList();
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

    private VortexGrid buildGrid(ZonedDateTime startTime, ZonedDateTime endTime, float[] data) {
        return VortexGrid.builder()
                .dx(baseGrid.dx())
                .dy(baseGrid.dy())
                .nx(baseGrid.nx())
                .ny(baseGrid.ny())
                .originX(baseGrid.originX())
                .originY(baseGrid.originY())
                .wkt(baseGrid.wkt())
                .data(data)
                .noDataValue(baseGrid.noDataValue())
                .units(baseGrid.units())
                .fileName(baseGrid.fileName())
                .shortName(baseGrid.shortName())
                .fullName(baseGrid.fullName())
                .description(baseGrid.description())
                .startTime(startTime)
                .endTime(endTime)
                .interval(Duration.between(startTime, endTime))
                .dataType(baseGrid.dataType())
                .build();
    }
}
