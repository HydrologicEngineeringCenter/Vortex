package mil.army.usace.hec.vortex.convert;

import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecTimeSeries;
import hec.io.TimeSeriesContainer;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GridToPointConverterTest {

    @Test
    void QpeGridsConvertToPoint() {
        String pathToGrids = new File(getClass().getResource(
                "/truckee-river-qpe.dss").getFile()).toString();

        Set<String> variables = DataReader.getVariables(pathToGrids);

        Path pathToShp = new File(getClass().getResource(
                "/truckee_subbasins/truckee_subbasins.shp").getFile()).toPath();

        Path destination = new File(getClass().getResource(
                "/regression/grid-to-point-converter/grid-to-point-converter.dss").getFile()).toPath();

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToGrids)
                .variables(variables)
                .pathToFeatures(pathToShp)
                .field("Name")
                .destination(destination)
                .build();

        converter.convert();

        TimeSeriesContainer tscRead = new TimeSeriesContainer();
        tscRead.fullName = "//TruckeeRv_S10//01Jan2017/1Hour//";
        HecTimeSeries dssTimeSeriesRead = new HecTimeSeries();
        dssTimeSeriesRead.setDSSFileName(destination.toString());
        int status = dssTimeSeriesRead.read(tscRead, true);
        dssTimeSeriesRead.done();
        if (status != 0) return;
        double[] values = tscRead.values;
        assertTrue((int) Arrays.stream(values).filter(i -> i > 0).count() > 0);
        HecDataManager.closeAllFiles();
    }

    @Test
    void CpcTmaxGridsConvertToPoint() {
        String pathToGrids = new File(getClass().getResource(
                "/cpc-tmax-2017.dss").getFile()).toString();

        Set<String> variables = DataReader.getVariables(pathToGrids);

        Path pathToShp = new File(getClass().getResource(
                "/truckee_subbasins/truckee_subbasins.shp").getFile()).toPath();

        Path destination = new File(getClass().getResource(
                "/regression/grid-to-point-converter/grid-to-point-converter.dss").getFile()).toPath();

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

        TimeSeriesContainer tscRead = new TimeSeriesContainer();
        tscRead.fullName = "//TruckeeRv_S10//01Jan2017/1Hour/test/";
        HecTimeSeries dssTimeSeriesRead = new HecTimeSeries();
        dssTimeSeriesRead.setDSSFileName(destination.toString());
        int status = dssTimeSeriesRead.read(tscRead, true);
        dssTimeSeriesRead.done();
        if (status != 0) return;
        double[] values = tscRead.values;
        assertTrue((int) Arrays.stream(values).filter(i -> i > 0).count() > 0);
        HecDataManager.closeAllFiles();


    }



}