package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NetcdfDataReaderTest {

    @Test
    void Sresa1bPrecipRateImport(){
        Path inFile = new File(getClass().getResource("/sresa1b_ncar_ccsm3-example.nc").getFile()).toPath();
        String variableName = "pr";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        assertEquals(32768, dtos.get(0).data().length);
    }

    @Test
    void GpcpPrecipImport(){
        Path inFile = new File(getClass().getResource("/gpcp_cdr_v23rB1_y2019_m01.nc").getFile()).toPath();
        String variableName = "precip";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        assertEquals(10368, dtos.get(0).data().length);
    }

    @Test
    void CpcTmaxImport(){
        Path inFile = new File(getClass().getResource("/tmax.2017.nc").getFile()).toPath();
        String variableName = "tmax";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(365, dtos.size());
        assertEquals(259200, dtos.get(0).data().length);
    }

    @Test
    void CpcPrecipImport(){
        Path inFile = new File(getClass().getResource("/precip.2017.nc").getFile()).toPath();
        String variableName = "precip";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(365, dtos.size());
        assertEquals(259200, dtos.get(0).data().length);
    }

    @Test
    void EcmwfEra40Import(){
        Path inFile = new File(getClass().getResource("/ECMWF_ERA-40_subset.nc").getFile()).toPath();
        String variableName = "p2t";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(62, dtos.size());
        assertEquals(10512, dtos.get(0).data().length);
    }
}