package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
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

        List<VortexGrid> dtos = reader.getDTOs().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
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

        List<VortexGrid> dtos = reader.getDTOs().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
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

        List<VortexGrid> dtos = reader.getDTOs().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
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

        List<VortexGrid> dtos = reader.getDTOs().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
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

        List<VortexGrid> dtos = reader.getDTOs().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(62, dtos.size());
        assertEquals(10512, dtos.get(0).data().length);
    }

    @Test
    void DatasetTest() throws IOException {
        //String location = new File(getClass().getResource("/sresa1b_ncar_ccsm3-example.nc").getFile()).toString();
        String location = new File("D:/data/data.nc").toString();
        NetcdfFile ncf = NetcdfFile.openInMemory(location);
        NetcdfDataset nc = new NetcdfDataset(ncf);
        Formatter errlog = new Formatter();
        try (FeatureDataset fdataset = FeatureDatasetFactoryManager.wrap(FeatureType.ANY, nc, null, errlog)) {
            if (fdataset == null) {
                System.out.printf("**failed on %s %n --> %s %n", location, errlog);
                return;
            }

            FeatureType ftype = fdataset.getFeatureType();

            if (ftype == FeatureType.GRID) {
                assert (fdataset instanceof GridDataset);
                GridDataset griddedDataset = (GridDataset) fdataset;
                System.out.println(griddedDataset);

            } else if (ftype == FeatureType.RADIAL) {
                assert (fdataset instanceof RadialDatasetSweep);
                RadialDatasetSweep radialDataset = (RadialDatasetSweep) fdataset;

            } else if (ftype.isPointFeatureType()) {
                assert fdataset instanceof FeatureDatasetPoint;
                FeatureDatasetPoint pointDataset = (FeatureDatasetPoint) fdataset;
            }
        }
    }
}