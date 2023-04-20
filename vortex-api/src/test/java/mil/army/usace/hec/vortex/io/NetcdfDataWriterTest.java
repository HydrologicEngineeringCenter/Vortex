package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class NetcdfDataWriterTest {
    @Test
    void dayMetPrecipitation() {
        String inFile = TestUtil.getResourceFile("/netcdf/daily_prcp_2022.nc").getAbsolutePath();
        Assertions.assertNotNull(inFile);
        DataReader dataReader = DataReader.builder()
                .path(inFile)
                .variable("prcp")
                .build();
        List<VortexData> vortexDataList = dataReader.getDtos();

        String outFile = TestUtil.createTempFile("daymet_precip.nc");
        Assertions.assertNotNull(outFile);
        DataWriter dataWriter = DataWriter.builder()
                .data(vortexDataList)
                .destination(outFile)
                .build();
        dataWriter.write();
        Assertions.assertTrue(Files.exists(Path.of(outFile)));
        System.out.println(outFile);

        DataReader dataReaderEnd = DataReader.builder()
                .path(outFile)
                .variable("prcp")
                .build();
        List<VortexData> vortexDataListEnd = dataReaderEnd.getDtos();
        System.out.println("Hi");
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