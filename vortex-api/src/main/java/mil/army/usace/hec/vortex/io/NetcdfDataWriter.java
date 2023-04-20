package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexGridCollection;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.time.Calendar;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NetcdfDataWriter extends DataWriter {
    private static final Logger logger = Logger.getLogger(NetcdfDataWriter.class.getName());
    private final VortexGridCollection collection;

    // NetCDF4 Settings
    private static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    private static final int DEFLATE_LEVEL = 9;
    private static final boolean SHUFFLE = false;
    private static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    // Attributes
    private static final String TIME_BOUNDS = "time_bnds";

    /* Constructor */
    NetcdfDataWriter(Builder builder) {
        super(builder);

        List<VortexGrid> grids = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .collect(Collectors.toList());
        collection = new VortexGridCollection(grids);
    }

    /* Write */
    @Override
    public void write() {
        NetcdfFormatWriter.Builder writerBuilder = writerBuilder();
        writeData(writerBuilder);
    }

    private NetcdfFormatWriter.Builder writerBuilder() {
        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter.builder()
                .setNewFile(true)
                .setFormat(NETCDF_FORMAT)
                .setLocation(destination.toString())
                .setChunker(chunker);
        addGlobalAttributes(writerBuilder);
        addDimensions(writerBuilder);
        addVariables(writerBuilder);
        return writerBuilder;
    }

    private void writeData(NetcdfFormatWriter.Builder writerBuilder) {
        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            writer.write("time", Array.makeFromJavaArray(collection.getTimeData()));
            writer.write("y", Array.makeFromJavaArray(collection.getYCoordinates()));
            writer.write("x", Array.makeFromJavaArray(collection.getXCoordinates()));
            writer.write("lat", Array.makeFromJavaArray(collection.getLatCoordinates()));
            writer.write("lon", Array.makeFromJavaArray(collection.getLonCoordinates()));
//            writer.write(TIME_BOUNDS, Array.makeFromJavaArray(collection.getTimeBoundsArray()));
            writer.write(collection.getShortName(), Array.makeFromJavaArray(collection.getData3D()));
        } catch (IOException | InvalidRangeException e) {
            logger.severe(e.getMessage());
        }
    }

    /* Add Global Attributes */
    private void addGlobalAttributes(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.10"));
    }

    /* Add Dimensions */
    private void addDimensions(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addUnlimitedDimension(CF.TIME);
        writerBuilder.addDimension("nv", VortexGridCollection.NV_LENGTH);
        writerBuilder.addDimension("y", collection.getNy());
        writerBuilder.addDimension("x", collection.getNx());
    }

    /* Add Variables */
    private void addVariables(NetcdfFormatWriter.Builder writerBuilder) {
        addVariableTime(writerBuilder);
//        addVariableTimeBounds(writerBuilder);
        addVariableX(writerBuilder);
        addVariableY(writerBuilder);
        addVariableLat(writerBuilder);
        addVariableLon(writerBuilder);
        addVariableProjection(writerBuilder);
        addVariableGridCollection(writerBuilder);
    }

    private void addVariableTime(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable("time", DataType.FLOAT, "time")
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME))
                .addAttribute(new Attribute(CF.CALENDAR, Calendar.getDefault().name()))
                .addAttribute(new Attribute(CF.UNITS, collection.getTimeUnits()))
//                .addAttribute(new Attribute(CF.BOUNDS, TIME_BOUNDS))
                .addAttribute(new Attribute(CF.LONG_NAME, ""));
    }

    private void addVariableTimeBounds(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(TIME_BOUNDS, DataType.FLOAT, "time nv");
    }

    private void addVariableX(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable("x", DataType.DOUBLE, "x")
                .addAttribute(new Attribute(CF.UNITS, collection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
    }

    private void addVariableY(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable("y", DataType.DOUBLE, "y")
                .addAttribute(new Attribute(CF.UNITS, collection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
    }

    private void addVariableLat(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable("lat", DataType.DOUBLE, "y x")
                .addAttribute(new Attribute(CF.UNITS, "degrees_north"))
                .addAttribute(new Attribute(CF.LONG_NAME, "latitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE))
                .addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    }

    private void addVariableLon(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable("lon", DataType.DOUBLE, "y x")
                .addAttribute(new Attribute(CF.UNITS, "degrees_east"))
                .addAttribute(new Attribute(CF.LONG_NAME, "longitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE))
                .addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    }

    private void addVariableProjection(NetcdfFormatWriter.Builder writerBuilder) {
        Projection projection = collection.getProjection();

        Set<Parameter> parameterList = new LinkedHashSet<>(projection.getProjectionParameters());
        String projectionType = parameterList.stream()
                .filter(p -> p.getName().equals(CF.GRID_MAPPING_NAME))
                .filter(Parameter::isString)
                .findFirst()
                .map(Parameter::getStringValue)
                .orElse("");
        Variable.Builder<?> variableBuilder = writerBuilder.addVariable(projectionType, DataType.SHORT, "");

        for (Parameter parameter : parameterList) {
            String name = parameter.getName();
            if (parameter.getLength() == 0)
                variableBuilder.addAttribute(new Attribute(name, String.valueOf(parameter.getStringValue())));
            else {
                double[] values = parameter.getNumericValues();
                if (values == null) {
                    logger.severe("Values is null");
                    values = new double[0];
                }
                variableBuilder.addAttribute(Attribute.builder().setName(name).setValues(Array.makeFromJavaArray(values)).build());
            }
        }
    }

    private void addVariableGridCollection(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addVariable(collection.getShortName(), DataType.FLOAT, "time y x")
                .addAttribute(new Attribute(CF.LONG_NAME, collection.getDescription()))
                .addAttribute(new Attribute(CF.UNITS, collection.getDataUnit()))
                .addAttribute(new Attribute(CF.GRID_MAPPING, collection.getProjectionName()))
                .addAttribute(new Attribute(CF.COORDINATES, "lat lon"))
//                .addAttribute(new Attribute(CF.CELL_METHODS, "area: mean time: sum"))
                .addAttribute(new Attribute(CF.MISSING_VALUE, collection.getNoDataValue()))
                .addAttribute(new Attribute(CF._FILLVALUE, collection.getNoDataValue()));
    }
}
