package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class NetcdfDataWriterTest {
    @Test
    void DssToNetcdf() {
        String dssPath = TestUtil.getResourceFile("/precip.dss").getAbsolutePath();
        DataReader dataReader = DataReader.builder()
                .path(dssPath)
                .variable("*")
                .build();
        List<VortexData> originalList = dataReader.getDtos();

        String outFile = TestUtil.createTempFile("precip.nc");
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(originalList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
        System.out.println("Output Path: " + outFile);
    }

    @Test
    void DayMetPrecipitation() {
        String ncPath = TestUtil.getResourceFile("/netcdf/daily_prcp_2022.nc").getAbsolutePath();
        runFullCycleTest(ncPath, "prcp");
    }

    @Test
    void MRMS() {
        String ncPath = TestUtil.getResourceFile("/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getAbsolutePath();
        runFullCycleTest(ncPath, "GaugeCorrQPE01H_altitude_above_msl");
    }

    @Test
    void AorcPrecip() {
        String ncPath = TestUtil.getResourceFile("/AORC_APCP_MARFC_1984010100.nc4").getAbsolutePath();
        runFullCycleTest(ncPath, "APCP_surface");
    }

    @Test
    void AorcTemperature() {
        String ncPath = TestUtil.getResourceFile("/AORC_TMP_MARFC_1984010100.nc4").getAbsolutePath();
        runFullCycleTest(ncPath, "TMP_2maboveground");
    }

    @Test
    void CpcPrecip() {
        String ncPath = TestUtil.getResourceFile("/precip.2017.nc").getAbsolutePath();
        runFullCycleTest(ncPath, "precip");
    }

    @Test
    void CpcTemperature() {
        String ncPath = TestUtil.getResourceFile("/tmax.2017.nc").getAbsolutePath();
        runFullCycleTest(ncPath, "tmax");
    }

    void runFullCycleTest(String ncPath, String variableName) {
        DataReader dataReader = DataReader.builder()
                .path(ncPath)
                .variable(variableName)
                .build();
        List<VortexData> originalList = dataReader.getDtos();

        String outFile = TestUtil.createTempFile(ncPath.substring(ncPath.lastIndexOf(System.getProperty("file.separator")) + 1));
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(originalList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
        System.out.println("Output Path: " + outFile);

        DataReader dataReaderEnd = DataReader.builder()
                .path(outFile)
                .variable(variableName)
                .build();
        List<VortexData> generatedList = dataReaderEnd.getDtos();

        for (int i = 0; i < originalList.size(); i++) {
            VortexGrid original = (VortexGrid) originalList.get(i);
            VortexGrid generated = (VortexGrid) generatedList.get(i);
            boolean equals = original.equals(generated);
            Assertions.assertEquals(original, generated);
        }
    }

    @Test
    void dssFile() {
        String inFile = "/Users/work/Documents-Local/HMS-Dataset/Transposing_Gridded_Precipitation_Final/Punx_HMS/data/precip.dss";
        DataReader dataReader = DataReader.builder()
                .path(inFile)
                .variable("*")
                .build();
        List<VortexData> vortexDataList = dataReader.getDtos();

        String outFile = "/Users/work/Documents-Local/HMS-Dataset/Transposing_Gridded_Precipitation_Final/Punx_HMS/data/precip.nc";
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(vortexDataList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
    }

    @Test
    void mrms() {
        //Need to add conditional logic in WktParser for SpatialReference::IsGeographic
        File inFile = TestUtil.getResourceFile("/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2");
        Assertions.assertNotNull(inFile);
        DataReader dataReader = DataReader.builder()
                .path(inFile.getAbsolutePath())
                .variable("GaugeCorrQPE01H_altitude_above_msl")
                .build();
        List<VortexData> vortexDataList = dataReader.getDtos();

        String outFile = TestUtil.createTempFile("mrms.nc4");
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(vortexDataList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
        System.out.println(outFile);
    }

    @Test
    void aorc() {
        //Need to add conditional logic in WktParser for SpatialReference::IsGeographic
        File inFile = TestUtil.getResourceFile("/AORC_APCP_MARFC_1984010100.nc4");
        Assertions.assertNotNull(inFile);
        DataReader dataReader = DataReader.builder()
                .path(inFile.getAbsolutePath())
                .variable("APCP_surface")
                .build();
        List<VortexData> vortexDataList = dataReader.getDtos();

        String outFile = TestUtil.createTempFile("mrms.nc4");
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(vortexDataList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
    }

    @Test
    void write() {
        File inFile = TestUtil.getResourceFile("/netcdf/daily_vp_2022.nc");
        Assertions.assertNotNull(inFile);
        DataReader dataReader = DataReader.builder()
                .path(inFile.getAbsolutePath())
                .variable("vp")
                .build();
        List<VortexData> vortexDataList = dataReader.getDtos();
        
        String outFile = TestUtil.createTempFile("test.nc4");
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(vortexDataList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
    }
}