package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexDataType;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Holds metadata about a series of grids for gap filling operations.
 */
class GridMetadata {
    final List<ZonedDateTime> startTimes = new ArrayList<>();
    final List<Integer> indices = new ArrayList<>();
    final Set<ZonedDateTime> hasNoData = new HashSet<>();
    final Set<Integer> cellsNeedingInterpolation = new HashSet<>();
    final List<byte[]> isNoData = new ArrayList<>();

    final Set<Double> noDataValues = new HashSet<>();
    final Set<Duration> intervals = new HashSet<>();
    final Set<Integer> sizes = new HashSet<>();
    final Set<Double> dx = new HashSet<>();
    final Set<Double> dy = new HashSet<>();
    final Set<Integer> nx = new HashSet<>();
    final Set<Integer> ny = new HashSet<>();
    final Set<Double> originX = new HashSet<>();
    final Set<Double> originY = new HashSet<>();
    final Set<String> wkt = new HashSet<>();
    final Set<String> units = new HashSet<>();
    final Set<String> shortName = new HashSet<>();
    final Set<VortexDataType> dataType = new HashSet<>();

    GridMetadata() {
    }

    Duration getInterval() {
        if (intervals.size() != 1) {
            throw new IllegalStateException("Intervals size must be 1");
        }
        return intervals.iterator().next();
    }

    int getSize() {
        if (sizes.size() != 1) {
            throw new IllegalStateException("Sizes size must be 1");
        }
        return sizes.iterator().next();
    }

    double getNoDataValue() {
        if (noDataValues.size() != 1) {
            throw new IllegalStateException("No data values size must be 1");
        }
        return noDataValues.iterator().next();
    }

    double getOriginX() {
        if (originX.size() != 1) {
            throw new IllegalStateException("originX size must be 1");
        }
        return originX.iterator().next();
    }

    double getOriginY() {
        if (originY.size() != 1) {
            throw new IllegalStateException("originY size must be 1");
        }
        return originY.iterator().next();
    }

    double getDx() {
        if (dx.size() != 1) {
            throw new IllegalStateException("dx size must be 1");
        }
        return dx.iterator().next();
    }

    double getDy() {
        if (dy.size() != 1) {
            throw new IllegalStateException("dy size must be 1");
        }
        return dy.iterator().next();
    }

    int getNx() {
        if (nx.size() != 1) {
            throw new IllegalStateException("nx size must be 1");
        }
        return nx.iterator().next();
    }

    int getNy() {
        if (ny.size() != 1) {
            throw new IllegalStateException("ny size must be 1");
        }
        return ny.iterator().next();
    }

    String getWkt() {
        if (wkt.size() != 1) {
            throw new IllegalStateException("wkt size must be 1");
        }
        return wkt.iterator().next();
    }

    String getUnits() {
        if (units.size() != 1) {
            throw new IllegalStateException("units size must be 1");
        }
        return units.iterator().next();
    }

    String getShortName() {
        if (shortName.size() != 1) {
            throw new IllegalStateException("shortName size must be 1");
        }
        return shortName.iterator().next();
    }

    VortexDataType getVortexDataType() {
        if (dataType.size() != 1) {
            throw new IllegalStateException("dataType size must be 1");
        }
        return dataType.iterator().next();
    }
}
