package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecDssCatalog;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnodasDataReaderTest {
    private static VortexGrid grid;

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
