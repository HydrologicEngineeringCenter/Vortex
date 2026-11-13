package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexVariable;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gets a NetCDF destination into the state a write needs, and says which record of it each time step
 * of the batch belongs to.
 *
 * <p>A destination that does not exist is created with the dimensions, variables and attributes the
 * batch needs. A destination that exists and was created here is kept and extended: the records it
 * already holds stay at the indices they are at, the records of the batch it does not hold are
 * appended to its record dimension, and a record it already holds is written again over the one that
 * is there. That is what lets an export too large to hold in memory at once be written one batch of
 * time steps at a time.
 *
 * <p>Everything else about an existing destination is refused rather than discarded. netCDF-Java can
 * write data into the variables an existing file declares and nothing else: a variable, dimension or
 * attribute cannot be added to a file once it is created, so a batch the file cannot hold has nowhere
 * to go. Overwriting the file would drop the time steps that are already in it, which is the loss this
 * class exists to prevent, so the caller is told instead.
 */
final class NetcdfWriterPrep {
    private static final Logger logger = Logger.getLogger(NetcdfWriterPrep.class.getName());

    // NetCDF4 Settings
    private static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    private static final int DEFLATE_LEVEL = 9;
    private static final boolean SHUFFLE = false;
    private static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    private static final String TIME = CF.TIME;
    private static final String TIME_BOUNDS = NetcdfTimeAxis.TIME_BOUNDS_NAME;

    private NetcdfWriterPrep() {
        // Utility Class
    }

    /**
     * Creates or extends a destination so it can hold the variables described by
     * {@code gridCollection} at every time step in {@code batchRecords}. Returns the resulting
     * record axis for indexing grid writes.
     *
     * @param destination path of the NetCDF file to create or extend; an existing file must be
     *                    compatible with the supplied grid collection unless {@code overwrite} is true
     * @param gridCollection representative grids for every variable to write, with the geometry,
     *                       projection, and time encoding required by the destination; it need not
     *                       contain a grid for every time step
     * @param batchRecords all time intervals to be written by this operation, including intervals
     *                     absent from {@code gridCollection}; order and uniqueness are not required
     * @param overwrite whether to replace an existing destination instead of extending it
     * @throws DataWriteException if the destination cannot be created or cannot hold the batch
     */
    static NetcdfTimeAxis prepareForWriting(String destination, VortexGridCollection gridCollection,
                                            Collection<VortexDataInterval> batchRecords, boolean overwrite) {
        return overwrite || !hasContent(destination)
                ? create(destination, gridCollection, batchRecords)
                : extend(destination, gridCollection, batchRecords);
    }

    static NetcdfFormatWriter.Builder appendingWriterBuilder(String destination) {
        return writerBuilder(destination, false);
    }

    /* Create */
    private static NetcdfTimeAxis create(String destination, VortexGridCollection gridCollection,
                                         Collection<VortexDataInterval> batchRecords) {
        NetcdfTimeAxis timeAxis = createTimeAxis(destination, gridCollection, batchRecords);

        try (NetcdfFormatWriter writer = newFileWriterBuilder(destination, gridCollection).build()) {
            writeProjectionDimensions(writer, gridCollection);
            writeTimeRecords(writer, timeAxis, timeAxis.records(), 0);
        } catch (DataWriteException e) {
            throw e;
        } catch (IOException | InvalidRangeException | RuntimeException e) {
            throw DataWriteException.of(destination, "it cannot be created (" + e.getMessage() + ")", e);
        }

        logger.info(() -> "Created NetCDF file with " + timeAxis.recordCount() + " records for appending.");
        return timeAxis;
    }

    private static NetcdfTimeAxis createTimeAxis(String destination, VortexGridCollection gridCollection,
                                                 Collection<VortexDataInterval> batchRecords) {
        if (!gridCollection.hasTimeDimension()) {
            return NetcdfTimeAxis.of(ChronoUnit.MINUTES, false, List.of());
        }

        ChronoUnit unit = getDeclaredTimeUnit(destination, gridCollection);
        return NetcdfTimeAxis.of(unit, gridCollection.hasTimeBounds(), sorted(batchRecords));
    }

    private static ChronoUnit getDeclaredTimeUnit(String destination, VortexGridCollection gridCollection) {
        String units = gridCollection.getTimeUnits();
        try {
            return NetcdfTimeUtils.getDeclaredTimeUnit(units);
        } catch (IllegalArgumentException e) {
            throw DataWriteException.of(destination,
                    "its time cannot be encoded: '" + units + "' is not a units attribute a CF grid file uses", e);
        }
    }

    /* Extend */
    private static NetcdfTimeAxis extend(String destination, VortexGridCollection gridCollection,
                                         Collection<VortexDataInterval> batchRecords) {
        NetcdfTimeAxis stored = readTimeAxis(destination, gridCollection);

        NetcdfTimeAxis.Merge merge = stored.mergedWith(sorted(batchRecords));
        if (merge.newRecords().isEmpty()) {
            return merge.axis();
        }

        try (NetcdfFormatWriter writer = appendingWriterBuilder(destination).build()) {
            writeTimeRecords(writer, merge.axis(), merge.newRecords(), stored.recordCount());
        } catch (DataWriteException e) {
            throw e;
        } catch (IOException | InvalidRangeException | RuntimeException e) {
            throw DataWriteException.of(destination, "its time axis cannot be extended (" + e.getMessage() + ")", e);
        }

        logger.info(() -> "Extended the time axis of an existing NetCDF file by " + merge.newRecords().size() + " records.");
        return merge.axis();
    }

    private static NetcdfTimeAxis readTimeAxis(String destination, VortexGridCollection gridCollection) {
        try (NetcdfFile file = NetcdfFiles.open(destination)) {
            verifyDimensions(destination, file, gridCollection);
            verifyCoordinates(destination, file, gridCollection);
            NetcdfTimeAxis stored = NetcdfTimeAxis.readFrom(destination, file);
            verifyTimeEncoding(destination, stored, gridCollection);
            verifyVariables(destination, file, gridCollection);
            return stored;
        } catch (IOException e) {
            throw DataWriteException.of(destination,
                    "it cannot be opened as a NetCDF file (delete it or choose another destination)", e);
        }
    }

    /* Verify */
    private static void verifyDimensions(String destination, NetcdfFile file, VortexGridCollection gridCollection) {
        Dimension timeDimension = file.findDimension(TIME);
        boolean hasRecordDimension = timeDimension != null && timeDimension.isUnlimited();
        boolean hasCoordinateDimensions = file.findDimension(getYName(gridCollection)) != null
                && file.findDimension(getXName(gridCollection)) != null;

        if (!hasRecordDimension || !hasCoordinateDimensions) {
            throw DataWriteException.of(destination, notWrittenByVortex());
        }
    }

    private static void verifyCoordinates(String destination, NetcdfFile file, VortexGridCollection gridCollection) {
        double[] storedY = readCoordinates(destination, file, getYName(gridCollection));
        double[] storedX = readCoordinates(destination, file, getXName(gridCollection));

        boolean sameShape = storedY.length == gridCollection.getNy() && storedX.length == gridCollection.getNx();
        boolean sameLocation = Arrays.equals(storedY, gridCollection.getYCoordinates())
                && Arrays.equals(storedX, gridCollection.getXCoordinates());
        Variable projection = file.findVariable(gridCollection.getProjectionName());

        if (!sameShape || !sameLocation) {
            throw DataWriteException.of(destination,
                    "it holds a " + storedY.length + " x " + storedX.length + " grid at a different location or"
                            + " resolution than the " + gridCollection.getNy() + " x " + gridCollection.getNx()
                            + " grid being written (delete it or choose another destination)");
        }

        if (projection == null) {
            throw DataWriteException.of(destination,
                    "it does not hold the '" + gridCollection.getProjectionName() + "' projection of the grids"
                            + " being written (delete it or choose another destination)");
        }

        String storedWkt = projection.findAttributeString("crs_wkt", null);
        String incomingWkt = gridCollection.getWkt();
        if (storedWkt == null || storedWkt.isBlank()
                || incomingWkt == null || incomingWkt.isBlank()
                || !ReferenceUtils.equals(storedWkt, incomingWkt)) {
            throw DataWriteException.of(destination,
                    "its '" + gridCollection.getProjectionName() + "' projection differs from the grids"
                            + " being written (delete it or choose another destination)");
        }
    }

    private static void verifyTimeEncoding(String destination, NetcdfTimeAxis stored, VortexGridCollection gridCollection) {
        ChronoUnit storedUnit = stored.unit();

        if (gridCollection.hasTimeDimension()) {
            ChronoUnit batchUnit = getDeclaredTimeUnit(destination, gridCollection);
            if (storedUnit.getDuration().getSeconds() > batchUnit.getDuration().getSeconds()) {
                throw DataWriteException.of(destination, "it encodes time in " + storedUnit + ", which is too coarse"
                        + " for records written in " + batchUnit + " (delete it or choose another destination)");
            }
        }

        if (stored.hasBounds() != gridCollection.hasTimeBounds()) {
            throw DataWriteException.of(destination, stored.hasBounds()
                    ? "it holds time bounds but the grids being written have no interval, so the records they"
                    + " need cannot be described (delete it or choose another destination)"
                    : "it holds no '" + TIME_BOUNDS + "' variable, so the interval records the grids being written"
                    + " span cannot be recorded (delete it or choose another destination)");
        }
    }

    private static void verifyVariables(String destination, NetcdfFile file, VortexGridCollection gridCollection) {
        Set<String> missing = new TreeSet<>();
        for (String variableName : gridCollection.getVariableNames()) {
            Variable variable = file.findVariable(variableName);
            if (variable == null) {
                missing.add(variableName);
                continue;
            }

            if (variable.getRank() != 3) {
                throw DataWriteException.of(destination, "its '" + variableName + "' variable holds "
                        + variable.getRank() + " dimensions, not the time, y and x this write needs");
            }
        }

        if (!missing.isEmpty()) {
            throw DataWriteException.of(destination,
                    "it does not hold " + missing + ", and a variable cannot be added to a NetCDF file once the file"
                            + " is created. Write every variable of a batch in one call, declare them all in the"
                            + " first write to this destination, or choose another destination"
                            + " (it does hold " + variablesOf(file) + ")");
        }
    }

    private static String variablesOf(NetcdfFile file) {
        List<String> names = new ArrayList<>();
        for (Variable variable : file.getVariables()) {
            if (variable.getRank() == 3) {
                names.add(variable.getShortName());
            }
        }
        return names.toString();
    }

    private static double[] readCoordinates(String destination, NetcdfFile file, String name) {
        Variable variable = file.findVariable(name);
        if (variable == null) {
            throw DataWriteException.of(destination, notWrittenByVortex());
        }

        try {
            return (double[]) variable.read().get1DJavaArray(DataType.DOUBLE);
        } catch (IOException e) {
            throw DataWriteException.of(destination, "its '" + name + "' coordinates cannot be read", e);
        }
    }

    /** The name of the coordinate variable the rows of the grid are measured along. */
    private static String getYName(VortexGridCollection gridCollection) {
        return gridCollection.isGeographic() ? CF.LATITUDE : "y";
    }

    /** The name of the coordinate variable the columns of the grid are measured along. */
    private static String getXName(VortexGridCollection gridCollection) {
        return gridCollection.isGeographic() ? CF.LONGITUDE : "x";
    }

    private static String notWrittenByVortex() {        return "it is not a CF grid file written by Vortex, which is the only kind of file this writer can add"
                + " time steps to (delete it or choose another destination)";
    }

    /* Writer Builders */
    private static NetcdfFormatWriter.Builder newFileWriterBuilder(String destination, VortexGridCollection gridCollection) {
        NetcdfFormatWriter.Builder writerBuilder = writerBuilder(destination, true);
        addDimensions(writerBuilder, gridCollection);
        addVariables(writerBuilder, gridCollection);
        addGlobalAttributes(writerBuilder);
        return writerBuilder;
    }

    private static NetcdfFormatWriter.Builder writerBuilder(String destination, boolean isNewFile) {
        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        return NetcdfFormatWriter.builder()
                .setNewFile(isNewFile)
                .setFormat(NETCDF_FORMAT)
                .setLocation(destination)
                .setChunker(chunker);
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
        return Dimension.builder().setName(TIME).setIsUnlimited(true).build();
    }

    private static Dimension getTimeBoundsDimension() {
        return Dimension.builder().setName("nv").setLength(2).build();
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

        Variable.Builder<?> v = writerBuilder.addVariable(TIME, DataType.ULONG, List.of(getTimeDimension()));
        v.addAttribute(new Attribute(CF.STANDARD_NAME, TIME));
        v.addAttribute(new Attribute(CF.CALENDAR, "standard"));
        v.addAttribute(new Attribute(CF.UNITS, gridCollection.getTimeUnits()));

        if (gridCollection.hasTimeBounds()) {
            v.addAttribute(new Attribute(CF.BOUNDS, TIME_BOUNDS));
        }

    }

    private static void addVariableTimeBounds(NetcdfFormatWriter.Builder writerBuilder) {
        Dimension timeDim = getTimeDimension();
        Dimension timeBoundsDim = getTimeBoundsDimension();
        writerBuilder.addVariable(TIME_BOUNDS, DataType.ULONG, List.of(timeDim, timeBoundsDim));
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
        // CF Conventions: https://cfconventions.org/Data/cf-conventions/cf-conventions-1.10/cf-conventions.html#use-of-the-crs-well-known-text-format
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
            VortexVariable variable = VortexVariable.fromGrid(vortexGrid);
            writerBuilder.addVariable(variable.getShortName(), DataType.FLOAT, dimensions)
                    .addAttribute(new Attribute(CF.LONG_NAME, variable.getLongName()))
                    .addAttribute(new Attribute(CF.UNITS, vortexGrid.units()))
                    .addAttribute(new Attribute(CF.GRID_MAPPING, gridCollection.getProjectionName()))
                    .addAttribute(new Attribute(CF.COORDINATES, "latitude longitude"))
                    .addAttribute(new Attribute(CF.MISSING_VALUE, (float) vortexGrid.noDataValue()))
                    .addAttribute(new Attribute(CF._FILLVALUE, (float) vortexGrid.noDataValue()))
                    .addAttribute(new Attribute(CF.CELL_METHODS, getCellMethods(vortexGrid)));
        }

    }

    /**
     * Builds the CF cell_methods attribute for a grid. CF-1.10 §7.3 requires each entry to name the
     * dimension the method was applied to, so the method is keyed on the time dimension this writer
     * creates, and the file declares "CF-1.10" globally. Earlier versions wrote the bare method
     * ("mean", "sum", "point"); NetcdfDataReader still reads that form, so files written before this
     * change keep resolving to the same data type.
     */
    private static String getCellMethods(VortexGrid vortexGrid) {
        String method = vortexGrid.dataType().getNcString();
        return method.isBlank() ? method : TIME + ": " + method;
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

    /**
     * Writes the records at {@code origin} of the destination's time axis. The destination declares the
     * unit its times are encoded in, once, when it is created, and {@code timeAxis} reads that unit from
     * it, so a batch appended later is encoded the same way as the records already there.
     */
    private static void writeTimeRecords(NetcdfFormatWriter writer, NetcdfTimeAxis timeAxis,
                                         List<VortexDataInterval> records, int origin) throws InvalidRangeException, IOException {
        if (records.isEmpty()) {
            return;
        }

        Variable timeVariable = writer.findVariable(TIME);
        if (timeVariable == null) {
            return;
        }
        writer.write(timeVariable, new int[]{origin}, Array.makeFromJavaArray(timeAxis.midTimes(records)));

        if (!timeAxis.hasBounds()) {
            return;
        }
        Variable timeBoundsVariable = writer.findVariable(TIME_BOUNDS);
        if (timeBoundsVariable != null) {
            writer.write(timeBoundsVariable, new int[]{origin, 0}, Array.makeFromJavaArray(timeAxis.bounds(records)));
        }
    }

    /* Helpers */
    private static boolean hasContent(String destination) {
        Path path = Path.of(destination);
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException e) {
            throw DataWriteException.of(destination, "its size cannot be read", e);
        }
    }

    private static List<VortexDataInterval> sorted(Collection<VortexDataInterval> records) {
        List<VortexDataInterval> sorted = records.stream()
                .filter(VortexDataInterval::isDefined)
                .collect(Collectors.toCollection(ArrayList::new));
        sorted.sort(Comparator.comparing((VortexDataInterval record) -> record.startTime())
                .thenComparing(VortexDataInterval::endTime));
        return sorted;
    }
}
