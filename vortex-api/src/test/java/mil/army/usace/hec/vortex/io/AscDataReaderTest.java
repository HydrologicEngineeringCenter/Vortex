package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AscDataReaderTest {

    @Test
    void PrismImportPassesRegression() {

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
    void QpfHourlyImport(){
        String path = new File(getClass().getResource("/QPF_1HR_AZ_19121900.asc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .variable("precipitation")
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);
        assertEquals(grid.endTime(), ZonedDateTime.of(2019, 12,19, 0, 0, 0, 0, ZoneId.of("UTC")));
    }

    @Test
    void Atlas14Import(){
        String path = new File(getClass().getResource("/orb100yr24ha/orb100yr24ha.asc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .variable("orb100yr24ha")
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);
        assertNotNull(grid);
    }

    @Test
    void TwoIsoDatesImport(){
        String path = new File(getClass().getResource("/2000-01-01T0000_2000-01-01T0100.tif").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);

        assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), grid.startTime());
        assertEquals(ZonedDateTime.of(2000, 1, 1, 1, 0, 0, 0, ZoneId.of("Z")), grid.endTime());
    }

    @Test
    void OneIsoDateImport(){
        String path = new File(getClass().getResource("/2000-01-01T0000.tif").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .build();

        VortexGrid grid = (VortexGrid) reader.getDtos().get(0);

        assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), grid.startTime());
        assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), grid.endTime());
    }
}