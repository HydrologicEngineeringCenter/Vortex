package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.geo.WktFactory;
import mil.army.usace.hec.vortex.util.UnitUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;

import javax.measure.Unit;
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

class VariableDsReader extends NetcdfDataReader {
    private static final Logger logger = Logger.getLogger(VariableDsReader.class.getName());

    private final NetcdfDataset ncd;
    private final VariableDS variableDS;
    private final Grid gridDefinition;
    private final List<VortexDataInterval> timeBounds;

    /* Constructor */
    public VariableDsReader(NetcdfDataset ncd, VariableDS variableDS, String variableName) {
        super(new DataReaderBuilder().path(variableDS.getDatasetLocation()).variable(variableName));
        this.ncd = ncd;
        this.variableDS = getVariableDS(ncd, variableName);
        CoordinateSystem coordinateSystem = getCoordinateSystem(variableDS);
        this.gridDefinition = getGridDefinition(ncd, coordinateSystem);
        this.timeBounds = getTimeBounds();
    }

    private static VariableDS getVariableDS(NetcdfDataset ncd, String variableName) {
        Variable variable = ncd.findVariable(variableName);
        return variable instanceof VariableDS variableDS ? variableDS : null;
    }

    private static CoordinateSystem getCoordinateSystem(VariableDS variableDS) {
        List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
        return !coordinateSystems.isEmpty() ? coordinateSystems.get(0) : null;
    }

    /* Public Methods */
    @Override
    public List<VortexData> getDtos() {
        List<VortexData> dataList = new ArrayList<>();
        for (int i = 0; i < getDtoCount(); i++) {
            VortexData data = getDto(i);
            dataList.add(data);
        }

        return dataList;
    }

    @Override
    public VortexGrid getDto(int index) {
        return getTimeAxis() != null ? buildGridWithTimeAxis(index) : buildGridWithoutTimeAxis();
    }

    @Override
    public int getDtoCount() {
        List<Dimension> dimensions = variableDS.getDimensions();
        for (Dimension dimension : dimensions) {
            if (dimension.getShortName().equals("time")) {
                return dimension.getLength();
            }
        }

        return 1;
    }

    @Override
    public List<VortexDataInterval> getDataIntervals() {
        if (!(ncd.findCoordinateAxis(AxisType.Time) instanceof CoordinateAxis1D timeAxis)) {
            return Collections.emptyList();
        }

        String timeAxisUnits = timeAxis.getUnitsString();

        int count = (int) timeAxis.getSize();
        double[] startTimes = timeAxis.getBound1();
        double[] endTimes = timeAxis.getBound2();

        List<VortexDataInterval> timeRecords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ZonedDateTime startTime = parseTime(timeAxisUnits, startTimes[i]);
            ZonedDateTime endTime = parseTime(timeAxisUnits, endTimes[i]);
            VortexDataInterval timeRecord = VortexDataInterval.of(startTime, endTime);
            timeRecords.add(timeRecord);
        }

        return timeRecords;
    }

    /* Helpers */
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

    private List<VortexDataInterval> getTimeBounds() {
        CoordinateAxis1D timeAxis = getTimeAxis();
        if (timeAxis == null) return Collections.emptyList();

        String timeAxisUnits = timeAxis.getUnitsString();
        boolean isYearMonth = timeAxisUnits.equalsIgnoreCase("yyyymm");
        return isYearMonth ? getYearMonthTimeRecords(timeAxis) : getDataIntervals();
    }

    private CoordinateAxis1D getTimeAxis() {
        CoordinateAxis timeAxis = ncd.findCoordinateAxis(AxisType.Time);
        timeAxis = timeAxis == null ? ncd.findCoordinateAxis("time") : timeAxis;
        return timeAxis instanceof CoordinateAxis1D axis ? axis : null;
    }

    private List<VortexDataInterval> getYearMonthTimeRecords(CoordinateAxis1D timeAxis) {
        List<VortexDataInterval> timeRecords = new ArrayList<>();
        for (int i = 0; i < getDtoCount(); i++) {
            VortexDataInterval timeRecord = getYearMonthTimeRecord(timeAxis, i);
            timeRecords.add(timeRecord);
        }
        return timeRecords;
    }

    private VortexDataInterval getYearMonthTimeRecord(CoordinateAxis1D timeAxis, int timeIndex) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMM");
            YearMonth startYearMonth = YearMonth.parse(String.valueOf(Math.round(timeAxis.getBound1()[timeIndex])), formatter);
            YearMonth endYearMonth = YearMonth.parse(String.valueOf(Math.round(timeAxis.getBound2()[timeIndex])), formatter);
            ZonedDateTime startTime = startYearMonth.atDay(1).atStartOfDay(ZoneId.of("UTC"));
            ZonedDateTime endTime = endYearMonth.atDay(1).atStartOfDay(ZoneId.of("UTC"));
            return VortexDataInterval.of(startTime, endTime);
        } catch (DateTimeParseException e) {
            logger.info(e::getMessage);
            return undefinedTimeRecord();
        }
    }

    private VortexDataInterval undefinedTimeRecord() {
        ZonedDateTime undefinedStartTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime undefinedEndTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        return VortexDataInterval.of(undefinedStartTime, undefinedEndTime);
    }

    private VortexGrid buildGridWithTimeAxis(int timeIndex) {
        int dimensionIndex = getTimeDimensionIndex();
        float[] data = readSlicedData(dimensionIndex, timeIndex);
        VortexDataInterval timeRecord = timeBounds.get(timeIndex);
        return buildGrid(data, timeRecord);
    }

    private VortexGrid buildGridWithoutTimeAxis() {
        float[] data = readAllData();
        return buildGrid(data, undefinedTimeRecord());
    }

    private int getTimeDimensionIndex() {
        List<Dimension> dimensions = variableDS.getDimensions();

        int timeDimension = -1;
        for (int i = 0; i < dimensions.size(); i++) {
            if (dimensions.get(i).getShortName().contains("time")) {
                timeDimension = i;
            }
        }

        return timeDimension;
    }

    private float[] readAllData() {
        try {
            Array array = variableDS.read();
            return getFloatArray(array);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return new float[0];
        }
    }

    private float[] readSlicedData(int timeDimension, int timeIndex) {
        if (timeDimension < 0) {
            return readAllData();
        }

        try {
            Array array = variableDS.slice(timeDimension, timeIndex).read();
            return getFloatArray(array);
        } catch (IOException | InvalidRangeException e) {
            logger.severe("Error reading sliced data: " + e.getMessage());
            return new float[0];
        }
    }

    private VortexGrid buildGrid(float[] data, VortexDataInterval timeRecord) {
        // Grid must be shifted after getData call since getData uses the original locations to map values.
        Grid grid = Grid.toBuilder(gridDefinition).build();
        shiftGrid(grid);

        return VortexGrid.builder()
                .dx(grid.getDx()).dy(grid.getDy())
                .nx(grid.getNx()).ny(grid.getNy())
                .originX(grid.getOriginX()).originY(grid.getOriginY())
                .wkt(grid.getCrs())
                .data(data)
                .noDataValue(variableDS.getFillValue())
                .units(variableDS.getUnitsString())
                .fileName(ncd.getLocation())
                .shortName(variableDS.getShortName())
                .fullName(variableDS.getFullName())
                .description(variableDS.getDescription())
                .startTime(timeRecord.startTime())
                .endTime(timeRecord.endTime())
                .interval(timeRecord.getRecordDuration())
                .dataType(getVortexDataType(variableDS))
                .build();
    }

    private float[] getFloatArray(Array array) {
        return (float[]) array.get1DJavaArray(DataType.FLOAT);
    }

    private static Grid getGridDefinition(NetcdfDataset ncd, CoordinateSystem coordinateSystem) {
        CoordinateAxis lonAxis = ncd.findCoordinateAxis(AxisType.Lon);
        CoordinateAxis latAxis = ncd.findCoordinateAxis(AxisType.Lat);

        CoordinateAxis geoXAxis = ncd.findCoordinateAxis(AxisType.GeoX);
        CoordinateAxis geoYAxis = ncd.findCoordinateAxis(AxisType.GeoY);

        CoordinateAxis xAxis;
        CoordinateAxis yAxis;

        if (geoXAxis != null && geoYAxis != null) {
            xAxis = geoXAxis;
            yAxis = geoYAxis;
        } else if (lonAxis != null && latAxis != null) {
            xAxis = lonAxis;
            yAxis = latAxis;
        } else {
            throw new IllegalStateException();
        }

        int nx = (int) xAxis.getSize();
        int ny = (int) yAxis.getSize();

        double[] edgesX = ((CoordinateAxis1D) xAxis).getCoordEdges();
        double ulx = edgesX[0];

        double urx = edgesX[edgesX.length - 1];
        double dx = (urx - ulx) / nx;

        double[] edgesY = ((CoordinateAxis1D) yAxis).getCoordEdges();
        double uly = edgesY[0];
        double lly = edgesY[edgesY.length - 1];
        double dy = (lly - uly) / ny;

        String wkt = null;
        if (coordinateSystem != null) {
            wkt = WktFactory.createWkt(coordinateSystem.getProjection());
        } else if (lonAxis != null && latAxis != null) {
            wkt = WktFactory.fromEpsg(4326);
        }

        Grid gridDefinition = Grid.builder()
                .nx(nx).ny(ny)
                .dx(dx).dy(dy)
                .originX(ulx).originY(uly)
                .crs(wkt)
                .build();

        String xAxisUnits = Objects.requireNonNull(xAxis).getUnitsString();
        Unit<?> cellUnits = UnitUtil.parse(xAxisUnits.toLowerCase());
        Unit<?> csUnits = ReferenceUtils.getLinearUnits(wkt);

        if (cellUnits.isCompatible(csUnits) && !cellUnits.equals(csUnits)) {
            gridDefinition = scaleGrid(gridDefinition, cellUnits, csUnits);
        }

        return gridDefinition;
    }

    @Override
    public void close() throws Exception {
        ncd.close();
    }
}


