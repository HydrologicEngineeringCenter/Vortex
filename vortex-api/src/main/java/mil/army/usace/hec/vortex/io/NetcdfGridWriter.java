package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexGridCollection;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.VortexVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.Parameter;

import javax.measure.Unit;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static javax.measure.MetricPrefix.*;
import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.unit.Units.HOUR;
import static tech.units.indriya.unit.Units.*;

public class NetcdfGridWriter {
    private static final Logger logger = Logger.getLogger(NetcdfGridWriter.class.getName());
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    public static final int BOUNDS_LEN = 2;
    public static final String CRS_WKT = "crs_wkt";

    private final Map<String, VortexGridCollection> gridCollectionMap;
    private final VortexGridCollection defaultCollection;
    // Dimensions
    private final Dimension timeDim;
    private final Dimension latDim;
    private final Dimension lonDim;
    private final Dimension yDim;
    private final Dimension xDim;
    private final Dimension boundsDim; // For all axis that has bounds (interval data)

    public NetcdfGridWriter(List<VortexGrid> vortexGridList) {
        gridCollectionMap = initGridCollectionMap(vortexGridList);
        defaultCollection = getAnyCollection();
        // Dimensions
        timeDim = Dimension.builder().setName(CF.TIME).setIsUnlimited(true).build();
        latDim = Dimension.builder().setName(CF.LATITUDE).setLength(defaultCollection.getNy()).build();
        lonDim = Dimension.builder().setName(CF.LONGITUDE).setLength(defaultCollection.getNx()).build();
        yDim = Dimension.builder().setName("y").setLength(defaultCollection.getNy()).build();
        xDim = Dimension.builder().setName("x").setLength(defaultCollection.getNx()).build();
        boundsDim = Dimension.builder().setName("nv").setIsUnlimited(true).build();
    }

    private Map<String, VortexGridCollection> initGridCollectionMap(List<VortexGrid> vortexGridList) {
        Map<String, VortexGridCollection> map = vortexGridList.stream()
                .collect(Collectors.groupingBy(
                        VortexGrid::shortName,
                        Collectors.collectingAndThen(Collectors.toList(), VortexGridCollection::new))
                );
        boolean isValidMap = verifyGridCollectionMap(map);
        return isValidMap ? map : Collections.emptyMap();
    }

    private boolean verifyGridCollectionMap(Map<String, VortexGridCollection> map) {
        boolean projectionMatched = isUnique(VortexGridCollection::getProjection, map);
        // No need to check lat & lon since they are generated from (x & y & projection)
        boolean yMatched = isUnique(VortexGridCollection::getYCoordinates, map);
        boolean xMatched = isUnique(VortexGridCollection::getXCoordinates, map);
        return projectionMatched && yMatched && xMatched;
    }

    private boolean isUnique(Function<VortexGridCollection, ?> propertyGetter, Map<String, VortexGridCollection> map) {
        boolean isUnique = map.values().stream()
                .map(propertyGetter)
                .map(o -> (o instanceof double[] || o instanceof float[]) ? Arrays.deepToString(new Object[] {o}) : o)
                .distinct()
                .count() == 1;
        if (!isUnique) {
            logger.severe("Data is not the same for all grids");
        }
        return isUnique;
    }

    private VortexGridCollection getAnyCollection() {
        return gridCollectionMap.values().stream().findAny().orElse(null);
    }

    /* Write Methods */
    public void write(NetcdfFormatWriter.Builder writerBuilder) {
        addDimensions(writerBuilder);
        addVariables(writerBuilder);

        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            writeDimensions(writer);
            writeVariableGrids(writer, 0);
        } catch (IOException | InvalidRangeException e) {
            logger.severe(e.getMessage());
        }
    }

    private void writeDimensions(NetcdfFormatWriter writer) throws InvalidRangeException, IOException {
        if (defaultCollection.hasTimeDimension()) {
            writer.write(timeDim.getShortName(), Array.makeFromJavaArray(defaultCollection.getTimeData()));
        }

        if (defaultCollection.hasTimeBounds()) {
            writer.write(getBoundsName(timeDim), Array.makeFromJavaArray(defaultCollection.getTimeBoundsArray()));
        }

        if (defaultCollection.isGeographic()) {
            writeDimensionsGeographic(writer);
        } else {
            writeDimensionsProjected(writer);
        }
    }

    private void writeDimensionsGeographic(NetcdfFormatWriter writer) throws InvalidRangeException, IOException {
        writer.write(latDim.getShortName(), Array.makeFromJavaArray(defaultCollection.getYCoordinates()));
        writer.write(lonDim.getShortName(), Array.makeFromJavaArray(defaultCollection.getXCoordinates()));
    }

    private void writeDimensionsProjected(NetcdfFormatWriter writer) throws InvalidRangeException, IOException {
        writer.write(yDim.getShortName(), Array.makeFromJavaArray(defaultCollection.getYCoordinates()));
        writer.write(xDim.getShortName(), Array.makeFromJavaArray(defaultCollection.getXCoordinates()));
    }

    private void writeVariableGrids(NetcdfFormatWriter writer, int startIndex) {
        AtomicBoolean hasErrors = new AtomicBoolean(false);

        for (VortexGridCollection collection : gridCollectionMap.values()) {
            VortexVariable meteorologicalVariable = getVortexVariable(collection);
            Variable variable = writer.findVariable(meteorologicalVariable.getLowerCasedVariableName());
            if (variable == null) {
                logger.severe("Failed to locate variable: " + meteorologicalVariable.getLowerCasedVariableName());
                hasErrors.set(true);
                continue;
            }

            collection.getCollectionDataStream().forEach(entry -> {
                try {
                    int index = entry.getKey() + startIndex;
                    VortexGrid grid = entry.getValue();
                    int[] origin = {index, 0, 0};
                    writer.write(variable, origin, Array.makeFromJavaArray(grid.data3D()));
                } catch (IOException | InvalidRangeException e) {
                    logger.warning(e.getMessage());
                    hasErrors.set(true);
                }
            });
        }

        if (hasErrors.get()) {
            boolean isAppend = startIndex > 0;
            String overwriteErrorMessage = "Failed to overwrite file.";
            String appendErrorMessage = "Some reasons may be:\n* Attempted to append to non-existing variable\n* Attempted to append data with different projection\n* Attempted to append data with different location";
            String message = isAppend ? appendErrorMessage : overwriteErrorMessage;
            support.firePropertyChange(VortexProperty.ERROR, null, message);
        }
    }

    /* Add Dimensions */
    private void addDimensions(NetcdfFormatWriter.Builder writerBuilder) {
        if (defaultCollection.hasTimeDimension()) {
            writerBuilder.addDimension(timeDim);
        }

        if (defaultCollection.hasTimeBounds()) {
            writerBuilder.addDimension(boundsDim);
        }

        if (defaultCollection.isGeographic()) {
            writerBuilder.addDimension(latDim);
            writerBuilder.addDimension(lonDim);
        } else {
            writerBuilder.addDimension(yDim);
            writerBuilder.addDimension(xDim);
        }
    }

    /* Add Variables */
    private void addVariables(NetcdfFormatWriter.Builder writerBuilder) {
        addVariableTime(writerBuilder);
        if (defaultCollection.hasTimeBounds()) {
            addVariableTimeBounds(writerBuilder);
        }
        addVariableLat(writerBuilder);
        addVariableLon(writerBuilder);
        addVariableGridCollection(writerBuilder);

        if (!defaultCollection.isGeographic()) {
            addVariableProjection(writerBuilder);
            addVariableX(writerBuilder);
            addVariableY(writerBuilder);
        }
    }

    private void addVariableTime(NetcdfFormatWriter.Builder writerBuilder) {
        if (!defaultCollection.hasTimeDimension()) {
            return;
        }

        Variable.Builder<?> v = writerBuilder.addVariable(timeDim.getShortName(), DataType.DOUBLE, List.of(timeDim));
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
        v.addAttribute(new Attribute(CF.CALENDAR, "standard"));
        v.addAttribute(new Attribute(CF.UNITS, defaultCollection.getTimeUnits()));

        if (defaultCollection.hasTimeBounds()) {
            v.addAttribute(new Attribute(CF.BOUNDS, getBoundsName(timeDim)));
        }
    }

    private void addVariableTimeBounds(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(getBoundsName(timeDim), DataType.DOUBLE, List.of(timeDim, boundsDim));
    }

    private void addVariableLat(NetcdfFormatWriter.Builder writerBuilder) {
        boolean isGeographic = defaultCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(latDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(latDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LAT_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "latitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
    }

    private void addVariableLon(NetcdfFormatWriter.Builder writerBuilder) {
        boolean isGeographic = defaultCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(lonDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(lonDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LON_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "longitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
    }

    private void addVariableY(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(yDim.getShortName(), DataType.DOUBLE, List.of(yDim))
                .addAttribute(new Attribute(CF.UNITS, defaultCollection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
    }

    private void addVariableX(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(xDim.getShortName(), DataType.DOUBLE, List.of(xDim))
                .addAttribute(new Attribute(CF.UNITS, defaultCollection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
    }

    private void addVariableProjection(NetcdfFormatWriter.Builder writerBuilder) {
        Variable.Builder<?> variableBuilder = writerBuilder.addVariable(defaultCollection.getProjectionName(), DataType.SHORT, Collections.emptyList());
        for (Parameter parameter : defaultCollection.getProjection().getProjectionParameters()) {
            String name = parameter.getName();
            String stringValue = parameter.getStringValue();
            double[] numericValues = parameter.getNumericValues();

            if (stringValue == null && numericValues == null) {
                String logMessage = String.format("Parameter '%s' has no value", parameter.getName());
                logger.severe(logMessage);
                continue;
            }

            Attribute attribute = (stringValue != null) ?
                    Attribute.builder().setName(name).setStringValue(stringValue).build() :
                    Attribute.builder().setName(name).setValues(Array.makeFromJavaArray(numericValues)).build();
            variableBuilder.addAttribute(attribute);
        }

        // Adding CRS WKT for Grid's Coordinate System information
        // CF Conventions: https://cfconventions.org/Data/cf-conventions/cf-conventions-1.11/cf-conventions.html#use-of-the-crs-well-known-text-format
        variableBuilder.addAttribute(new Attribute(CRS_WKT, defaultCollection.getWkt()));
    }

    private void addVariableGridCollection(NetcdfFormatWriter.Builder writerBuilder) {
        boolean isGeographic = defaultCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(timeDim, latDim, lonDim) : List.of(timeDim, yDim, xDim);
        for (VortexGridCollection collection : gridCollectionMap.values()) {
            VortexVariable variable = getVortexVariable(collection);
            writerBuilder.addVariable(variable.getLowerCasedVariableName(), DataType.FLOAT, dimensions)
                    .addAttribute(new Attribute(CF.LONG_NAME, variable.getLongName()))
                    .addAttribute(new Attribute(CF.UNITS, collection.getDataUnit()))
                    .addAttribute(new Attribute(CF.GRID_MAPPING, defaultCollection.getProjectionName()))
                    .addAttribute(new Attribute(CF.COORDINATES, "latitude longitude"))
                    .addAttribute(new Attribute(CF.MISSING_VALUE, collection.getNoDataValue()))
                    .addAttribute(new Attribute(CF._FILLVALUE, collection.getNoDataValue()))
                    .addAttribute(new Attribute(CF.CELL_METHODS, collection.getDataType().getNcString()));
        }
    }

    /* Helpers */
    private String getBoundsName(Dimension dimension) {
        return dimension.getShortName() + "_bnds";
    }

    private static VortexVariable getVortexVariable(VortexGridCollection collection) {
        VortexVariable name = VortexVariable.fromName(collection.getShortName());
        return name.equals(VortexVariable.UNDEFINED) ? VortexVariable.fromName(collection.getDescription()) : name;
    }

    private static String getUnitsString(Unit<?> unit){
        if (unit.equals(MILLI(METRE).divide(SECOND))) return "mm/s";
        if (unit.equals(MILLI(METRE).divide(HOUR))) return "mm/hr";
        if (unit.equals(MILLI(METRE).divide(DAY))) return "mm/day";
        if (unit.equals(MILLI(METRE))) return "mm";
        if (unit.equals(INCH)) return "in";
        if (unit.equals(CELSIUS)) return "degC";
        if (unit.equals(CELSIUS.multiply(DAY))) return "degC-d";
        if (unit.equals(FAHRENHEIT)) return "degF";
        if (unit.equals(KELVIN)) return "K";
        if (unit.equals(WATT.divide(SQUARE_METRE))) return "W m-2";
        if (unit.equals(JOULE.divide(SQUARE_METRE))) return "J m-2";
        if (unit.equals(KILO(METRE).divide(HOUR))) return "kph";
        if (unit.equals(KILOMETRE_PER_HOUR)) return "km h-1";
        if (unit.equals(PERCENT)) return "%";
        if (unit.equals(HECTO(PASCAL))) return "hPa";
        if (unit.equals(PASCAL)) return "Pa";
        if (unit.equals(METRE)) return "m";
        if (unit.equals(CUBIC_METRE.divide(SECOND))) return "m3 s-1";
        if (unit.equals(CUBIC_FOOT.divide(SECOND))) return "cfs";
        if (unit.equals(FOOT)) return "ft";
        if (unit.equals(METRE_PER_SECOND)) return "m/s";
        if (unit.equals(MILE_PER_HOUR)) return "mph";
        if (unit.equals(FOOT_PER_SECOND)) return "ft/s";
        if (unit.equals(KILO(PASCAL))) return "kPa";
        if (unit.equals(KILO(METRE))) return "km";
        if (unit.equals(MILE)) return "mi";
        if (unit.equals(TON)) return "ton";
        if (unit.equals(MILLI(GRAM).divide(LITRE))) return "mg L-1";
        return "Unspecified";
    }

    /* Property Change */
    public void addListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }

    /* Append Data */
    public void appendData(NetcdfFormatWriter.Builder writerBuilder) {
        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            Variable timeVar = writer.findVariable(CF.TIME);
            if (timeVar == null) {
                logger.severe("Time variable not found");
                return;
            }

            int startIndex = timeVar.getShape(0);
            writeVariableGrids(writer, startIndex);
            writer.write(timeVar, new int[] {startIndex}, Array.makeFromJavaArray(defaultCollection.getTimeData()));

            if (defaultCollection.hasTimeBounds()) {
                writer.write(getBoundsName(timeDim), new int[] {startIndex, 0}, Array.makeFromJavaArray(defaultCollection.getTimeBoundsArray()));
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
}
