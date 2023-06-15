package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static mil.army.usace.hec.vortex.VortexDataType.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemporalDataReaderTest {
    @Test
    void DssTest() {
        long startTime = System.currentTimeMillis();
        TemporalDataReader reader = new TemporalDataReader("/Users/work/Documents-Local/HMS-Dataset/gridded_et/data/EF_Russian_Precip.dss", "*");
        long endTime = System.currentTimeMillis();
        System.out.println("Setup Time (seconds): " + (endTime - startTime) / 1000);
        startTime = System.currentTimeMillis();
        reader.read(ZonedDateTime.parse("2004-10-01T06:00Z"), ZonedDateTime.parse("2004-10-01T07:00Z"));
        endTime = System.currentTimeMillis();
        System.out.println("Read time (seconds): " + (endTime - startTime) / 1000);
    }

    /* Accumulation Tests */
    @Test
    void ReadAccumulationTime_noOverlap() {
        TemporalDataReader reader = setupAccumulationReader();

        VortexGrid actualGrid = reader.read(buildTestTime(7,0), buildTestTime(8,0));
        assertNull(actualGrid);
    }

    @Test
    void ReadAccumulationTime_singleGridOverlap() {
        TemporalDataReader reader = setupAccumulationReader();

        VortexGrid actualGrid = reader.read(buildTestTime(2,0), buildTestTime(3,0));
        float[] expectedData = buildTestData(20f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_fullOverlap() {
        TemporalDataReader reader = setupAccumulationReader();

        VortexGrid actualGrid = reader.read(buildTestTime(1,0), buildTestTime(6,0));
        float[] expectedData = buildTestData(95f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_partialFirstGridOverlap() {
        TemporalDataReader reader = setupAccumulationReader();

        VortexGrid actualGrid = reader.read(buildTestTime(1,30), buildTestTime(3,0));
        float[] expectedData = buildTestData(30f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_partialMultipleGridsOverlap() {
        TemporalDataReader reader = setupAccumulationReader();

        VortexGrid actualGrid = reader.read(buildTestTime(2,30), buildTestTime(5,30));
        float[] expectedData = buildTestData(57.5f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAccumulationTime_singleData() {
        List<VortexGrid> accumulationGrid = List.of(buildTestGrid(ACCUMULATION, 1, 2, 20));
        TemporalDataReader reader = new TemporalDataReader(mockBufferedDataReader(accumulationGrid));

        VortexGrid actualGrid = reader.read(buildTestTime(1,0), buildTestTime(2,0));
        float[] expectedData = buildTestData(20f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    /* Average Time */
    @Test
    void ReadAverageTime_noOverlap() {
        TemporalDataReader reader = setupAverageReader();

        VortexGrid actualGrid = reader.read(buildTestTime(7,0), buildTestTime(8,0));
        assertNull(actualGrid);
    }

    @Test
    void ReadAverageTime_singleGridOverlap() {
        TemporalDataReader reader = setupAverageReader();

        VortexGrid actualGrid = reader.read(buildTestTime(1,0), buildTestTime(2,0));
        float[] expectedData = buildTestData(5f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAverageTime_fullOverlap() {
        TemporalDataReader reader = setupAverageReader();

        VortexGrid actualGrid = reader.read(buildTestTime(1,0), buildTestTime(6,0));
        float[] expectedData = buildTestData(5.4f);  // (5 + 2 + 10 + 7 + 3) / 5 = 5.4
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAverageTime_partialFirstGridOverlap() {
        TemporalDataReader reader = setupAverageReader();

        VortexGrid actualGrid = reader.read(buildTestTime(1,30), buildTestTime(3,0));
        float[] expectedData = buildTestData(3.5f); // (5 + 2) / 2 = 3.5
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadAverageTime_partialMultipleGridsOverlap() {
        TemporalDataReader reader = setupAverageReader();

        VortexGrid actualGrid = reader.read(buildTestTime(3,30), buildTestTime(5,30));
        float[] expectedData = buildTestData(20/3f);  // (10 + 7 + 3) / 3 = 20/3
        assertArrayEquals(expectedData, actualGrid.data());
    }

    /* Instantaneous Time */
    @Test
    void ReadInstantTime_sameGridTime() {
        TemporalDataReader reader = setupInstantReader();

        ZonedDateTime time = buildTestTime(3,0);
        VortexGrid actualGrid = reader.read(time, time);
        float[] expectedData = buildTestData(10f);
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadInstantTime_betweenGridTimes() {
        TemporalDataReader reader = setupInstantReader();

        ZonedDateTime time = buildTestTime(3,30);
        VortexGrid actualGrid = reader.read(time, time);
        float[] expectedData = buildTestData(8.5f);  // (10 + 7) / 2 = 8.5
        assertArrayEquals(expectedData, actualGrid.data());
    }

    @Test
    void ReadInstantTime_beforeFirstGrid() {
        TemporalDataReader reader = setupInstantReader();

        ZonedDateTime time = buildTestTime(0,30);
        VortexGrid actualGrid = reader.read(time, time);
        assertNull(actualGrid);
    }

    @Test
    void ReadInstantTime_afterLastGrid() {
        TemporalDataReader reader = setupInstantReader();

        ZonedDateTime time = buildTestTime(6,0);
        VortexGrid actualGrid = reader.read(time, time);
        assertNull(actualGrid);
    }

    /* Helpers */
    private TemporalDataReader setupAccumulationReader() {
        List<VortexGrid> accumulationGrids = List.of(
                buildTestGrid(ACCUMULATION, 1, 2, 20),
                buildTestGrid(ACCUMULATION, 2, 3, 20),
                buildTestGrid(ACCUMULATION, 3, 4, 30),
                buildTestGrid(ACCUMULATION, 4, 5, 10),
                buildTestGrid(ACCUMULATION, 5, 6, 15)
        );

        return new TemporalDataReader(mockBufferedDataReader(accumulationGrids));
    }

    private TemporalDataReader setupAverageReader() {
        List<VortexGrid> averageGrids = List.of(
                buildTestGrid(AVERAGE, 1, 2, 5),
                buildTestGrid(AVERAGE, 2, 3, 2),
                buildTestGrid(AVERAGE, 3, 4, 10),
                buildTestGrid(AVERAGE, 4, 5, 7),
                buildTestGrid(AVERAGE, 5, 6, 3)
        );

        return new TemporalDataReader(mockBufferedDataReader(averageGrids));
    }

    private TemporalDataReader setupInstantReader() {
        List<VortexGrid> instantGrids = List.of(
                buildTestGrid(INSTANTANEOUS, 1, 1, 5),
                buildTestGrid(INSTANTANEOUS, 2, 2, 2),
                buildTestGrid(INSTANTANEOUS, 3, 3, 10),
                buildTestGrid(INSTANTANEOUS, 4, 4, 7),
                buildTestGrid(INSTANTANEOUS, 5, 5, 3)
        );

        return new TemporalDataReader(mockBufferedDataReader(instantGrids));
    }

    private BufferedDataReader mockBufferedDataReader(List<VortexGrid> gridList) {
        BufferedDataReader bufferedReader = Mockito.mock(BufferedDataReader.class);

        int numGrids = gridList.size();
        Mockito.when(bufferedReader.getCount()).thenReturn(numGrids);
        Mockito.when(bufferedReader.getType()).thenReturn(gridList.get(0).dataType());

        for (int i = 0; i < numGrids; i++) {
            VortexGrid grid = gridList.get(i);
            Mockito.when(bufferedReader.get(i)).thenReturn(grid);
        }

        return bufferedReader;
    }

    private VortexGrid buildTestGrid(VortexDataType type, int hourStart, int hourEnd, int dataValue) {
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
}