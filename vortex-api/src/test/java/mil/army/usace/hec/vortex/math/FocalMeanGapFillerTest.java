package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FocalMeanGapFillerTest {

    private static final float NO_DATA = -9999f;
    private FocalMeanGapFiller gapFiller;

    @BeforeEach
    void setUp() {
        gapFiller = FocalMeanGapFiller.newInstance();
    }

    @Test
    @DisplayName("Should return same grid when there are no gaps")
    void testNoGaps() {
        // Create a grid with no gaps (all values are 10)
        float[] data = new float[] {
                10f, 10f, 10f,
                10f, 10f, 10f,
                10f, 10f, 10f
        };

        VortexGrid grid = createTestGrid(3, 3, data);
        VortexGrid result = gapFiller.fill(grid);

        // The result should be the same as the input
        assertArrayEquals(data, result.data());
    }

    @Test
    @DisplayName("Should fill a single gap with mean of neighbors")
    void testSingleGap() {
        // Create a grid with a single gap in the center
        float[] data = new float[] {
                10f, 20f, 30f,
                40f, NO_DATA, 50f,
                60f, 70f, 80f
        };

        VortexGrid grid = createTestGrid(3, 3, data);
        VortexGrid result = gapFiller.fill(grid);

        // The mean of the 8 surrounding values is (10+20+30+40+50+60+70+80)/8 = 45
        assertEquals(45f, result.data()[4], 0.001f);
    }

    @Test
    @DisplayName("Should fill multiple gaps")
    void testMultipleGaps() {
        // Create a grid with multiple gaps
        float[] data = new float[] {
                10f, NO_DATA, 30f,
                40f, NO_DATA, 50f,
                60f, 70f, 80f
        };

        VortexGrid grid = createTestGrid(3, 3, data);
        VortexGrid result = gapFiller.fill(grid);

        // First gap mean = (10+30+40+50)/4 = 32.5
        assertEquals(32.5f, result.data()[1], 0.001f);

        // Second gap mean = (10+30+40+50+60+70+80)/7 ≈ 48.57
        assertEquals(48.57f, result.data()[4], 0.05f);
    }

    @Test
    @DisplayName("Should handle gaps at the edges")
    void testGapsAtEdges() {
        // Create a grid with gaps at edges
        float[] data = new float[] {
                NO_DATA, 20f, 30f,
                40f, 50f, 60f,
                70f, 80f, NO_DATA
        };

        VortexGrid grid = createTestGrid(3, 3, data);
        VortexGrid result = gapFiller.fill(grid);

        // Top-left gap mean = (20+40+50)/3 ≈ 36.67
        assertEquals(36.67f, result.data()[0], 0.01f);

        // Bottom-right gap mean = (50+60+80)/3 ≈ 63.33
        assertEquals(63.33f, result.data()[8], 0.01f);
    }

    @Test
    @DisplayName("Should use progressively larger kernels for isolated gaps")
    void testProgressiveLargerKernels() {
        // Create a 5x5 grid with a pattern requiring larger kernels
        float[] data = new float[25];
        // Fill with NO_DATA
        Arrays.fill(data, NO_DATA);

        // Set some values at distance 2 from center (requires kernel size 5)
        data[1] = 10f;  // Top row, second column
        data[5] = 20f;  // Second row, first column
        data[9] = 30f;  // Second row, last column
        data[15] = 40f; // Fourth row, first column
        data[19] = 50f; // Fourth row, last column
        data[23] = 60f; // Bottom row, fourth column

        VortexGrid grid = createTestGrid(5, 5, data);
        VortexGrid result = gapFiller.fill(grid);

        // Center cell should be filled with mean of the 6 values = 35
        assertEquals(35f, result.data()[12], 0.001f);
    }

    @Test
    @DisplayName("Should leave gaps unfilled when no valid neighbors exist")
    void testUnfillableGaps() {
        // Create a grid where a region has no valid neighbors
        float[] data = new float[25];
        // Fill with NO_DATA
        Arrays.fill(data, NO_DATA);

        // Set one corner to have valid values
        data[0] = 10f;
        data[1] = 20f;
        data[23] = 30f;
        data[24] = 40f;

        VortexGrid grid = createTestGrid(5, 5, data);

        GapFiller gapFiller = FocalMeanGapFiller.newInstance(3, 3, 2);
        VortexGrid result = gapFiller.fill(grid);

        // Center cell should still be NO_DATA
        assertEquals(NO_DATA, result.data()[12], 0.001f);
    }

    @Test
    @DisplayName("Should throw exception for null grid")
    void testNullGrid() {
        assertThrows(IllegalArgumentException.class, () -> gapFiller.fill(null));
    }

    @Test
    @DisplayName("Should handle empty grids")
    void testEmptyGrid() {
        VortexGrid emptyGrid = createTestGrid(0, 0, new float[0]);
        VortexGrid result = gapFiller.fill(emptyGrid);

        assertEquals(0, result.data().length);
    }

    @Test
    @DisplayName("Should handle grid with all no data values")
    void testAllNoDataGrid() {
        float[] data = new float[] {
                NO_DATA, NO_DATA, NO_DATA,
                NO_DATA, NO_DATA, NO_DATA,
                NO_DATA, NO_DATA, NO_DATA
        };

        VortexGrid grid = createTestGrid(3, 3, data);
        VortexGrid result = gapFiller.fill(grid);

        // All cells should still be NO_DATA
        for (float value : result.data()) {
            assertEquals(NO_DATA, value, 0.001f);
        }
    }

    /**
     * Helper method to create a test grid with the specified dimensions and data
     */
    private VortexGrid createTestGrid(int nx, int ny, float[] data) {
        return VortexGrid.builder()
                .nx(nx)
                .ny(ny)
                .dx(1.0)
                .dy(1.0)
                .originX(0.0)
                .originY(0.0)
                .data(data)
                .noDataValue(FocalMeanGapFillerTest.NO_DATA)
                .dataType(VortexDataType.INSTANTANEOUS)
                .build();
    }
}