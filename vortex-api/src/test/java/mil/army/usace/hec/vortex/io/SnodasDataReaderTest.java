package mil.army.usace.hec.vortex.io;

import org.gdal.gdal.gdal;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class SnodasDataReaderTest {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    boolean deleteFile(Path path) {
        try {
            Files.delete(path);
            return true;
        }
        catch(IOException exception) { return false; }
    }

    boolean deleteFolder(String folderPath) {
        try {
            Files.walk(Path.of(folderPath.substring(0, folderPath.lastIndexOf(".tar")) + "_unzip"))
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::deleteFile);
            return true;
        }
        catch(IOException exception) { return false; }
    }

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

        // Removing generated files
        AtomicBoolean filesRemoved = new AtomicBoolean(false);
        while(!filesRemoved.get()) {
            Runnable attemptRemove = () -> {
                boolean fileDeleted = deleteFile(Path.of(outFile));
                if(!fileDeleted) return;
                boolean folderDeleted = deleteFolder(path);
                if(!folderDeleted) return;
                filesRemoved.set(true);
            };

            ScheduledFuture<?> beeperHandle = scheduler.scheduleAtFixedRate(attemptRemove, 10, 10, TimeUnit.SECONDS);
            Runnable canceller = () -> beeperHandle.cancel(false);
            scheduler.schedule(canceller, 1, TimeUnit.HOURS);
        }

    } // SnodasTest
} // BilZipDataReaderTest
