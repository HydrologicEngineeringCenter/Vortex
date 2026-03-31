package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Fills gaps in time series by generating grids at missing time steps
 * and interpolating their values from neighboring time steps.
 */
class TimeStepFiller extends LinearInterpGapFiller {

    TimeStepFiller(Builder builder) {
        super(builder);
    }

    @Override
    protected String notifyStartMessage() {
        return Message.format("time_step_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        if (processed <= 0) return null;
        return Message.format("time_step_filler_end", processed, destination);
    }

    @Override
    protected GridMetadata analyzeGridData(DataReader reader) {
        GridMetadata metadata = new GridMetadata();

        int dtoCount = reader.getDtoCount();
        Set<ZonedDateTime> existingTimes = new LinkedHashSet<>(dtoCount);

        for (int i = 0; i < dtoCount; i++) {
            VortexGrid grid = (VortexGrid) reader.getDto(i);
            existingTimes.add(grid.startTime());
            metadata.intervals.add(grid.interval());
            metadata.sizes.add(grid.data().length);
            metadata.noDataValues.add(grid.noDataValue());
            metadata.originX.add(grid.originX());
            metadata.originY.add(grid.originY());
            metadata.dx.add(grid.dx());
            metadata.dy.add(grid.dy());
            metadata.nx.add(grid.nx());
            metadata.ny.add(grid.ny());
            metadata.wkt.add(grid.wkt());
            metadata.units.add(grid.units());
            metadata.shortName.add(grid.shortName());
            metadata.dataType.add(grid.dataType());
        }

        Duration interval = getMostCommonInterval(existingTimes);
        if (interval == null) return metadata;

        int cellCount = metadata.getSize();
        byte[] validFlags = new byte[cellCount];
        byte[] missingFlags = new byte[cellCount];
        Arrays.fill(missingFlags, (byte) 1);

        List<ZonedDateTime> sortedTimes = new ArrayList<>(existingTimes);
        Collections.sort(sortedTimes);
        ZonedDateTime first = sortedTimes.get(0);
        ZonedDateTime last = sortedTimes.get(sortedTimes.size() - 1);

        int readerIndex = 0;
        boolean hasMissingSteps = false;

        for (ZonedDateTime time = first; time.isBefore(last); time = time.plus(interval)) {
            if (existingTimes.contains(time)) {
                metadata.indices.add(readerIndex++);
                metadata.isNoData.add(validFlags);
            } else {
                metadata.indices.add(-1);
                metadata.hasNoData.add(time);
                metadata.isNoData.add(missingFlags);
                hasMissingSteps = true;
            }
            metadata.startTimes.add(time);
        }

        if (hasMissingSteps) {
            for (int i = 0; i < cellCount; i++) {
                metadata.cellsNeedingInterpolation.add(i);
            }
        }

        return metadata;
    }

    private static Duration getMostCommonInterval(Collection<ZonedDateTime> times) {
        if (times.size() < 2)
            return null;

        List<ZonedDateTime> sorted = new ArrayList<>(times);
        Collections.sort(sorted);

        Map<Duration, Integer> counts = new HashMap<>();
        for (int i = 1; i < sorted.size(); i++) {
            Duration d = Duration.between(sorted.get(i - 1), sorted.get(i));
            counts.merge(d, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
