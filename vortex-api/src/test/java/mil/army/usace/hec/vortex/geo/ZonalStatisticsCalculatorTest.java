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

import static org.junit.jupiter.api.Assertions.*;

class ZonalStatisticsCalculatorTest {
    static{
        GdalRegister.getInstance();
    }

    @Test
    void ZonalStatisticsCalculationValidates(){
        Path inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toPath();
        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDTOs();
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
    }

    @Test
    void CreateZoneMasksCreatesZoneMasks(){
        Path inFile = new File(getClass().getResource(
                "/truckee-river-qpe.dss").getFile()).toPath();
        Set<String> variables = DataReader.getVariables(inFile);

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variables.iterator().next())
                .build();

        List<VortexGrid> data = reader.getDTOs();
        VortexGrid dto = data.get(0);

        Path pathToShp = new File(getClass().getResource(
                "/truckee_subbasins/truckee_subbasins.shp").getFile()).toPath();

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
}