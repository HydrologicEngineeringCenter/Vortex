package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.WktParser;
import mil.army.usace.hec.vortex.util.VortexTimeUtils;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class VortexGridCollection {
    private static final Logger logger = Logger.getLogger(VortexGridCollection.class.getName());

    private final List<VortexGrid> vortexGridList;
    private final VortexGrid defaultGrid;
    private final double[] xCoordinates;
    private final double[] yCoordinates;
    private final ZonedDateTime baseTime;

    public VortexGridCollection(List<VortexGrid> vortexGrids) {
        baseTime = VortexTimeUtils.BASE_TIME;

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
    private static List<VortexGrid> sanitizeCollection(List<VortexGrid> vortexGrids) {
        if (vortexGrids == null || vortexGrids.isEmpty()) {
            return Collections.emptyList();
        }

        VortexGrid baseGrid = vortexGrids.get(0);
        String baseWkt = baseGrid.wkt();

        Predicate<VortexGrid> predicate = vortexGrid -> {
            boolean sameWkt = Objects.equals(baseWkt, vortexGrid.wkt());

            if (sameWkt) {
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

    public float getNoDataValue() {
        return (float) defaultGrid.noDataValue();
    }

    public VortexDataType getDataType() {
        return defaultGrid.dataType();
    }

    /* Name & Description */
    public Map<String, VortexGrid> getRepresentativeGridNameMap() {
        return vortexGridList.stream().collect(Collectors.toMap(VortexGrid::shortName, g -> g, (existing, replacement) -> existing));
    }

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

    public String getTimeUnits() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
        String durationUnit = VortexTimeUtils.getDurationUnit(getBaseDuration()).toString();
        return durationUnit + " since " + baseTime.format(dateTimeFormatter);
    }

    public Duration getInterval() {
        return defaultGrid.interval();
    }

    private Duration getBaseDuration() {
        Duration interval = getInterval();
        return interval.isZero() ? Duration.ofMinutes(1) : interval;
    }
}
