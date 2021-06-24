package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDSSFileDataManager;
import org.gdal.gdal.gdal;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

class SnodasDataReaderTest {

    @Test
    void gdalTest() {
        String path = new File(getClass().getResource(
                "/regression/io/snodas_reader/SNODAS_20191031.tar").getFile()).toString();
        String virtualPath = "/vsizip/" + path;
        gdal.ReadDir(virtualPath);
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

        HecDSSFileDataManager fileManager = new HecDSSFileDataManager();
        fileManager.closeFile(outFile);

        try {
            Files.delete(Path.of(outFile));
            Path folderPath = Paths.get("src/test/resources/regression/io/snodas_reader/SNODAS_20191101_unzip");
            Files.walk(folderPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); }
                        catch(IOException exception) { exception.printStackTrace(); }
                    });
        } catch(IOException exception) { throw new IllegalArgumentException(); }
    } // SnodasTest
} // BilZipDataReaderTest
