package mil.army.usace.hec.vortex.convert;

import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GridToPointConverterTest {

    @Test
    void QpeGridsConvertToPoint() {
        String pathToGrids = new File(getClass().getResource(
                "/truckee/truckee_river_qpe.dss").getFile()).toString();

        Set<String> variables = DataReader.getVariables(pathToGrids);

        String pathToShp = new File(getClass().getResource(
                "/truckee/truckee_subbasins.shp").getFile()).toString();

        String destination = new File(getClass().getResource(
                "/truckee/truckee_river_qpe_ts.dss").getFile()).toString();

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToGrids)
                .variables(variables)
                .pathToFeatures(pathToShp)
                .field("Name")
                .destination(destination)
                .build();

        converter.convert();

        try {
            TimeSeriesContainer tscRead = new TimeSeriesContainer();
            tscRead.fullName = "//TruckeeRv_S10/PRECIPITATION/*/1Hour//";
            HecTimeSeries dssTimeSeriesRead = new HecTimeSeries();
            dssTimeSeriesRead.setDSSFileName(destination);
            int status = dssTimeSeriesRead.read(tscRead, true);
            assertEquals(0, status);
            dssTimeSeriesRead.done();

            double[] values = tscRead.values;
            assertEquals(49, values.length);

            HecTime startTime = new HecTime();
            startTime.set("31 December 2016, 24:00");
            int[] times = tscRead.times;
            assertEquals(startTime.value(), times[0]);
        } finally {
            HecDataManager.closeAllFiles();
        }
    }

    @Test
    void PrismGridsConvertToPoint() {
        String pathToGrids = new File(getClass().getResource(
                "/canyon/Canyon_PRISM.dss").getFile()).toString();

        Set<String> variables = DataReader.getVariables(pathToGrids);

        String pathToShp = new File(getClass().getResource(
                "/canyon/canyon.shp").getFile()).toString();

        String destination = new File(getClass().getResource(
                "/canyon/canyon_ts.dss").getFile()).toString();

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToGrids)
                .variables(variables)
                .pathToFeatures(pathToShp)
                .field("Name")
                .destination(destination)
                .build();

        converter.convert();

        try {
            TimeSeriesContainer tscRead = new TimeSeriesContainer();
            tscRead.fullName = "//canyon/PRECIPITATION/*/1Day//";
            HecTimeSeries dssTimeSeriesRead = new HecTimeSeries();
            dssTimeSeriesRead.setDSSFileName(destination);
            int status = dssTimeSeriesRead.read(tscRead, true);
            dssTimeSeriesRead.done();
            assertEquals(0, status);

            double[] values = tscRead.values;
            assertEquals(3, values.length);

            HecTime startTime = new HecTime();
            startTime.set("21 June 1997, 12:00");
            int[] times = tscRead.times;
            assertEquals(startTime.value(), times[0]);
        } finally {
            HecDataManager.closeAllFiles();
        }
    }

    @Test
    void CpcTmaxGridsConvertToPoint() {
        String pathToGrids = new File(getClass().getResource(
                "/truckee/truckee_temperature.dss").getFile()).toString();

        Set<String> variables = DataReader.getVariables(pathToGrids);

        String pathToShp = new File(getClass().getResource(
                "/truckee/truckee_subbasins.shp").getFile()).toString();

        String destination = new File(getClass().getResource(
                "/truckee/truckee_temperature_ts.dss").getFile()).toString();

        Map<String, String> options = new HashMap<>();
        options.put("partF", "test");

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToGrids)
                .variables(variables)
                .pathToFeatures(pathToShp)
                .field("Name")
                .destination(destination)
                .writeOptions(options)
                .build();

        converter.convert();

        try {
            TimeSeriesContainer tscRead = new TimeSeriesContainer();
            tscRead.fullName = "//TRUCKEERV_S10//*/1HOUR/TEST/";
            HecTimeSeries dssTimeSeriesRead = new HecTimeSeries();
            dssTimeSeriesRead.setDSSFileName(destination);
            int status = dssTimeSeriesRead.read(tscRead, true);
            dssTimeSeriesRead.done();
            assertEquals(0, status);
            double[] values = tscRead.values;
            assertEquals(12, values.length);

            HecTime startTime = new HecTime();
            startTime.set("15 December 1996, 1:00");
            int[] times = tscRead.times;
            assertEquals(startTime.value(), times[0]);
        } finally {
            HecDataManager.closeAllFiles();
        }
    }



}