package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BilZipDataReaderTest {

    @Test
    void BilZipTest() {

        String path = new File(getClass().getResource(
                "/regression/io/bil_reader/PRISM_ppt_stable_4kmD2_20170101_20170131_bil.zip").getFile()).toString();

        List<String> inFiles = new ArrayList<>();
        inFiles.add(path);
        String outFile = new File(getClass().getResource(
                "/regression/io/bil_reader/bil_zip_test.dss").getFile()).toString();

        String variableName = "ppt";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .destination(outFile)
                .build();

        importer.process();

        HecDataManager.setDefaultDSSFileName(outFile);
        HecDataManager dataManager = new HecDataManager();
        int status = dataManager.open();
        if(status != 0) {
            System.out.println("Cannot access file: " + HecDataManager.defaultDSSFileName());
        }

        HecDssCatalog catalog = new HecDssCatalog();
        List<String> dateLogs = Arrays.asList(catalog.getCatalog(false, "*"));
        int numDates = dateLogs.size();

        // Check the number of dates
        assertEquals(3, numDates);
        // Check the dates
        String[] dssPaths = new String[]{"///PRECIPITATION/31DEC2016:1200/01JAN2017:1200//",
                "///PRECIPITATION/01JAN2017:1200/02JAN2017:1200//",
                "///PRECIPITATION/30JAN2017:1200/31JAN2017:1200//"};

        Arrays.stream(dssPaths).forEach(dssPath -> Assertions.assertTrue(dateLogs.contains(dssPath)));
    }
} // BilZipDataReaderTest
