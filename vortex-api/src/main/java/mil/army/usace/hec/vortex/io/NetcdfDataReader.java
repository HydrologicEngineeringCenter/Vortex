package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.geo.WktFactory;
import si.uom.NonSI;
import tec.units.indriya.unit.MetricPrefix;
import tech.units.indriya.AbstractUnit;
import tech.units.indriya.unit.Units;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class NetcdfDataReader extends DataReader {
    private NetcdfDataset ncd;
    private VariableDS variableDS;
    private CoordinateSystem coordinateSystem;

    private int nx;
    private int ny;
    private double dx;
    private double dy;
    private double ulx;
    private double uly;
    private String wkt;
    private String units;
    private String fileName;
    private String shortName;
    private String fullName;
    private String description;

    NetcdfDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    private void readDataset() {
        try {
            ncd = NetcdfDataset.openDataset(path.toString());
            Variable variable = ncd.findVariable(variableName);
            if (variable instanceof VariableDS) {
                this.variableDS = (VariableDS) variable;

                List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                if (!coordinateSystems.isEmpty()) {
                    coordinateSystem = coordinateSystems.get(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processData() {
        processCellInfo();
        fileName = path.getFileName().toString();
        shortName = variableDS.getShortName();
        fullName = variableDS.getFullName();
        description = variableDS.getDescription();
        units = variableDS.getUnitsString();
    }

    private void processCellInfo() {

        CoordinateAxis xAxis = null;
        CoordinateAxis yAxis;

        if (coordinateSystem.getXaxis() != null && coordinateSystem.getYaxis() != null) {
            xAxis = coordinateSystem.getXaxis();
            yAxis = coordinateSystem.getYaxis();

            nx = (int) xAxis.getSize();
            ny = (int) yAxis.getSize();

            double[] edgesX = ((CoordinateAxis1D) xAxis).getCoordEdges();
            ulx = edgesX[0];

            double urx = edgesX[edgesX.length - 1];
            dx = (urx - ulx) / nx;

            double[] edgesY = ((CoordinateAxis1D) yAxis).getCoordEdges();
            uly = edgesY[0];
            double lly = edgesY[edgesY.length - 1];
            dy = (lly - uly) / ny;

        } else if (coordinateSystem.getLatAxis() != null && coordinateSystem.getLonAxis() != null) {
            xAxis = coordinateSystem.getLonAxis();
            yAxis = coordinateSystem.getLatAxis();

            nx = (int) xAxis.getSize();
            ny = (int) yAxis.getSize();

            double[] edgesX = ((CoordinateAxis1D) xAxis).getCoordEdges();
            ulx = edgesX[0];

            double urx = edgesX[edgesX.length - 1];
            dx = (urx - ulx) / nx;

            double[] edgesY = ((CoordinateAxis1D) yAxis).getCoordEdges();
            uly = edgesY[0];
            double lly = edgesY[edgesY.length - 1];
            dy = (lly - uly) / ny;
        }

        List<String> gribExtensions = new ArrayList<>(Arrays.asList(".grib", ".gb2", ".grb2", ".grib2", ".grb"));
        gribExtensions.forEach(extension -> {
                    if (path.toString().endsWith(extension)) {
                        wkt = RasterUtils.getWkt(path);
                    }
                });
        if (wkt == null || wkt.isEmpty()) {
            wkt = WktFactory.createWkt(coordinateSystem.getProjection());
        }

        String xAxisUnits = Objects.requireNonNull(xAxis).getUnitsString();

        Unit<?> cellUnits;
        switch (xAxisUnits.toLowerCase()) {
            case "m":
            case "meter":
            case "metre":
                cellUnits = Units.METRE;
                break;
            case "km":
                cellUnits = MetricPrefix.KILO(Units.METRE);
                break;
            case "degrees_east":
            case "degrees_north":
                cellUnits = NonSI.DEGREE_ANGLE;
                break;
            default:
                cellUnits = AbstractUnit.ONE;
        }

        if (cellUnits == NonSI.DEGREE_ANGLE && ulx == 0){
            ulx = -180;
        }

        if (cellUnits == NonSI.DEGREE_ANGLE && ulx > 180){
            ulx = ulx - 360;
        }

        Unit<?> csUnits;
        switch (ReferenceUtils.getMapUnits(wkt).toLowerCase()) {
            case "m":
            case "meter":
            case "metre":
                csUnits = Units.METRE;
                break;
            case "km":
                csUnits = MetricPrefix.KILO(Units.METRE);
                break;
            default:
                csUnits = AbstractUnit.ONE;
        }

        if (cellUnits.isCompatible(csUnits)) {
            try {
                UnitConverter converter = cellUnits.getConverterToAny(csUnits);
                ulx = converter.convert(ulx);
                uly = converter.convert(uly);
                dx = converter.convert(dx);
                dy = converter.convert(dy);
            } catch (IncommensurableException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<VortexGrid> getDTOs() {
        List<VortexGrid> dtos = new ArrayList<>();
        readDataset();
        processData();

        CoordinateAxis1D timeAxis;
        if (ncd.findCoordinateAxis(AxisType.Time) != null) {
            timeAxis = (CoordinateAxis1D) ncd.findCoordinateAxis(AxisType.Time);
        } else {
            timeAxis = (CoordinateAxis1D) ncd.findCoordinateAxis("time");
        }

        if (timeAxis != null) {
            List<Dimension> dimensions = variableDS.getDimensions();
            AtomicInteger timeDimension = new AtomicInteger();
            timeDimension.set(-1);
            IntStream.range(0, dimensions.size()).forEach(dimension -> {
                if (dimensions.get(dimension).getShortName().contains("time")) {
                    timeDimension.set(dimension);
                }
            });

            AtomicInteger iterations = new AtomicInteger();
            iterations.set((int) timeAxis.getSize());
            if (timeDimension.get() >= 0){
                iterations.set((int) Math.min(timeAxis.getSize(), dimensions.get(timeDimension.get()).getLength()));
            }

            IntStream.range(0, iterations.get()).forEach(time -> {

                Array array;
                try {
                    if (timeDimension.get() >= 0) {
                        array = variableDS.slice(timeDimension.get(), time).read();
                    } else {
                        array = variableDS.read();
                    }
                } catch (IOException | InvalidRangeException e) {
                    e.printStackTrace();
                    array = ucar.ma2.Array.factory(new float[]{});
                }

                float[] data = getFloatArray(array);

                String units = timeAxis.getUnitsString();

                if (units.equalsIgnoreCase("yyyymm")) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMM");
                    try {
                        YearMonth startYearMonth = YearMonth.parse(String.valueOf(Math.round(timeAxis.getBound1()[time])), formatter);
                        YearMonth endYearMonth = YearMonth.parse(String.valueOf(Math.round(timeAxis.getBound2()[time])), formatter);
                        ZonedDateTime startTime = ZonedDateTime.of(startYearMonth.getYear(), startYearMonth.getMonth().getValue(),
                                1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        ZonedDateTime endTime = ZonedDateTime.of(endYearMonth.getYear(), endYearMonth.getMonth().getValue(),
                                1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        Duration interval = Duration.between(startTime, endTime);
                        dtos.add(createDTO(data, startTime, endTime, interval));
                    } catch (DateTimeParseException e) {
                        System.out.println(e);
                        ZonedDateTime startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        ZonedDateTime endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        Duration interval = Duration.ofMinutes(0);
                        dtos.add(createDTO(data, startTime, endTime, interval));
                    }
                } else {
                    String dateTimeString = (units.split(" ", 3)[2]).replaceFirst(" ", "T").split(" ")[0].replace(".", "");

                    ZonedDateTime origin;
                    if (dateTimeString.contains("T")) {
                        origin = ZonedDateTime.of(LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME), ZoneId.of("UTC"));
                    } else {
                        origin = ZonedDateTime.of(LocalDate.parse(dateTimeString, DateTimeFormatter.ofPattern("uuuu-M-d")), LocalTime.of(0, 0), ZoneId.of("UTC"));
                    }

                    try {
                        ZonedDateTime startTime;
                        ZonedDateTime endTime;
                        if (units.toLowerCase().matches("^month[s]? since.*$")) {
                            startTime = origin.plusMonths((long) timeAxis.getBound1()[time]);
                            endTime = origin.plusMonths((long) timeAxis.getBound2()[time]);
                        } else if (units.toLowerCase().matches("^day[s]? since.*$")) {
                            startTime = origin.plusSeconds((long) timeAxis.getBound1()[time] * 86400);
                            endTime = origin.plusSeconds((long) timeAxis.getBound2()[time] * 86400);
                        } else if (units.toLowerCase().matches("^hour[s]? since.*$")) {
                            startTime = origin.plusSeconds((long) timeAxis.getBound1()[time] * 3600);
                            if (fileName.toLowerCase().matches("hrrr.*wrfsfcf.*")){
                                endTime = startTime.plusHours(1);
                            } else {
                                endTime = origin.plusSeconds((long) timeAxis.getBound2()[time] * 3600);
                            }
                        } else if (units.toLowerCase().matches("^minute[s]? since.*$")) {
                            endTime = origin.plusSeconds((long) timeAxis.getBound2()[time] * 60);
                            if (variableDS.getDescription().toLowerCase().contains("qpe01h")) {
                                startTime = endTime.minusHours(1);
                            } else {
                                startTime = origin.plusSeconds((long) timeAxis.getBound1()[time] * 60);
                            }
                        } else if (units.toLowerCase().matches("^second[s]? since.*$")) {
                            startTime = origin.plusSeconds((long) timeAxis.getBound1()[time]);
                            endTime = origin.plusSeconds((long) timeAxis.getBound2()[time]);
                        } else {
                            startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                            endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        }
                        Duration interval = Duration.between(startTime, endTime);
                        dtos.add(createDTO(data, startTime, endTime, interval));
                    } catch (Exception e) {
                        System.out.println(e);
                        ZonedDateTime startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        ZonedDateTime endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                        Duration interval = Duration.ofMinutes(0);
                        dtos.add(createDTO(data, startTime, endTime, interval));
                    }
                }
            });
        }
        closeDataset();
        return dtos;
    }

    private VortexGrid createDTO(float[] data, ZonedDateTime startTime, ZonedDateTime endTime, Duration interval) {
        return VortexGrid.builder()
                .dx(dx).dy(dy).nx(nx).ny(ny)
                .originX(ulx).originY(uly)
                .wkt(wkt).data(data).units(units)
                .fileName(fileName).shortName(shortName).fullName(fullName).description(description)
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval).build();
    }

    public static Set<String> getVariables(Path path) {
        try (NetcdfDataset ncd = NetcdfDataset.openDataset(path.toString())) {
            List<Variable> variables = ncd.getVariables();
            Set<String> variableNames = new HashSet<>();
            variables.forEach(variable -> {
                if (variable instanceof VariableDS) {
                    VariableDS variableDS = (VariableDS) variable;
                    List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                    if (!coordinateSystems.isEmpty()) {
                        variableNames.add(variable.getFullName());
                    }
                }
            });
            return variableNames;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    private float[] getFloatArray(ucar.ma2.Array array) {
        float[] data;
        Object myArr;
        try {
            myArr = array.copyTo1DJavaArray();
            if (myArr instanceof float[]) {
                data = (float[]) myArr;
                return data;
            } else if (myArr instanceof double[]) {
                double[] doubleArray = (double[]) myArr;
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

    private void closeDataset() {
        try {
            ncd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
