package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.convert.NetcdfGridWriter;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static mil.army.usace.hec.vortex.io.NetcdfDataWriter.*;

class NetcdfDataWriterTest {
    @Test
    void IntervalTimeTest() {
        // TODO: Clean up dev tests
        String inFile = TestUtil.getResourceFile("/cordex/precip.nc").getAbsolutePath().toString();
        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("pr")
                .build();
        List<VortexData> originalGrids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        String outFile = TestUtil.createTempFile("precip-cordex.nc");
        DataWriter writer = DataWriter.builder()
                .data(originalGrids)
                .destination(outFile)
                .build();
        writer.write();

        DataReader readerCircle = DataReader.builder()
                .path(outFile)
                .variable("precipitation")
                .build();
        List<VortexData> generatedGrids = readerCircle.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }
    }

    @Test
    void InstantTimeTest() {
        // TODO: Get rid before final submission
        String inFile = "/Users/work/Documents-Local/HMS-Dataset/NetcdfWriterTest/temp-Tom.dss";
        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("*")
                .build();
        List<VortexData> originalGrids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        String outFile = "/Users/work/Documents-Local/HMS-Dataset/NetcdfWriterTest/temp-Tom.nc";
        DataWriter writer = DataWriter.builder()
                .data(originalGrids)
                .destination(outFile)
                .build();
        writer.write();

        DataReader readerCircle = DataReader.builder()
                .path(outFile)
                .variable("temperature")
                .build();
        List<VortexData> generatedGrids = readerCircle.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }
        System.out.println("Hi");
    }

    @Test
    void InstantTimeCircleTest() throws IOException {
        ZonedDateTime time = ZonedDateTime.of(1900,2,2,0,0,0,0, ZoneId.of("Z"));
        List<VortexData> originalGrids = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            float[] data = new float[9];
            Arrays.fill(data, i);

            VortexGrid grid = VortexGrid.builder()
                    .originX(0).originY(0)
                    .nx(3).ny(3)
                    .dx(1).dy(1)
                    .wkt(WktFactory.createWkt(new LambertConformalConicEllipse()))
                    .data(data)
                    .noDataValue(-9999)
                    .units("degC")
                    .shortName("temperature")
                    .description("air_temperature")
                    .startTime(time.plusHours(i))
                    .endTime(time.plusHours(i))
                    .interval(Duration.between(time, time))
                    .dataType(VortexDataType.INSTANTANEOUS)
                    .build();

            originalGrids.add(grid);
        }

        String outputPath = TestUtil.createTempFile("InstantTimeCircleTest.nc");
        Assertions.assertNotNull(outputPath);

        DataWriter writer = DataWriter.builder()
                .data(originalGrids)
                .destination(outputPath)
                .build();

        writer.write();

        DataReader reader = DataReader.builder()
                .variable("temperature")
                .path(outputPath)
                .build();

        List<VortexData> generatedGrids = reader.getDtos();

        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }

        Files.deleteIfExists(Path.of(outputPath));
    }

    @Test
    void IntervalTimeCircleTest() throws IOException {
        ZonedDateTime startTime = ZonedDateTime.of(1900,2,2,0,0,0,0, ZoneId.of("Z"));
        ZonedDateTime endTime = startTime.plusHours(1);
        List<VortexData> originalGrids = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            float[] data = new float[9];
            Arrays.fill(data, i);

            VortexGrid grid = VortexGrid.builder()
                    .originX(0).originY(0)
                    .nx(3).ny(3)
                    .dx(1).dy(1)
                    .wkt(WktFactory.createWkt(new LambertConformalConicEllipse()))
                    .data(data)
                    .noDataValue(-9999)
                    .units("m")
                    .shortName("precipitation")
                    .description("lwe_thickness_of_precipitation_amount")
                    .startTime(startTime.plusHours(i))
                    .endTime(endTime.plusHours(i))
                    .interval(Duration.between(startTime, endTime))
                    .dataType(VortexDataType.ACCUMULATION)
                    .build();

            originalGrids.add(grid);
        }

        String outputPath = TestUtil.createTempFile("IntervalTimeCircleTest.nc");
        Assertions.assertNotNull(outputPath);

        DataWriter writer = DataWriter.builder()
                .data(originalGrids)
                .destination(outputPath)
                .build();

        writer.write();

        DataReader reader = DataReader.builder()
                .variable("precipitation")
                .path(outputPath)
                .build();

        List<VortexData> generatedGrids = reader.getDtos();

        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }

        Files.deleteIfExists(Path.of(outputPath));
    }

    @Test
    void AppendTestNew() {
        List<String> inFiles = List.of("/Users/work/Documents-Local/HMS-Dataset/NetcdfWriterTest/temp-Tom.dss");
        String outputPath = TestUtil.createTempFile("tempAppend.nc");

        List<String> variables = inFiles.stream()
                .map(DataReader::getVariables)
                .flatMap(Collection::stream).distinct().sorted().collect(Collectors.toList());
        List<String> firstBatchVariables = variables.subList(0, 10);

        BatchImporter firstBatchImporter = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(firstBatchVariables)
                .destination(outputPath)
                .build();
        firstBatchImporter.process();

        Map<String, String> writeOptions = new HashMap<>();
        writeOptions.put("isOverride", "false");
        List<String> secondBatchVariables = variables.subList(10, variables.size());
        BatchImporter secondBatchImporter = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(secondBatchVariables)
                .destination(outputPath)
                .writeOptions(writeOptions)
                .build();
        secondBatchImporter.process();
        System.out.println(outputPath);
    }

    @Test
    void AppendTest() {
        List<String> inFiles = List.of("/Users/work/Documents-Local/HMS-Dataset/NetcdfWriterTest/temp-Tom.dss");
        String outputPath = TestUtil.createTempFile("tempAppend.nc");

        List<String> variables = inFiles.stream()
                .map(DataReader::getVariables)
                .flatMap(Collection::stream).distinct().sorted().collect(Collectors.toList());
        List<String> firstBatchVariables = variables.subList(0, 10);
        List<String> secondBatchVariables = variables.subList(10, variables.size());

        BatchImporter firstBatchImporter = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(firstBatchVariables)
                .destination(outputPath)
                .build();
        firstBatchImporter.process();

        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter.builder()
                .setNewFile(false)
                .setFormat(NETCDF_FORMAT)
                .setLocation(outputPath)
                .setChunker(chunker);

        List<VortexGrid> secondBatchGridList = secondBatchVariables.stream()
                .map(var -> DataReader.builder().path(inFiles.get(0)).variable(var).build())
                .map(DataReader::getDtos)
                .flatMap(List::stream)
                .map(VortexGrid.class::cast)
                .collect(Collectors.toList());

        NetcdfGridWriter gridWriter = new NetcdfGridWriter(secondBatchGridList);
        gridWriter.appendData(writerBuilder);
        System.out.println(outputPath);
    }

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