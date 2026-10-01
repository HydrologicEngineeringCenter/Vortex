package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class GeographicProcessorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t"})
    void WarnWhenProcessingWithoutProjectionEvenWithNoGeoOptions(String wkt) {
        GeographicProcessor processor = new GeographicProcessor(Map.of());
        VortexGrid grid = VortexGrid.builder().fileName("missing.asc").wkt(wkt).build();
        List<LogRecord> warnings = new ArrayList<>();
        Logger logger = Logger.getLogger(GeographicProcessor.class.getName());
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record);
                }
            }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(handler);
        try {
            assertSame(grid, processor.process(grid));
            assertSame(grid, processor.process(grid));
            assertEquals(1, warnings.size(), "Warn once per source, not once per time step");
            assertTrue(warnings.get(0).getMessage().contains("missing.asc"));
            assertTrue(warnings.get(0).getMessage().contains("no projection"));
            processor.process(VortexGrid.toBuilder(grid).fileName("other.asc").build());
            assertEquals(2, warnings.size(), "Each unprojected source should be identified");
            processor.process(VortexGrid.toBuilder(grid).fileName("projected.asc")
                    .wkt(WktFactory.fromEpsg(4267)).build());
            assertEquals(2, warnings.size(), "A defined geographic CRS should not warn");
        } finally {
            logger.removeHandler(handler);
        }
    }

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
