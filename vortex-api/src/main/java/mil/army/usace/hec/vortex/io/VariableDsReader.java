package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.*;
import org.locationtech.jts.geom.Coordinate;
import si.uom.NonSI;
import tech.units.indriya.AbstractUnit;
import tech.units.indriya.unit.Units;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.measure.MetricPrefix.KILO;

public class VariableDsReader {
    private static final Logger logger = Logger.getLogger(VariableDsReader.class.getName());

    private final NetcdfDataset ncd;
    private final String variableName;

    private VariableDS variableDS;
    private CoordinateSystem coordinateSystem;
    private int nx;
    private int ny;
    private double dx;
    private double dy;
    private double ulx;
    private double uly;
    private String wkt;

    private VariableDsReader(Builder builder) {
        ncd = builder.ncd;
        variableName = builder.variableName;
    }

    public static class Builder {
        private NetcdfDataset ncd;
        private String variableName;

        public Builder setNetcdfFile(NetcdfDataset ncd) {
            this.ncd = ncd;
            return this;
        }

        public Builder setVariableName(String variableName) {
            this.variableName = variableName;
            return this;
        }

        public VariableDsReader build() {
            return new VariableDsReader(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public VortexGrid read(int index) {

        try {
            Variable variable = ncd.findVariable(variableName);
            if (variable instanceof VariableDS) {
                this.variableDS = (VariableDS) variable;

                List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                if (!coordinateSystems.isEmpty()) {
                    coordinateSystem = coordinateSystems.get(0);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        processCellInfo();

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

        double noDataValue = variableDS.getFillValue();

        float[] data = getFloatArray(array);
        float[] indexed = getData(data, noDataValue);

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
                Duration interval = Duration.between(startTime, endTime);
                return createDTO(indexed, startTime, endTime, interval);
            } catch (DateTimeParseException e) {
                logger.info(e::getMessage);
                ZonedDateTime startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                ZonedDateTime endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                Duration interval = Duration.ofMinutes(0);
                return createDTO(indexed, startTime, endTime, interval);
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
                Duration interval = Duration.between(startTime, endTime);
                return createDTO(indexed, startTime, endTime, interval);
            } catch (Exception e) {
                logger.info(e::getMessage);
                ZonedDateTime startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                ZonedDateTime endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                Duration interval = Duration.ofMinutes(0);
                return createDTO(indexed, startTime, endTime, interval);
            }
        }
    }

    private VortexGrid createDTO(float[] data, ZonedDateTime startTime, ZonedDateTime endTime, Duration interval) {
        return VortexGrid.builder()
                .dx(dx)
                .dy(dy)
                .nx(nx)
                .ny(ny)
                .originX(ulx)
                .originY(uly)
                .wkt(wkt)
                .data(data)
                .noDataValue(variableDS.getFillValue())
                .units(variableDS.getUnitsString())
                .fileName(ncd.getLocation())
                .shortName(variableDS.getShortName())
                .fullName(variableDS.getFullName())
                .description(variableDS.getDescription())
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval)
                .build();
    }

    private VortexGrid createDTO(float[] data) {
        return createDTO(data, null, null, Duration.ZERO);
    }

    private float[] getFloatArray(ucar.ma2.Array array) {
        float[] data;
        Object myArr;
        try {
            myArr = array.copyTo1DJavaArray();
            if (myArr instanceof float[]) {
                data = (float[]) myArr;
                return data;
            } else if (myArr instanceof double[] doubleArray) {
                data = new float[(int) array.getSize()];
                float[] datalocal = data;
                for (int i = 0; i < data.length; i++) {
                    datalocal[i] = (float) (doubleArray[i]);
                }
            } else {
                // Could not parse
                data = new float[]{};
            }
        } catch (ClassCastException e) {
            data = new float[]{};
        }
        return data;
    }

    private CoordinateAxes getCoordinateAxes() {
        CoordinateAxis lonAxis = ncd.findCoordinateAxis(AxisType.Lon);
        CoordinateAxis latAxis = ncd.findCoordinateAxis(AxisType.Lat);

        CoordinateAxis geoXAxis = ncd.findCoordinateAxis(AxisType.GeoX);
        CoordinateAxis geoYAxis = ncd.findCoordinateAxis(AxisType.GeoY);

        Variable latVar = ncd.findVariable("lat");
        Variable lonVar = ncd.findVariable("lon");

        CoordinateAxis xAxis;
        CoordinateAxis yAxis;

        if (geoXAxis != null && geoYAxis != null) {
            xAxis = geoXAxis;
            yAxis = geoYAxis;
        } else if (lonAxis != null && latAxis != null) {
            xAxis = lonAxis;
            yAxis = latAxis;
        } else if (lonVar != null && latVar != null) {
            xAxis = CoordinateAxis.fromVariableDS(VariableDS.builder().copyFrom(lonVar)).build(ncd.getRootGroup());
            yAxis = CoordinateAxis.fromVariableDS(VariableDS.builder().copyFrom(latVar)).build(ncd.getRootGroup());
        } else {
            throw new IllegalStateException();
        }

        return new CoordinateAxes(xAxis, yAxis);
    }

    private void processCellInfo() {
        CoordinateAxes coordinateAxes = getCoordinateAxes();
        CoordinateAxis xAxis = coordinateAxes.xAxis();
        CoordinateAxis yAxis = coordinateAxes.yAxis();

        if (xAxis instanceof CoordinateAxis1D xAxis1D
                && yAxis instanceof CoordinateAxis1D yAxis1D) {
            nx = (int) xAxis.getSize();
            ny = (int) yAxis.getSize();

            double[] edgesX = xAxis1D.getCoordEdges();
            ulx = edgesX[0];

            double urx = edgesX[edgesX.length - 1];
            dx = (urx - ulx) / nx;

            double[] edgesY = yAxis1D.getCoordEdges();
            uly = edgesY[0];

            double lly = edgesY[edgesY.length - 1];
            dy = (lly - uly) / ny;
        }

        if (xAxis instanceof CoordinateAxis2D xAxis2D
                && yAxis instanceof CoordinateAxis2D yAxis2D) {
            int shapeX = xAxis2D.getEdges().getShape()[1] - 1;
            double minX = xAxis2D.getMinValue();
            double maxX = xAxis2D.getMaxValue();
            dx = (maxX - minX) / (shapeX - 1);

            int shapeY = xAxis2D.getEdges().getShape()[0] - 1;
            double minY = yAxis2D.getMinValue();
            double maxY = yAxis2D.getMaxValue();
            dy = (maxY - minY) / (shapeY - 1);

            double cellSize = (dx + dy) / 2;

            nx = (int) Math.round((maxX - minX) / cellSize);
            ny = (int) Math.round((maxY - minY) / cellSize);

            double[] edgesX = new double[nx];
            for (int i = 0; i < nx; i++) {
                edgesX[i] = minX + i * cellSize;
            }

            double[] edgesY = new double[ny];
            for (int i = 0; i < ny; i++) {
                edgesY[i] = minY + i * cellSize;
            }

            ulx = edgesX[0];
            uly = edgesY[0];
        }

        if (coordinateSystem != null) {
            wkt = WktFactory.createWkt(coordinateSystem.getProjection());
        } else {
            wkt = WktFactory.fromEpsg(4326);
        }

        String xAxisUnits = Objects.requireNonNull(xAxis).getUnitsString();

        Unit<?> cellUnits = switch (xAxisUnits.toLowerCase()) {
            case "m", "meter", "metre" -> Units.METRE;
            case "km" -> KILO(Units.METRE);
            case "degrees_east", "degree_east", "degrees_north" -> NonSI.DEGREE_ANGLE;
            default -> AbstractUnit.ONE;
        };

        if (cellUnits == NonSI.DEGREE_ANGLE && ulx == 0) {
            ulx = -180;
        }

        if (cellUnits == NonSI.DEGREE_ANGLE && ulx > 180) {
            ulx = ulx - 360;
        }

        Unit<?> csUnits = switch (ReferenceUtils.getMapUnits(wkt).toLowerCase()) {
            case "m", "meter", "metre" -> Units.METRE;
            case "km" -> KILO(Units.METRE);
            default -> AbstractUnit.ONE;
        };

        if (cellUnits.isCompatible(csUnits)) {
            try {
                UnitConverter converter = cellUnits.getConverterToAny(csUnits);
                ulx = converter.convert(ulx);
                uly = converter.convert(uly);
                dx = converter.convert(dx);
                dy = converter.convert(dy);
            } catch (IncommensurableException e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }
        }
    }

    private float[] getData(float[] slice, double noDataValue) {
        CoordinateAxes coordinateAxes = getCoordinateAxes();
        CoordinateAxis xAxis = coordinateAxes.xAxis();
        CoordinateAxis yAxis = coordinateAxes.yAxis();

        if (xAxis.isInterval() && yAxis.isInterval())
            return slice;

        Grid gridDefinition = Grid.builder()
                .nx(nx)
                .ny(ny)
                .dx(dx)
                .dy(dy)
                .originX(ulx)
                .originY(uly)
                .crs(wkt)
                .build();

        IndexSearcher indexSearcher = IndexSearcherFactory.INSTANCE.getOrCreate(coordinateAxes);
        Coordinate[] coordinates = gridDefinition.getGridCellCentroidCoords();
        int[] indices = indexSearcher.getIndices(coordinates);

        float[] data = new float[coordinates.length];
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            data[i] = index != -1 ? slice[index] : (float) noDataValue;
        }

        return data;
    }
}


