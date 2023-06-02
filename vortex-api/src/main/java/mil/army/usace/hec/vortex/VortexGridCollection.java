package mil.army.usace.hec.vortex;

import mil.army.usace.hec.vortex.io.NetcdfGridWriter;
import mil.army.usace.hec.vortex.geo.WktParser;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VortexGridCollection {
    private static final Logger logger = Logger.getLogger(VortexGridCollection.class.getName());

    private final List<VortexGrid> vortexGridList;
    private final VortexGrid defaultGrid;
    private final ZonedDateTime baseTime;

    public VortexGridCollection(List<VortexGrid> vortexGrids) {
        vortexGridList = vortexGrids;
        defaultGrid = !vortexGridList.isEmpty() ? vortexGrids.get(0) : null;
        baseTime = initBaseTime();
        cleanCollection();
    }

    /* Init */
    private ZonedDateTime initBaseTime() {
        return ZonedDateTime.of(1900,1,1,0,0,0,0, ZoneId.of("UTC"));
    }

    private void cleanCollection() {
        String shortName = defaultGrid.shortName();
        String wkt = defaultGrid.wkt();
        Duration interval = defaultGrid.interval();

        if (shortName.isEmpty()) logger.warning("Short name not found");
        if (wkt.isEmpty()) logger.warning("Wkt not found");
        if (interval.equals(Duration.ZERO)) logger.warning("Interval not found");

        vortexGridList.removeIf(g -> !Objects.equals(shortName, g.shortName()));
        vortexGridList.removeIf(g -> !Objects.equals(wkt, g.wkt()));
        vortexGridList.removeIf(g -> !Objects.equals(interval, g.interval()));
    }

    /* Conditionals */
    public boolean isGeographic() {
        return getProjection() instanceof LatLonProjection;
    }

    public boolean hasTimeBounds() {
        return !getInterval().isZero();
    }

    /* Data */
    public Stream<Map.Entry<Integer, VortexGrid>> getCollectionDataStream() {
        return IntStream.range(0, vortexGridList.size()).parallel().mapToObj(i -> Map.entry(i, vortexGridList.get(i)));
    }

    public float getNoDataValue() {
        return (float) defaultGrid.noDataValue();
    }

    public String getDataUnit() {
        return defaultGrid.units();
    }

    public VortexDataType getDataType() {
        return defaultGrid.dataType();
    }

    /* Name & Description */
    public String getShortName() {
        return defaultGrid.shortName();
    }

    public String getDescription() {
        return defaultGrid.description();
    }

    /* Projection */
    public Projection getProjection() {
        return WktParser.getProjection(getWkt());
    }

    public String getProjectionName() {
        Projection projection = getProjection();
        for (Parameter parameter : projection.getProjectionParameters()) {
            if (parameter.getName().equals(CF.GRID_MAPPING_NAME)) {
                return parameter.getStringValue();
            }
        }
        return null;
    }

    public String getProjectionUnit() {
        return WktParser.getProjectionUnit(getWkt());
    }

    public String getWkt() {
        return defaultGrid.wkt();
    }

    /* Y and X */
    public double[] getYCoordinates() {
        return defaultGrid.yCoordinates();
    }

    public double[] getXCoordinates() {
        return defaultGrid.xCoordinates();
    }

    public int getNy() {
        return defaultGrid.ny();
    }

    public int getNx() {
        return defaultGrid.nx();
    }

    /* Lat & Lon */
    public Map<String, double[][]> getLatLonCoordinates() {
        Map<String, double[][]> latLonMap = new HashMap<>();

        int ny = getNy();
        int nx = getNx();
        double[] yCoordinates = getYCoordinates();
        double[] xCoordinates = getXCoordinates();
        Projection projection = getProjection();

        double[][] latData = new double[ny][nx];
        double[][] lonData = new double[ny][nx];

        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                ProjectionPoint point = ProjectionPoint.create(xCoordinates[x], yCoordinates[y]);
                LatLonPoint latlonPoint = projection.projToLatLon(point);
                latData[y][x] = latlonPoint.getLatitude();
                lonData[y][x] = latlonPoint.getLongitude();
            }
        }

        latLonMap.put("lat", latData);
        latLonMap.put("lon", lonData);

        return latLonMap;
    }

    /* Time */
    public int getTimeLength() {
        return vortexGridList.size();
    }

    public String getTimeUnits() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
        String durationUnit = getDurationUnit(getBaseDuration()).toString();
        return durationUnit + " since " + baseTime.format(dateTimeFormatter);
    }

    public float[] getTimeData() {
        if (vortexGridList.size() == 1) {
            return new float[] {getNumDurationsFromBaseTime(vortexGridList.get(0).endTime())};
        }

        int numData = vortexGridList.size();
        float[] timeData = new float[numData];
        for (int i = 0; i < numData; i++) {
            VortexGrid grid = vortexGridList.get(i);
            float startTime = getNumDurationsFromBaseTime(grid.startTime());
            float endTime = getNumDurationsFromBaseTime(grid.endTime());
            float midTime = (startTime + endTime) / 2;
            timeData[i] = midTime;
        }

        return timeData;
    }

    public float[][] getTimeBoundsArray() {
        float[][] timeBoundArray = new float[getTimeLength()][NetcdfGridWriter.BOUNDS_LEN];

        for (int i = 0; i < vortexGridList.size(); i++) {
            VortexGrid grid = vortexGridList.get(i);
            float startTime = getNumDurationsFromBaseTime(grid.startTime());
            float endTime = getNumDurationsFromBaseTime(grid.endTime());
            timeBoundArray[i][0] = startTime;
            timeBoundArray[i][1] = endTime;
        }

        return timeBoundArray;
    }

    public Duration getInterval() {
        return defaultGrid.interval();
    }

    /* Helpers */
    private float getNumDurationsFromBaseTime(ZonedDateTime dateTime) {
        ZonedDateTime zDateTime = dateTime.withZoneSameInstant(ZoneId.of("Z"));
        Duration durationBetween = Duration.between(baseTime, zDateTime);
        Duration divisor = getBaseDuration();
        return durationBetween.dividedBy(divisor);
    }

    private ChronoUnit getDurationUnit(Duration duration) {
        if (duration.toDays() > 0) return ChronoUnit.DAYS;
        if (duration.toHours() > 0) return ChronoUnit.HOURS;
        if (duration.toMinutes() > 0) return ChronoUnit.MINUTES;
        return ChronoUnit.SECONDS;
    }

    private Duration getBaseDuration() {
        Duration interval = getInterval();
        return interval.isZero() ? Duration.ofHours(1) : interval;
    }
}
