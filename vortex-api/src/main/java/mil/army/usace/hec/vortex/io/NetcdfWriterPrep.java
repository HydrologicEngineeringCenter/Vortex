package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexGridCollection;
import mil.army.usace.hec.vortex.VortexVariable;
import mil.army.usace.hec.vortex.util.VortexGridUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class NetcdfWriterPrep {
    private static final Logger logger = Logger.getLogger(NetcdfWriterPrep.class.getName());

    // NetCDF4 Settings
    private static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    private static final int DEFLATE_LEVEL = 9;
    private static final boolean SHUFFLE = false;
    private static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    public static void prepFile(String destination, VortexGridCollection defaultCollection) {
        NetcdfFormatWriter.Builder builder = overwriteWriterBuilder(destination, defaultCollection);
        try (NetcdfFormatWriter writer = builder.build()) {
//            writeDimensions(writer, defaultCollection);
        } catch (Exception e) {
            logger.warning("Failed to prep file");
            logger.warning(e.getMessage());
        }
    }

    private static NetcdfFormatWriter.Builder overwriteWriterBuilder(String destination, VortexGridCollection defaultCollection) {
        try {
            Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
            NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.builder()
                    .setNewFile(true)
                    .setFormat(NETCDF_FORMAT)
                    .setLocation(destination)
                    .setChunker(chunker);
            addDimensions(builder, defaultCollection);
            addVariables(builder, defaultCollection);
            return builder;
        } catch (Exception e) {
            logger.warning("Failed to create builder");
            logger.warning(e.getMessage());
            return null;
        }
    }

    /* Prep Write */
    private static void writeDimensions(NetcdfFormatWriter writer, VortexGridCollection defaultCollection) throws InvalidRangeException, IOException {
        if (defaultCollection.hasTimeDimension()) {
            writer.write(CF.TIME, Array.makeFromJavaArray(defaultCollection.getTimeData()));
        }

        if (defaultCollection.hasTimeBounds()) {
            writer.write("time_bnds", Array.makeFromJavaArray(defaultCollection.getTimeBoundsArray()));
        }

        if (defaultCollection.isGeographic()) {
            writeDimensionsGeographic(writer, defaultCollection);
        } else {
            writeDimensionsProjected(writer, defaultCollection);
        }
    }

    private static void writeDimensionsGeographic(NetcdfFormatWriter writer, VortexGridCollection defaultCollection) throws InvalidRangeException, IOException {
        writer.write(CF.LATITUDE, Array.makeFromJavaArray(defaultCollection.getYCoordinates()));
        writer.write(CF.LONGITUDE, Array.makeFromJavaArray(defaultCollection.getXCoordinates()));
    }

    private static void writeDimensionsProjected(NetcdfFormatWriter writer, VortexGridCollection defaultCollection) throws InvalidRangeException, IOException {
        writer.write("y", Array.makeFromJavaArray(defaultCollection.getYCoordinates()));
        writer.write("x", Array.makeFromJavaArray(defaultCollection.getXCoordinates()));
    }

    /* Dimensions */
    private static NetcdfFormatWriter.Builder addDimensions(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        if (defaultCollection.hasTimeDimension()) {
            writerBuilder.addDimension(getTimeDimension());
        }

        if (defaultCollection.hasTimeBounds()) {
            writerBuilder.addDimension(getTimeBoundsDimension());
        }

        if (defaultCollection.isGeographic()) {
            writerBuilder.addDimension(getLatitudeDimension(defaultCollection.getNy()));
            writerBuilder.addDimension(getLongitudeDimension(defaultCollection.getNx()));
        } else {
            writerBuilder.addDimension(getYDimension(defaultCollection.getNy()));
            writerBuilder.addDimension(getXDimension(defaultCollection.getNx()));
        }

        return writerBuilder;
    }

    private static Dimension getTimeDimension() {
        return Dimension.builder().setName(CF.TIME).setIsUnlimited(true).build();
    }

    private static Dimension getTimeBoundsDimension() {
        return Dimension.builder().setName("nv").setIsUnlimited(true).build();
    }

    private static List<Dimension> getGeographicLatLonDimensions(int ny, int nx) {
        return List.of(getLatitudeDimension(ny), getLatitudeDimension(nx));
    }

    private static Dimension getLatitudeDimension(int ny) {
        return Dimension.builder().setName(CF.LATITUDE).setLength(ny).build();
    }

    private static Dimension getLongitudeDimension(int nx) {
        return Dimension.builder().setName(CF.LONGITUDE).setLength(nx).build();
    }

    private static Dimension getYDimension(int ny) {
        return Dimension.builder().setName("y").setLength(ny).build();
    }

    private static Dimension getXDimension(int nx) {
        return Dimension.builder().setName("x").setLength(nx).build();
    }

    /* Variables */
    private static NetcdfFormatWriter.Builder addVariables(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        if (defaultCollection.hasTimeDimension()) {
            addTimeVariable(writerBuilder, defaultCollection);
        }

        if (defaultCollection.hasTimeBounds()) {
            addVariableTimeBounds(writerBuilder);
        }

        if (defaultCollection.isGeographic()) {
            addVariableLat(writerBuilder, defaultCollection);
            addVariableLon(writerBuilder, defaultCollection);
        } else {
            addVariableY(writerBuilder, defaultCollection);
            addVariableX(writerBuilder, defaultCollection);
        }

        addVariableProjection(writerBuilder, defaultCollection);
        addVariableGridCollection(writerBuilder, defaultCollection);

        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addTimeVariable(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        if (!defaultCollection.hasTimeDimension()) {
            return writerBuilder;
        }

        Variable.Builder<?> v = writerBuilder.addVariable(CF.TIME, DataType.ULONG, List.of(getTimeDimension()));
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
        v.addAttribute(new Attribute(CF.CALENDAR, "standard"));
        v.addAttribute(new Attribute(CF.UNITS, defaultCollection.getTimeUnits()));

        if (defaultCollection.hasTimeBounds()) {
            v.addAttribute(new Attribute(CF.BOUNDS, "time_bnds"));
        }

        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableTimeBounds(NetcdfFormatWriter.Builder writerBuilder) {
        Dimension timeDim = getTimeDimension();
        Dimension timeBoundsDim = getTimeBoundsDimension();
        writerBuilder.addVariable("time_bnds", DataType.ULONG, List.of(timeDim, timeBoundsDim));
        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableLat(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        Dimension latDim = getLatitudeDimension(defaultCollection.getNy());
        Dimension yDim = getYDimension(defaultCollection.getNy());
        Dimension xDim = getXDimension(defaultCollection.getNx());

        boolean isGeographic = defaultCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(latDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(latDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LAT_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "latitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));

        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableLon(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        Dimension lonDim = getLongitudeDimension(defaultCollection.getNx());
        Dimension yDim = getYDimension(defaultCollection.getNy());
        Dimension xDim = getXDimension(defaultCollection.getNx());

        boolean isGeographic = defaultCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(lonDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(lonDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LON_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "longitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));

        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableY(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        Dimension yDim = getYDimension(defaultCollection.getNy());
        writerBuilder.addVariable(yDim.getShortName(), DataType.DOUBLE, List.of(yDim))
                .addAttribute(new Attribute(CF.UNITS, defaultCollection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableX(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        Dimension xDim = getXDimension(defaultCollection.getNx());
        writerBuilder.addVariable(xDim.getShortName(), DataType.DOUBLE, List.of(xDim))
                .addAttribute(new Attribute(CF.UNITS, defaultCollection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableProjection(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
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
        variableBuilder.addAttribute(new Attribute("crs_wkt", defaultCollection.getWkt()));

        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder addVariableGridCollection(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection defaultCollection) {
        Dimension timeDim = getTimeDimension();

        List<Dimension> dimensions = new ArrayList<>();
        if (defaultCollection.hasTimeDimension()) {
            dimensions.add(timeDim);
        }

        if (defaultCollection.isGeographic()) {
            dimensions.add(getLatitudeDimension(defaultCollection.getNy()));
            dimensions.add(getLongitudeDimension(defaultCollection.getNx()));
        } else {
            dimensions.add(getYDimension(defaultCollection.getNy()));
            dimensions.add(getXDimension(defaultCollection.getNx()));
        }

        Map<String, VortexGrid> availableVortexVariables = defaultCollection.getRepresentativeGridNameMap();
        for (VortexGrid vortexGrid : availableVortexVariables.values()) {
            VortexVariable variable = VortexGridUtils.inferVortexVariableFromNames(vortexGrid);
            writerBuilder.addVariable(variable.getShortName(), DataType.FLOAT, dimensions)
                    .addAttribute(new Attribute(CF.LONG_NAME, variable.getLongName()))
                    .addAttribute(new Attribute(CF.UNITS, vortexGrid.units()))
                    .addAttribute(new Attribute(CF.GRID_MAPPING, defaultCollection.getProjectionName()))
                    .addAttribute(new Attribute(CF.COORDINATES, "latitude longitude"))
                    .addAttribute(new Attribute(CF.MISSING_VALUE, (float) vortexGrid.noDataValue()))
                    .addAttribute(new Attribute(CF._FILLVALUE, (float) vortexGrid.noDataValue()))
                    .addAttribute(new Attribute(CF.CELL_METHODS, vortexGrid.dataType().getNcString()));
        }

        return writerBuilder;
    }
}