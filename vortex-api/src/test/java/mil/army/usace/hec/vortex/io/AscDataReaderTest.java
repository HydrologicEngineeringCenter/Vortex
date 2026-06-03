package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AscDataReaderTest {

    @Test
    void PrismImportPassesRegression() throws Exception {

        String path = new File(getClass().getResource(
                "/regression/io/asc_reader/PRISM_ppt_stable_4kmD2_20170101_asc.asc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .variable("ppt")
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);

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

    @Test
    void QpfHourlyImport() throws Exception {
        String path = new File(getClass().getResource("/QPF_1HR_AZ_19121900.asc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .variable("precipitation")
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);
        assertEquals(grid.endTime(), ZonedDateTime.of(2019, 12,19, 0, 0, 0, 0, ZoneId.of("UTC")));
    }

    @Test
    void Atlas14Import() throws Exception {
        String path = new File(getClass().getResource("/orb100yr24ha/orb100yr24ha.asc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .variable("orb100yr24ha")
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);
        assertNotNull(grid);
    }

    @Test
    void TwoIsoDatesImport() throws Exception {
        String path = new File(getClass().getResource("/2000-01-01T0000_2000-01-01T0100.tif").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);

        assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), grid.startTime());
        assertEquals(ZonedDateTime.of(2000, 1, 1, 1, 0, 0, 0, ZoneId.of("Z")), grid.endTime());
    }

    @Test
    void missingFile_surfacesAsDataReadException() throws Exception {
        // A non-existent .asc path is routed to AscDataReader; GDAL fails to
        // open it. The reader should surface that as a DataReadException with
        // Kind.IO_ERROR and the path embedded in the message — not silently
        // return an empty list as it did before this branch.
        DataReader reader = DataReader.builder()
                .path("/does/not/exist/missing.asc")
                .variable("ppt")
                .build();

        try {
            reader.getDtos();
            fail("expected DataReadException for missing .asc but got no exception");
        } catch (DataReadException e) {
            assertEquals(DataReadException.Kind.IO_ERROR, e.getKind());
            assertTrue(e.getMessage().contains("missing.asc"),
                    "expected path in message but got: " + e.getMessage());
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            // GDAL native lib not available in this environment — the test
            // can't exercise the GDAL failure path. Skip rather than fail.
            Assumptions.abort("GDAL native library not available: " + e.getMessage());
        }
    }

    @Test
    void OneIsoDateImport() throws Exception {
        String path = new File(getClass().getResource("/2000-01-01T0000.tif").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);

        assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), grid.startTime());
        assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), grid.endTime());
    }
}