package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static mil.army.usace.hec.vortex.geo.ZonalStatisticsCalculator.computeMedian;
import static org.junit.jupiter.api.Assertions.*;

class ZonalStatisticsCalculatorTest {
    static{
        GdalRegister.getInstance();
    }

    @Test
    void ZonalStatisticsCalculationValidates(){
        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();
        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid dto = dtos.get(0);

        Path pathToShp = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile()).toPath();

        String field = "NAME";

        Map<String, Integer[]> zoneMasks = ZonalStatisticsCalculator.createZoneMasks(pathToShp, field, dto);

        ZonalStatisticsCalculator zonalStatisticsCalculator = ZonalStatisticsCalculator.builder()
                .grid(dto)
                .zoneMasks(zoneMasks)
                .build();

        List<ZonalStatistics> zonalStatistics = zonalStatisticsCalculator.getZonalStatistics();
        ZonalStatistics zone = zonalStatistics.get(0);
        assertEquals("Truckee", zone.getId());
        assertEquals(0.0767, zone.getAverage(), 1E-4);
        assertEquals(0.9000, zone.getMax(), 1E-4);
        assertEquals(0.0, zone.getMin(), 1E-4);
        assertEquals(0.0, zone.getMedian(), 1E-4);
        assertEquals(0.0, zone.getFirstQuartile(), 1E-4);
        assertEquals(0.1000, zone.getThirdQuartile(), 1E-4);
        assertEquals(28.0, zone.getPctCellsGreaterThanZero(), 1E-1);
        assertEquals(28.0, zone.getPctCellsGreaterThanFirstQuartile(), 1E-1);
    }

    @Test
    void CreateZoneMasksCreatesZoneMasks(){
        String inFile = new File(getClass().getResource(
                "/truckee/truckee_river_qpe.dss").getFile()).toString();
        Set<String> variables = DataReader.getVariables(inFile);

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variables.iterator().next())
                .build();

        List<VortexGrid> data = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid dto = data.get(0);

        Path pathToShp = new File(getClass().getResource(
                "/truckee/truckee_subbasins.shp").getFile()).toPath();

        String field = "Name";

        Map<String, Integer[]> zoneMasks = ZonalStatisticsCalculator.createZoneMasks(pathToShp, field, dto);
        zoneMasks.forEach((id, mask) -> {
            assertEquals(3782, mask.length);
            assertTrue(Arrays.stream(mask).anyMatch(i -> i > 0));
        });

        assertEquals(16, (int) Arrays.stream(zoneMasks.get("DogCk_S10")).filter(i -> i > 0).count());
        assertEquals(10, (int) Arrays.stream(zoneMasks.get("DonnerCk_S10")).filter(i -> i > 0).count());
        assertEquals(86, (int) Arrays.stream(zoneMasks.get("LtTruckeeRv_S30")).filter(i -> i > 0).count());
        assertEquals(11, (int) Arrays.stream(zoneMasks.get("NTruckeeDt_S10")).filter(i -> i > 0).count());
    }

    @Test
    void CreateGridCellsCreatesGridCells(){
        double x = -180;
        double y = 90;
        double dx = 0.5;
        double dy = -0.5;
        int nx = 3;
        int ny = 3;
        List<GridCell> gridCells = Grid.builder()
                .originX(x)
                .originY(y)
                .dx(dx)
                .dy(dy)
                .nx(nx)
                .ny(ny)
                .build()
                .getGridCells();

        Point2D point = new Point2D.Double(-179.75, 89.75);
        GridCell gridCell = gridCells.get(0);
        boolean isContained = gridCell.contains(point);
        assertTrue(isContained);
        assertEquals(9, gridCells.size());
    }

    @Test
    void computeMedianTest() {
        List<Double> values = List.of(new Double[]{4.0, 5.0, 6.0, 7.0});
        double median = computeMedian(values);
        assertEquals(5.5, median);
    }

}