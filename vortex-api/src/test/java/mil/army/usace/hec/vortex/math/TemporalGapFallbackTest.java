package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemporalGapFallbackTest {
    private static final float MISSING = -9999;

    private VortexGrid grid(int hour, float... values) {
        return VortexGrid.builder().nx(values.length).ny(1).dx(1).dy(1)
                .originX(0).originY(0).data(values).noDataValue(MISSING)
                .startTime(ZonedDateTime.parse("2020-01-01T00:00:00Z").plusHours(hour))
                .build();
    }

    @Test
    void bridgesLongRunsUsingActualTimesAndOriginalObservations() throws Exception {
        for (GapFillMethod method : List.of(GapFillMethod.REGRESSION, GapFillMethod.KRIGING)) {
            List<VortexGrid> grids = new ArrayList<>();
            List<byte[]> flags = new ArrayList<>();
            grids.add(grid(0, 0, 100));
            flags.add(new byte[2]);
            for (int i = 1; i <= 7; i++) {
                grids.add(grid(i, MISSING, MISSING));
                flags.add(new byte[]{1, 1});
            }
            grids.add(grid(10, 100, 200));
            flags.add(new byte[2]);
            BatchGapFiller.Builder builder = BatchGapFiller.builder().source("source.dss")
                    .destination("destination.dss").variables(List.of("test")).method(method);
            for (int i = 1; i <= 7; i++) {
                VortexGrid original = grids.get(i);
                VortexGrid spatial = method == GapFillMethod.REGRESSION
                        ? new RegressionGapFiller(builder).fillGridGaps(original, i, 9, null, null, null, null)
                        : new KrigingGapFiller(builder).fillGridGaps(original, i, 9, null, null, null, null);
                VortexGrid filled = TemporalGapFallback.fill(spatial, i, flags, grids::get);
                assertArrayEquals(new float[]{i * 10, 100 + i * 10}, filled.data(), 0.001f);
                assertArrayEquals(new float[]{MISSING, MISSING}, original.data());
                // Simulate writing back to the source; metadata must exclude estimates.
                grids.set(i, filled);
            }
        }
    }

    @Test
    void preservesObservedCellsAndLeavesUnboundedGapsMissing() throws Exception {
        List<VortexGrid> grids = List.of(grid(0, MISSING, 12), grid(1, MISSING, 24),
                grid(2, 30, 36));
        List<byte[]> flags = List.of(new byte[]{1, 0}, new byte[]{1, 0}, new byte[2]);
        VortexGrid result = TemporalGapFallback.fill(grids.get(1), 1, flags, grids::get);
        assertSame(grids.get(1), result);
        assertArrayEquals(new float[]{MISSING, 24}, result.data());
    }
}
