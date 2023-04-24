package mil.army.usace.hec.vortex.convert;

import mil.army.usace.hec.vortex.VortexGridCollection;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class NetcdfGridWriter {
    private static final Logger logger = Logger.getLogger(NetcdfGridWriter.class.getName());

    private final VortexGridCollection collection;
    private final boolean isGeographic;
    private final boolean hasTimeBounds;

    // Dimensions
    private final Dimension timeDim;
    private final Dimension latDim;
    private final Dimension lonDim;
    private final Dimension yDim;
    private final Dimension xDim;
    private final Dimension boundsDim;

    public NetcdfGridWriter(VortexGridCollection collection) {
        this.collection = collection;
        isGeographic = collection.getProjection() instanceof LatLonProjection;
        hasTimeBounds = collection.getTimeLength() > 1;
        // Dimensions
        timeDim = Dimension.builder().setName(CF.TIME).setIsUnlimited(true).build();
        latDim = Dimension.builder().setName(CF.LATITUDE).setLength(collection.getNy()).build();
        lonDim = Dimension.builder().setName(CF.LONGITUDE).setLength(collection.getNx()).build();
        yDim = Dimension.builder().setName("y").setLength(collection.getNy()).build();
        xDim = Dimension.builder().setName("x").setLength(collection.getNx()).build();
        boundsDim = Dimension.builder().setName("nv").setLength(2).build();
    }

    /* Write Methods */
    public void write(NetcdfFormatWriter.Builder writerBuilder) {
        addDimensions(writerBuilder);
        addVariables(writerBuilder);

        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            writer.write(timeDim.getShortName(), Array.makeFromJavaArray(collection.getTimeData()));
            if (hasTimeBounds) {
                writer.write(getBoundsName(timeDim), Array.makeFromJavaArray(collection.getTimeBoundsArray()));
            }

            if (isGeographic) {
                writer.write(CF.LATITUDE, Array.makeFromJavaArray(collection.getYCoordinates()));
                writer.write(CF.LONGITUDE, Array.makeFromJavaArray(collection.getXCoordinates()));
            } else {
                writer.write("y", Array.makeFromJavaArray(collection.getYCoordinates()));
                writer.write("x", Array.makeFromJavaArray(collection.getXCoordinates()));
                writer.write(CF.LATITUDE, Array.makeFromJavaArray(collection.getLatCoordinates()));
                writer.write(CF.LONGITUDE, Array.makeFromJavaArray(collection.getLonCoordinates()));
            }

            writer.write(collection.getShortName(), Array.makeFromJavaArray(collection.getData3D()));
        } catch (IOException | InvalidRangeException e) {
            logger.severe(e.getMessage());
        }
    }

    /* Add Dimensions */
    private void addDimensions(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addDimension(timeDim);
        if (hasTimeBounds) writerBuilder.addDimension(boundsDim);

        if (isGeographic) {
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
        if (hasTimeBounds) addVariableTimeBounds(writerBuilder);
        addVariableLat(writerBuilder);
        addVariableLon(writerBuilder);
        addVariableGridCollection(writerBuilder);

        if (!isGeographic) {
            addVariableProjection(writerBuilder);
            addVariableX(writerBuilder);
            addVariableY(writerBuilder);
        }
    }

    private void addVariableTime(NetcdfFormatWriter.Builder writerBuilder) {
        Variable.Builder<?> v = writerBuilder.addVariable(timeDim.getShortName(), DataType.FLOAT, List.of(timeDim));
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
        v.addAttribute(new Attribute(CF.CALENDAR, "standard"));
        v.addAttribute(new Attribute(CF.UNITS, collection.getTimeUnits()));
        v.addAttribute(new Attribute(CF.LONG_NAME, ""));
        if (hasTimeBounds) v.addAttribute(new Attribute(CF.BOUNDS, getBoundsName(timeDim)));
    }

    private void addVariableTimeBounds(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(getBoundsName(timeDim), DataType.FLOAT, List.of(timeDim, boundsDim));
    }

    private void addVariableLat(NetcdfFormatWriter.Builder writerBuilder) {
        List<Dimension> dimensions = isGeographic ? List.of(latDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(latDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LAT_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "latitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
    }

    private void addVariableLon(NetcdfFormatWriter.Builder writerBuilder) {
        List<Dimension> dimensions = isGeographic ? List.of(lonDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(lonDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LON_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "longitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
    }

    private void addVariableY(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(yDim.getShortName(), DataType.DOUBLE, List.of(yDim))
                .addAttribute(new Attribute(CF.UNITS, collection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
    }

    private void addVariableX(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(xDim.getShortName(), DataType.DOUBLE, List.of(xDim))
                .addAttribute(new Attribute(CF.UNITS, collection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
    }

    private void addVariableProjection(NetcdfFormatWriter.Builder writerBuilder) {
        Variable.Builder<?> variableBuilder = writerBuilder.addVariable(collection.getProjectionName(), DataType.SHORT, Collections.emptyList());
        for (Parameter parameter : collection.getProjection().getProjectionParameters()) {
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
    }

    private void addVariableGridCollection(NetcdfFormatWriter.Builder writerBuilder) {
        List<Dimension> dimensions = isGeographic ? List.of(timeDim, latDim, lonDim) : List.of(timeDim, yDim, xDim);
        writerBuilder.addVariable(collection.getShortName(), DataType.FLOAT, dimensions)
                .addAttribute(new Attribute(CF.LONG_NAME, collection.getDescription()))
                .addAttribute(new Attribute(CF.UNITS, collection.getDataUnit()))
                .addAttribute(new Attribute(CF.GRID_MAPPING, collection.getProjectionName()))
                .addAttribute(new Attribute(CF.COORDINATES, "latitude longitude"))
                .addAttribute(new Attribute(CF.MISSING_VALUE, collection.getNoDataValue()))
                .addAttribute(new Attribute(CF._FILLVALUE, collection.getNoDataValue()));
    }

    /* Helpers */
    private String getBoundsName(Dimension dimension) {
        return dimension.getShortName() + "_bnds";
    }
}
