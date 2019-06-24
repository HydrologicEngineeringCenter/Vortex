package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AscDataReaderTest {

    @Test
    void PrismImportPassesRegression() {

        Path path = null;
        try {
            path = Paths.get(getClass().getClassLoader()
                    .getResource("PRISM_ppt_stable_4kmD2/PRISM_ppt_stable_4kmD2_20170101_asc.asc").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        DataReader reader = DataReader.builder()
                .path(path)
                .build();

        VortexGrid grid = reader.getDTOs().get(0);

        float[] data = grid.data();
        float max = Float.MIN_VALUE;
        for (float datum : data) {
            max = Math.max(max, datum);
        }
        assertEquals(208.298, max, 1E-3);
        assertEquals(.04167, grid.dx(), 1E-3);
        assertEquals(621, grid.ny());
        assertEquals(1405, grid.nx());
    }
}