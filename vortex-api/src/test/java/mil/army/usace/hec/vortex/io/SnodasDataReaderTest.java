package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecDssCatalog;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnodasDataReaderTest {
    private static VortexGrid grid;
    @Test
    void gdalTest() {
        Path path = Paths.get("C:\\Projects\\Vortex\\vortex-api\\build\\resources\\test\\regression\\io\\snodas_reader\\unzipFolder\\us_ssmv01025SlL00T0024TTNATS2019110105DP001.dat");
        System.out.println(path.toAbsolutePath());
        // Dataset in = gdal.Open("C:\\Projects\\Vortex\\vortex-api\\build\\resources\\test\\regression\\io\\snodas_reader\\unzipFolder\\us_ssmv01025SlL00T0024TTNATS2019110105DP001.dat");
    } // gdalTest

    @Test
    void SnodasTest() {
        String path = new File(getClass().getResource(
                "/regression/io/snodas_reader/SNODAS_20191101.tar").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(path)
                .build();
        grid = (VortexGrid) reader.getDtos().get(0);
    } // SnodasTest
} // BilZipDataReaderTest
