package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Fills gaps in time series by generating missing time steps.
 * <p>
 * This class identifies missing time steps in a temporal dataset and generates
 * the corresponding grids through interpolation or other methods.
 * </p>
 */
class TimeStepFiller extends LinearInterpGapFiller {

    /**
     * Creates a new TimeStepFiller instance.
     *
     * @param builder The builder containing configuration
     */
    TimeStepFiller(Builder builder) {
        super(builder);
    }

    /**
     * Start message
     *
     * @return The start processing message
     */
    @Override
    protected String notifyStartMessage() {
        return MessageStore.getInstance().getMessage("time_step_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        if (processed <= 0)
            return null;

        String templateEnd = MessageStore.getInstance().getMessage("time_step_filler_end");
        return String.format(templateEnd, processed, destination);
    }

    /**
     * Analyzes all grids for a variable to collect metadata needed for interpolation.
     *
     * @param reader The data reader
     * @return Metadata about the grid series
     */
    @Override
    protected GridMetadata analyzeGridData(DataReader reader) {

        GridMetadata metadata = new GridMetadata();

        int dtoCount = reader.getDtoCount();

        List<ZonedDateTime> startTimes = new ArrayList<>();
        for (int i = 0; i < dtoCount; i++) {
            VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);
            startTimes.add(vortexGrid.startTime());
            metadata.intervals.add(vortexGrid.interval());
            metadata.sizes.add(vortexGrid.data().length);
            metadata.noDataValues.add(vortexGrid.noDataValue());
            metadata.originX.add(vortexGrid.originX());
            metadata.originY.add(vortexGrid.originY());
            metadata.dx.add(vortexGrid.dx());
            metadata.dy.add(vortexGrid.dy());
            metadata.nx.add(vortexGrid.nx());
            metadata.ny.add(vortexGrid.ny());
            metadata.wkt.add(vortexGrid.wkt());
            metadata.units.add(vortexGrid.units());
            metadata.shortName.add(vortexGrid.shortName());
            metadata.dataType.add(vortexGrid.dataType());
        }

        Duration interval = getMostCommonInterval(startTimes);

        if (interval == null)
            return new GridMetadata();

        ZonedDateTime startTime0 = startTimes.get(0);
        ZonedDateTime startTimeN = startTimes.get(startTimes.size() - 1);

        VortexGrid vortexGrid0 = (VortexGrid) reader.getDto(0);
        float[] data = vortexGrid0.data();

        byte[] noData0 = new byte[data.length];
        Arrays.fill(noData0, (byte) 0);

        byte[] noData1 = new byte[data.length];
        Arrays.fill(noData1, (byte) 1);

        int i = 0;
        ZonedDateTime startTime = startTime0;
        while (startTime.isBefore(startTimeN)) {
            if (startTimes.contains(startTime)) {
                metadata.indices.add(i);
                metadata.isNoData.add(noData0);
                i++;
            } else {
                metadata.indices.add(-1);
                metadata.hasNoData.add(startTime);
                metadata.isNoData.add(noData1);
            }
            metadata.startTimes.add(startTime);
            startTime = startTime.plus(interval);
        }

        if (metadata.indices.contains(-1)) {
            Set<Integer> cellsNeedingInterpolation = IntStream.range(0, data.length)
                    .boxed()
                    .collect(Collectors.toSet());

            metadata.cellsNeedingInterpolation.addAll(cellsNeedingInterpolation);
        }

        return metadata;
    }

    /**
     * Calculates the most common time interval between consecutive time points.
     *
     * @param times The list of time points
     * @return The most common interval, or null if no consistent interval is found
     */
    private static Duration getMostCommonInterval(List<ZonedDateTime> times) {
        if (times.size() < 2) {
            return null;
        }

        // Sort times to ensure correct calculation
        List<ZonedDateTime> sortedTimes = new ArrayList<>(times);
        Collections.sort(sortedTimes);

        // Count occurrences of each interval
        Map<Duration, Integer> durationCounts = new HashMap<>();
        for (int i = 1; i < sortedTimes.size(); i++) {
            Duration interval = Duration.between(sortedTimes.get(i - 1), sortedTimes.get(i));
            durationCounts.merge(interval, 1, Integer::sum);
        }

        // Find the most common interval
        return durationCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}