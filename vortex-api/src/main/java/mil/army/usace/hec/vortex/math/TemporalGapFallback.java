package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReadException;

import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Interpolates unresolved cells between the nearest original observations. */
final class TemporalGapFallback {
    @FunctionalInterface
    interface GridSource {
        VortexGrid get(int index) throws DataReadException;
    }

    static VortexGrid fill(VortexGrid grid, int index, List<byte[]> missing,
                           GridSource source) throws DataReadException {
        float[] result = null;
        float[] data = grid.data();
        // A small cache avoids reading the same bounding grids for every cell.
        Map<Integer, VortexGrid> cache = new LinkedHashMap<>(4, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, VortexGrid> eldest) {
                return size() > 4;
            }
        };
        for (int cell = 0; cell < data.length; cell++) {
            if (Float.compare(data[cell], (float) grid.noDataValue()) != 0) continue;
            int before = index - 1;
            while (before >= 0 && missing.get(before)[cell] != 0) before--;
            int after = index + 1;
            while (after < missing.size() && missing.get(after)[cell] != 0) after++;
            // Do not extrapolate outside the observed period or reuse filled values.
            if (before < 0 || after >= missing.size()) continue;
            VortexGrid left = cache.get(before);
            if (left == null) {
                left = source.get(before);
                cache.put(before, left);
            }
            VortexGrid right = cache.get(after);
            if (right == null) {
                right = source.get(after);
                cache.put(after, right);
            }
            double span = Duration.between(left.startTime(), right.startTime()).toMillis();
            double offset = Duration.between(left.startTime(), grid.startTime()).toMillis();
            if (span <= 0 || offset < 0 || offset > span) continue;
            if (result == null) result = data.clone();
            result[cell] = (float) (left.data()[cell]
                    + offset / span * ((double) right.data()[cell] - left.data()[cell]));
        }
        return result == null ? grid : VortexGrid.toBuilder(grid).data(result).build();
    }
}
