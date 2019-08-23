package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TransposerTest {

    @Test
    void testTranspose(){
        Path inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toPath();
        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> grids = reader.getDTOs().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);

        Transposer transposer = Transposer.builder()
                .grid(grid)
                .angle(89)
                .build();

        transposer.transpose();
    }
}