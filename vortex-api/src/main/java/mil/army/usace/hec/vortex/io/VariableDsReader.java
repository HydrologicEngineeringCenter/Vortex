package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexTimeRecord;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VariableDsReader extends NetcdfDataReader {
    private static final Logger logger = Logger.getLogger(VariableDsReader.class.getName());

    private final VariableDS variableDS;
    private final Grid gridDefinition;

    /* Constructor */
    public VariableDsReader(VariableDS variableDS, String variableName) {
        super(new DataReaderBuilder().path(variableDS.getDatasetLocation()).variable(variableName));
        this.variableDS = getVariableDS(ncd, variableName);
        CoordinateSystem coordinateSystem = getCoordinateSystem(variableDS);
        this.gridDefinition = getGridDefinition(ncd, coordinateSystem);
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
        CoordinateAxis1D timeAxis;
        if (ncd.findCoordinateAxis(AxisType.Time) != null) {
            timeAxis = (CoordinateAxis1D) ncd.findCoordinateAxis(AxisType.Time);
        } else {
            timeAxis = (CoordinateAxis1D) ncd.findCoordinateAxis("time");
        }

        if (timeAxis == null) {
            Array array;
            try {
                array = variableDS.read();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e, e::getMessage);
                array = ucar.ma2.Array.factory(DataType.FLOAT, new int[]{});
            }

            float[] data = getFloatArray(array);
            return createDTO(data);
        }

        List<Dimension> dimensions = variableDS.getDimensions();
        int timeDimension = -1;
        for (int i = 0; i < dimensions.size(); i++) {
            if (dimensions.get(i).getShortName().contains("time")) {
                timeDimension = i;
            }
        }

        Array array;
        try {
            if (timeDimension >= 0) {
                array = variableDS.slice(timeDimension, index).read();
            } else {
                array = variableDS.read();
            }
        } catch (IOException | InvalidRangeException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
            array = ucar.ma2.Array.factory(DataType.FLOAT, new int[]{});
        }

        float[] data = getFloatArray(array);

        String timeAxisUnits = timeAxis.getUnitsString();

        if (timeAxisUnits.equalsIgnoreCase("yyyymm")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMM");
            try {
                YearMonth startYearMonth = YearMonth.parse(String.valueOf(Math.round(timeAxis.getBound1()[index])), formatter);
                YearMonth endYearMonth = YearMonth.parse(String.valueOf(Math.round(timeAxis.getBound2()[index])), formatter);
                ZonedDateTime startTime = ZonedDateTime.of(startYearMonth.getYear(), startYearMonth.getMonth().getValue(),
                        1, 0, 0, 0, 0, ZoneId.of("UTC"));
                ZonedDateTime endTime = ZonedDateTime.of(endYearMonth.getYear(), endYearMonth.getMonth().getValue(),
                        1, 0, 0, 0, 0, ZoneId.of("UTC"));
                Duration.between(startTime, endTime);
                return buildGrid(data, new VortexTimeRecord(startTime, endTime));
            } catch (DateTimeParseException e) {
                logger.info(e::getMessage);
                ZonedDateTime startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                ZonedDateTime endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                return buildGrid(data, new VortexTimeRecord(startTime, endTime));
            }
        } else {
            String dateTimeString = (timeAxisUnits.split(" ", 3)[2]).replaceFirst(" ", "T").split(" ")[0].replace(".", "");

            ZonedDateTime origin;
            if (dateTimeString.contains("T")) {
                origin = ZonedDateTime.of(LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME), ZoneId.of("UTC"));
            } else {
                origin = ZonedDateTime.of(LocalDate.parse(dateTimeString, DateTimeFormatter.ofPattern("uuuu-M-d")), LocalTime.of(0, 0), ZoneId.of("UTC"));
            }

            try {
                ZonedDateTime startTime;
                ZonedDateTime endTime;
                if (timeAxisUnits.toLowerCase().matches("^month[s]? since.*$")) {
                    startTime = origin.plusMonths((long) timeAxis.getBound1()[index]);
                    endTime = origin.plusMonths((long) timeAxis.getBound2()[index]);
                } else if (timeAxisUnits.toLowerCase().matches("^day[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) timeAxis.getBound1()[index] * 86400);
                    endTime = origin.plusSeconds((long) timeAxis.getBound2()[index] * 86400);
                } else if (timeAxisUnits.toLowerCase().matches("^hour[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) timeAxis.getBound1()[index] * 3600);
                    if (ncd.getLocation().toLowerCase().matches("hrrr.*wrfsfcf.*")) {
                        endTime = startTime.plusHours(1);
                    } else {
                        endTime = origin.plusSeconds((long) timeAxis.getBound2()[index] * 3600);
                    }
                } else if (timeAxisUnits.toLowerCase().matches("^minute[s]? since.*$")) {
                    endTime = origin.plusSeconds((long) timeAxis.getBound2()[index] * 60);
                    if (variableDS.getDescription().toLowerCase().contains("qpe01h")) {
                        startTime = endTime.minusHours(1);
                    } else {
                        startTime = origin.plusSeconds((long) timeAxis.getBound1()[index] * 60);
                    }
                } else if (timeAxisUnits.toLowerCase().matches("^second[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) timeAxis.getBound1()[index]);
                    endTime = origin.plusSeconds((long) timeAxis.getBound2()[index]);
                } else {
                    startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                    endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                }
                return buildGrid(data, new VortexTimeRecord(startTime, endTime));
            } catch (Exception e) {
                logger.info(e::getMessage);
                ZonedDateTime startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                ZonedDateTime endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                return buildGrid(data, new VortexTimeRecord(startTime, endTime));
            }
        }
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

    /* Helpers */
    private VortexGrid buildGrid(float[] data, VortexTimeRecord timeRecord) {
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

    private VortexGrid createDTO(float[] data) {
        return buildGrid(data, new VortexTimeRecord(null, null));
    }

    private float[] getFloatArray(ucar.ma2.Array array) {
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
        Unit<?> cellUnits = UnitUtil.getUnits(xAxisUnits.toLowerCase());
        Unit<?> csUnits = UnitUtil.getUnits(ReferenceUtils.getMapUnits(wkt).toLowerCase());

        if (cellUnits.isCompatible(csUnits) && !cellUnits.equals(csUnits)) {
            gridDefinition = scaleGrid(gridDefinition, cellUnits, csUnits);
        }

        return gridDefinition;
    }
}


