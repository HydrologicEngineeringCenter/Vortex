package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.temporal.VortexTimeRecord;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.util.TimeConverter;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class NetcdfDataReader extends DataReader {
    private static final Logger logger = Logger.getLogger(NetcdfDataReader.class.getName());
    final NetcdfDataset ncd;

    /* Factory Method */
    public static NetcdfDataReader createInstance(String pathToFile, String pathToData) {
        NetcdfDataset dataset = getNetcdfDataset(pathToFile);
        if (dataset == null) {
            return null;
        }

        GridDataset gridDataset = getGridDataset(dataset);
        if (gridDataset != null) {
            return new GridDatasetReader(gridDataset, pathToData);
        }

        VariableDS variableDS = getVariableDataset(dataset, pathToData);
        if (variableDS != null) {
            return new VariableDsReader(variableDS, pathToData);
        }

        return null;
    }

    NetcdfDataReader(DataReaderBuilder builder) {
        super(builder);
        ncd = getNetcdfDataset(path);
    }

    @Override
    public abstract List<VortexData> getDtos();

    @Override
    public abstract VortexData getDto(int idx);

    @Override
    public List<VortexTimeRecord> getTimeRecords() {
        if (! (ncd.findCoordinateAxis(AxisType.Time) instanceof CoordinateAxis1D timeAxis)) {
            return Collections.emptyList();
        }

        String timeAxisUnits = timeAxis.getUnitsString();

        if (!timeAxis.isInterval()) {
            double[] instantTimeValues = timeAxis.getCoordValues();
            return Arrays.stream(instantTimeValues)
                    .mapToObj(timeValue -> parseTime(timeAxisUnits, timeValue))
                    .map(z -> VortexTimeRecord.of(z, z))
                    .toList();
        }

        int count = (int) timeAxis.getSize();
        double[] startTimes = timeAxis.getBound1();
        double[] endTimes = timeAxis.getBound2();

        List<VortexTimeRecord> timeRecords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ZonedDateTime startTime = parseTime(timeAxisUnits, startTimes[i]);
            ZonedDateTime endTime = parseTime(timeAxisUnits, endTimes[i]);
            VortexTimeRecord timeRecord = VortexTimeRecord.of(startTime, endTime);
            timeRecords.add(timeRecord);
        }

        return timeRecords;
    }

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

                    boolean isLatLon = ncd.findCoordinateAxis(AxisType.Lon) != null
                            && ncd.findCoordinateAxis(AxisType.Lat) != null;

                    if (!coordinateSystems.isEmpty() || isLatLon) {
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
    private static NetcdfDataset getNetcdfDataset(String pathToFile) {
        try {
            return NetcdfDatasets.openDataset(pathToFile);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return null;
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

    private ZonedDateTime parseTime(String timeAxisUnits, double timeValue) {
        String[] split = timeAxisUnits.split("since");
        String chronoUnitStr = split[0].trim().toUpperCase(Locale.ROOT);
        String dateTimeStr = split[1].trim();
        ChronoUnit chronoUnit = ChronoUnit.valueOf(chronoUnitStr);
        ZonedDateTime origin = TimeConverter.toZonedDateTime(dateTimeStr);
        if (origin == null) return undefinedTime();
        return origin.plus((long) timeValue, chronoUnit);
    }

    private ZonedDateTime undefinedTime() {
        return ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
    }

    private static boolean isSelectableVariable(Variable variable) {
        boolean isVariableDS = variable instanceof VariableDS;
        boolean isNotAxis = !(variable instanceof CoordinateAxis);
        return isVariableDS && isNotAxis;
    }

    static Grid scaleGrid(Grid grid, Unit<?> cellUnits, Unit<?> csUnits){
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

    static VortexDataType getVortexDataType(VariableDS variableDS) {
        String cellMethods = variableDS.findAttributeString(CF.CELL_METHODS, "");
        return VortexDataType.fromString(cellMethods);
    }
}
