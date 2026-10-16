package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReadException;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeStepFillerTest {

    private static final float NO_DATA = -9999f;
    private static final ZonedDateTime T0 = ZonedDateTime.parse("2020-01-01T00:00:00Z");

    @Test
    @DisplayName("Timeline must include the final time step of the series")
    void timelineIncludesLastTimeStep() throws DataReadException {
        // hourly series with the 2-hour step absent; 4-hour step is the final record
        List<VortexGrid> grids = List.of(
                createGrid(T0),
                createGrid(T0.plusHours(1)),
                createGrid(T0.plusHours(3)),
                createGrid(T0.plusHours(4)));

        GridMetadata metadata = analyze(grids);

        assertTrue(metadata.startTimes.contains(T0.plusHours(4)),
                "final time step must not be dropped from the output timeline");
        assertEquals(5, metadata.startTimes.size(), "timeline should span first..last inclusive");
        assertTrue(metadata.hasNoData.contains(T0.plusHours(2)),
                "absent interior step should be marked for interpolation");
        assertEquals(-1, metadata.indices.get(metadata.startTimes.indexOf(T0.plusHours(2))),
                "absent step should have no source index");
    }

    @Test
    @DisplayName("Continuous series maps every existing step once")
    void continuousSeriesKeepsAllSteps() throws DataReadException {
        List<VortexGrid> grids = List.of(
                createGrid(T0),
                createGrid(T0.plusHours(1)),
                createGrid(T0.plusHours(2)));

        GridMetadata metadata = analyze(grids);

        assertEquals(3, metadata.startTimes.size());
        assertTrue(metadata.hasNoData.isEmpty());
        assertEquals(List.of(0, 1, 2), metadata.indices);
    }

    private static GridMetadata analyze(List<VortexGrid> grids) throws DataReadException {
        DataReader reader = mock(DataReader.class);
        when(reader.getDtoCount()).thenReturn(grids.size());
        for (int i = 0; i < grids.size(); i++) {
            when(reader.getDto(i)).thenReturn(grids.get(i));
        }

        BatchGapFiller.Builder builder = BatchGapFiller.builder()
                .source("dummy-source.dss")
                .destination("dummy-dest.dss")
                .variables(List.of("test"))
                .method(GapFillMethod.TIME_STEP);

        TimeStepFiller filler = new TimeStepFiller(builder);
        return filler.analyzeGridData(reader);
    }

    private static VortexGrid createGrid(ZonedDateTime time) {
        float[] data = {10f, 20f, 30f, 40f};
        return VortexGrid.builder()
                .nx(2)
                .ny(2)
                .dx(1.0)
                .dy(1.0)
                .originX(0.0)
                .originY(0.0)
                .data(data)
                .noDataValue(NO_DATA)
                .dataType(VortexDataType.INSTANTANEOUS)
                .startTime(time)
                .endTime(time.plus(Duration.ofHours(1)))
                .interval(Duration.ofHours(1))
                .build();
    }
}
