package mil.army.usace.hec.vortex;

import mil.army.usace.hec.vortex.geo.WktParser;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.util.Parameter;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VortexGridCollection {
    private static final Logger logger = Logger.getLogger(VortexGridCollection.class.getName());
    public static final int NV_LENGTH = 2;

    private final List<VortexGrid> vortexGridList;
    private final String shortName;
    private final String description;
    private final String dataUnit;
    private final int nx;
    private final int ny;
    private final double[] xCoordinates;
    private final double[] yCoordinates;
    private final double[][] lonCoordinates;
    private final double[][] latCoordinates;
    private final String wkt;
    private final Projection projection;
    private final String projectionUnit;
    private final Duration interval;
    private final ZoneId zoneId;
    private final ZonedDateTime baseTime;

    public VortexGridCollection(List<VortexGrid> vortexGrids) {
        vortexGridList = vortexGrids;

        shortName = mode(VortexGrid::shortName, "");
        description = mode(VortexGrid::description, "");
        dataUnit = mode(VortexGrid::units, "");
        nx = mode(VortexGrid::nx, 0);
        ny = mode(VortexGrid::ny, 0);
        xCoordinates = mode(VortexGrid::xCoordinates, new double[0]);
        yCoordinates = mode(VortexGrid::yCoordinates, new double[0]);
        wkt = mode(VortexGrid::wkt, "");
        projection = WktParser.getProjection(wkt);
        projectionUnit = WktParser.getProjectionUnit(wkt);
        Map<String, double[][]> latLonMap = getLatLonCoordinatesNew();
        lonCoordinates = latLonMap.get("lon");
        latCoordinates = latLonMap.get("lat");
        interval = initInterval();
        zoneId = mode(g -> g.startTime().getZone(), ZoneId.systemDefault());
        baseTime = ZonedDateTime.of(1900,1,1,0,0,0,0, zoneId);
        cleanCollection();
    }

    /* Helpers */
    private <T> T mode(Function<VortexGrid, T> gridFunction, T defaultReturn) {
        return vortexGridList.stream()
                .map(gridFunction)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultReturn);
    }

    private Duration initInterval() {
        Duration modeInterval = mode(VortexGrid::interval, Duration.ZERO);
        return (modeInterval.isZero()) ? Duration.ofDays(1) : modeInterval;
    }

    private Map<String, double[]> getLatLonCoordinates() {
        Map<String, double[]> latLonMap = new HashMap<>();

        int yLength = yCoordinates.length;
        int xLength = xCoordinates.length;

        double[] latCoords = new double[yLength * xLength];
        double[] lonCoords = new double[yLength * xLength];

        for (int y = 0; y < yLength; y++) {
            for (int x = 0; x < xLength; x++) {
                ProjectionPoint projPoint = ProjectionPoint.create(xCoordinates[x], yCoordinates[y]);
                LatLonPoint latlonPoint = projection.projToLatLon(projPoint);
                latCoords[y * xLength + x] = latlonPoint.getLatitude();
                lonCoords[y * xLength + x] = latlonPoint.getLongitude();
            }
        }

        latLonMap.put("lat", latCoords);
        latLonMap.put("lon", lonCoords);

        return latLonMap;
    }

    private Map<String, double[][]> getLatLonCoordinatesNew() {
        Map<String, double[][]> latLonMap = new HashMap<>();
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


    private float getNumDurationsFromBaseTime(ZonedDateTime dateTime) {
        Duration durationBetween = Duration.between(baseTime, dateTime);
        return durationBetween.dividedBy(interval);
    }

    private ChronoUnit getDurationUnit(Duration duration) {
        if (duration.toDays() > 0) return ChronoUnit.DAYS;
        if (duration.toHours() > 0) return ChronoUnit.HOURS;
        if (duration.toMinutes() > 0) return ChronoUnit.MINUTES;
        return ChronoUnit.SECONDS;
    }

    private void cleanCollection() {
        if (shortName.isEmpty()) logger.warning("Short name not found");
        if (wkt.isEmpty()) logger.warning("Wkt not found");
        if (interval.equals(Duration.ZERO)) logger.warning("Interval not found");

        vortexGridList.removeIf(g -> !Objects.equals(shortName, g.shortName()));
        vortexGridList.removeIf(g -> !Objects.equals(wkt, g.wkt()));
//        vortexGridList.removeIf(g -> !Objects.equals(interval, g.interval()));
        vortexGridList.removeIf(g -> !Objects.equals(zoneId, g.startTime().getZone()));
        vortexGridList.removeIf(g -> !Objects.equals(zoneId, g.endTime().getZone()));
    }

    /* Public API */
    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public String getDataUnit() {
        return dataUnit;
    }

    public String getWkt() {
        return wkt;
    }

    public Projection getProjection() {
        return projection;
    }

    public String getProjectionUnit() {
        return projectionUnit;
    }

    public Duration getInterval() {
        return interval;
    }

    public int getNx() {
        return nx;
    }

    public int getNy() {
        return ny;
    }

    public double[] getXCoordinates() {
        return Arrays.copyOf(xCoordinates, xCoordinates.length);
    }

    public double[] getYCoordinates() {
        return Arrays.copyOf(yCoordinates, yCoordinates.length);
    }

    public double[][] getLonCoordinates() {
        return Arrays.copyOf(lonCoordinates, lonCoordinates.length);
    }

    public double[][] getLatCoordinates() {
        return Arrays.copyOf(latCoordinates, latCoordinates.length);
    }

    public int getTimeLength() {
        return vortexGridList.size();
    }

    public String getTimeUnits() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
        return getDurationUnit(interval).toString() + " since " + baseTime.format(dateTimeFormatter);
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

    public float[][][] getData3D() {
        float[] timeData = getTimeData();
        float[][][] data3D = new float[timeData.length][ny][nx];

        for (int timeIndex = 0; timeIndex < timeData.length; timeIndex++) {
            VortexGrid grid = vortexGridList.get(timeIndex);
            data3D[timeIndex] = grid.data2D();
        }

        return data3D;
    }

    public float[][] getTimeBoundsArray() {
        float[][] timeBoundArray = new float[getTimeLength()][NV_LENGTH];

        for (int i = 0; i < vortexGridList.size(); i++) {
            VortexGrid grid = vortexGridList.get(i);
            float startTime = getNumDurationsFromBaseTime(grid.startTime());
            float endTime = getNumDurationsFromBaseTime(grid.endTime());
            timeBoundArray[i][0] = startTime;
            timeBoundArray[i][1] = endTime;
        }

        return timeBoundArray;
    }

    public float getNoDataValue() {
        return (float) vortexGridList.get(0).noDataValue();
    }

    public String getProjectionName() {
        for (Parameter parameter : projection.getProjectionParameters()) {
            if (parameter.getName().equals(CF.GRID_MAPPING_NAME)) {
                return parameter.getStringValue();
            }
        }
        return null;
    }

    public List<VortexGrid> getVortexGridList() {
        return List.copyOf(vortexGridList);
    }
}
