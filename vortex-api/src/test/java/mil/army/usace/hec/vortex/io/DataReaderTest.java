package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DataReaderTest {

    @Test
    void ExtentsOfMrmsGridAreCorrect() {
        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();
        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());
        VortexGrid dto = dtos.get(0);
        double left = dto.originX();
        double top = dto.originY();
        double bottom = dto.originY() + dto.dy() * dto.ny();
        double right = dto.originX() + dto.dx() * dto.nx();
        assertEquals(55, top, 1E-5);
        assertEquals(20, bottom, 1E-5);
        assertEquals(-130, left, 1E-5);
        assertEquals(-60, right, 1E-5);
    }

    @Test
    void isVariableRequiredForReader() {
        assertFalse(DataReader.isVariableRequired("file.asc"));
        assertFalse(DataReader.isVariableRequired("file.asc.zip"));
        assertFalse(DataReader.isVariableRequired("file.bil"));
        assertFalse(DataReader.isVariableRequired("file.bil.zip"));
        assertFalse(DataReader.isVariableRequired("file.tif"));
        assertFalse(DataReader.isVariableRequired("file.tiff"));
        assertTrue(DataReader.isVariableRequired("file.nc4"));
        assertTrue(DataReader.isVariableRequired("file.hdf"));
    }
}