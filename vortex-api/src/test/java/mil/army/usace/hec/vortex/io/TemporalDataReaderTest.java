package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static mil.army.usace.hec.vortex.VortexDataType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TemporalDataReaderTest {
    private static String tempDssFile;
    private static TemporalDataReader accumulationReader;
    private static TemporalDataReader averageReader;
    private static TemporalDataReader instantReader;

    /* Set Up */
    @BeforeAll
    static void setUp() {
        tempDssFile = TestUtil.createTempFile("TemporalDataReaderTest.dss");

        writeAccumulationData();
        writeAverageData();
        writeInstantData();

        accumulationReader = createTemporalDataReader(ACCUMULATION);
        averageReader = createTemporalDataReader(AVERAGE);
        instantReader = createTemporalDataReader(INSTANTANEOUS);
    }

    /* Getter Tests */
    @Test
    void getAccumulationMinMaxGridData() {
        ZonedDateTime startTime = buildTestTime(2, 0);
        ZonedDateTime endTime = buildTestTime(4, 15);
        float[] expectedMin = buildTestData(0f);
        float[] expectedMax = buildTestData(3f);
        VortexGrid[] actual = accumulationReader.getMinMaxGridData(startTime, endTime);
        assertFloatArrayEquals(expectedMin, actual[0].data());
        assertFloatArrayEquals(expectedMax, actual[1].data());
    }

    @Test
    void getAverageMinMaxGridData() {
        ZonedDateTime startTime = buildTestTime(1, 30);
        ZonedDateTime endTime = buildTestTime(3, 20);
        float[] expectedMin = buildTestData(7.5f);
        float[] expectedMax = buildTestData(7.7f);
        VortexGrid[] actual = averageReader.getMinMaxGridData(startTime, endTime);
        assertFloatArrayEquals(expectedMin, actual[0].data());
        assertFloatArrayEquals(expectedMax, actual[1].data());
    }

    @Test
    void getInstantMinMaxGridData() {
        ZonedDateTime startTime = buildTestTime(3, 0);
        ZonedDateTime endTime = buildTestTime(5, 30);
        float[] expectedMin = buildTestData(18f);
        float[] expectedMax = buildTestData(22f);
        VortexGrid[] actual = instantReader.getMinMaxGridData(startTime, endTime);
        assertFloatArrayEquals(expectedMin, actual[0].data());
        assertFloatArrayEquals(expectedMax, actual[1].data());
    }

    @Test
    void getGridDefinition() {
        String expectedWkt = WktFactory.getShg();

        Optional<Grid> actualAccumulationGridDefinition = accumulationReader.getGridDefinition();
        assertEquals(expectedWkt, actualAccumulationGridDefinition.map(Grid::getCrs).orElse(""));

        Optional<Grid> actualAverageGridDefinition = averageReader.getGridDefinition();
        assertEquals(expectedWkt, actualAverageGridDefinition.map(Grid::getCrs).orElse(""));

        Optional<Grid> actualInstantGridDefinition = instantReader.getGridDefinition();
        assertEquals(expectedWkt, actualInstantGridDefinition.map(Grid::getCrs).orElse(""));
    }

    @Test
    void getStartAndEndTime() {
        ZonedDateTime expectedAccumulationStartTime = TimeConverter.toZonedDateTime("01JAN2020:0100");
        ZonedDateTime expectedAccumulationEndTime = TimeConverter.toZonedDateTime("01JAN2020:0600");
        assertEquals(expectedAccumulationStartTime, accumulationReader.getStartTime().orElse(null));
        assertEquals(expectedAccumulationEndTime, accumulationReader.getEndTime().orElse(null));

        ZonedDateTime expectedAverageStartTime = TimeConverter.toZonedDateTime("01JAN2020:0100");
        ZonedDateTime expectedAverageEndTime = TimeConverter.toZonedDateTime("01JAN2020:0600");
        assertEquals(expectedAverageStartTime, averageReader.getStartTime().orElse(null));
        assertEquals(expectedAverageEndTime, averageReader.getEndTime().orElse(null));

        ZonedDateTime expectedInstantStartTime = TimeConverter.toZonedDateTime("01JAN2020:0100");
        ZonedDateTime expectedInstantEndTime = TimeConverter.toZonedDateTime("01JAN2020:0500");
        assertEquals(expectedInstantStartTime, instantReader.getStartTime().orElse(null));
        assertEquals(expectedInstantEndTime, instantReader.getEndTime().orElse(null));
    }

    @Test
    void getDataUnits() {
        String expectedAccumulationUnits = "MM";
        assertEquals(expectedAccumulationUnits, accumulationReader.getDataUnits().orElse(null));

        String expectedAverageUnits = "DEG C";
        assertEquals(expectedAverageUnits, averageReader.getDataUnits().orElse(null));

        String expectedInstantUnits = "UNSPECIF";
        assertEquals(expectedInstantUnits, instantReader.getDataUnits().orElse(null));
    }

    /* Accumulation Tests */
    @Test
    void readAccumulationSingleGridOverlap() {
        TemporalDataReader reader = accumulationReader;
        ZonedDateTime start = buildTestTime(2, 0);
        ZonedDateTime end = buildTestTime(3, 0);

        float[] expectedData = buildTestData(1.5f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAccumulationFullDataOverlap() {
        TemporalDataReader reader = accumulationReader;
        ZonedDateTime start = buildTestTime(1, 0);
        ZonedDateTime end = buildTestTime(6, 0);

        float[] expectedData = buildTestData(9f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAccumulationPartialFirstGridOverlap() {
        TemporalDataReader reader = accumulationReader;
        ZonedDateTime start = buildTestTime(1, 30);
        ZonedDateTime end = buildTestTime(3, 0);

        float[] expectedData = buildTestData(2.5f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAccumulationPartialMultipleGridsOverlap() {
        TemporalDataReader reader = accumulationReader;
        ZonedDateTime start = buildTestTime(1, 15);
        ZonedDateTime end = buildTestTime(3, 15);

        float[] expectedData = buildTestData(3f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAccumulationBeforeStart() {
        TemporalDataReader reader = accumulationReader;
        ZonedDateTime start = buildTestTime(0, 15);
        ZonedDateTime end = buildTestTime(3, 0);

        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    @Test
    void readAccumulationTimeAfterEnd() {
        TemporalDataReader reader = accumulationReader;
        ZonedDateTime start = buildTestTime(4, 30);
        ZonedDateTime end = buildTestTime(6, 15);

        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    /* Average Tests */
    @Test
    void readAverageNoOverlap() {
        TemporalDataReader reader = averageReader;
        ZonedDateTime start = buildTestTime(7, 0);
        ZonedDateTime end = buildTestTime(8, 0);

        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    @Test
    void readAverageSingleGridOverlap() {
        TemporalDataReader reader = averageReader;
        ZonedDateTime start = buildTestTime(1, 0);
        ZonedDateTime end = buildTestTime(2, 0);

        float[] expectedData = buildTestData(7.5f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAverageFullDataOverlap() {
        TemporalDataReader reader = averageReader;
        ZonedDateTime start = buildTestTime(1, 0);
        ZonedDateTime end = buildTestTime(6, 0);

        float[] expectedData = buildTestData(7.56f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAveragePartialFirstGridOverlap() {
        TemporalDataReader reader = averageReader;
        ZonedDateTime start = buildTestTime(1, 30);
        ZonedDateTime end = buildTestTime(3, 0);

        float[] expectedData = buildTestData(7.566667f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readAveragePartialMultipleGridsOverlap() {
        TemporalDataReader reader = averageReader;
        ZonedDateTime start = buildTestTime(1, 15);
        ZonedDateTime end = buildTestTime(3, 15);

        float[] expectedData = buildTestData(7.575f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    /* Point Instant Tests */
    @Test
    void readPointInstantSingleGridOverlap() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime time = buildTestTime(2, 0);

        float[] expectedData = buildTestData(20f);
        VortexGrid actualGrid = reader.read(time, time).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readPointInstantInBetweenGrids() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime time = buildTestTime(1, 15);

        float[] expectedData = buildTestData(16.25f);
        VortexGrid actualGrid = reader.read(time, time).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readPointInstantBeforeStart() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime time = buildTestTime(0, 15);

        VortexGrid actualGrid = reader.read(time, time).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    @Test
    void readPointInstantAfterEnd() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime time = buildTestTime(5, 30);

        VortexGrid actualGrid = reader.read(time, time).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    /* Period Instant Tests */
    @Test
    void readPeriodInstantPartialMultipleGrids() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime start = buildTestTime(2, 15);
        ZonedDateTime end = buildTestTime(4, 50);

        float[] expectedData = buildTestData(19.341398f);
        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void readPeriodInstantMissingSomeDataBeforeStart() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime start = buildTestTime(0, 30);
        ZonedDateTime end = buildTestTime(1, 0);

        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    @Test
    void readPeriodInstantMissingSomeDataAfterEnd() {
        TemporalDataReader reader = instantReader;
        ZonedDateTime start = buildTestTime(5, 0);
        ZonedDateTime end = buildTestTime(6, 15);

        VortexGrid actualGrid = reader.read(start, end).orElse(null);

        assertNotNull(actualGrid);
        assertMissingData(actualGrid);
    }

    /* Helpers */
    private static void writeAccumulationData() {
        List<VortexData> accumulationGrids = List.of(
                buildTestGridNew(ACCUMULATION, 1, 2, 2),
                buildTestGridNew(ACCUMULATION, 2, 3, 1.5f),
                buildTestGridNew(ACCUMULATION, 3, 4, 0),
                buildTestGridNew(ACCUMULATION, 4, 5, 3),
                buildTestGridNew(ACCUMULATION, 5, 6, 2.5f)
        );


        DataWriter dataWriter = DataWriter.builder()
                .data(accumulationGrids)
                .destination(tempDssFile)
                .build();

        dataWriter.write();
    }

    private static void writeAverageData() {
        List<VortexData> averageGrids = List.of(
                buildTestGridNew(AVERAGE, 1, 2, 7.5f),
                buildTestGridNew(AVERAGE, 2, 3, 7.6f),
                buildTestGridNew(AVERAGE, 3, 4, 7.7f),
                buildTestGridNew(AVERAGE, 4, 5, 7.4f),
                buildTestGridNew(AVERAGE, 5, 6, 7.6f)
        );

        DataWriter dataWriter = DataWriter.builder()
                .data(averageGrids)
                .destination(tempDssFile)
                .build();

        dataWriter.write();
    }

    private static void writeInstantData() {
        List<VortexData> instantGrids = List.of(
                buildTestGridNew(INSTANTANEOUS, 1, 1, 15),
                buildTestGridNew(INSTANTANEOUS, 2, 2, 20),
                buildTestGridNew(INSTANTANEOUS, 3, 3, 18),
                buildTestGridNew(INSTANTANEOUS, 4, 4, 22),
                buildTestGridNew(INSTANTANEOUS, 5, 5, 15)
        );

        DataWriter dataWriter = DataWriter.builder()
                .data(instantGrids)
                .destination(tempDssFile)
                .build();

        dataWriter.write();
    }

    private static TemporalDataReader createTemporalDataReader(VortexDataType type) {
        String variableName = getTestGridDataName(type);
        String pathToData = String.format("///%s/*/*//", variableName);
        DataReader dataReader = DataReader.builder().path(tempDssFile).variable(pathToData).build();
        return TemporalDataReader.create(dataReader);
    }

    private static VortexGrid buildTestGridNew(VortexDataType type, int hourStart, int hourEnd, float dataValue) {
        float[] data = buildTestData(dataValue);

        ZonedDateTime startTime = buildTestTime(hourStart, 0);
        ZonedDateTime endTime = buildTestTime(hourEnd, 0);

        return VortexGrid.builder()
                .dx(1).dy(1)
                .nx(3).ny(3)
                .originX(0).originY(0)
                .fileName(tempDssFile)
                .shortName(getTestGridDataName(type))
                .fullName(getTestGridDataName(type))
                .units(getTestGridDataUnits(type))
                .description("")
                .wkt(WktFactory.getShg())
                .data(data)
                .noDataValue(Heclib.UNDEFINED_FLOAT)
                .startTime(startTime)
                .endTime(endTime)
                .interval(Duration.between(startTime, endTime))
                .dataType(type)
                .build();
    }

    private static String getTestGridDataName(VortexDataType type) {
        return switch (type) {
            case ACCUMULATION -> "PRECIPITATION";
            case AVERAGE -> "TEMPERATURE";
            case INSTANTANEOUS -> "ALBEDO";
            default -> "";
        };
    }

    private static String getTestGridDataUnits(VortexDataType type) {
        return switch (type) {
            case ACCUMULATION -> "MM";
            case AVERAGE -> "DEG C";
            case INSTANTANEOUS -> "UNSPECIF";
            default -> "";
        };
    }

    private static ZonedDateTime buildTestTime(int hour, int minute) {
        ZoneId zoneId = ZoneId.of("UTC");
        return ZonedDateTime.of(2020, 1, 1, hour, minute, 0, 0, zoneId);
    }

    private static float[] buildTestData(float value) {
        float[] data = new float[9];
        Arrays.fill(data, value);
        return data;
    }

    private void assertMissingData(VortexGrid grid) {
        float[] expectedData = new float[grid.data().length];
        Arrays.fill(expectedData, (float) grid.noDataValue());
        assertFloatArrayEquals(expectedData, grid.data());
    }

    private void assertFloatArrayEquals(float[] expected, float[] actual) {
        assertEquals(expected.length, actual.length, "Array lengths are different");

        float epsilon = (float) Math.pow(10, -4);

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], epsilon,
                    "Arrays differ at index " + i + ". Expected: " + expected[i] + ", but was: " + actual[i]);
        }
    }

    /* Tear Down */
    @AfterAll
    static void tearDown() throws Exception {
        if (tempDssFile != null) {
            HecDSSUtilities.close(tempDssFile, true);
            Files.deleteIfExists(Path.of(tempDssFile));
        }
    }
}