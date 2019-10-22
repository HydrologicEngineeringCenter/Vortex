package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecDssCatalog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AscZipDataReaderTest {

    @Test
    void AscZipTest() {

        String path = new File(getClass().getResource(
                "/regression/io/asc_reader/PRISM_ppt_stable_4kmD2_20170101_20170131_asc.zip").getFile()).toString();

        List<String> inFiles = new ArrayList<>();
        inFiles.add(path);
        String outFile = new File(getClass().getResource(
                "/regression/io/asc_reader/asc_zip_test.dss").getFile()).toString();

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
        String dateLogs[] = catalog.getCatalog(false, "*");
        int numDates = dateLogs.length;

        // Check the number of dates
        assertEquals(3, numDates);
        // Check the dates
        assertEquals("///PRECIPITATION/31DEC2016:1200/01JAN2017:1200//", dateLogs[0]);
        assertEquals("///PRECIPITATION/01JAN2017:1200/02JAN2017:1200//", dateLogs[1]);
        assertEquals("///PRECIPITATION/30JAN2017:1200/31JAN2017:1200//", dateLogs[2]);
    }
} // AscZipDataReaderTest
