package mil.army.usace.hec.vortex.io;

import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GridUtilities;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
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
        String path = "///PRECIPITATION/02JAN2017:1100/02JAN2017:1200//";
        GridData gridData = GridUtilities.retrieveGridFromDss(destination.toString(), path, status);

        GridInfo info = gridData.getGridInfo();
        assertEquals(996, info.getLowerLeftCellY());
        assertEquals(-1036, info.getLowerLeftCellX());

        File persisted = new File(getClass().getResource(
                "/regression/mrms/mrms_data_serialized").getFile());
        ArrayList arraylist = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(persisted.toString());
            ObjectInputStream ois = new ObjectInputStream(fis);
            arraylist = (ArrayList) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        float[] data = gridData.getData();
        for (int i = 0; i < arraylist.size(); i++) {
            assertEquals(arraylist.get(i), data[i]);
        }
    }
}