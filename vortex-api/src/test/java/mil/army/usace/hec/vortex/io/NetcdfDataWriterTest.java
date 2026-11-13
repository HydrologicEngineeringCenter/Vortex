package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

class NetcdfDataWriterTest {
    private static final Logger LOGGER = Logger.getLogger(NetcdfDataWriterTest.class.getName());

    @Test
    void InstantTimeCircleTest() {
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

        try (DataReader reader = DataReader.builder()
                .variable("temperature")
                .path(outputPath)
                .build()) {

            List<VortexData> generatedGrids = reader.getDtos();

            for (int i = 0; i < originalGrids.size(); i++) {
                Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            Assertions.fail();
        }


        try {
            Files.deleteIfExists(Path.of(outputPath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            Assertions.fail();
        }
    }

    @Test
    void IntervalTimeCircleTest() {
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

        try (DataReader reader = DataReader.builder()
                .variable("precipitation")
                .path(outputPath)
                .build()) {

            List<VortexData> generatedGrids = reader.getDtos();

            for (int i = 0; i < originalGrids.size(); i++) {
                Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            Assertions.fail();
        }

        try {
            Files.deleteIfExists(Path.of(outputPath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            Assertions.fail();
        }
    }

    /**
     * An export too large to hold in memory at once is written one batch of time steps at a time.
     * Every write() call to the same destination must add its time steps to the file. Keeping only
     * the time steps of the last call is silent data loss.
     */
    @Test
    void BatchedWriteKeepsEveryTimeStepTest() {
        String outputPath = TestUtil.createTempFile("BatchedWriteKeepsEveryTimeStepTest.nc");
        Assertions.assertNotNull(outputPath);

        List<VortexData> originalGrids = intervalGrids("swe", 5);

        for (int start = 0; start < originalGrids.size(); start += 2) {
            List<VortexData> batch = originalGrids.subList(start, Math.min(start + 2, originalGrids.size()));
            write(outputPath, batch);
        }

        List<VortexData> generatedGrids = readGrids(outputPath, "swe");
        Assertions.assertEquals(originalGrids.size(), generatedGrids.size(),
                "every time step written in every batch must be readable");

        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }

        delete(outputPath);
    }

    /**
     * Grids of several variables share a time step. The record list indexing the time dimension is
     * built from every grid of the batch, so repeated times must collapse into one record.
     */
    @Test
    void MultipleVariablesSameTimeStepsTest() {
        String outputPath = TestUtil.createTempFile("MultipleVariablesSameTimeStepsTest.nc");
        Assertions.assertNotNull(outputPath);

        List<VortexData> originalGrids = new ArrayList<>(intervalGrids("swe", 3));
        originalGrids.addAll(intervalGrids("precipitation", 3));

        write(outputPath, originalGrids);

        Assertions.assertEquals(2, DataReader.getVariables(outputPath).size());

        for (String variable : List.of("swe", "precipitation")) {
            Assertions.assertEquals(3, readGrids(outputPath, variable).size(),
                    variable + " must hold one record for each time step");
        }

        delete(outputPath);
    }

    /**
     * Two variables written in the same batch share its time steps, and a destination written in
     * batches holds both variables in every one of those steps.
     */
    @Test
    void BatchedWriteMultipleVariablesTest() {
        String outputPath = TestUtil.createTempFile("BatchedWriteMultipleVariablesTest.nc");
        Assertions.assertNotNull(outputPath);

        List<VortexData> batch = new ArrayList<>();
        for (int step = 0; step < 5; step++) {
            batch.addAll(intervalGrids("swe", List.of(step)));
            batch.addAll(intervalGrids("precipitation", List.of(step)));

            if (batch.size() == 4) {
                write(outputPath, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            write(outputPath, batch);
        }

        for (String variable : List.of("swe", "precipitation")) {
            Assertions.assertEquals(5, readGrids(outputPath, variable).size(),
                    variable + " must hold a record for every time step of every batch");
        }

        delete(outputPath);
    }

    /**
     * The extension only decides which writer a destination is routed to; every NetCDF file this writer
     * produces is NetCDF-4, so a ".nc4" destination is written in batches the same way a ".nc" one is.
     */
    @Test
    void BatchedWriteNc4DestinationTest() {
        String outputPath = TestUtil.createTempFile("BatchedWriteNc4DestinationTest.nc4");
        Assertions.assertNotNull(outputPath);

        List<VortexData> originalGrids = intervalGrids("swe", 5);
        for (int start = 0; start < originalGrids.size(); start += 2) {
            write(outputPath, new ArrayList<>(originalGrids.subList(start, Math.min(start + 2, originalGrids.size()))));
        }

        List<VortexData> generatedGrids = readGrids(outputPath, "swe");
        Assertions.assertEquals(originalGrids.size(), generatedGrids.size());
        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }

        delete(outputPath);
    }

    /**
     * A variable cannot be added to a NetCDF file once the file is created, so a write that introduces
     * one has nowhere to put its data. Refusing the write is the only outcome that does not throw away
     * the time steps the destination already holds.
     */
    @Test
    void LaterWriteWithNewVariableIsRefusedTest() {
        String outputPath = TestUtil.createTempFile("LaterWriteWithNewVariableIsRefusedTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", 3));

        DataWriteException error = Assertions.assertThrows(DataWriteException.class,
                () -> write(outputPath, intervalGrids("temperature", 3)));
        Assertions.assertTrue(error.getMessage().contains(outputPath), error.getMessage());
        Assertions.assertTrue(error.getMessage().contains("temperature"), error.getMessage());

        Assertions.assertEquals(3, readGrids(outputPath, "swe").size(), "the refused write must leave the file alone");
        delete(outputPath);
    }

    /**
     * A destination written twice over the same time steps holds each of them once, rewritten in place.
     */
    @Test
    void RewritingTimeStepsDoesNotDuplicateThemTest() {
        String outputPath = TestUtil.createTempFile("RewritingTimeStepsDoesNotDuplicateThemTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", 3));
        write(outputPath, intervalGrids("swe", List.of(1, 2), 500));

        List<VortexData> grids = readGrids(outputPath, "swe");
        Assertions.assertEquals(3, grids.size(), "the time steps already written must not be appended twice");

        List<Float> values = grids.stream().map(grid -> ((VortexGrid) grid).data()[0]).toList();
        Assertions.assertEquals(List.of(1f, 511f, 521f), values, "the rewritten steps hold their new data, in place");
        delete(outputPath);
    }

    /**
     * isOverwrite is the one way a write replaces its destination. It has to be asked for by name:
     * doing it by default is what left an export with only its last batch.
     */
    @Test
    void OverwriteOptionReplacesDestinationTest() {
        String outputPath = TestUtil.createTempFile("OverwriteOptionReplacesDestinationTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", 5));
        write(outputPath, intervalGrids("swe", List.of(0, 1)), Map.of("isOverwrite", "true"));

        List<VortexData> grids = readGrids(outputPath, "swe");
        Assertions.assertEquals(2, grids.size(), "isOverwrite=true replaces what the destination held");
        Assertions.assertEquals(List.of(1f, 11f),
                grids.stream().map(grid -> ((VortexGrid) grid).data()[0]).toList());
        delete(outputPath);
    }

    /**
     * Time steps do not have to arrive in chronological order. The file holds them in the order it was
     * given them, because records can only be added at the end of a record dimension, but none are lost.
     */
    @Test
    void OutOfOrderBatchesTest() {
        String outputPath = TestUtil.createTempFile("OutOfOrderBatchesTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", List.of(3, 4)));
        write(outputPath, intervalGrids("swe", List.of(0, 1, 2)));

        List<Float> values = readGrids(outputPath, "swe").stream()
                .map(grid -> ((VortexGrid) grid).data()[0])
                .sorted()
                .toList();
        Assertions.assertEquals(List.of(1f, 11f, 21f, 31f, 41f), values);
        delete(outputPath);
    }

    /**
     * A destination cannot hold a grid it was not built for. Writing one anyway used to replace the
     * file, which discarded every time step in it.
     */
    @Test
    void OtherGridIsRefusedTest() {
        String outputPath = TestUtil.createTempFile("OtherGridIsRefusedTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", 3));

        DataWriteException error = Assertions.assertThrows(DataWriteException.class,
                () -> write(outputPath, intervalGrids("swe", steps(3), 4, WktFactory.createWkt(new LatLonProjection()), 0)));
        Assertions.assertTrue(error.getMessage().contains(outputPath), error.getMessage());

        Assertions.assertEquals(3, readGrids(outputPath, "swe").size(), "the refused write must leave the file alone");
        delete(outputPath);
    }

    @Test
    void DifferentProjectedCrsIsRefusedTest() throws IOException {
        String outputPath = TestUtil.createTempFile("DifferentProjectedCrsIsRefusedTest.nc");
        Assertions.assertNotNull(outputPath);

        String firstWkt = WktFactory.fromEpsg(32610);
        String secondWkt = WktFactory.fromEpsg(32611);
        List<VortexData> first = intervalGrids("swe", List.of(0), 3, firstWkt, 0);
        List<VortexData> second = intervalGrids("swe", List.of(1), 3, secondWkt, 0);
        Assertions.assertEquals(
                VortexGridCollection.of(List.of((VortexGrid) first.get(0))).getProjectionName(),
                VortexGridCollection.of(List.of((VortexGrid) second.get(0))).getProjectionName());

        write(outputPath, first);

        DataWriteException error = Assertions.assertThrows(DataWriteException.class,
                () -> write(outputPath, second));
        Assertions.assertTrue(error.getMessage().contains("projection differs"), error.getMessage());
        try (NetcdfFile file = NetcdfFiles.open(outputPath)) {
            Assertions.assertEquals(1, file.findDimension("time").getLength(),
                    "a rejected CRS must not append a time record");
        }
        delete(outputPath);
    }

    @Test
    void DifferentVariableUnitsAreRefusedTest() throws IOException {
        String outputPath = TestUtil.createTempFile("DifferentVariableUnitsAreRefusedTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", List.of(0)));
        VortexGrid next = (VortexGrid) intervalGrids("swe", List.of(1)).get(0);
        VortexGrid changedUnits = VortexGrid.toBuilder(next).units("cm").build();

        DataWriteException error = Assertions.assertThrows(DataWriteException.class,
                () -> write(outputPath, List.of(changedUnits)));
        Assertions.assertTrue(error.getMessage().contains("units"), error.getMessage());
        try (NetcdfFile file = NetcdfFiles.open(outputPath)) {
            Assertions.assertEquals(1, file.findDimension("time").getLength());
        }
        delete(outputPath);
    }

    @Test
    void DifferentVariableNoDataValueIsRefusedTest() throws IOException {
        String outputPath = TestUtil.createTempFile("DifferentVariableNoDataValueIsRefusedTest.nc");
        Assertions.assertNotNull(outputPath);

        write(outputPath, intervalGrids("swe", List.of(0)));
        VortexGrid next = (VortexGrid) intervalGrids("swe", List.of(1)).get(0);
        VortexGrid changedNoDataValue = VortexGrid.toBuilder(next).noDataValue(-1).build();

        DataWriteException error = Assertions.assertThrows(DataWriteException.class,
                () -> write(outputPath, List.of(changedNoDataValue)));
        Assertions.assertTrue(error.getMessage().contains("no-data value"), error.getMessage());
        try (NetcdfFile file = NetcdfFiles.open(outputPath)) {
            Assertions.assertEquals(1, file.findDimension("time").getLength());
        }
        delete(outputPath);
    }

    /**
     * A projected grid is stored against x and y instead of latitude and longitude, and is written in
     * batches the same way.
     */
    @Test
    void BatchedWriteProjectedGridTest() {
        String outputPath = TestUtil.createTempFile("BatchedWriteProjectedGridTest.nc");
        Assertions.assertNotNull(outputPath);

        String wkt = WktFactory.createWkt(new LambertConformalConicEllipse());
        List<VortexData> originalGrids = intervalGrids("swe", steps(5), 3, wkt, 0);

        for (int start = 0; start < originalGrids.size(); start += 2) {
            List<VortexData> batch = new ArrayList<>(originalGrids.subList(start, Math.min(start + 2, originalGrids.size())));
            write(outputPath, batch);
        }

        List<VortexData> generatedGrids = readGrids(outputPath, "swe");
        Assertions.assertEquals(originalGrids.size(), generatedGrids.size());
        for (int i = 0; i < originalGrids.size(); i++) {
            Assertions.assertEquals(originalGrids.get(i), generatedGrids.get(i));
        }

        delete(outputPath);
    }

    /* Helpers */
    private static List<VortexData> intervalGrids(String shortName, int numTimeSteps) {
        return intervalGrids(shortName, steps(numTimeSteps), 3, WktFactory.createWkt(new LatLonProjection()), 0);
    }

    /** The time steps 0 through numTimeSteps - 1. */
    private static List<Integer> steps(int numTimeSteps) {
        return IntStream.range(0, numTimeSteps).boxed().toList();
    }

    private static List<VortexData> intervalGrids(String shortName, List<Integer> timeSteps) {
        return intervalGrids(shortName, timeSteps, 3, WktFactory.createWkt(new LatLonProjection()), 0);
    }

    /** The time steps written with their values moved, for telling a rewrite of a step from a no-op. */
    private static List<VortexData> intervalGrids(String shortName, List<Integer> timeSteps, int valueShift) {
        return intervalGrids(shortName, timeSteps, 3, WktFactory.createWkt(new LatLonProjection()), valueShift);
    }

    /**
     * @param valueShift moves the values written into the grid, so that a second write of the same time
     *                   step can be told apart from one that changed nothing.
     */
    private static List<VortexData> intervalGrids(String shortName, List<Integer> timeSteps, int side, String wkt, int valueShift) {
        ZonedDateTime firstEndTime = ZonedDateTime.of(2020, 6, 17, 3, 0, 0, 0, ZoneId.of("UTC"));
        Duration interval = Duration.ofMinutes(15);

        List<VortexData> grids = new ArrayList<>();
        for (int step : timeSteps) {
            ZonedDateTime endTime = firstEndTime.plus(interval.multipliedBy(step));

            float[] data = new float[side * side];
            Arrays.fill(data, step * 10 + 1 + valueShift);

            VortexGrid grid = VortexGrid.builder()
                    .originX(0).originY(0)
                    .nx(side).ny(side)
                    .dx(1).dy(1)
                    .wkt(wkt)
                    .data(data)
                    .noDataValue(-9999)
                    .units("MM")
                    .shortName(shortName)
                    .description("lwe_thickness_of_snow_amount")
                    .startTime(endTime.minus(interval))
                    .endTime(endTime)
                    .interval(interval)
                    .dataType(VortexDataType.ACCUMULATION)
                    .build();

            grids.add(grid);
        }
        return grids;
    }

    private static void write(String outputPath, List<VortexData> grids) {
        write(outputPath, grids, Map.of());
    }

    private static void write(String outputPath, List<VortexData> grids, Map<String, String> options) {
        DataWriter.builder()
                .data(new ArrayList<>(grids))
                .destination(outputPath)
                .options(options)
                .build()
                .write();
    }

    private static List<VortexData> readGrids(String outputPath, String variable) {
        try (DataReader reader = DataReader.builder()
                .variable(variable)
                .path(outputPath)
                .build()) {
            return reader.getDtos();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            throw new IllegalStateException(e);
        }
    }

    private static void delete(String outputPath) {
        try {
            Files.deleteIfExists(Path.of(outputPath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            Assertions.fail();
        }
    }
}
