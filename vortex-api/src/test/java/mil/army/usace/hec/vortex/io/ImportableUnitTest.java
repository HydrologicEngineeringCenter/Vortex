package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DssDataType;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportableUnitTest {

    @Test
    void MrmsPrecipPassesRegression() {

        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();

        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        Map<String, String> geoOptions = new HashMap<>();
        geoOptions.put("minX", "-6796355.995329514");
        geoOptions.put("maxX", "-6400167.648368146");
        geoOptions.put("minY", "6540076.418591026");
        geoOptions.put("maxY", "6937174.540723451");
        geoOptions.put("envWkt", "PROJCS[\"USA_Contiguous_Albers_Equal_Area_Conic_USGS_version\",\n" +
                "    GEOGCS[\"NAD83\",\n" +
                "        DATUM[\"North_American_Datum_1983\",\n" +
                "            SPHEROID[\"GRS 1980\",6378137,298.257222101,\n" +
                "                AUTHORITY[\"EPSG\",\"7019\"]],\n" +
                "            AUTHORITY[\"EPSG\",\"6269\"]],\n" +
                "        PRIMEM[\"Greenwich\",0],\n" +
                "        UNIT[\"Degree\",0.0174532925199433]],\n" +
                "    PROJECTION[\"Albers_Conic_Equal_Area\"],\n" +
                "    PARAMETER[\"latitude_of_center\",23],\n" +
                "    PARAMETER[\"longitude_of_center\",-96],\n" +
                "    PARAMETER[\"standard_parallel_1\",29.5],\n" +
                "    PARAMETER[\"standard_parallel_2\",45.5],\n" +
                "    PARAMETER[\"false_easting\",0],\n" +
                "    PARAMETER[\"false_northing\",0],\n" +
                "    UNIT[\"US survey foot\",0.304800609601219,\n" +
                "        AUTHORITY[\"EPSG\",\"9003\"]],\n" +
                "    AXIS[\"Easting\",EAST],\n" +
                "    AXIS[\"Northing\",NORTH]]");
        geoOptions.put("targetCellSize", "2000");
        geoOptions.put("targetWkt", WktFactory.getShg());
        geoOptions.put("resamplingMethod", "Bilinear");

        Path destination = new File(getClass().getResource(
                "/importable_unit/mrms.dss").getFile()).toPath();
        
        ImportableUnit importableUnit = ImportableUnit.builder()
                .reader(reader)
                .geoOptions(geoOptions)
                .destination(destination)
                .build();

        importableUnit.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination.toString());
        griddedData.setPathname("///PRECIPITATION/02JAN2017:1100/02JAN2017:1200//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("2 January 2017, 11:00", gridInfo.getStartTime());
        Assertions.assertEquals("2 January 2017, 12:00", gridInfo.getEndTime());
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals(0.065, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(1.204, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(0.0, gridInfo.getMinValue(), 1E-3);
        assertEquals(996, gridInfo.getLowerLeftCellY());
        assertEquals(-1036, gridInfo.getLowerLeftCellX());

        float[] data = gridData.getData();

        griddedData.setPathname("///PRECIPITATION/02JAN2017:1100/02JAN2017:1200/PERSISTED/");
        GridData persistedGridData = new GridData();
        griddedData.retrieveGriddedData(true, persistedGridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        float[] persistedData = persistedGridData.getData();

        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], persistedData[i], 1E-4);
        }

        griddedData.done();
    }
}