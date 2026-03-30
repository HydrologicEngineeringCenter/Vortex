package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeographicProcessorTest {

    @Test
    void CellSizeOnlyResample() {
        String inFile = new File(getClass().getResource("/tif_to_dss/hms_cn_grid.tif").getFile()).toString();

        try (DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("hms_cn_grid")
                .build()) {

            VortexGrid original = (VortexGrid) reader.getDto(0);
            double targetCellSize = original.dx() * 2;

            GeographicProcessor processor = new GeographicProcessor(Map.of(
                    "targetCellSize", String.valueOf(targetCellSize),
                    "targetCellSizeUnits", "Feet"
            ));

            VortexGrid resampled = processor.process(original);

            assertEquals(targetCellSize, resampled.dx(), 1e-6);
            assertEquals(targetCellSize, Math.abs(resampled.dy()), 1e-6);
            assertTrue(resampled.nx() < original.nx());
            assertTrue(resampled.ny() < original.ny());
            assertEquals(resampled.data().length, resampled.nx() * resampled.ny());
        } catch (Exception e) {
            fail(e);
        }
    }
}
