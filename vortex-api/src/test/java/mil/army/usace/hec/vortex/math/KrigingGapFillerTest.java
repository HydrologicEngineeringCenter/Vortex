package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class KrigingGapFillerTest {

    private static final float NO_DATA = -9999f;
    private static final ZonedDateTime T0 = ZonedDateTime.parse("2020-01-01T00:00:00Z");

    @Test
    @DisplayName("Should return same grid when there are no gaps")
    void testNoGaps() {
        float[] data = {10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f};

        VortexGrid grid = createTestGrid(3, 3, data, T0);
        VortexGrid result = fillSingleGrid(grid, null, null, 0, 1);

        assertSame(grid, result);
    }

    @Test
    @DisplayName("Should fill a single gap using spatial neighbors only (no temporal neighbors)")
    void testSingleGapSpatialOnly() {
        float[] data = {
                10f, 20f, 30f,
                40f, NO_DATA, 50f,
                60f, 70f, 80f
        };

        VortexGrid grid = createTestGrid(3, 3, data, T0);
        VortexGrid result = fillSingleGrid(grid, null, null, 0, 1);

        assertNotEquals(NO_DATA, result.data()[4], 0.001f);
    }

    @Test
    @DisplayName("Should use temporal neighbors to inform gap filling")
    void testTemporalNeighborsUsed() {
        float[] currentData = {
                10f, 20f, 30f,
                40f, NO_DATA, 50f,
                60f, 70f, 80f
        };

        float[] prevData = {
                10f, 20f, 30f,
                40f, 44f, 50f,
                60f, 70f, 80f
        };
        float[] nextData = {
                10f, 20f, 30f,
                40f, 46f, 50f,
                60f, 70f, 80f
        };

        VortexGrid current = createTestGrid(3, 3, currentData, T0.plusHours(1));
        VortexGrid prev = createTestGrid(3, 3, prevData, T0);
        VortexGrid next = createTestGrid(3, 3, nextData, T0.plusHours(2));

        VortexGrid result = fillSingleGrid(current, prev, next, 1, 3);

        float filled = result.data()[4];
        assertNotEquals(NO_DATA, filled, 0.001f);
        assertEquals(45f, filled, 15f);
    }

    @Test
    @DisplayName("Should capture north-south gradient across a larger grid")
    void testGradientAccuracy() {
        // 10x10 grid with a north-south gradient: row * 10
        float[] data = new float[100];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                data[row * 10 + col] = row * 10f;
            }
        }

        float[] prevData = new float[100];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                prevData[row * 10 + col] = row * 10f - 2f;
            }
        }

        int[] gapIndices = {34, 35, 54, 55, 74, 75};
        for (int idx : gapIndices) {
            data[idx] = NO_DATA;
        }

        VortexGrid grid = createTestGrid(10, 10, data, T0.plusHours(1));
        VortexGrid prev = createTestGrid(10, 10, prevData, T0);

        VortexGrid result = fillSingleGrid(grid, prev, null, 1, 2);

        assertEquals(30f, result.data()[34], 10f);
        assertEquals(30f, result.data()[35], 10f);
        assertEquals(50f, result.data()[54], 10f);
        assertEquals(50f, result.data()[55], 10f);
        assertEquals(70f, result.data()[74], 10f);
        assertEquals(70f, result.data()[75], 10f);
    }

    @Test
    @DisplayName("Should fill gaps at grid edges with temporal context")
    void testEdgeGapsWithTemporalContext() {
        float[] data = new float[25];
        Arrays.fill(data, 50f);
        data[0] = NO_DATA;
        data[4] = NO_DATA;
        data[20] = NO_DATA;
        data[24] = NO_DATA;

        float[] prevData = new float[25];
        Arrays.fill(prevData, 48f);

        VortexGrid grid = createTestGrid(5, 5, data, T0.plusHours(1));
        VortexGrid prev = createTestGrid(5, 5, prevData, T0);

        VortexGrid result = fillSingleGrid(grid, prev, null, 1, 2);

        assertNotEquals(NO_DATA, result.data()[0], 0.001f);
        assertNotEquals(NO_DATA, result.data()[4], 0.001f);
        assertNotEquals(NO_DATA, result.data()[20], 0.001f);
        assertNotEquals(NO_DATA, result.data()[24], 0.001f);
    }

    @Test
    @DisplayName("Should return same grid when all cells are no-data")
    void testAllNoDataGrid() {
        float[] data = new float[9];
        Arrays.fill(data, NO_DATA);

        VortexGrid grid = createTestGrid(3, 3, data, T0);
        VortexGrid result = fillSingleGrid(grid, null, null, 0, 1);

        // Falls back to FocalMean which returns original for all-nodata
        for (float value : result.data()) {
            assertEquals(NO_DATA, value, 0.001f);
        }
    }

    @Test
    @DisplayName("Should fall back to focal mean when too few valid cells")
    void testFallbackToFocalMean() {
        float[] data = {
                10f, 20f, 30f,
                40f, NO_DATA, 50f,
                NO_DATA, NO_DATA, NO_DATA
        };

        VortexGrid grid = createTestGrid(3, 3, data, T0);
        VortexGrid result = fillSingleGrid(grid, null, null, 0, 1);

        VortexGrid focalResult = FocalMeanGapFiller.newInstance().fill(grid);
        assertArrayEquals(focalResult.data(), result.data());
    }

    @Test
    @DisplayName("Should handle empty grids")
    void testEmptyGrid() {
        VortexGrid emptyGrid = createTestGrid(0, 0, new float[0], T0);
        VortexGrid result = fillSingleGrid(emptyGrid, null, null, 0, 1);

        assertSame(emptyGrid, result);
    }

    @Test
    @DisplayName("Should fill multiple scattered gaps with temporal context")
    void testMultipleScatteredGapsWithTemporal() {
        float[] data = new float[25];
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                data[row * 5 + col] = (row + col) * 10f;
            }
        }

        float[] prevData = data.clone();
        float[] nextData = data.clone();

        data[6] = NO_DATA;
        data[12] = NO_DATA;
        data[18] = NO_DATA;

        VortexGrid grid = createTestGrid(5, 5, data, T0.plusHours(1));
        VortexGrid prev = createTestGrid(5, 5, prevData, T0);
        VortexGrid next = createTestGrid(5, 5, nextData, T0.plusHours(2));

        VortexGrid result = fillSingleGrid(grid, prev, next, 1, 3);

        assertEquals(20f, result.data()[6], 15f);
        assertEquals(40f, result.data()[12], 15f);
        assertEquals(60f, result.data()[18], 15f);
    }

    @Test
    @DisplayName("Should return a constant value when the field has no spatial variance")
    void testConstantFieldFallsBackToMean() {
        float[] data = new float[100];
        Arrays.fill(data, 25f);
        int[] gapIndices = {11, 34, 55, 78};
        for (int idx : gapIndices) {
            data[idx] = NO_DATA;
        }

        VortexGrid grid = createTestGrid(10, 10, data, T0);
        VortexGrid result = fillSingleGrid(grid, null, null, 0, 1);

        for (int idx : gapIndices) {
            assertEquals(25f, result.data()[idx], 0.001f);
        }
    }

    /**
     * Helper to call fillGridGaps directly, bypassing file I/O.
     */
    private VortexGrid fillSingleGrid(VortexGrid grid, VortexGrid prevGrid,
                                       VortexGrid nextGrid, int gridIndex, int totalGrids) {
        BatchGapFiller.Builder builder = BatchGapFiller.builder()
                .source("dummy-source.dss")
                .destination("dummy-dest.dss")
                .variables(java.util.List.of("test"))
                .method(GapFillMethod.KRIGING);

        // Use the builder to create a KrigingGapFiller, then call fillGridGaps directly
        KrigingGapFiller filler = new KrigingGapFiller(builder);
        return filler.fillGridGaps(grid, gridIndex, totalGrids, prevGrid, nextGrid, null, null);
    }

    private VortexGrid createTestGrid(int nx, int ny, float[] data, ZonedDateTime time) {
        return VortexGrid.builder()
                .nx(nx)
                .ny(ny)
                .dx(1.0)
                .dy(1.0)
                .originX(0.0)
                .originY(0.0)
                .data(data)
                .noDataValue(NO_DATA)
                .dataType(VortexDataType.INSTANTANEOUS)
                .startTime(time)
                .endTime(time.plus(Duration.ofHours(1)))
                .interval(Duration.ofHours(1))
                .build();
    }
}
