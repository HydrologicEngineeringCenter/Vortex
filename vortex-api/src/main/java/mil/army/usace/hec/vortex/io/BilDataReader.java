package mil.army.usace.hec.vortex.io;

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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads in a BIL file to a VortexData object
 */
class BilDataReader extends DataReader {
    private static final Logger logger = Logger.getLogger(BilDataReader.class.getName());

    static {
        GdalRegister.getInstance();
    }

    BilDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {

        Dataset in = gdal.Open(path);
        ArrayList<String>  options =  new ArrayList<>();
        options.add("-of");
        options.add("MEM");
        TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
        Dataset raster = gdal.Translate("raster", in, translateOptions);
        raster.FlushCache();

        String fileName = new File(path).getName().toLowerCase();
        String shortName;
        String fullName;
        String description;
        AtomicBoolean isPrismTemporalDaily = new AtomicBoolean();
        AtomicBoolean isPrismTemporalMonthly = new AtomicBoolean();
        AtomicBoolean isPrismNormal = new AtomicBoolean();
        if (fileName.matches("prism.*ppt.*(stable|provisional|early).*")) {
            shortName = "precipitation";
            fullName = "precipitation";
            description = "precipitation";
            if (fileName.matches("prism.*ppt.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*ppt.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        } else if (fileName.matches("prism.*ppt.*normal.*")) {
            shortName = "precipitation";
            fullName = "precipitation";
            description = "precipitation";
            isPrismNormal.set(true);
        } else if (fileName.matches("prism.*tmean.*(stable|provisional|early).*")){
            shortName = "mean temperature";
            fullName = "mean temperature";
            description = "mean temperature";
            if (fileName.matches("prism.*tmean.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*tmean.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        } else if (fileName.matches("prism.*tmin.*(stable|provisional|early).*")){
            shortName = "minimum temperature";
            fullName = "minimum temperature";
            description = "minimum temperature";
            if (fileName.matches("prism.*tmin.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*tmin.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        } else if (fileName.matches("prism.*tmax.*(stable|provisional|early).*")){
            shortName = "maximum temperature";
            fullName = "maximum temperature";
            description = "maximum temperature";
            if (fileName.matches("prism.*tmax.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*tmax.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        } else if (fileName.matches("prism.*tdmean.*(stable|provisional|early).*")){
            shortName = "mean dewpoint temperature";
            fullName = "mean dewpoint temperature";
            description = "mean dewpoint temperature";
            if (fileName.matches("prism.*tdmean.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*tdmean.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        } else if (fileName.matches("prism.*vpdmin.*(stable|provisional|early).*")){
            shortName = "minimum vapor pressure deficit";
            fullName = "minimum vapor pressure deficit";
            description = "minimum vapor pressure deficit";
            if (fileName.matches("prism.*vpdmin.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*vpdmin.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        } else if (fileName.matches("prism.*vpdmax.*(stable|provisional|early).*")){
            shortName = "maximum vapor pressure deficit";
            fullName = "maximum vapor pressure deficit";
            description = "maximum vapor pressure deficit";
            if (fileName.matches("prism.*vpdmax.*(stable|provisional|early).*(d1|d2).*"))
                isPrismTemporalDaily.set(true);
            if (fileName.matches("prism.*vpdmax.*(stable|provisional|early).*m3.*"))
                isPrismTemporalMonthly.set(true);
        }   else {
            shortName = "";
            fullName = "";
            description = "";
        }

        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        if (isPrismTemporalDaily.get()) {
            String string1 = path;
            String string2 = string1.substring(0, string1.lastIndexOf('_'));
            String string3 = string2.substring(string2.length() - 8);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date;
            try {
                date = LocalDate.parse(string3, formatter);
            } catch (DateTimeParseException e) {
                logger.log(Level.WARNING, e, e::getMessage);
                return Collections.emptyList();
            }
            startTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(0, 0)), ZoneId.of("UTC")).minusHours(12);
            endTime = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 0)), ZoneId.of("UTC")).minusHours(12);
            interval = Duration.between(startTime, endTime);

        } else if (isPrismTemporalMonthly.get()) {
            Pattern pattern = Pattern.compile("\\d{6}");
            Matcher matcher = pattern.matcher(path);
            String dateString = "";
            while (matcher.find()) {
                dateString = matcher.group(0);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
            YearMonth yearMonth;
            try {
                yearMonth = YearMonth.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                logger.log(Level.WARNING, e, e::getMessage);
                return Collections.emptyList();
            }
            LocalDateTime startDay = LocalDateTime.of(yearMonth.getYear(), yearMonth.getMonth(), 1, 0, 0);
            startTime = ZonedDateTime.of(startDay, ZoneId.of("UTC"));
            endTime = startTime.plusMonths(1);
            interval = Duration.between(startTime, endTime);

        } else if (isPrismNormal.get()){
            startTime = ZonedDateTime.of(1981, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            endTime = ZonedDateTime.of(2010, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC"));
            interval = Duration.between(startTime, endTime);
        } else {
            startTime = null;
            endTime = null;
            interval = null;
        }

        String units;
        if (fileName.contains("ppt")){
            units = "mm";
        } else if (fileName.contains("tmean")){
            units = "Degrees C";
        } else if (fileName.contains("tmin")){
            units = "Degrees C";
        } else if (fileName.contains("tmax")){
            units = "Degrees C";
        } else if (fileName.contains("tdmean")){
            units = "Degrees C";
        } else if (fileName.contains("vpdmin")){
            units = "hPa";
        } else if (fileName.contains("vpdmax")){
            units = "hPa";
        } else {
            units = "";
        }

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

        Double[] value = new Double[1];
        band.GetNoDataValue(value);
        double noDataValue = value[0];

        raster.delete();
        band.delete();

        VortexGrid dto = VortexGrid.builder()
                .dx(dx)
                .dy(dy)
                .nx(nx)
                .ny(ny)
                .originX(ulx)
                .originY(uly)
                .wkt(wkt)
                .data(data)
                .noDataValue(noDataValue)
                .units(units)
                .fileName(path)
                .shortName(shortName)
                .fullName(fullName)
                .description(description)
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval)
                .build();

        List<VortexData> list = new ArrayList<>();
        list.add(dto);
        return list;
    }

    public static Set<String> getVariables(String pathToBil){
        String fileName = new File(pathToBil).getName();
        if (fileName.startsWith("PRISM_ppt")) {
            return new HashSet<>(Collections.singletonList("ppt"));
        }
        if (fileName.startsWith("PRISM_tmean")) {
            return new HashSet<>(Collections.singletonList("tmean"));
        }
        if (fileName.startsWith("PRISM_tmin")) {
            return new HashSet<>(Collections.singletonList("tmin"));
        }
        if (fileName.startsWith("PRISM_tmax")) {
            return new HashSet<>(Collections.singletonList("tmax"));
        }
        if (fileName.startsWith("PRISM_tdmean")) {
            return new HashSet<>(Collections.singletonList("tdmean"));
        }
        if (fileName.startsWith("PRISM_vpdmin")) {
            return new HashSet<>(Collections.singletonList("vpdmin"));
        }
        if (fileName.startsWith("PRISM_vpdmax")) {
            return new HashSet<>(Collections.singletonList("vpdmax"));
        }
        return new HashSet<>(Collections.singletonList(fileName.substring(0, fileName.lastIndexOf('.'))));
    }

    @Override
    public int getDtoCount() {
        return 1;
    }

    @Override
    public VortexData getDto(int idx) {
        if (idx == 0)
            return getDtos().get(0);
        else {
            return null;
        }
    }

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

    @Override
    public void close() throws Exception {
        // No op
    }
} // BilDataReader class
