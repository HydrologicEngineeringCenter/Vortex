package mil.army.usace.hec.vortex.io.reader;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexTimeRecord;
import mil.army.usace.hec.vortex.util.FilenameUtil;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AscDataReader implements FileDataReader {
    private static final Logger logger = Logger.getLogger(AscDataReader.class.getName());
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private final String pathToFile;

    static {
        GdalRegister.getInstance();
    }

    AscDataReader(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    @Override
    public List<VortexData> getDtos() {

        String fileName = new File(pathToFile).getName().toLowerCase();
        String shortName;
        String fullName;
        String description;

        AtomicBoolean isPrismTemporalDaily = new AtomicBoolean();
        AtomicBoolean isPrismTemporalMonthly = new AtomicBoolean();
        AtomicBoolean isPrismNormal = new AtomicBoolean();
        AtomicBoolean isQpfHourly = new AtomicBoolean();

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
            if (fileName.matches("prism.*tmin.*(stable|provisional|early).*d2.*(d1|d2).*"))
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
        } else if (fileName.matches("qpf.*1hr.*")) {
            shortName = "precipitation";
            fullName = "precipitation";
            description = "precipitation";
            isQpfHourly.set(true);
        } else if (fileName.matches(".*yr.*(ha|ma|da).*")) {
            shortName = "precipitation-frequency";
            fullName = "precipitation-frequency";
            description = FilenameUtil.removeExtension(Paths.get(fileName).getFileName().toString(), true);
        } else if (fileName.toLowerCase().matches("windspeed.*")) {
            shortName = "windspeed";
            fullName = "windspeed";
            description = "windspeed";
        } else {
            shortName = "";
            fullName = "";
            description = "";
        }

        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        if (isPrismTemporalDaily.get()) {
            String string1 = pathToFile;
            String string2 = string1.substring(0, string1.lastIndexOf('_'));
            String string3 = string2.substring(string2.length() - 8);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date = LocalDate.parse(string3, formatter);
            startTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(0, 0)), ZoneId.of("UTC")).minusHours(12);
            endTime = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 0)), ZoneId.of("UTC")).minusHours(12);
            interval = Duration.between(startTime, endTime);
        } else if (isPrismNormal.get()){
            startTime = ZonedDateTime.of(1981, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            endTime = ZonedDateTime.of(2010, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC"));
            interval = Duration.between(startTime, endTime);
        } else if (isPrismTemporalMonthly.get()) {
            Pattern pattern = Pattern.compile("\\d{6}");
            Matcher matcher = pattern.matcher(pathToFile);
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
        } else if (isQpfHourly.get()) {
            String filenameSansExt = fileName.replaceFirst("[.][^.]+$", "");
            String dateString = filenameSansExt.substring(filenameSansExt.length() - 8);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHH");
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
            endTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
            startTime = endTime.minusHours(1);
            interval = Duration.ofHours(1);
        } else {
            Pattern pattern = Pattern.compile("\\d{4}[_-]\\d{2}[_-]\\d{2}[t_-]\\d{4}");
            Matcher matcher = pattern.matcher(fileName);
            List<String> dateStrings = new ArrayList<>();
            while (matcher.find()){
                dateStrings.add(matcher.group(0));
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy['_']['-']MM['_']['-']dd['t']['_']['-']Hmm");
            if (dateStrings.size() == 2) {
                startTime = ZonedDateTime.of(LocalDateTime.parse(dateStrings.get(0), formatter), ZoneId.of("Z"));
                endTime = ZonedDateTime.of(LocalDateTime.parse(dateStrings.get(1), formatter), ZoneId.of("Z"));
                interval = Duration.between(startTime, endTime);
            } else if(dateStrings.size() == 1 ) {
                startTime = ZonedDateTime.of(LocalDateTime.parse(dateStrings.get(0), formatter), ZoneId.of("Z"));
                endTime = ZonedDateTime.from(startTime);
                interval = Duration.ZERO;
            } else {
                startTime = null;
                endTime = null;
                interval = null;
            }

        }

        String units;
        if (fileName.contains("ppt") || fileName.contains("qpf")){
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
        } else if (fileName.contains("vpdmax")) {
            units = "hPa";
        } else if (fileName.matches(".*yr.*(ha|ma|da).*")) {
            units = "1/1000 in";
        } else {
            units = "";
        }

        Dataset dataset = gdal.Open(pathToFile);

        double[] geoTransform = dataset.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double ulx = geoTransform[0];
        double uly = geoTransform[3];
        int nx = dataset.GetRasterXSize();
        int ny = dataset.GetRasterYSize();
        String wkt = dataset.GetProjection();
        Band band = dataset.GetRasterBand(1);
        float[] data = new float[nx * ny];
        band.ReadRaster(0, 0, nx, ny, gdalconst.GDT_Float32, data);

        Double[] value = new Double[1];
        band.GetNoDataValue(value);
        double noDataValue = value[0] != null ? value[0] : -9999.0;

        dataset.delete();
        band.delete();

        VortexGrid dto = VortexGrid.builder()
                .dx(dx).dy(dy)
                .nx(nx).ny(ny)
                .originX(ulx)
                .originY(uly)
                .wkt(wkt)
                .data(data)
                .noDataValue(noDataValue)
                .units(units)
                .fileName(pathToFile)
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

    public static Set<String> getVariables(String pathToAsc){
        String fileName = new File(pathToAsc).getName();
        if (fileName.startsWith("PRISM_ppt")) {
            return new HashSet<>(Collections.singletonList("ppt"));
        }
        if (fileName.startsWith("QPF_1HR")) {
            return new HashSet<>(Collections.singletonList("precipitation"));
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
    public List<VortexTimeRecord> getTimeRecords() {
        return getDtos()
                .stream()
                .map(VortexTimeRecord::of)
                .toList();
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return support;
    }
}
