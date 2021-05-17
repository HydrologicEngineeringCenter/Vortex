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
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnodasDataReaderTest {
    private static VortexGrid grid;
    @Test
    void gdalTest() {
        String path = new File(getClass().getResource(
                "/regression/io/snodas_reader/SNODAS_20191031.tar").getFile()).toString();
        String virtualPath = "/vsizip/" + path;
        Vector x = gdal.ReadDir(virtualPath);
        
    } // gdalTest

    @Test
    void SnodasTest() {
        String path = Paths.get("src/test/resources/regression/io/snodas_reader/SNODAS_20191101.tar").toAbsolutePath().toString();

        List<String> inFiles = new ArrayList<>();
        inFiles.add(path);
        String outFile = Paths.get("src/test/resources/regression/io/snodas_reader/snodas_test.dss").toAbsolutePath().toString();

        List<String> availableVariables = Arrays.asList("SWE", "Snow Depth", "Snow Melt Runoff at the Base of the Snow Pack", "Sublimation from the Snow Pack",
                "Sublimation of Blowing Snow", "Solid Precipitation", "Liquid Precipitation", "Snow Pack Average Temperature");

        for(String variableName : availableVariables) {
            List<String> variables = new ArrayList<>();
            variables.add(variableName);

            BatchImporter importer = BatchImporter.builder()
                    .inFiles(inFiles)
                    .variables(variables)
                    .destination(outFile)
                    .build();

            importer.process();
        } // Import all variables
    } // SnodasTest
} // BilZipDataReaderTest
