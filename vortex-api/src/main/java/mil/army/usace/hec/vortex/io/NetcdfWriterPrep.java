package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.temporal.VortexTimeRecord;
import mil.army.usace.hec.vortex.VortexVariable;
import mil.army.usace.hec.vortex.util.IndexMap;
import mil.army.usace.hec.vortex.util.VortexGridUtils;
import mil.army.usace.hec.vortex.util.VortexTimeUtils;
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
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

final class NetcdfWriterPrep {
    private static final Logger logger = Logger.getLogger(NetcdfWriterPrep.class.getName());

    // NetCDF4 Settings
    private static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    private static final int DEFLATE_LEVEL = 9;
    private static final boolean SHUFFLE = false;
    private static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    // Map
    private static final Map<String, IndexMap<VortexTimeRecord>> recordIndexMap = new HashMap<>();

    private NetcdfWriterPrep() {
        // Utility Class
    }

    public static void initializeForAppend(String destination, VortexGridCollection gridCollection, Stream<VortexTimeRecord> sortedRecordStream) {
        NetcdfFormatWriter.Builder builder = overwriteWriterBuilder(destination, gridCollection);
        if (builder == null) {
            logger.warning("Failed to create writer builder to prep file");
            return;
        }
        
        try (NetcdfFormatWriter writer = builder.build()) {
            writeProjectionDimensions(writer, gridCollection);
            List<VortexTimeRecord> sortedRecordList = sortedRecordStream.toList();
            writeTimeDimension(writer, sortedRecordList);
            recordIndexMap.put(destination, IndexMap.of(sortedRecordList));
            logger.info("Generated NetCDF File. Ready for Append.");
        } catch (Exception e) {
            logger.warning("Failed to prep file");
            logger.warning(e.getMessage());
        }
    }

    public static int getTimeRecordIndex(String destination, VortexTimeRecord timeRecord) {
        return recordIndexMap.get(destination).indexOf(timeRecord);
    }

    /* Helpers */
    private static NetcdfFormatWriter.Builder overwriteWriterBuilder(String destination, VortexGridCollection gridCollection) {
        try {
            Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
            NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.builder()
                    .setNewFile(true)
                    .setFormat(NETCDF_FORMAT)
                    .setLocation(destination)
                    .setChunker(chunker);
            addDimensions(builder, gridCollection);
            addVariables(builder, gridCollection);
            addGlobalAttributes(builder);
            return builder;
        } catch (Exception e) {
            logger.warning("Failed to create builder");
            logger.warning(e.getMessage());
            return null;
        }
    }

    /* Dimensions */
    private static void addDimensions(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        if (gridCollection.hasTimeDimension()) {
            writerBuilder.addDimension(getTimeDimension());
        }

        if (gridCollection.hasTimeBounds()) {
            writerBuilder.addDimension(getTimeBoundsDimension());
        }

        if (gridCollection.isGeographic()) {
            writerBuilder.addDimension(getLatitudeDimension(gridCollection.getNy()));
            writerBuilder.addDimension(getLongitudeDimension(gridCollection.getNx()));
        } else {
            writerBuilder.addDimension(getYDimension(gridCollection.getNy()));
            writerBuilder.addDimension(getXDimension(gridCollection.getNx()));
        }

    }

    private static Dimension getTimeDimension() {
        return Dimension.builder().setName(CF.TIME).setIsUnlimited(true).build();
    }

    private static Dimension getTimeBoundsDimension() {
        return Dimension.builder().setName("nv").setIsUnlimited(true).build();
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
    private static void addVariables(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        if (gridCollection.hasTimeDimension()) {
            addTimeVariable(writerBuilder, gridCollection);
        }

        if (gridCollection.hasTimeBounds()) {
            addVariableTimeBounds(writerBuilder);
        }

        if (gridCollection.isGeographic()) {
            addVariableLat(writerBuilder, gridCollection);
            addVariableLon(writerBuilder, gridCollection);
        } else {
            addVariableY(writerBuilder, gridCollection);
            addVariableX(writerBuilder, gridCollection);
        }

        addVariableProjection(writerBuilder, gridCollection);
        addVariableGridCollection(writerBuilder, gridCollection);

    }

    private static void addTimeVariable(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        if (!gridCollection.hasTimeDimension()) {
            return;
        }

        Variable.Builder<?> v = writerBuilder.addVariable(CF.TIME, DataType.ULONG, List.of(getTimeDimension()));
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
        v.addAttribute(new Attribute(CF.CALENDAR, "standard"));
        v.addAttribute(new Attribute(CF.UNITS, gridCollection.getTimeUnits()));

        if (gridCollection.hasTimeBounds()) {
            v.addAttribute(new Attribute(CF.BOUNDS, "time_bnds"));
        }

    }

    private static void addVariableTimeBounds(NetcdfFormatWriter.Builder writerBuilder) {
        Dimension timeDim = getTimeDimension();
        Dimension timeBoundsDim = getTimeBoundsDimension();
        writerBuilder.addVariable("time_bnds", DataType.ULONG, List.of(timeDim, timeBoundsDim));
    }

    private static void addVariableLat(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        Dimension latDim = getLatitudeDimension(gridCollection.getNy());
        Dimension yDim = getYDimension(gridCollection.getNy());
        Dimension xDim = getXDimension(gridCollection.getNx());

        boolean isGeographic = gridCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(latDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(latDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LAT_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "latitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));

    }

    private static void addVariableLon(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        Dimension lonDim = getLongitudeDimension(gridCollection.getNx());
        Dimension yDim = getYDimension(gridCollection.getNy());
        Dimension xDim = getXDimension(gridCollection.getNx());

        boolean isGeographic = gridCollection.isGeographic();
        List<Dimension> dimensions = isGeographic ? List.of(lonDim) : List.of(yDim, xDim);
        writerBuilder.addVariable(lonDim.getShortName(), DataType.DOUBLE, dimensions)
                .addAttribute(new Attribute(CF.UNITS, CDM.LON_UNITS))
                .addAttribute(new Attribute(CF.LONG_NAME, "longitude coordinate"))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));

    }

    private static void addVariableY(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        Dimension yDim = getYDimension(gridCollection.getNy());
        writerBuilder.addVariable(yDim.getShortName(), DataType.DOUBLE, List.of(yDim))
                .addAttribute(new Attribute(CF.UNITS, gridCollection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
    }

    private static void addVariableX(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        Dimension xDim = getXDimension(gridCollection.getNx());
        writerBuilder.addVariable(xDim.getShortName(), DataType.DOUBLE, List.of(xDim))
                .addAttribute(new Attribute(CF.UNITS, gridCollection.getProjectionUnit()))
                .addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
    }

    private static void addVariableProjection(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        Variable.Builder<?> variableBuilder = writerBuilder.addVariable(gridCollection.getProjectionName(), DataType.SHORT, Collections.emptyList());
        for (Parameter parameter : gridCollection.getProjection().getProjectionParameters()) {
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
        variableBuilder.addAttribute(new Attribute("crs_wkt", gridCollection.getWkt()));

    }

    private static void addVariableGridCollection(NetcdfFormatWriter.Builder writerBuilder, VortexGridCollection gridCollection) {
        Dimension timeDim = getTimeDimension();

        List<Dimension> dimensions = new ArrayList<>();
        if (gridCollection.hasTimeDimension()) {
            dimensions.add(timeDim);
        }

        if (gridCollection.isGeographic()) {
            dimensions.add(getLatitudeDimension(gridCollection.getNy()));
            dimensions.add(getLongitudeDimension(gridCollection.getNx()));
        } else {
            dimensions.add(getYDimension(gridCollection.getNy()));
            dimensions.add(getXDimension(gridCollection.getNx()));
        }

        Map<String, VortexGrid> availableVortexVariables = gridCollection.getRepresentativeGridNameMap();
        for (VortexGrid vortexGrid : availableVortexVariables.values()) {
            VortexVariable variable = VortexGridUtils.inferVortexVariableFromNames(vortexGrid);
            writerBuilder.addVariable(variable.getShortName(), DataType.FLOAT, dimensions)
                    .addAttribute(new Attribute(CF.LONG_NAME, variable.getLongName()))
                    .addAttribute(new Attribute(CF.UNITS, vortexGrid.units()))
                    .addAttribute(new Attribute(CF.GRID_MAPPING, gridCollection.getProjectionName()))
                    .addAttribute(new Attribute(CF.COORDINATES, "latitude longitude"))
                    .addAttribute(new Attribute(CF.MISSING_VALUE, (float) vortexGrid.noDataValue()))
                    .addAttribute(new Attribute(CF._FILLVALUE, (float) vortexGrid.noDataValue()))
                    .addAttribute(new Attribute(CF.CELL_METHODS, vortexGrid.dataType().getNcString()));
        }

    }

    private static void addGlobalAttributes(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.10"));
    }
    
    /* Write Dimensions */
    private static void writeProjectionDimensions(NetcdfFormatWriter writer, VortexGridCollection gridCollection) throws InvalidRangeException, IOException {
        if (gridCollection.isGeographic()) {
            writer.write(CF.LATITUDE, Array.makeFromJavaArray(gridCollection.getYCoordinates()));
            writer.write(CF.LONGITUDE, Array.makeFromJavaArray(gridCollection.getXCoordinates()));
        } else {
            writer.write("y", Array.makeFromJavaArray(gridCollection.getYCoordinates()));
            writer.write("x", Array.makeFromJavaArray(gridCollection.getXCoordinates()));
        }
    }

    private static void writeTimeDimension(NetcdfFormatWriter writer, List<VortexTimeRecord> timeRecords) throws InvalidRangeException, IOException {
        int numData = timeRecords.size();
        long[] timeData = new long[numData];

        for (int i = 0; i < numData; i++) {
            VortexTimeRecord grid = timeRecords.get(i);
            long startTime = VortexTimeUtils.getNumDurationsFromBaseTime(grid.startTime(), grid);
            long endTime = VortexTimeUtils.getNumDurationsFromBaseTime(grid.endTime(), grid);
            long midTime = (startTime + endTime) / 2;
            timeData[i] = midTime;
        }

        Variable timeVar = writer.findVariable(CF.TIME);
        writer.write(timeVar, new int[] {0}, Array.makeFromJavaArray(timeData));
    }

    private static long getNumDurationsFromBaseTime(ZonedDateTime dateTime, VortexTimeRecord timeRecord) {
        ZonedDateTime baseTime = VortexTimeUtils.BASE_TIME;
        ZonedDateTime zDateTime = dateTime.withZoneSameInstant(VortexTimeUtils.BASE_ZONE_ID);
        Duration durationBetween = Duration.between(baseTime, zDateTime);
        Duration divisor = Duration.of(1, getDurationUnit(getBaseDuration(timeRecord)));
        return durationBetween.dividedBy(divisor);
    }

    private static ChronoUnit getDurationUnit(Duration duration) {
        if (duration.toHours() > 0) {
            return ChronoUnit.HOURS;
        } else if (duration.toMinutes() > 0) {
            return ChronoUnit.MINUTES;
        } else {
            return ChronoUnit.SECONDS;
        }
    }

    private static Duration getBaseDuration(VortexTimeRecord timeRecord) {
        Duration interval = timeRecord.getRecordDuration();
        return interval.isZero() ? Duration.ofMinutes(1) : interval;
    }
}