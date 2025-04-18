package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FocalMeanGapFillerTest {

    @Test
    void testGapFill5x5() {
        float[] data = new float[] {
                1, 1, 0, 1, 1,
                1, 1, 0, 1, 1,
                0, 0, 0, 0, 0,
                3, 3, 0, 3, 3,
                3, 3, 0, 3, 3
        };

        float[] filled = new float[] {
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                2, 2, 2, 2, 2,
                3, 3, 3, 3, 3,
                3, 3, 3, 3, 3
        };

        VortexGrid grid = VortexGrid.builder()
                .nx(5)
                .ny(5)
                .data(data)
                .noDataValue(0)
                .build();

        FocalMeanGapFiller filler = FocalMeanGapFiller.newInstance();

        filler.fill(grid);

        assertArrayEquals(filled, data);
    }
}