package mil.army.usace.hec.vortex.io;

import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static mil.army.usace.hec.vortex.VortexDataType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TemporalDataReaderTest {
    /* Accumulation Tests */
    @Test
    void ReadAccumulationTime_singleGridOverlap() {
        TemporalDataReader reader = setupAccumulationReader();
        ZonedDateTime start = buildTestTime(2,0);
        ZonedDateTime end = buildTestTime(3,0);

        float[] expectedData = buildTestData(1.5f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_mixData() {
        TemporalDataReader reader = setupMixedAccumulationReader();
        ZonedDateTime start = buildTestTime(10,0);
        ZonedDateTime end = buildTestTime(10,15);

        float[] expectedData = buildTestData(1.25f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_fullOverlap() {
        TemporalDataReader reader = setupAccumulationReader();
        ZonedDateTime start = buildTestTime(1,0);
        ZonedDateTime end = buildTestTime(6,0);

        float[] expectedData = buildTestData(9f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_partialFirstGridOverlap() {
        TemporalDataReader reader = setupAccumulationReader();
        ZonedDateTime start = buildTestTime(1,30);
        ZonedDateTime end = buildTestTime(3,0);

        float[] expectedData = buildTestData(2.5f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_partialMultipleGridsOverlap() {
        TemporalDataReader reader = setupAccumulationReader();
        ZonedDateTime start = buildTestTime(1,15);
        ZonedDateTime end = buildTestTime(3,15);

        float[] expectedData = buildTestData(3f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_beforeStart() {
        TemporalDataReader reader = setupAccumulationReader();
        ZonedDateTime start = buildTestTime(0,15);
        ZonedDateTime end = buildTestTime(3,0);

        VortexGrid actualGrid = reader.read(start, end);
        assertMissingData(actualGrid);
    }

    @Test
    void ReadAccumulationTime_afterEnd() {
        TemporalDataReader reader = setupAccumulationReader();
        ZonedDateTime start = buildTestTime(4,30);
        ZonedDateTime end = buildTestTime(6,15);

        VortexGrid actualGrid = reader.read(start, end);
        assertMissingData(actualGrid);
    }

    /* Average Tests */
    @Test
    void ReadAverageTime_noOverlap() {
        TemporalDataReader reader = setupAverageReader();
        ZonedDateTime start = buildTestTime(7,0);
        ZonedDateTime end = buildTestTime(8,0);

        VortexGrid actualGrid = reader.read(start, end);

        assertMissingData(actualGrid);
    }

    @Test
    void ReadAverageTime_singleGridOverlap() {
        TemporalDataReader reader = setupAverageReader();
        ZonedDateTime start = buildTestTime(1,0);
        ZonedDateTime end = buildTestTime(2,0);

        float[] expectedData = buildTestData(7.5f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAverageTime_fullOverlap() {
        TemporalDataReader reader = setupAverageReader();
        ZonedDateTime start = buildTestTime(1,0);
        ZonedDateTime end = buildTestTime(6,0);

        float[] expectedData = buildTestData(7.56f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAverageTime_partialFirstGridOverlap() {
        TemporalDataReader reader = setupAverageReader();
        ZonedDateTime start = buildTestTime(1,30);
        ZonedDateTime end = buildTestTime(3,0);

        float[] expectedData = buildTestData(7.566667f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAverageTime_partialMultipleGridsOverlap() {
        TemporalDataReader reader = setupAverageReader();
        ZonedDateTime start = buildTestTime(1,15);
        ZonedDateTime end = buildTestTime(3,15);

        float[] expectedData = buildTestData(7.575f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    /* Point Instant Tests */
    @Test
    void ReadInstantTime_sameGridTime() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime time = buildTestTime(2,0);

        float[] expectedData = buildTestData(20f);
        VortexGrid actualGrid = reader.read(time, time);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadInstantTime_betweenGridTimes() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime time = buildTestTime(1,15);

        float[] expectedData = buildTestData(16.25f);
        VortexGrid actualGrid = reader.read(time, time);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadInstantTime_beforeFirstGrid() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime time = buildTestTime(0,15);

        VortexGrid actualGrid = reader.read(time, time);

        assertMissingData(actualGrid);
    }

    @Test
    void ReadInstantTime_afterLastGrid() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime time = buildTestTime(5,30);

        VortexGrid actualGrid = reader.read(time, time);

        assertMissingData(actualGrid);
    }

    /* Period Instant Tests */
    @Test
    void ReadPeriodInstantTime_partialMultipleGrids() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime start = buildTestTime(2,15);
        ZonedDateTime end = buildTestTime(4,50);

        float[] expectedData = buildTestData(19.341398f);
        VortexGrid actualGrid = reader.read(start, end);

        assertFloatArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadPeriodInstantTime_missingDataBefore() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime start = buildTestTime(0,30);
        ZonedDateTime end = buildTestTime(1,0);

        VortexGrid actualGrid = reader.read(start, end);

        assertMissingData(actualGrid);
    }

    @Test
    void ReadPeriodInstantTime_missingDataAfter() {
        TemporalDataReader reader = setupInstantReader();
        ZonedDateTime start = buildTestTime(5,0);
        ZonedDateTime end = buildTestTime(6,15);

        VortexGrid actualGrid = reader.read(start, end);

        assertMissingData(actualGrid);
    }

    /* Helpers */
    private TemporalDataReader setupAccumulationReader() {
        List<VortexData> accumulationGrids = List.of(
                buildTestGrid(ACCUMULATION, 1, 2, 2),
                buildTestGrid(ACCUMULATION, 2, 3, 1.5f),
                buildTestGrid(ACCUMULATION, 3, 4, 0),
                buildTestGrid(ACCUMULATION, 4, 5, 3),
                buildTestGrid(ACCUMULATION, 5, 6, 2.5f)
        );

        return new TemporalDataReader(mockDataReader(accumulationGrids));
    }

    private TemporalDataReader setupMixedAccumulationReader() {
        List<VortexData> accumulationGrids = List.of(
                buildTestGrid(ACCUMULATION, 10, 13, 5.23f),
                buildTestGrid(ACCUMULATION, 10, 11, 5)
        );

        return new TemporalDataReader(mockDataReader(accumulationGrids));
    }

    private TemporalDataReader setupAverageReader() {
        List<VortexData> averageGrids = List.of(
                buildTestGrid(AVERAGE, 1, 2, 7.5f),
                buildTestGrid(AVERAGE, 2, 3, 7.6f),
                buildTestGrid(AVERAGE, 3, 4, 7.7f),
                buildTestGrid(AVERAGE, 4, 5, 7.4f),
                buildTestGrid(AVERAGE, 5, 6, 7.6f)
        );

        return new TemporalDataReader(mockDataReader(averageGrids));
    }

    private TemporalDataReader setupInstantReader() {
        List<VortexData> instantGrids = List.of(
                buildTestGrid(INSTANTANEOUS, 1, 1, 15),
                buildTestGrid(INSTANTANEOUS, 2, 2, 20),
                buildTestGrid(INSTANTANEOUS, 3, 3, 18),
                buildTestGrid(INSTANTANEOUS, 4, 4, 22),
                buildTestGrid(INSTANTANEOUS, 5, 5, 15)
        );

        return new TemporalDataReader(mockDataReader(instantGrids));
    }

    private BufferedDataReader mockDataReader(List<VortexData> gridList) {
        BufferedDataReader dataReader = Mockito.mock(BufferedDataReader.class);
        Mockito.when(dataReader.getTimeRecords()).thenReturn(gridList.stream().map(VortexTimeRecord::of).toList());
        Mockito.when(dataReader.getBaseGrid()).thenReturn((VortexGrid) gridList.get(0));
        for (int i = 0; i < gridList.size(); i++)
            Mockito.when(dataReader.get(i)).thenReturn((VortexGrid) gridList.get(i));
        return dataReader;
    }

    private VortexGrid buildTestGrid(VortexDataType type, int hourStart, int hourEnd, float dataValue) {
        float[] data = buildTestData(dataValue);

        ZonedDateTime startTime = buildTestTime(hourStart, 0);
        ZonedDateTime endTime = buildTestTime(hourEnd, 0);

        return VortexGrid.builder()
                .dx(1)
                .dy(1)
                .nx(3)
                .ny(3)
                .originX(0)
                .originY(0)
                .wkt("")
                .data(data)
                .units("")
                .fileName("")
                .shortName("")
                .fullName("")
                .description("")
                .noDataValue(Heclib.UNDEFINED_FLOAT)
                .startTime(startTime)
                .endTime(endTime)
                .interval(Duration.between(startTime, endTime))
                .dataType(type)
                .build();
    }

    private static ZonedDateTime buildTestTime(int hour, int minute) {
        ZoneId zoneId = ZoneId.of("UTC");
        return ZonedDateTime.of(2020,1,1, hour, minute,0,0, zoneId);
    }

    private float[] buildTestData(float value) {
        float[] data = new float[9];
        Arrays.fill(data, value);
        return data;
    }

    private void assertMissingData(VortexGrid grid) {
        float[] expectedData = new float[grid.data().length];
        Arrays.fill(expectedData, (float) grid.noDataValue());
        assertFloatArrayEquals(expectedData, grid.data());
    }

    public void assertFloatArrayEquals(float[] expected, float[] actual) {
        assertEquals(expected.length, actual.length, "Array lengths are different");

        float epsilon = (float) Math.pow(10, -4);

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], epsilon,
                    "Arrays differ at index " + i + ". Expected: " + expected[i] + ", but was: " + actual[i]);
        }
    }
}