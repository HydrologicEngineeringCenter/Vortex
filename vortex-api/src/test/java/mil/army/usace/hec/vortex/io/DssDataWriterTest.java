package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public class DssDataWriterTest {


    // @Test
    void write15MinPrecip() {
        Duration interval = Duration.ofMinutes(15);
        String variable = "PRECIPITATION";
        String units = "IN";

        ZonedDateTime zdt1 = ZonedDateTime.of(2000, 1, 1,0, 0, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt2 = ZonedDateTime.of(2000, 1, 1,0, 15, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt3 = ZonedDateTime.of(2000, 1, 1,0, 30, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt4 = ZonedDateTime.of(2000, 1, 1,0, 45, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt5 = ZonedDateTime.of(2000, 1, 1,1, 0, 0,0, ZoneId.of("Z"));

        float[] data1 = new float[16];
        Arrays.fill(data1, 1);

        float[] data2 = new float[16];
        Arrays.fill(data2, 2);

        float[] data3 = new float[16];
        Arrays.fill(data3, 3);

        float[] data4 = new float[16];
        Arrays.fill(data4, 4);

        VortexData grid1 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data1)
                .shortName(variable)
                .units(units)
                .startTime(zdt1)
                .endTime(zdt2)
                .interval(interval)
                .build();

        VortexData grid2 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data2)
                .shortName(variable)
                .units(units)
                .startTime(zdt2)
                .endTime(zdt3)
                .interval(interval)
                .build();

        VortexData grid3 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data3)
                .shortName(variable)
                .units(units)
                .startTime(zdt3)
                .endTime(zdt4)
                .interval(interval)
                .build();

        VortexData grid4 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data4)
                .shortName(variable)
                .units(units)
                .startTime(zdt4)
                .endTime(zdt5)
                .interval(interval)
                .build();

        List<VortexData> grids = List.of(grid1, grid2, grid3, grid4);

        DataWriter dataWriter = DataWriter.builder()
                .data(grids)
                .destination("C:/Temp/15MinPrecip.dss")
                .build();

        dataWriter.write();
    }

    // @Test
    void write15MinTemp() {
        Duration interval = Duration.ofMinutes(15);
        String variable = "TEMPERATURE";
        String units = "F";

        ZonedDateTime zdt1 = ZonedDateTime.of(2000, 1, 1,0, 0, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt2 = ZonedDateTime.of(2000, 1, 1,0, 15, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt3 = ZonedDateTime.of(2000, 1, 1,0, 30, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt4 = ZonedDateTime.of(2000, 1, 1,0, 45, 0,0, ZoneId.of("Z"));
        ZonedDateTime zdt5 = ZonedDateTime.of(2000, 1, 1,2, 0, 0,0, ZoneId.of("Z"));

        float[] data1 = new float[16];
        Arrays.fill(data1, 1);

        float[] data2 = new float[16];
        Arrays.fill(data2, 2);

        float[] data3 = new float[16];
        Arrays.fill(data3, 3);

        float[] data4 = new float[16];
        Arrays.fill(data4, 4);

        VortexData grid1 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data1)
                .shortName(variable)
                .units(units)
                .startTime(zdt1)
                .endTime(zdt2)
                .interval(interval)
                .build();

        VortexData grid2 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data2)
                .shortName(variable)
                .units(units)
                .startTime(zdt2)
                .endTime(zdt3)
                .interval(interval)
                .build();

        VortexData grid3 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data3)
                .shortName(variable)
                .units(units)
                .startTime(zdt3)
                .endTime(zdt4)
                .interval(interval)
                .build();

        VortexData grid4 = VortexGrid.builder()
                .originX(0)
                .originY(0)
                .dx(2000)
                .dy(2000)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.getShg())
                .data(data4)
                .shortName(variable)
                .units(units)
                .startTime(zdt4)
                .endTime(zdt5)
                .interval(interval)
                .build();

        List<VortexData> grids = List.of(grid1, grid2, grid3, grid4);

        DataWriter dataWriter = DataWriter.builder()
                .data(grids)
                .destination("C:/Temp/15MinTemp.dss")
                .build();

        dataWriter.write();
    }
}
