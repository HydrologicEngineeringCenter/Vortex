package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BilDataReaderTest {
    private static VortexGrid grid;

    static {
        String path = new File(AscDataReaderTest.class.getResource(
                "/regression/io/bil_reader/PRISM_ppt_stable_4kmD2_20170101_bil.bil").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .build();

        grid = (VortexGrid) reader.getDtos().get(0);
    }

    @Test
    void PrismPrecipImportMaxValuePassesRegression() {
        float[] data = grid.data();
        float max = Float.MIN_VALUE;
        for (float datum : data) {
            max = Math.max(max, datum);
        }
        assertEquals(208.298, max, 1E-3);
    }

    @Test
    void PrismPrecipImportDxPassesRegression() {
        assertEquals(.04167, grid.dx(), 1E-3);
    }

    @Test
    void PrismPrecipImportNyPassesRegression() {
        assertEquals(621, grid.ny());
    }

    @Test
    void PrismPrecipImportNxPassesRegression() {
        assertEquals(1405, grid.nx());
    }

}