package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DssDataType;
import hec.heclib.dss.HecDSSFileDataManager;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import mil.army.usace.hec.vortex.TestUtil;
import org.gdal.gdal.gdal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Disabled
class SnodasDataReaderTest {
    private static final String folder = "/regression/io/snodas_reader/";
    private static final File inFile = TestUtil.getResourceFile(folder + "SNODAS_20191031.tar");
    private static final File outFile = TestUtil.getResourceFile(folder + "snodas_test.dss");

    @BeforeAll
    static void setUp() {
        if(inFile == null || outFile == null) Assertions.fail();
        String virtualPath = "/vsizip/" + inFile;
        gdal.ReadDir(virtualPath);

        List<String> inFiles = Collections.singletonList(inFile.getAbsolutePath());
        List<String> variables = new ArrayList<>(DataReader.getVariables(inFile.getAbsolutePath()));

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .destination(outFile.getAbsolutePath())
                .build();

        importer.process();

        HecDSSFileDataManager fileManager = new HecDSSFileDataManager();
        fileManager.closeFile(outFile.getAbsolutePath());
    }

    @Test
    void swe() {
        if(outFile == null) Assertions.fail();
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getAbsolutePath());
        griddedData.setPathname("///SWE/31OCT2019:0600///");

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.INST_VAL.value(), gridInfo.getDataType());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getEndTime());
        Assertions.assertEquals(-4257.471, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(30693.0, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-9999.0, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void snowDepth() {
        if(outFile == null) Assertions.fail();
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getAbsolutePath());
        griddedData.setPathname("///SNOW DEPTH/31OCT2019:0600///");

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.INST_VAL.value(), gridInfo.getDataType());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getEndTime());
        Assertions.assertEquals(-4257.028, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(32767.0, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-9999.0, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void snowMeltRunoff() {
        if(outFile == null) Assertions.fail();
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getAbsolutePath());
        griddedData.setPathname("///LIQUID WATER/31OCT2019:0600/01NOV2019:0600//");

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("M", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("1 November 2019, 06:00", gridInfo.getEndTime());
        Assertions.assertEquals(-0.090, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(0.024, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-0.100, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void snowSublimation() {
        if(outFile == null) Assertions.fail();
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getAbsolutePath());
        griddedData.setPathname("///SNOW SUBLIMATION/31OCT2019:0600/01NOV2019:0600//");

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("M", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("1 November 2019, 06:00", gridInfo.getEndTime());
        Assertions.assertEquals(-0.090, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(0.001, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-0.100, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void precipitation() {
        if(outFile == null) Assertions.fail();
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getAbsolutePath());
        griddedData.setPathname("///PRECIPITATION/31OCT2019:0600/01NOV2019:0600//");

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("1 November 2019, 06:00", gridInfo.getEndTime());
        Assertions.assertEquals(-492.912, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(311.8, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-999.9, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void temperature() {
        if(outFile == null) Assertions.fail();
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getAbsolutePath());
        griddedData.setPathname("///TEMPERATURE/31OCT2019:0600///");

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("DEG C", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.INST_VAL.value(), gridInfo.getDataType());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("31 October 2019, 06:00", gridInfo.getEndTime());
        Assertions.assertEquals(-9899.17, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(-0.15, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-10272.15, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }
}
