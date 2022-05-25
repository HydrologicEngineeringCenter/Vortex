package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DssDataType;
import hec.heclib.dss.HecDataManager;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GridUtilities;
import hec.heclib.grid.GriddedData;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchImporterTest {

    @Test
    void MrmsPrecipPassesRegression() {
        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        String variableName = "GaugeCorrQPE01H_altitude_above_msl";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        Path shapefile = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile()).toPath();

        Map<String, String> options = new HashMap<>();
        options.put("pathToShp", shapefile.toString());
        options.put("targetCellSize", "2000");
        options.put("targetWkt", WktFactory.getShg());
        options.put("resamplingMethod", "Bilinear");

        File outFile = new File(getClass().getResource(
                "/regression/mrms/mrms.dss").getFile());

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .geoOptions(options)
                .destination(outFile.toString())
                .build();

        importer.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile.getPath());
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

    @Test
    void RtmaTemperaturePassesRegression() {
        String inFile = new File(getClass().getResource(
                "/201701021200_TMPK.grib2").getFile()).toString();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        String variableName = "Temperature_height_above_ground";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        Path shapefile = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile()).toPath();

        Map<String, String> options = new HashMap<>();
        options.put("pathToShp", shapefile.toString());
        options.put("targetCellSize", "2000");
        options.put("targetWkt", WktFactory.getShg());
        options.put("resamplingMethod", "Bilinear");

        Map<String, String> writeOptions = new HashMap<>();
        writeOptions.put("partB", "Truckee River");

        File outFile = new File(getClass().getResource(
                "/regression/rtma/rtma.dss").getFile());

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .geoOptions(options)
                .destination(outFile.toString())
                .writeOptions(writeOptions)
                .build();

        importer.process();

        int[] status = new int[1];
        String path = "//TRUCKEE RIVER/TEMPERATURE/02JAN2017:1200///";
        GridData gridData = GridUtilities.retrieveGridFromDss(outFile.getPath(), path, status);

        GridInfo info = gridData.getGridInfo();
        assertEquals(996, info.getLowerLeftCellY());
        assertEquals(-1036, info.getLowerLeftCellX());

        File persisted = new File(getClass().getResource(
                "/regression/rtma/rtma_data_serialized").getFile());
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

//        float[] vortex = gridData.getData();
//        List<Float> list = new ArrayList<>();
//        for (int i = 0; i < vortex.length; i++){
//            list.add(vortex[i]);
//        }
//
//        try{
//            FileOutputStream fos= new FileOutputStream("C:/Temp/rtma_data_serialized");
//            ObjectOutputStream oos= new ObjectOutputStream(fos);
//            oos.writeObject(list);
//            oos.close();
//            fos.close();
//        }catch(IOException ioe){
//            ioe.printStackTrace();
//        }
    }

    @Test
    void bilZipReadTest() {
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/bil_zip/PRISM_ppt_stable_4kmD2_20170101_bil.zip"));

        String inFile = new File(inUrl.getFile()).toString();

        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        URL outURL = Objects.requireNonNull(getClass().getResource(
                "/bil_zip/bil_zip.dss"));

        String outFile = new File(outURL.getFile()).toString();

        String variableName = "ppt";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .destination(outFile)
                .build();

        importer.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile);
        griddedData.setPathname("///PRECIPITATION/31DEC2016:1200/01JAN2017:1200//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("31 December 2016, 12:00", gridInfo.getStartTime());
        Assertions.assertEquals("1 January 2017, 12:00", gridInfo.getEndTime());
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals(2.986, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(208.298, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(0.0, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void PrismTmeanBil() {
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/prism_tmean_bil/PRISM_tmean_stable_4kmM3_196001_bil.bil"));

        String inFile = new File(inUrl.getFile()).toString();

        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        URL outURL = Objects.requireNonNull(getClass().getResource(
                "/prism_tmean_bil/prism_tmean_bil.dss"));

        String outFile = new File(outURL.getFile()).toString();

        String variableName = "tmean";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .destination(outFile)
                .build();

        importer.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile);
        griddedData.setPathname("///TEMPERATURE/01JAN1960:0000/01FEB1960:0000//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("1 January 1960, 00:00", gridInfo.getStartTime());
        Assertions.assertEquals("31 January 1960, 24:00", gridInfo.getEndTime());
        Assertions.assertEquals("DEG C", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_AVER.value(), gridInfo.getDataType());
        Assertions.assertEquals(-1.326, gridInfo.getMeanValue(), 1E-3);
        Assertions.assertEquals(21.233, gridInfo.getMaxValue(), 1E-3);
        Assertions.assertEquals(-17.596, gridInfo.getMinValue(), 1E-3);

        griddedData.done();
    }

    @Test
    void cmorph() {
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/cmorph/CMORPH_V1.0_ADJ_0.25deg-HLY_2017010101.nc"));

        String inFile = new File(inUrl.getFile()).toString();

        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        Path pathToDestination = Paths.get(System.getProperty("java.io.tmpdir"), "cmorph.dss");

        List<String> variables = new ArrayList<>();
        variables.add("cmorph");

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .destination(pathToDestination.toString())
                .build();

        importer.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(pathToDestination.toString());
        griddedData.setPathname("///PRECIPITATION/01JAN2017:0100/01JAN2017:0200//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("1 January 2017, 01:00", gridInfo.getStartTime());
        Assertions.assertEquals("1 January 2017, 02:00", gridInfo.getEndTime());
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals(36.17, gridInfo.getMaxValue(), 1E-2);

        griddedData.done();

        try {
            //Not sure what the boolean is here
            HecDataManager.close(pathToDestination.toString(), false);
            Files.deleteIfExists(pathToDestination);
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, e, e::getMessage);
        }
    }
}