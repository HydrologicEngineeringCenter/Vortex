package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

class SnodasDataReader extends DataReader {
    static {GdalRegister.getInstance();}
    SnodasDataReader(DataReaderBuilder builder) {super(builder);}

    @Override
    public List<VortexData> getDtos() {
        // Read in raster using Gdal
        Dataset in = gdal.Open(this.path);
        Vector options = gdal.ParseCommandLine("-of MEM -a_srs \"+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs\" -a_nodata -9999 -a_ullr  -124.73333333 52.87500000 -66.94166667 24.95000000");
        TranslateOptions translateOptions = new TranslateOptions(options);
        Dataset raster = gdal.Translate("raster", in, translateOptions);
        raster.FlushCache();
        
        // Get the file name
        String fileSeparator = System.getProperty("file.separator");
        String filePath = this.path;
        String fileName = filePath.substring(filePath.lastIndexOf(fileSeparator) + 1);

        // Build the grid and add to list of Data Transferable Objects (DTOs)
        VortexData dto = getGrid(raster, fileName);
        List<VortexData> list = new ArrayList<>();
        list.add(dto);

        return list;
    } // getDtos()

    @Override
    public int getDtoCount() {
        return 1;
    } // getDtoCount()

    @Override
    public VortexData getDto(int idx) {
        if (idx == 0)
            return getDtos().get(0);
        else
            return null;
    } // getDto()

    @Override
    public List<VortexDataInterval> getDataIntervals() {
        return getDtos().stream()
                .map(VortexDataInterval::of)
                .toList();
    }

    @Override
    public Validation isValid() {
        return Validation.of(true);
    }

    private static Map<String,String> parseFile(String fileName) {
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

    private static String getUnits(Map<String,String> info) {
        String productCode = info.get("product");
        String unit;

        switch (productCode) {
            case "1034":
            case "1036":
                unit = "mm";
                break;
            case "1044":
            case "1050":
            case "1039":
                unit = "m";
                break;
            case "1025":
                unit = "kg/m^2";
                break;
            case "1038":
                unit = "k"; // kelvin
                break;
            default:
                unit = "Error: Invalid product code";
                break;
        }

        return unit;
    } // getUnits()

    private static float getScaleFactor(Map<String, String> info) {
        String productCode = info.get("product");
        switch (productCode) {
            case "1044":
            case "1050":
            case "1039":
                return 100000.0f;
            case "1025":
                return 10.0f;
            default:
                return 1.0f;
        }
    }

    private static String getName(Map<String, String> info) {
        String productCode = info.get("product");
        String dataType = info.get("dType");
        String name = "";

        if(productCode.equals("1034"))
            name = "SWE";
        else if(productCode.equals("1036"))
            name = "Snow Depth";
        else if(productCode.equals("1044"))
            name = "Snow Melt Runoff at the Base of the Snow Pack";
        else if(productCode.equals("1050"))
            name = "Sublimation from the Snow Pack";
        else if(productCode.equals("1039"))
            name = "Sublimation of Blowing Snow";
        else if(productCode.equals("1025") && dataType.equals("lL01"))
            name = "Solid Precipitation";
        else if(productCode.equals("1025") && dataType.equals("lL00"))
            name = "Liquid Precipitation";
        else if(productCode.equals("1038"))
            name = "Snow Pack Average Temperature";
        else
            name = "Error: Invalid product code";

        return name;
    } // getName()

    private static ZonedDateTime[] getTimeSpan(Map<String,String> info) {
        ZonedDateTime[] timeSpan = new ZonedDateTime[2];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(info.get("date"), formatter);
        ZonedDateTime startTime = null, endTime = null;

        // Product Code meanings: https://nsidc.org/data/G02158
        switch(info.get("product")) {
            case "1044": case "1050": case "1039": case "1025":
                startTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(6,0)), ZoneId.of("UTC"));
                endTime   = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), LocalTime.of(6,0)), ZoneId.of("UTC"));
                break;
            case "1034": case "1036": case "1038":
                startTime = endTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(6,0)), ZoneId.of("UTC"));
                break;
        } // Switch case for product

        timeSpan[0] = startTime;
        timeSpan[1] = endTime;

        return timeSpan;
    } // getTimeSpan()

    private static VortexGrid getGrid(Dataset raster, String fileName) {
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
        units = getUnits(parsedInfo);
        shortName = fullName = description = getName(parsedInfo);
        ZonedDateTime startTime, endTime;
        ZonedDateTime[] timeSpan = getTimeSpan(parsedInfo);
        startTime = timeSpan[0];
        endTime   = timeSpan[1];
        Duration interval = Duration.between(startTime, endTime);

        float scaleFactor = getScaleFactor(parsedInfo);
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] / scaleFactor;
        }

        // Build grid from extracted info
        VortexGrid dto = VortexGrid.builder()
                .dx(dx).dy(dy).nx(nx).ny(ny)
                .originX(ulx).originY(uly)
                .wkt(wkt).data(data).units(units)
                .fileName(fileName).shortName(shortName)
                .fullName(fullName).description(description)
                .startTime(startTime).endTime(endTime)
                .interval(interval)
                .build();
        // FIXME: Add to stop  memory leaks
        data = null;

        return dto;
    } // getGrid()

    public static Set<String> getVariables(String pathToFile) {
        if(pathToFile.endsWith(".dat")) {
            return Collections.singleton(getVariable(pathToFile));
        }

        // Create input stream for Tar, to read its contents
        String directoryPath = Paths.get(pathToFile).getParent().toString();
        String tarFileName = Paths.get(pathToFile).getFileName().toString();
        File tarFile = new File(directoryPath, tarFileName);
        List<String> fileNames = new ArrayList<>();

        try {
            TarArchiveInputStream tarStream = new TarArchiveInputStream(new FileInputStream(tarFile));

            // Loop through tar contents
            ArchiveEntry nextEntry;
            while((nextEntry = tarStream.getNextEntry()) != null) {
                String entryFileName = nextEntry.getName();
                if (entryFileName.contains(".dat")) {
                    fileNames.add(entryFileName);
                } // If the entry is .dat file
            } // Get a list of file names (that ended with .dat)
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Parse file names to map with variable, and save to a HashSet
        HashSet<String> variables = new HashSet<>();
        fileNames.forEach(f -> variables.add(getVariable(f)));

        return variables;
    } // getVariables()

    public static String getVariable(String pathToDat) {
        if(pathToDat.contains(File.separator)) {
            File datFile = new File(pathToDat);
            pathToDat = datFile.getName();
        }
        return getName(parseFile(pathToDat.substring(0, pathToDat.lastIndexOf(".dat"))));
    }

    @Override
    public void close() throws Exception {
        // No op
    }
} // SnodasDataReader class
