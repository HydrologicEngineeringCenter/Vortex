package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.*;
import tec.units.indriya.unit.MetricPrefix;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static javax.measure.MetricPrefix.KILO;
import static systems.uom.common.USCustomary.DEGREE_ANGLE;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.METRE;

public class NetcdfDataReader extends DataReader {

    NetcdfDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDTOs() {
        String location = path.toString();
        Formatter errlog = new Formatter();
        try (FeatureDataset dataset = FeatureDatasetFactoryManager.open(FeatureType.ANY, location, null, errlog)) {
            if (dataset == null) {
                System.out.printf("**failed on %s %n --> %s %n", location, errlog);
                return Collections.emptyList();
            }

            FeatureType ftype = dataset.getFeatureType();

            if (ftype == FeatureType.GRID) {
                assert (dataset instanceof GridDataset);
                GridDataset gridDataset = (GridDataset) dataset;
                return getData(gridDataset, variableName);
            } else if (ftype == FeatureType.RADIAL) {
                assert (dataset instanceof RadialDatasetSweep);
                RadialDatasetSweep radialDataset = (RadialDatasetSweep) dataset;
            } else if (ftype.isPointFeatureType()) {
                assert dataset instanceof FeatureDatasetPoint;
                FeatureDatasetPoint pointDataset = (FeatureDatasetPoint) dataset;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return Collections.emptyList();
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

    private float[] getFloatArray(Array array) {
        DataType type = array.getDataType();
        try {
            if (type == DataType.FLOAT) {
                return (float[]) array.copyTo1DJavaArray();
            } else if (type == DataType.DOUBLE) {
                double[] dataIn = (double[]) array.copyTo1DJavaArray();
                float[] dataOut = new float[(int) array.getSize()];
                for (int i = 0; i < dataIn.length; i++) {
                    dataOut[i] = (float) (dataIn[i]);
                }
                return dataOut;
            }
        } catch (ClassCastException e) {
            return new float[]{};
        }
        return new float[]{};
    }

    private List<VortexData> getData(GridDataset dataset, String variable) {
        GridDatatype gridDatatype = dataset.findGridDatatype(variable);
        GridCoordSystem gcs = gridDatatype.getCoordinateSystem();

        Grid grid = getGrid(gcs);
        String wkt = getWkt(gcs.getProjection());

        List<ZonedDateTime[]> times = getTimeBounds(gcs);

        Dimension timeDim = gridDatatype.getTimeDimension();
        Dimension endDim = gridDatatype.getEnsembleDimension();
        Dimension rtDim = gridDatatype.getRunTimeDimension();

        List<VortexData> grids = new ArrayList<>();

        if (timeDim != null && endDim != null && rtDim != null) {
            IntStream.range(0, rtDim.getLength()).forEach(rtIndex ->
                    IntStream.range(0, endDim.getLength()).forEach(ensIndex ->
                            IntStream.range(0, timeDim.getLength()).forEach(timeIndex -> {
                                Array array;
                                try {
                                    array = gridDatatype.readDataSlice(rtIndex, ensIndex, timeIndex, -1, -1, -1);
                                    float[] data = getFloatArray(array);
                                    ZonedDateTime startTime = times.get(timeIndex)[0];
                                    ZonedDateTime endTime = times.get(timeIndex)[1];
                                    Duration interval = Duration.between(startTime, endTime);
                                    grids.add(VortexGrid.builder()
                                            .dx(grid.getDx()).dy(grid.getDy())
                                            .nx(grid.getNx()).ny(grid.getNy())
                                            .originX(grid.getOriginX()).originY(grid.getOriginY())
                                            .wkt(wkt)
                                            .data(data)
                                            .units(gridDatatype.getUnitsString())
                                            .fileName(dataset.getLocation())
                                            .shortName(gridDatatype.getShortName())
                                            .fullName(gridDatatype.getFullName())
                                            .description(gridDatatype.getDescription())
                                            .startTime(startTime)
                                            .endTime(endTime)
                                            .interval(interval)
                                            .build());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            })));
        } else {
            IntStream.range(0, times.size()).forEach(timeIndex -> {
                try {
                    Array array = gridDatatype.readDataSlice(timeIndex, -1, -1, -1);
                    float[] data = getFloatArray(array);
                    ZonedDateTime startTime = times.get(timeIndex)[0];
                    ZonedDateTime endTime = times.get(timeIndex)[1];
                    Duration interval = Duration.between(startTime, endTime);
                    grids.add(VortexGrid.builder()
                            .dx(grid.getDx()).dy(grid.getDy())
                            .nx(grid.getNx()).ny(grid.getNy())
                            .originX(grid.getOriginX()).originY(grid.getOriginY())
                            .wkt(wkt)
                            .data(data)
                            .units(gridDatatype.getUnitsString())
                            .fileName(dataset.getLocation())
                            .shortName(gridDatatype.getShortName())
                            .fullName(gridDatatype.getFullName())
                            .description(gridDatatype.getDescription())
                            .startTime(startTime)
                            .endTime(endTime)
                            .interval(interval)
                            .build());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        return grids;
    }

    private static String getWkt(Projection projection) {
        return WktFactory.createWkt((ProjectionImpl) projection);
    }

    private List<ZonedDateTime[]> getTimeBounds(GridCoordSystem gcs) {
        List<ZonedDateTime[]> list = new ArrayList<>();
        if (gcs.hasTimeAxis1D()) {
            CoordinateAxis1DTime tAxis = gcs.getTimeAxis1D();
            IntStream.range(0, (int) tAxis.getSize()).forEach(time -> {
                ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
                CalendarDate[] dates = tAxis.getCoordBoundsDate(time);

                String fileName = path.getFileName().toString().toLowerCase();
                if (fileName.matches(".*gaugecorr.*qpe.*01h.*grib2")
                        || fileName.matches(".*radaronly.*qpe.*01h.*grib2")){
                    zonedDateTimes[0] = convert(dates[0]).minusHours(1);
                } else {
                    zonedDateTimes[0] = convert(dates[0]);
                }

                if (fileName.matches("hrrr.*wrfsfcf.*")){
                    zonedDateTimes[1] = zonedDateTimes[0].plusHours(1);
                } else {
                    zonedDateTimes[1] = convert(dates[1]);
                }

                list.add(zonedDateTimes);
            });
            return list;
        } else if (gcs.hasTimeAxis()) {
            CoordinateAxis1D tAxis = (CoordinateAxis1D) gcs.getTimeAxis();
            String units = tAxis.getUnitsString();

            IntStream.range(0, (int) tAxis.getSize()).forEach(time -> {
                String dateTimeString = (units.split(" ", 3)[2]).replaceFirst(" ", "T").split(" ")[0].replace(".", "");

                ZonedDateTime origin;

                if (dateTimeString.contains("T")) {
                    origin = ZonedDateTime.of(LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME), ZoneId.of("UTC"));
                } else {
                    origin = ZonedDateTime.of(LocalDate.parse(dateTimeString, DateTimeFormatter.ofPattern("uuuu-M-d")), LocalTime.of(0, 0), ZoneId.of("UTC"));
                }
                ZonedDateTime startTime;
                ZonedDateTime endTime;
                if (units.toLowerCase().matches("^month[s]? since.*$")) {
                    startTime = origin.plusMonths((long) tAxis.getBound1()[time]);
                    endTime = origin.plusMonths((long) tAxis.getBound2()[time]);
                } else if (units.toLowerCase().matches("^day[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) tAxis.getBound1()[time] * 86400);
                    endTime = origin.plusSeconds((long) tAxis.getBound2()[time] * 86400);
                } else if (units.toLowerCase().matches("^hour[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) tAxis.getBound1()[time] * 3600);
                        endTime = origin.plusSeconds((long) tAxis.getBound2()[time] * 3600);
                } else if (units.toLowerCase().matches("^minute[s]? since.*$")) {
                    endTime = origin.plusSeconds((long) tAxis.getBound2()[time] * 60);
                        startTime = origin.plusSeconds((long) tAxis.getBound1()[time] * 60);
                } else if (units.toLowerCase().matches("^second[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) tAxis.getBound1()[time]);
                    endTime = origin.plusSeconds((long) tAxis.getBound2()[time]);
                } else {
                    startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                    endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                }
                ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
                zonedDateTimes[0] = startTime;
                zonedDateTimes[1] = endTime;
                list.add(zonedDateTimes);
            });
            return list;
        }
        return Collections.emptyList();
    }

    private static ZonedDateTime convert(CalendarDate date) {
        return ZonedDateTime.parse(date.toString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    private static Grid getGrid(GridCoordSystem coordinateSystem) {
        AtomicReference<Grid> grid = new AtomicReference<>();

        CoordinateAxis xAxis = coordinateSystem.getXHorizAxis();
        CoordinateAxis yAxis = coordinateSystem.getYHorizAxis();

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

        grid.set(Grid.builder()
                .nx(nx)
                .ny(ny)
                .dx(dx)
                .dy(dy)
                .originX(ulx)
                .originY(uly)
                .build());

        String wkt = getWkt(coordinateSystem.getProjection());

        String xAxisUnits = Objects.requireNonNull(xAxis).getUnitsString();

        Unit<?> cellUnits;
        switch (xAxisUnits.toLowerCase()) {
            case "m":
            case "meter":
            case "metre":
                cellUnits = METRE;
                break;
            case "km":
                cellUnits = KILO(METRE);
                break;
            case "degrees_east":
            case "degrees_north":
                cellUnits = DEGREE_ANGLE;
                break;
            default:
                cellUnits = ONE;
        }

        Unit<?> csUnits;
        switch (ReferenceUtils.getMapUnits(wkt).toLowerCase()) {
            case "m":
            case "meter":
            case "metre":
                csUnits = METRE;
                break;
            case "km":
                csUnits = MetricPrefix.KILO(METRE);
                break;
            default:
                csUnits = ONE;
        }

        if (cellUnits == DEGREE_ANGLE && (grid.get().getOriginX() == 0 || grid.get().getOriginX() > 180)) {
            grid.set(shiftGrid(grid.get()));
        }

        if (cellUnits.isCompatible(csUnits) && !cellUnits.equals(csUnits)) {
            grid.set(scaleGrid(grid.get(), cellUnits, csUnits));
        }

        return grid.get();
    }

    private static Grid shiftGrid(Grid grid){
        if (grid.getOriginX() > 180) {
            return Grid.builder()
                    .originX(grid.getOriginX() - 360)
                    .originY(grid.getOriginY())
                    .dx(grid.getDx())
                    .dy(grid.getDy())
                    .nx(grid.getNx())
                    .ny(grid.getNy())
                    .build();
        } else {
            return grid;
        }
    }

    private static Grid scaleGrid(Grid grid, Unit<?> cellUnits, Unit<?> csUnits){
        Grid scaled;
        try {
            UnitConverter converter = cellUnits.getConverterToAny(csUnits);
            scaled = Grid.builder()
                    .originX(converter.convert(grid.getOriginX()))
                    .originY(converter.convert(grid.getOriginY()))
                    .dx(converter.convert(grid.getDx()))
                    .dy(converter.convert(grid.getDy()))
                    .nx(grid.getNx())
                    .ny(grid.getNy())
                    .build();
        } catch (IncommensurableException e) {
            return null;
        }
        return scaled;
    }
}
