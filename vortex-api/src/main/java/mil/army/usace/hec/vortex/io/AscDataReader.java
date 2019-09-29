package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class AscDataReader extends DataReader {

    static {
        GdalRegister.getInstance();
    }

    AscDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {

        Dataset in = gdal.Open(path.toString());
        ArrayList<String>  options =  new ArrayList<>();
        options.add("-of");
        options.add("MEM");
        TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
        Dataset raster = gdal.Translate("raster", in, translateOptions);
        raster.FlushCache();

        String fileName = path.getFileName().toString().toLowerCase();
        String shortName;
        String fullName;
        String description;
        AtomicBoolean isPrismTemporal = new AtomicBoolean();
        AtomicBoolean isPrismNormal = new AtomicBoolean();
        if (fileName.matches("prism.*ppt.*stable.*")) {
            shortName = "precipitation";
            fullName = "precipitation";
            description = "precipitation";
            isPrismTemporal.set(true);
        } else if (fileName.matches("prism.*ppt.*normal.*")) {
            shortName = "precipitation";
            fullName = "precipitation";
            description = "precipitation";
            isPrismNormal.set(true);
        } else if (fileName.matches("prism.*tmean.*stable.*")){
            shortName = "mean temperature";
            fullName = "mean temperature";
            description = "mean temperature";
            isPrismTemporal.set(true);
        } else if (fileName.matches("prism.*tmin.*stable.*")){
            shortName = "minimum temperature";
            fullName = "minimum temperature";
            description = "minimum temperature";
            isPrismTemporal.set(true);
        } else if (fileName.matches("prism.*tmax.*stable.*")){
            shortName = "maximum temperature";
            fullName = "maximum temperature";
            description = "maximum temperature";
            isPrismTemporal.set(true);
        } else if (fileName.matches("prism.*tdmean.*stable.*")){
            shortName = "mean dewpoint temperature";
            fullName = "mean dewpoint temperature";
            description = "mean dewpoint temperature";
            isPrismTemporal.set(true);
        } else if (fileName.matches("prism.*vpdmin.*stable.*")){
            shortName = "minimum vapor pressure deficit";
            fullName = "minimum vapor pressure deficit";
            description = "minimum vapor pressure deficit";
            isPrismTemporal.set(true);
        } else if (fileName.matches("prism.*vpdmax.*stable.*")){
            shortName = "maximum vapor pressure deficit";
            fullName = "maximum vapor pressure deficit";
            description = "maximum vapor pressure deficit";
            isPrismTemporal.set(true);
        }   else {
            shortName = "";
            fullName = "";
            description = "";
        }

        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        if (isPrismTemporal.get()) {
            String string1 = path.toString();
            String string2 = string1.substring(0, string1.lastIndexOf('_'));
            String string3 = string2.substring(string2.length() - 8);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date = LocalDate.parse(string3, formatter);
            startTime = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(0, 0)), ZoneId.of("UTC"));
            endTime = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 0)), ZoneId.of("UTC"));
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

        raster.delete();
        band.delete();

        VortexGrid dto = VortexGrid.builder()
                .dx(dx).dy(dy).nx(nx).ny(ny)
                .originX(ulx).originY(uly)
                .wkt(wkt).data(data).units(units)
                .fileName(path.toString()).shortName(shortName)
                .fullName(fullName).description(description)
                .startTime(startTime).endTime(endTime)
                .interval(interval)
                .build();

        List<VortexData> list = new ArrayList<>();
        list.add(dto);
        return list;
    }

    public static Set<String> getVariables(Path pathToAsc){
        String fileName = pathToAsc.getFileName().toString();
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
}
