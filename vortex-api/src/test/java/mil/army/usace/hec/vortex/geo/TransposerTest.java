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
                "/normalizer/qpe.dss").getFile()).toPath();
        String variableName = "///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> grids = reader.getDTOs().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);

        Transposer transposer = Transposer.builder()
                .grid(grid)
                .angle(30)
                .debug(true)
                .tempDir("C:/Temp")
                .build();

        transposer.transpose();
    }
}