package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetcdfDataReaderTest {

    @Test
    void Sresa1bPrecipRateImport(){
        String inFile = new File(getClass().getResource("/sresa1b_ncar_ccsm3-example.nc").getFile()).toString();
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
        String inFile = new File(getClass().getResource("/gpcp_cdr_v23rB1_y2019_m01.nc").getFile()).toString();
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
        String inFile = new File(getClass().getResource("/tmax.2017.nc").getFile()).toString();
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
        String inFile = new File(getClass().getResource("/precip.2017.nc").getFile()).toString();
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
        String inFile = new File(getClass().getResource("/ECMWF_ERA-40_subset.nc").getFile()).toString();
        String variableName = "p2t";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(62, dtos.size());
        assertEquals(10512, dtos.get(0).data().length);
    }

    @Test
    void GpmSubImport(){
        String inFile = new File(getClass().getResource("/3B-HHR.MS.MRG.3IMERG.20170103-S110000-E112959.0660.V06B.HDF5.SUB.hdf5").getFile()).toString();
        String variableName = "precipitationCal";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
    }

    @Test
    void Sfav2Import(){
        String inFile = new File(getClass().getResource("/sfav2_CONUS_24h_2010030112.nc").getFile()).toString();
        String variableName = "Data";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);
        assertTrue(grid.startTime().isEqual(ZonedDateTime.of(2010, 2, 28, 12, 0, 0, 0, ZoneId.of("UTC"))));
        assertTrue(grid.endTime().isEqual(ZonedDateTime.of(2010, 3, 1, 12, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test
    void AorcTempImport(){
        String inFile = new File(getClass().getResource("/AORC_TMP_MARFC_1984010100.nc4").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("TMP_2maboveground")
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);
        assertTrue(grid.startTime().isEqual(ZonedDateTime.of(1984, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))));
        assertTrue(grid.endTime().isEqual(ZonedDateTime.of(1984, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test
    void AorcPrecipImport(){
        String inFile = new File(getClass().getResource("/AORC_APCP_MARFC_1984010100.nc4").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("APCP_surface")
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);
        assertTrue(grid.startTime().isEqual(ZonedDateTime.of(1983, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC"))));
        assertTrue(grid.endTime().isEqual(ZonedDateTime.of(1984, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Disabled
    @Test
    void CmipPrecipImport(){
        String inFile = new File("D:/data/CMIP/Extraction_pr.nc").toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("pr")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        float[] data = vortexGrid.data();

        List<Double> values = new ArrayList<>();
        for (int i = 1; i < data.length; i++) {
            if (!Float.isNaN(data[i])) {
                values.add((double) data[i]);
            }
        }

        double max = Collections.max(values);
        double min = Collections.min(values);

        assertEquals(66.21134185791016, max, 1E-5);
        assertEquals(0.04108993336558342, min, 1E-5);
    }

    @Disabled
    @Test
    void ArizonaSweDaily(){
        String inFile = new File(getClass().getResource("/01.nc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("SWE")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(2020, 3, 1, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(2020, 3, 2, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }
}