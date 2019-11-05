package mil.army.usace.hec.vortex.io;

import javafx.scene.transform.Translate;
import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class SnodasDataReader extends DataReader implements VirtualFileSystem{
    static { GdalRegister.getInstance(); }
    SnodasDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {
        // Create header files for SNODAS .dat files
        createHeaders(""); // FIXME: Get actual path

        // Read in raster using Gdal
        Dataset in = gdal.Open(path);
        ArrayList<String> options = new ArrayList<>();
        options.add("-of");
        options.add("MEM");
        TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
        Dataset raster = gdal.Translate("raster", in, translateOptions);
        raster.FlushCache();

        // Get the file name
        String fileName = "empty"; // FIXME: Get actual fileName

        // Build the grid and add to list of Data Transferable Objects (DTOs)
        VortexData dto = getGrid(raster, fileName);
        List<VortexData> list = new ArrayList<>();
        list.add(dto);

        return list;
    } // getDtos()

    @Override
    public int getDtoCount() {
        return 0;
    } // getDtoCount()

    @Override
    public VortexData getDto(int idx) {
        return null;
    } // getDto()

    private void createHeaders(String pathToFile) {
        // FIXME: ApacheCommons - Read & Write for tar. Add headers
        // Content for header file
        List<String> enviHeader = Arrays.asList("ENVI", "samples = 6935", "lines = 3351", "bands = 1",
                "header offset = 0", "file type = ENVI Standard", "data type = 2", "interleave = bsq", "byte order = 1");

        // Loop through each dat file, and create header file for each

    } // createHeaders()


    private Map<String,String> parseFile(String fileName) {
        Map<String,String> info = new HashMap<>();

        // Initialize indices
        int[] regionIndex  = new int[] {0,2};   // us = US, zz = Full unmasked version
        int[] modelIndex   = new int[] {3,6};   // ssm = simple snow model
        int[] vTypeIndex   = new int[] {6,8};   // v0 = Driving Data, v1 = Model Output
        int[] productIndex = new int[] {8,12};  // Product Code
        int[] scaleIndex   = new int[] {12,13}; // S = scaled down Driving Data
        int[] dTypeIndex   = new int[] {13,17}; // Precip Data or Snow Model Output
        int[] tCodeIndex   = new int[] {17,22}; // Time Code
        int[] dateIndex    = new int[] {28,36}; // Date
        int[] hourIndex    = new int[] {36,38}; // Hour

        // Check if scale exists
        String scale = fileName.substring(scaleIndex[0],scaleIndex[1]);
        if (!scale.equals("S")) {
            for (int i = 0; i < 2; i++) {
                dTypeIndex[i] -= 1;
                tCodeIndex[i] -= 1;
                dateIndex[i]  -= 1;
                hourIndex[i]  -= 1;
            } // Update indices
            scale = "";
        } // If model wasn't scaled down

        // Extract substrings
        String region  = fileName.substring(regionIndex[0],regionIndex[1]);
        String model   = fileName.substring(modelIndex[0],modelIndex[1]);
        String vType   = fileName.substring(vTypeIndex[0],vTypeIndex[1]);
        String product = fileName.substring(productIndex[0],productIndex[1]);
        String dType   = fileName.substring(dTypeIndex[0],dTypeIndex[1]);
        String tCode   = fileName.substring(tCodeIndex[0],tCodeIndex[1]);
        String date    = fileName.substring(dateIndex[0],dateIndex[1]);
        String hour    = fileName.substring(hourIndex[0],hourIndex[1]);

        // Putting data into a map
        info.put("region", region);
        info.put("model", model);
        info.put("vType", vType);
        info.put("product", product);
        info.put("scale", scale);
        info.put("dType", dType);
        info.put("tCode", tCode);
        info.put("date", date);
        info.put("hour", hour);

        return info;
    } // parseFile()

    private String getUnits() {
        return null;
    } // getUnits()

    private String getName() {
        return null;
    } // getName()

    private ZonedDateTime[] getTimeSpan(Map<String,String> info) {
        ZonedDateTime[] timeSpan = new ZonedDateTime[2];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(info.get("date"), formatter);
        ZonedDateTime startTime = null, endTime = null;

        switch(info.get("product")) {
            case "1044": case "1050": case "1039": case "1025":
                startTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(6,0)), ZoneId.of("UTC"));
                endTime   = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), LocalTime.of(6,0)), ZoneId.of("UTC"));
                break;
            case "1034": case "1036":
                startTime = endTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(6,0)), ZoneId.of("UTC"));
                break;
        } // Switch case for product

        timeSpan[0] = startTime;
        timeSpan[1] = endTime;

        return timeSpan;
    } // getTimeSpan()

    private VortexGrid getGrid(Dataset raster, String fileName) {
        // Getting grid info from raster
        double[] geoTransform = raster.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double ulx = geoTransform[0];
        double uly = geoTransform[3];
        int nx = raster.GetRasterXSize();
        int ny = raster.GetRasterYSize();
        String wkt = raster.GetProjection();
        Band band = raster.GetRasterBand(1);
        float[] data = new float[nx * ny];
        band.ReadRaster(0, 0, nx, ny, gdalconst.GDT_Float32, data);
        raster.delete();
        band.delete();

        // Extract: units, name, description, start/endTime, and interval from fileName
        Map<String,String> parsedInfo = parseFile(fileName);
        String units, shortName, fullName, description;
        units = getUnits(); // FIXME: Implement getUnits
        shortName = fullName = description = getName(); // FIXME: Get actual name
        ZonedDateTime startTime, endTime;
        ZonedDateTime[] timeSpan = getTimeSpan(parsedInfo);
        startTime = timeSpan[0];
        endTime   = timeSpan[1];
        Duration interval = Duration.between(startTime, endTime);

        // Build grid from extracted info
        VortexGrid dto = VortexGrid.builder()
                .dx(dx).dy(dy).nx(nx).ny(ny)
                .originX(ulx).originY(uly)
                .wkt(wkt).data(data).units(units)
                .fileName(path).shortName(shortName)
                .fullName(fullName).description(description)
                .startTime(startTime).endTime(endTime)
                .interval(interval)
                .build();

        return dto;
    } // getGrid()

} // SnodasDataReader class