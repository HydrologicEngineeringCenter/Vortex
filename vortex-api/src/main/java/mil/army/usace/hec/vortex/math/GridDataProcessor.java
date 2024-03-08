package mil.army.usace.hec.vortex.math;

import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexTimeRecord;
import mil.army.usace.hec.vortex.util.TimeConverter;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class GridDataProcessor {
    private static final Logger logger = Logger.getLogger(GridDataProcessor.class.getName());

    private GridDataProcessor() {
        // Utility Class
    }

    public static float[] calculateWeightedAccumulation(List<VortexGrid> grids, long start, long end) {
        // Accumulation Data = [Sum of grids' (data * overlap ratio)]
        if (hasNoTargetData(grids, start, end)) return undefinedGridData();
        double[] weights = calculateAccumulationWeights(grids, start, end);
        return calculateWeightedData(grids, weights);
    }

    public static float[] calculateWeightedAverage(List<VortexGrid> grids, long start, long end) {
        // Average Data = [Sum of grids' (data * overlap ratio)] / [total overlap ratio]
        if (hasNoTargetData(grids, start, end)) return undefinedGridData();
        double[] weights = calculateAverageWeights(grids, start, end);
        return calculateWeightedData(grids, weights);
    }

    public static float[] calculatePointInstant(List<VortexGrid> vortexGrids, long time) {
        // Instant Data = floor grid + (rate of change between floor and ceiling) * (overlap time)
        if (hasNoTargetData(vortexGrids, time, time)) return undefinedGridData();
        double[] weights = calculatePointInstantWeights(vortexGrids, time);
        return calculateWeightedData(vortexGrids, weights);
    }
    
    public static float[] calculatePeriodInstant(List<VortexGrid> grids, long start, long end) {
        if (hasNoTargetData(grids, start, end)) return undefinedGridData();
        float[] result = new float[grids.get(0).data().length];

        long remainingStart = start;

        for (int i = 0; i < grids.size() - 1; i++) {
            VortexGrid grid1 = grids.get(i);
            VortexGrid grid2 = grids.get(i + 1);

            long avgEnd = grid2.startTime().toEpochSecond();
            double[] avgData = calculateAverageOfTwoGrids(grid1, grid2, remainingStart, end);

            for (int j = 0; j < avgData.length; j++) {
                result[j] += avgData[j];
            }

            remainingStart = avgEnd;
        }

        int totalInterval = (int) Duration.ofSeconds(end - start).toMinutes();
        for (int i = 0; i < result.length; i++) {
            result[i] /= totalInterval;
        }

        return result;
    }

    public static float[][] getMinMaxForGrids(List<VortexGrid> grids) {
        int dataLength = grids.get(0).data().length;

        float[][] minMaxData = new float[2][dataLength];
        Arrays.fill(minMaxData[0], Float.MAX_VALUE);
        Arrays.fill(minMaxData[1], -Float.MAX_VALUE);

        for (VortexGrid grid : grids) {
            float[] currentData = grid.data();
            for (int i = 0; i < dataLength; i++) {
                float datum = currentData[i];

                if (grid.isNoDataValue(datum))
                    continue;

                if (datum < minMaxData[0][i])
                    minMaxData[0][i] = datum;

                if (datum > minMaxData[1][i])
                    minMaxData[1][i] = datum;
            }
        }

        return minMaxData;
    }

    /* Helpers */
    public static VortexGrid buildGrid(VortexGrid baseGrid, long startTime, long endTime, float[] data) {
        ZoneId zoneId = Optional.ofNullable(baseGrid.startTime())
                .map(ZonedDateTime::getZone)
                .orElse(ZoneId.of("UTC"));
        ZonedDateTime start = TimeConverter.toZonedDateTime(startTime, zoneId);
        ZonedDateTime end = TimeConverter.toZonedDateTime(endTime, zoneId);

        if (data == null || data.length == 0) {
            data = new float[baseGrid.data().length];
            Arrays.fill(data, (float) baseGrid.noDataValue());
        }

        return VortexGrid.builder()
                .dx(baseGrid.dx()).dy(baseGrid.dy())
                .nx(baseGrid.nx()).ny(baseGrid.ny())
                .originX(baseGrid.originX()).originY(baseGrid.originY())
                .wkt(baseGrid.wkt())
                .data(data)
                .noDataValue(baseGrid.noDataValue())
                .units(baseGrid.units())
                .fileName(baseGrid.fileName())
                .shortName(baseGrid.shortName())
                .fullName(baseGrid.fullName())
                .description(baseGrid.description())
                .startTime(start).endTime(end)
                .interval(Duration.between(start, end))
                .dataType(baseGrid.dataType())
                .build();
    }

    private static float[] calculateWeightedData(List<VortexGrid> grids, double[] weights) {
        // Result = data * weight. If any grid's data[i] is NaN, then result[i] is NaN
        if (grids.isEmpty() || grids.size() != weights.length) {
            logger.severe("Invalid grids and/or weights");
            return undefinedGridData();
        }

        int numGrids = grids.size();
        int numData = grids.get(0).data().length;

        float[] result = new float[numData];

        for (int gridIndex = 0; gridIndex < numGrids; gridIndex++) {
            VortexGrid grid = grids.get(gridIndex);
            float[] gridData = grid.data();

            for (int dataIndex = 0; dataIndex < numData; dataIndex++) {
                double data = gridData[dataIndex];
                boolean isNaN = grid.isNoDataValue(result[dataIndex]) || grid.isNoDataValue(data);

                if (!isNaN) {
                    double weightedData = gridData[dataIndex] * weights[gridIndex];
                    result[dataIndex] += weightedData;
                } else {
                    result[dataIndex] = (float) grid.noDataValue();
                }
            }
        }

        return result;
    }

    private static double[] calculatePointInstantWeights(List<VortexGrid> vortexGrids, long targetTime) {
        return switch (vortexGrids.size()) {
            case 1 -> new double[] {1f};
            case 2 -> {
                VortexGrid floorGrid = vortexGrids.get(0);
                VortexGrid ceilingGrid = vortexGrids.get(1);
                long grid1Time = floorGrid.startTime().toEpochSecond();
                long grid2Time = ceilingGrid.startTime().toEpochSecond();
                double fraction = (double) (targetTime - grid1Time) / (grid2Time - grid1Time);
                yield  new double[] {1 - fraction, fraction};
            }
            default -> {
                logger.severe("Should not have more than two grids to interpolate");
                yield new double[0];
            }
        };
    }

    private static double[] calculateAccumulationWeights(List<VortexGrid> grids, long start, long end) {
        return grids.stream()
                .map(VortexTimeRecord::of)
                .mapToDouble(timeRecord -> timeRecord.getPercentOverlapped(start, end))
                .toArray();
    }

    private static double[] calculateAverageWeights(List<VortexGrid> grids, long start, long end) {
        double[] ratios = new double[grids.size()];

        List<VortexTimeRecord> timeRecords = grids.stream().map(VortexTimeRecord::of).toList();
        long totalDuration = end - start;

        for (int i = 0; i < timeRecords.size(); i++) {
            VortexTimeRecord timeRecord = timeRecords.get(i);
            double overlapDuration = timeRecord.getOverlapDuration(start, end).toSeconds();
            double ratio = overlapDuration / totalDuration;
            ratios[i] = ratio;
        }

        return ratios;
    }

    private static double[] calculateAverageOfTwoGrids(VortexGrid firstGrid, VortexGrid secondGrid, long start, long end) {
        VortexTimeRecord timeRecord = new VortexTimeRecord(firstGrid.startTime(), secondGrid.startTime());
        int overlapInterval = (int) timeRecord.getOverlapDuration(start, end).toMinutes();
        int wholeInterval = (int) timeRecord.getRecordDuration().toMinutes();

        double percentOverlapped = timeRecord.getPercentOverlapped(start, end);
        if (Double.isNaN(percentOverlapped) || percentOverlapped == 0) logger.severe("Invalid Overlap Ratio");

        float[] data1 = firstGrid.data();
        float[] data2 = secondGrid.data();
        double[] result = new double[data1.length];

        for (int i = 0; i < data1.length; i++) {
            double value1 = data1[i];
            double value2 = data2[i];

            if (value1 == Heclib.UNDEFINED_DOUBLE || value2 == Heclib.UNDEFINED_DOUBLE) {
                result[i] = Heclib.UNDEFINED_DOUBLE;
                continue;
            }

            boolean adjustStart = timeRecord.startTime().toEpochSecond() != start;
            boolean adjustEnd = timeRecord.endTime().toEpochSecond() != end;
            double difference = value2 - value1;

            value1 = adjustStart ? value2 - overlapInterval * difference / wholeInterval : value1;
            value2 = adjustEnd ? value1 + overlapInterval * difference / wholeInterval : value2;

            double avg = 0.5 * (value1 + value2) * overlapInterval;
            result[i] = avg;
        }

        return result;
    }

    private static boolean hasNoTargetData(List<VortexGrid> vortexGrids, long targetStart, long targetEnd) {
        // No grids found for time period [targetStart, targetEnd]
        if (vortexGrids == null || vortexGrids.isEmpty()) return true;

        VortexGrid firstGrid = vortexGrids.get(0);
        VortexGrid lastGrid = vortexGrids.get(vortexGrids.size() - 1);

        boolean afterFirstGrid = targetStart >= firstGrid.startTime().toEpochSecond();
        boolean beforeLastGrid = targetEnd <= lastGrid.endTime().toEpochSecond();

        return !afterFirstGrid || !beforeLastGrid;
    }

    private static float[] undefinedGridData() {
        logger.info("Has undefined grid data");
        return new float[0];
    }
}
