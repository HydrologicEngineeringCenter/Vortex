package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class NetcdfDataReader extends DataReader {
    private static final Logger logger = Logger.getLogger(NetcdfDataReader.class.getName());

    private static final PathMatcher NC_MATCHER = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.nc4?");
    private static final String TIME_BOUNDS = "time_bnds";
    private static final Pattern CELL_METHODS_QUALIFIER = Pattern.compile("\\([^)]*\\)");
    private static final Pattern TIME_CELL_METHOD = compileCellMethodPattern(CF.TIME);

    /* Factory Method */
    public static NetcdfDataReader createInstance(String pathToFile, String pathToData) throws DataReadException {
        NetcdfDataset dataset = openDataset(pathToFile);

        GridDataset gridDataset = getGridDataset(dataset);
        if (gridDataset != null) {
            return new GridDatasetReader(gridDataset, pathToData);
        }

        VariableDS variableDS = getVariableDataset(dataset, pathToData);
        if (variableDS != null) {
            return new VariableDsReader(dataset, variableDS, pathToData);
        }

        throw DataReadException.ioError(pathToFile, pathToData,
                "NetCDF dataset at " + pathToFile + " contains no readable grid or variable for '" + pathToData + "'");
    }

    NetcdfDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public abstract List<VortexData> getDtos() throws DataReadException;

    @Override
    public abstract VortexData getDto(int idx) throws DataReadException;

    @Override
    public abstract int getDtoCount();

    public static Set<String> getVariables(String path) {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path)) {
            List<Variable> variables = ncd.getVariables();
            Set<String> variableNames = new HashSet<>();
            for (Variable variable : variables) {
                if (isSelectableVariable(variable)) {
                    VariableDS variableDS = (VariableDS) variable;
                    List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                    if (!coordinateSystems.isEmpty()) {
                        variableNames.add(variable.getFullName());
                    }
                }
            }
            return variableNames;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return Collections.emptySet();
    }

    /* Helpers */
    private static NetcdfDataset openDataset(String pathToFile) throws DataReadException {
        try {
            return NetcdfDatasets.openDataset(pathToFile);
        } catch (IOException e) {
            throw DataReadException.ioError(pathToFile, null,
                    "Could not open NetCDF dataset at " + pathToFile + ": " + e.getMessage(), e);
        }
    }

    private static GridDataset getGridDataset(NetcdfDataset netcdfDataset) {
        try {
            Formatter errorLog = new Formatter();
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, netcdfDataset, null, errorLog);
            if (dataset == null) return null;
            boolean isGrid = dataset.getFeatureType() == FeatureType.GRID;
            return isGrid && dataset instanceof GridDataset gridDataset ? gridDataset : null;
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    private static VariableDS getVariableDataset(NetcdfDataset netcdfDataset, String variableName) {
        return netcdfDataset.getVariables().stream()
                .filter(v -> v.getShortName().equals(variableName) && v instanceof VariableDS)
                .map(VariableDS.class::cast)
                .filter(v -> !v.getCoordinateSystems().isEmpty() || isLatLon(netcdfDataset))
                .findFirst()
                .orElse(null);
    }

    private static boolean isLatLon(NetcdfDataset ncd) {
        return ncd.findCoordinateAxis(AxisType.Lon) != null && ncd.findCoordinateAxis(AxisType.Lat) != null;
    }

    private static boolean isSelectableVariable(Variable variable) {
        boolean isVariableDS = variable instanceof VariableDS;
        boolean isLatLon = variable.findDimensionIndex("latitude") >= 0
                && variable.findDimensionIndex("longitude") >= 0;

        return isVariableDS || isLatLon;
    }

    static Grid scaleGrid(Grid grid, Unit<?> cellUnits, Unit<?> csUnits) {
        Grid scaled;
        try {
            // Converting xAxis and yAxis to be consistent with the wkt units
            UnitConverter converter = cellUnits.getConverterToAny(csUnits);
            scaled = Grid.builder()
                    .originX(converter.convert(grid.getOriginX()))
                    .originY(converter.convert(grid.getOriginY()))
                    .dx(converter.convert(grid.getDx()))
                    .dy(converter.convert(grid.getDy()))
                    .nx(grid.getNx())
                    .ny(grid.getNy())
                    .crs(grid.getCrs())
                    .build();
        } catch (IncommensurableException e) {
            return null;
        }
        return scaled;
    }

    static void shiftGrid(Grid grid) {
        String crs = grid.getCrs();
        boolean isGeographic = ReferenceUtils.isGeographic(crs);
        if (isGeographic && (grid.getOriginX() > 180 || grid.getTerminusX() > 180)) {
            grid.shift(-360, 0);
        }
    }

    /**
     * Resolves a variable's data type from its CF {@code cell_methods} attribute.
     *
     * @param timeAxisName the short name of the time coordinate variable, or null when it is unknown.
     *                     CF allows a cell_methods entry to be keyed on the time coordinate variable's
     *                     own name rather than the literal "time"; both are accepted.
     */
    static VortexDataType getVortexDataType(VariableDS variableDS, String timeAxisName) {
        String cellMethods = variableDS.findAttributeString(CF.CELL_METHODS, "");
        return VortexDataType.fromString(parseTimeCellMethod(cellMethods, timeAxisName));
    }

    /**
     * Extracts the method applied to the time coordinate from a CF cell_methods string. CF-1.11 §7.3
     * defines the attribute as a blank-separated list of "name: method [(qualifiers)]" entries, so the
     * method has to be pulled out before it can be mapped to a {@link VortexDataType}. A string with no
     * "name:" entry is returned unchanged, so the bare tokens written by {@link NetcdfWriterPrep}
     * before it emitted CF-conformant output ("mean", "sum", "point") keep resolving as they always have.
     *
     * @return the method applied to the time coordinate, or an empty string when the attribute declares
     * no method for it (a cell_methods of "area: mean" says nothing about how time was reduced).
     */
    static String parseTimeCellMethod(String cellMethods, String timeAxisName) {
        if (cellMethods == null || cellMethods.isBlank()) return "";

        // Qualifiers are dropped first: "(interval: 1 day)" contains a colon of its own.
        String stripped = CELL_METHODS_QUALIFIER.matcher(cellMethods).replaceAll(" ");
        if (!stripped.contains(":")) return stripped.trim();

        Matcher matcher = timeCellMethodPattern(timeAxisName).matcher(stripped);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Pattern timeCellMethodPattern(String timeAxisName) {
        boolean isDefaultName = timeAxisName == null || timeAxisName.isBlank() || timeAxisName.equalsIgnoreCase(CF.TIME);
        if (isDefaultName) return TIME_CELL_METHOD;
        return compileCellMethodPattern(CF.TIME + "|" + Pattern.quote(timeAxisName));
    }

    private static Pattern compileCellMethodPattern(String names) {
        return Pattern.compile("(?:^|\\s)(?:" + names + ")\\s*:\\s*([A-Za-z_]+)", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Validation isValid() {
        List<String> messages = new ArrayList<>();

        try (NetcdfDataset dataset = NetcdfDatasets.openDataset(path)) {
            Path pathToFile = Path.of(path);
            if (NC_MATCHER.matches(pathToFile)) {
                Variable variable = dataset.findVariable(TIME_BOUNDS);
                if (variable == null) {
                    messages.add(Message.format("warn_nc_time_bnds"));
                }
            }
        } catch (IOException e) {
            String message = Message.format("error_invalid_file", path);
            return Validation.of(false, message);
        }

        if (hasSpanningInstantaneousRecords()) {
            messages.add(Message.format("warn_nc_instantaneous_span", variableName));
        }

        return messages.isEmpty() ? Validation.of(true) : Validation.of(true, messages);
    }

    /**
     * Reports whether the variable declares itself instantaneous while its time bounds span a period.
     * Such records cannot be indexed as instants and are dropped, which leaves the reader with no time
     * range and every read empty. Without this check the condition is invisible until compute time.
     */
    private boolean hasSpanningInstantaneousRecords() {
        if (getDeclaredDataType() != VortexDataType.INSTANTANEOUS) {
            return false;
        }

        try {
            return getDataIntervals().stream()
                    .filter(VortexDataInterval::isDefined)
                    .anyMatch(interval -> !interval.isInstantaneous());
        } catch (DataReadException e) {
            logger.log(Level.INFO, e, e::getMessage);
            return false;
        }
    }

    /**
     * The data type declared by the source variable's CF cell_methods attribute, before any inference
     * {@link mil.army.usace.hec.vortex.VortexGrid#dataType()} applies on top of it.
     */
    abstract VortexDataType getDeclaredDataType();

    abstract double getNoDataValue();
}
