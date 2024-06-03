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
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VortexGridCollection {
    private static final Logger logger = Logger.getLogger(VortexGridCollection.class.getName());

    private final List<VortexGrid> vortexGridList;
    private final VortexGrid defaultGrid;
    private final double[] xCoordinates;
    private final double[] yCoordinates;
    private final ZonedDateTime baseTime;

    public VortexGridCollection(List<VortexGrid> vortexGrids) {
        baseTime = initBaseTime();

        vortexGridList = sanitizeCollection(vortexGrids);
        if (!vortexGridList.isEmpty()) {
            defaultGrid = vortexGrids.get(0);
            xCoordinates = generateCoordinates(defaultGrid.originX(), defaultGrid.dx(), defaultGrid.nx());
            yCoordinates = generateCoordinates(defaultGrid.originY(), defaultGrid.dy(), defaultGrid.ny());
        } else {
            defaultGrid = VortexGrid.noDataGrid();
            xCoordinates = new double[0];
            yCoordinates = new double[0];
        }
    }

    /* Init */
    private ZonedDateTime initBaseTime() {
        return ZonedDateTime.of(1900,1,1,0,0,0,0, ZoneId.of("UTC"));
    }

    private static List<VortexGrid> sanitizeCollection(List<VortexGrid> vortexGrids) {
        if (vortexGrids == null || vortexGrids.isEmpty()) {
            return Collections.emptyList();
        }

        VortexGrid baseGrid = vortexGrids.get(0);
        String baseShortName = baseGrid.shortName();
        String baseWkt = baseGrid.wkt();

        Predicate<VortexGrid> predicate = vortexGrid -> {
            boolean sameShortName = Objects.equals(baseShortName, vortexGrid.shortName());
            boolean sameWkt = Objects.equals(baseWkt, vortexGrid.wkt());

            if (sameShortName && sameWkt) {
                return true;
            } else {
                logger.info(() -> "Filtered from collection: " + vortexGrid);
                return false;
            }
        };

        return vortexGrids.stream()
                .filter(predicate)
                .toList();
    }

    private static double[] generateCoordinates(double origin, double stepSize, int count) {
        double[] coordinates = new double[count];

        for (int i = 0; i < count; i++) {
            coordinates[i] = origin + (i + 1) * stepSize - (stepSize / 2);
        }

        return coordinates;
    }

    /* Conditionals */
    public boolean isGeographic() {
        return getProjection() instanceof LatLonProjection;
    }

    public boolean hasTimeDimension() {
        return vortexGridList.stream().allMatch(VortexGrid::hasTime);
    }

    public boolean hasTimeBounds() {
        Duration interval = getInterval();
        return interval != null && !interval.isZero();
    }

    /* Data */
    public Stream<Map.Entry<Integer, VortexGrid>> getCollectionDataStream() {
        return IntStream.range(0, vortexGridList.size()).parallel().mapToObj(i -> Map.entry(i, vortexGridList.get(i)));
    }

    public double getNoDataValue() {
        return defaultGrid.noDataValue();
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
        return "Unknown Projection Name";
    }

    public String getProjectionUnit() {
        return WktParser.getProjectionUnit(getWkt());
    }

    public String getWkt() {
        return defaultGrid.wkt();
    }

    /* Y and X */
    public double[] getYCoordinates() {
        return yCoordinates.clone();
    }

    public double[] getXCoordinates() {
        return xCoordinates.clone();
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

    public long[] getTimeData() {
        if (vortexGridList.size() == 1) {
            return new long[] {getNumDurationsFromBaseTime(vortexGridList.get(0).endTime())};
        }

        int numData = vortexGridList.size();
        long[] timeData = new long[numData];
        for (int i = 0; i < numData; i++) {
            VortexGrid grid = vortexGridList.get(i);
            long startTime = getNumDurationsFromBaseTime(grid.startTime());
            long endTime = getNumDurationsFromBaseTime(grid.endTime());
            long midTime = (startTime + endTime) / 2;
            timeData[i] = midTime;
        }

        return timeData;
    }

    public long[][] getTimeBoundsArray() {
        long[][] timeBoundArray = new long[getTimeLength()][NetcdfGridWriter.BOUNDS_LEN];

        for (int i = 0; i < vortexGridList.size(); i++) {
            VortexGrid grid = vortexGridList.get(i);
            long startTime = getNumDurationsFromBaseTime(grid.startTime());
            long endTime = getNumDurationsFromBaseTime(grid.endTime());
            timeBoundArray[i][0] = startTime;
            timeBoundArray[i][1] = endTime;
        }

        return timeBoundArray;
    }

    public Duration getInterval() {
        return defaultGrid.interval();
    }

    /* Helpers */
    private long getNumDurationsFromBaseTime(ZonedDateTime dateTime) {
        ZonedDateTime zDateTime = dateTime.withZoneSameInstant(ZoneId.of("Z"));
        Duration durationBetween = Duration.between(baseTime, zDateTime);
        Duration divisor = Duration.of(1, getDurationUnit(getBaseDuration()));
        return durationBetween.dividedBy(divisor);
    }

    private ChronoUnit getDurationUnit(Duration duration) {
        if (duration.toHours() > 0) {
            return ChronoUnit.HOURS;
        } else if (duration.toMinutes() > 0) {
            return ChronoUnit.MINUTES;
        } else {
            return ChronoUnit.SECONDS;
        }
    }

    private Duration getBaseDuration() {
        Duration interval = getInterval();
        return interval.isZero() ? Duration.ofMinutes(1) : interval;
    }
}
