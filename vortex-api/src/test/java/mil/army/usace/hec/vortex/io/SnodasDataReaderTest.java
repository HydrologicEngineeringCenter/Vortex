package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDSSFileDataManager;
import org.gdal.gdal.gdal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

class SnodasDataReaderTest {
    private static final String snodasReaderFolder = "src/test/resources/regression/io/snodas_reader/";
    private static final Path tarPath = Paths.get(snodasReaderFolder + "SNODAS_20191031.tar");
    private static final Path dssPath = Paths.get(snodasReaderFolder + "snodas_test.dss");

    @BeforeAll
    static void setup() {
        if(Files.notExists(tarPath)) {
            System.out.println("File not found: " + tarPath);
            Assertions.fail();
        }

        String virtualPath = "/vsizip/" + tarPath.toAbsolutePath();
        gdal.ReadDir(virtualPath);
    }

    @Test
    void SnodasTest() {
        BatchImporter importer = BatchImporter.builder()
                .inFiles(Collections.singletonList(tarPath.toAbsolutePath().toString()))
                .variables(snodasVariablesAll())
                .destination(dssPath.toAbsolutePath().toString())
                .build();
        importer.process();
        Assertions.assertTrue(true);
    }

    @AfterAll
    static void clean() {
        HecDSSFileDataManager fileManager = new HecDSSFileDataManager();
        fileManager.closeFile(dssPath.toAbsolutePath().toString());

        try {
            Files.deleteIfExists(dssPath);
            String unzipFolder = snodasReaderFolder + tarPath.getFileName() + "_unzip";
            Files.walk(Path.of(unzipFolder))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }

    private List<String> snodasVariablesAll() {
        List<String> snodasVariables = new ArrayList<>();
        snodasVariables.add("SWE");
        snodasVariables.add("Snow Depth");
        snodasVariables.add("Snow Melt Runoff at the Base of the Snow Pack");
        snodasVariables.add("Sublimation from the Snow Pack");
        snodasVariables.add("Sublimation of Blowing Snow");
        snodasVariables.add("Solid Precipitation");
        snodasVariables.add("Liquid Precipitation");
        snodasVariables.add("Snow Pack Average Temperature");
        return snodasVariables;
    }
}
