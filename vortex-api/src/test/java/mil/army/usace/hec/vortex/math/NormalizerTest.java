package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NormalizerTest {

    @Test
    void DtoArraysAreNormalized() {
        VortexGrid dto1 = mock(VortexGrid.class);
        when(dto1.data()).thenReturn(new float[]{0,Float.NaN,2,4,2,2,2,2});
        when(dto1.startTime()).thenReturn(ZonedDateTime.of(0, 1, 1,0,0,0,0, ZoneId.of("UTC")));
        when(dto1.endTime()).thenReturn(ZonedDateTime.of(0, 1, 1,0,0,0,0, ZoneId.of("UTC")));

        VortexGrid dto2 = mock(VortexGrid.class);
        when(dto2.data()).thenReturn(new float[]{0,2,2,4,2,2,2,1});
        when(dto2.startTime()).thenReturn(ZonedDateTime.of(0, 1, 1,0,0,0,0, ZoneId.of("UTC")));
        when(dto2.endTime()).thenReturn(ZonedDateTime.of(0, 1, 1,0,0,0,0, ZoneId.of("UTC")));

        VortexGrid dto3 = mock(VortexGrid.class);
        when(dto3.data()).thenReturn(new float[]{0,2,Float.NaN,6,2,8,2,2});
        when(dto3.startTime()).thenReturn(ZonedDateTime.of(0, 1, 1,0,0,0,0, ZoneId.of("UTC")));
        when(dto3.endTime()).thenReturn(ZonedDateTime.of(0, 1, 1,0,0,0,0, ZoneId.of("UTC")));

        List<VortexGrid> inputs = new ArrayList<>();
        inputs.add(dto1);
        inputs.add(dto2);

        List<VortexGrid> normals = new ArrayList<>();
        normals.add(dto3);

        List<VortexGrid> normalized = Normalizer.normalize(inputs, normals);

        float[] expected1 = new float[]{0, Float.NaN, 2, 3, 1, 4, 1, Float.parseFloat("1.333")};

        float[] normalized1 = normalized.get(0).data();
        for (int i = 0; i < normalized1.length; i++){
            try {
                assertEquals(expected1[i], normalized1[i], 1E-3);
            }catch(AssertionError e){
                System.out.println("Error index: " + i);
                throw e;
            }
        }

        float[] expected2 = new float[]{0, 2, 2, 3, 1, 4, 1, Float.parseFloat("0.667")};

        float[] normalized2 = normalized.get(1).data();
        for (int i = 0; i < normalized2.length; i++) {
            try {
                assertEquals(expected2[i], normalized2[i], 1E-3);
            } catch (AssertionError e) {
                System.out.println("Error index: " + i);
                throw e;
            }
        }
    }

    @Test
    void NormalizerPassesRegression() {
        Path source = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toPath();
        Path normals = new File(getClass().getResource("/normalizer/prism.dss").getFile()).toPath();

        Set<String> sourceGrids = DataReader.getVariables(source);
        Set<String> normalsGrids = DataReader.getVariables(normals);

        ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(2017, 1, 1, 0, 0), ZoneId.of("UTC"));
        ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(2017, 1, 3, 0, 0), ZoneId.of("UTC"));
        Duration interval = Duration.ofDays(1);

        Path destination = new File(getClass().getResource("/normalizer/normalized.dss").getFile()).toPath();

        Normalizer normalizer = Normalizer.builder()
                .pathToSource(source).sourceVariables(sourceGrids)
                .pathToNormals(normals).normalsVariables(normalsGrids)
                .startTime(start).endTime(end).interval(interval)
                .destination(destination).build();

        normalizer.normalize();

        Set<String> paths = DataReader.getVariables(destination);
        assertEquals(48, paths.size());
    }
}