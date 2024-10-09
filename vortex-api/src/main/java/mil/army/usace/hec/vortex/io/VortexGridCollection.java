package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.WktParser;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private VortexGridCollection(List<VortexGrid> vortexGrids) {
        baseTime = NetcdfTimeUtils.ORIGIN_TIME;

        vortexGridList = sanitizeCollection(vortexGrids);
        if (!vortexGridList.isEmpty()) {
            defaultGrid = vortexGrids.get(0);
            xCoordinates = generateCoordinates(defaultGrid.originX(), defaultGrid.dx(), defaultGrid.nx());
            yCoordinates = generateCoordinates(defaultGrid.originY(), defaultGrid.dy(), defaultGrid.ny());
        } else {
            defaultGrid = VortexGrid.empty();
            xCoordinates = new double[0];
            yCoordinates = new double[0];
        }
    }

    /* Factory */
    static VortexGridCollection of(List<VortexGrid> vortexGrids) {
        return new VortexGridCollection(vortexGrids);
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
    boolean isGeographic() {
        return getProjection() instanceof LatLonProjection;
    }

    boolean hasTimeDimension() {
        return vortexGridList.stream().allMatch(VortexGrid::isTemporal);
    }

    boolean hasTimeBounds() {
        Duration interval = getInterval();
        return interval != null && !interval.isZero();
    }

    /* Data */
    Stream<Map.Entry<Integer, VortexGrid>> getCollectionDataStream() {
        return IntStream.range(0, vortexGridList.size()).parallel().mapToObj(i -> Map.entry(i, vortexGridList.get(i)));
    }

    /* Name & Description */
    Map<String, VortexGrid> getRepresentativeGridNameMap() {
        return vortexGridList.stream().collect(Collectors.toMap(VortexGrid::shortName, g -> g, (existing, replacement) -> existing));
    }

    String getShortName() {
        return defaultGrid.shortName();
    }

    String getDescription() {
        return defaultGrid.description();
    }

    /* Projection */
    Projection getProjection() {
        return WktParser.getProjection(getWkt());
    }

    String getProjectionName() {
        Projection projection = getProjection();
        for (Parameter parameter : projection.getProjectionParameters()) {
            if (parameter.getName().equals(CF.GRID_MAPPING_NAME)) {
                return parameter.getStringValue();
            }
        }
        return "Unknown Projection Name";
    }

    String getProjectionUnit() {
        return WktParser.getProjectionUnit(getWkt());
    }

    String getWkt() {
        return defaultGrid.wkt();
    }

    /* Y and X */
    double[] getYCoordinates() {
        return yCoordinates.clone();
    }

    double[] getXCoordinates() {
        return xCoordinates.clone();
    }

    int getNy() {
        return defaultGrid.ny();
    }

    int getNx() {
        return defaultGrid.nx();
    }

    String getTimeUnits() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
        String durationUnit = NetcdfTimeUtils.getDeltaTimeUnit(getBaseDuration()).toString();
        return durationUnit + " since " + baseTime.format(dateTimeFormatter);
    }

    private Duration getInterval() {
        return defaultGrid.interval();
    }

    private Duration getBaseDuration() {
        Duration interval = getInterval();
        return interval.isZero() ? Duration.ofMinutes(1) : interval;
    }
}
