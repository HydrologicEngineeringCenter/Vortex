package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class NetcdfDataWriterTest {
    @Test
    void InstantTimeCircleTest() throws IOException {
        String outputPath = TestUtil.createTempFile("InstantTimeCircleTest.nc");
        Assertions.assertNotNull(outputPath);

        ZonedDateTime time = ZonedDateTime.of(1900, 2, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
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
                    .fileName(outputPath)
                    .fullName("temperature")
                    .shortName("temperature")
                    .description("air_temperature")
                    .startTime(time.plusHours(i))
                    .endTime(time.plusHours(i))
                    .interval(Duration.between(time, time))
                    .dataType(VortexDataType.INSTANTANEOUS)
                    .build();

            originalGrids.add(grid);
        }

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
        String outputPath = TestUtil.createTempFile("IntervalTimeCircleTest.nc");
        Assertions.assertNotNull(outputPath);

        ZonedDateTime startTime = ZonedDateTime.of(1900, 2, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
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
                    .fileName(outputPath)
                    .shortName("precipitation")
                    .description("lwe_thickness_of_precipitation_amount")
                    .startTime(startTime.plusHours(i))
                    .endTime(endTime.plusHours(i))
                    .interval(Duration.between(startTime, endTime))
                    .dataType(VortexDataType.UNDEFINED)
                    .build();

            originalGrids.add(grid);
        }

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
}