package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.*;
import mil.army.usace.hec.vortex.geo.*;
import mil.army.usace.hec.vortex.util.TimeConverter;
import org.locationtech.jts.geom.Coordinate;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javax.measure.MetricPrefix.KILO;
import static systems.uom.common.USCustomary.DEGREE_ANGLE;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.METRE;

public class NetcdfDataReader extends DataReader {

    private static final Logger logger = Logger.getLogger(NetcdfDataReader.class.getName());

    NetcdfDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path); Formatter errlog = new Formatter()) {
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, ncd, null, errlog);
            if (dataset != null) {
                FeatureType ftype = dataset.getFeatureType();
                if (ftype == FeatureType.GRID
                        && dataset instanceof GridDataset gridDataset
                        && gridDataset.findGridDatatype(variableName) != null) {
                    return getData(gridDataset, variableName);
                }
            }

            List<Variable> variables = ncd.getVariables();
            for (Variable variable : variables) {
                if (variable.getShortName().equals(variableName)
                        && variable instanceof VariableDS variableDS) {
                    int count = getDtoCount(variableDS);

                    VariableDsReader reader = VariableDsReader.builder()
                            .setNetcdfFile(ncd)
                            .setVariableName(variableName)
                            .build();

                    List<VortexData> dataList = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        VortexData data = reader.read(i);
                        dataList.add(data);
                    }
                    return dataList;
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Override
    public VortexData getDto(int idx) {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path); Formatter errlog = new Formatter()) {
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, ncd, null, errlog);
            if (dataset != null) {
                FeatureType ftype = dataset.getFeatureType();
                if (ftype == FeatureType.GRID
                        && dataset instanceof GridDataset gridDataset
                        && gridDataset.findGridDatatype(variableName) != null) {
                    return getData(gridDataset, variableName, idx);
                }
            }

            List<Variable> variables = ncd.getVariables();
            for (Variable variable : variables) {
                if (variable.getShortName().equals(variableName)
                        && variable instanceof VariableDS variableDS) {
                    List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();

                    boolean isLatLon = ncd.findCoordinateAxis(AxisType.Lon) != null
                            && ncd.findCoordinateAxis(AxisType.Lat) != null;

                    if (!coordinateSystems.isEmpty() || isLatLon) {
                        VariableDsReader reader = VariableDsReader.builder()
                                .setNetcdfFile(ncd)
                                .setVariableName(variableName)
                                .build();

                        return reader.read(idx);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        return null;
    }

    @Override
    public List<VortexTimeRecord> getTimeRecords() {
        return getDtos().stream()
                .map(VortexTimeRecord::of)
                .toList();
    }

    @Override
    public int getDtoCount() {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path); Formatter errlog = new Formatter()) {
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, ncd, null, errlog);
            if (dataset != null) {
                FeatureType ftype = dataset.getFeatureType();
                if (ftype == FeatureType.GRID
                        && dataset instanceof GridDataset gridDataset
                        && gridDataset.findGridDatatype(variableName) != null) {
                    return getDtoCount(gridDataset, variableName);
                }
            }

            List<Variable> variables = ncd.getVariables();
            for (Variable variable : variables) {
                if (variable.getShortName().equals(variableName) && variable instanceof VariableDS variableDS) {
                    List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();

                    boolean isLatLon = ncd.findCoordinateAxis(AxisType.Lon) != null
                            && ncd.findCoordinateAxis(AxisType.Lat) != null;

                    if (!coordinateSystems.isEmpty() || isLatLon) {
                        return getDtoCount(variableDS);
                    }
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        return 0;
    }

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

    private static boolean isSelectableVariable(Variable variable) {
        boolean isVariableDS = variable instanceof VariableDS;
        boolean isNotAxis = !(variable instanceof CoordinateAxis);
        return isVariableDS && isNotAxis;
    }

    private float[] getFloatArray(Array array) {
        return (float[]) array.get1DJavaArray(DataType.FLOAT);
    }

    private List<VortexData> getData(GridDataset gridDataset, String variable) {
        GridDatatype gridDatatype = gridDataset.findGridDatatype(variable);
        GridCoordSystem gcs = gridDatatype.getCoordinateSystem();

        VariableDS variableDs = gridDatatype.getVariable();
        double noDataValue = variableDs.getFillValue();

        Grid grid = getGrid(gcs);
        String wkt = getWkt(gcs.getProjection());

        VortexDataType vortexDataType = getVortexDataType(variableDs);

        List<ZonedDateTime[]> times = getTimeBounds(gridDataset, variable);

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
                                    float[] slice = getFloatArray(array);
                                    float[] data = getData(slice, gcs, grid, noDataValue);

                                    ZonedDateTime startTime = times.get(timeIndex)[0];
                                    ZonedDateTime endTime = times.get(timeIndex)[1];
                                    Duration interval = Duration.between(startTime, endTime);

                                    // Grid must be shifted after getData call since getData uses the original locations
                                    // to map values.
                                    shiftGrid(grid);

                                    grids.add(VortexGrid.builder()
                                            .dx(grid.getDx()).dy(grid.getDy())
                                            .nx(grid.getNx()).ny(grid.getNy())
                                            .originX(grid.getOriginX()).originY(grid.getOriginY())
                                            .wkt(wkt)
                                            .data(data)
                                            .noDataValue(noDataValue)
                                            .units(gridDatatype.getUnitsString())
                                            .fileName(gridDataset.getLocation())
                                            .shortName(gridDatatype.getShortName())
                                            .fullName(gridDatatype.getFullName())
                                            .description(gridDatatype.getDescription())
                                            .startTime(startTime)
                                            .endTime(endTime)
                                            .interval(interval)
                                            .dataType(vortexDataType)
                                            .build());
                                } catch (IOException e) {
                                    logger.log(Level.SEVERE, e, e::getMessage);
                                }
                            })));
        } else if (timeDim != null) {
            IntStream.range(0, times.size()).forEach(timeIndex -> {
                try {
                    Array array = gridDatatype.readDataSlice(timeIndex, -1, -1, -1);
                    float[] slice = getFloatArray(array);
                    float[] data = getData(slice, gcs, grid, noDataValue);

                    ZonedDateTime startTime = times.get(timeIndex)[0];
                    ZonedDateTime endTime = times.get(timeIndex)[1];
                    Duration interval = Duration.between(startTime, endTime);

                    // Grid must be shifted after getData call since getData uses the original locations
                    // to map values.
                    shiftGrid(grid);

                    grids.add(VortexGrid.builder()
                            .dx(grid.getDx()).dy(grid.getDy())
                            .nx(grid.getNx()).ny(grid.getNy())
                            .originX(grid.getOriginX()).originY(grid.getOriginY())
                            .wkt(wkt)
                            .data(data)
                            .noDataValue(noDataValue)
                            .units(gridDatatype.getUnitsString())
                            .fileName(gridDataset.getLocation())
                            .shortName(gridDatatype.getShortName())
                            .fullName(gridDatatype.getFullName())
                            .description(gridDatatype.getDescription())
                            .startTime(startTime)
                            .endTime(endTime)
                            .interval(interval)
                            .dataType(vortexDataType)
                            .build());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                }
            });
        } else {
            try {
                Array array = gridDatatype.readDataSlice(1, -1, -1, -1);
                float[] slice = getFloatArray(array);
                float[] data = getData(slice, gcs, grid, noDataValue);

                ZonedDateTime startTime;
                ZonedDateTime endTime;
                Duration interval;
                if (!times.isEmpty()) {
                    startTime = times.get(0)[0];
                    endTime = times.get(0)[1];
                    interval = Duration.between(startTime, endTime);
                } else {
                    startTime = null;
                    endTime = null;
                    interval = null;
                }

                // Grid must be shifted after getData call since getData uses the original locations
                // to map values.
                shiftGrid(grid);

                grids.add(VortexGrid.builder()
                        .dx(grid.getDx()).dy(grid.getDy())
                        .nx(grid.getNx()).ny(grid.getNy())
                        .originX(grid.getOriginX()).originY(grid.getOriginY())
                        .wkt(wkt)
                        .data(data)
                        .noDataValue(noDataValue)
                        .units(gridDatatype.getUnitsString())
                        .fileName(gridDataset.getLocation())
                        .shortName(gridDatatype.getShortName())
                        .fullName(gridDatatype.getFullName())
                        .description(gridDatatype.getDescription())
                        .startTime(startTime)
                        .endTime(endTime)
                        .interval(interval)
                        .dataType(vortexDataType)
                        .build());
            } catch (IOException e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }
        }
        return grids;
    }

    private static String getWkt(Projection projection) {
        return WktFactory.createWkt((ProjectionImpl) projection);
    }

    private List<ZonedDateTime[]> getTimeBounds(GridDataset gridDataset, String variable) {
        GridDatatype gridDatatype = gridDataset.findGridDatatype(variable);
        GridCoordSystem gcs = gridDatatype.getCoordinateSystem();

        List<ZonedDateTime[]> list = new ArrayList<>();
        if (gcs.hasTimeAxis1D()) {
            CoordinateAxis1DTime tAxis = gcs.getTimeAxis1D();

            if (!tAxis.isInterval() && !isSpecialTimeBounds())
                return getTimeInstants(tAxis);

            for (int i = 0; i < tAxis.getSize(); i++) {
                ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
                CalendarDate[] dates = tAxis.getCoordBoundsDate(i);

                String fileName = new File(path).getName().toLowerCase();
                if (fileName.matches(".*gaugecorr.*qpe.*01h.*grib2")
                        || fileName.matches(".*radaronly.*qpe.*01h.*grib2")
                        || fileName.matches(".*multisensor.*qpe.*01h.*grib2")) {
                    zonedDateTimes[0] = convert(dates[0]).minusHours(1);
                } else if (fileName.matches("mrms_preciprate.*")) {
                    zonedDateTimes[0] = convert(dates[0]).minusMinutes(5);
                } else if (fileName.matches("preciprate_.*\\.grib2")) {
                    zonedDateTimes[0] = convert(dates[0]).minusMinutes(2);
                } else if (fileName.matches(".*hhr\\.ms\\.mrg.*hdf.*")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(i));
                } else if (fileName.matches(".*aorc.*apcp.*nc4.*")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(i)).minusHours(1);
                } else if (fileName.matches(".*aorc.*tmp.*nc4.*")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(i));
                } else if (fileName.matches("[0-9]{2}.nc")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(0));
                } else if (fileName.matches("nldas_fora0125_h.a.*") && variableName.equals("APCP")) {
                    zonedDateTimes[0] = convert(dates[0]).minusHours(1);
                } else if (fileName.matches("prec.[0-9]{4}.nc")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(i));
                } else if (fileName.matches("gfs.nc")) {
                    zonedDateTimes[0] = convert(dates[0]).plusMinutes(90);
                } else {
                    zonedDateTimes[0] = convert(dates[0]);
                }

                if (fileName.matches("hrrr.*wrfsfcf.*")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusHours(1);
                } else if (fileName.matches(".*hhr\\.ms\\.mrg.*hdf.*")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusMinutes(30);
                } else if (fileName.matches(".*aorc.*apcp.*nc4.*")) {
                    zonedDateTimes[1] = convert(tAxis.getCalendarDate(i));
                } else if (fileName.matches(".*aorc.*tmp.*nc4.*")) {
                    zonedDateTimes[1] = convert(tAxis.getCalendarDate(i));
                } else if (fileName.matches("[0-9]{2}.nc")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusDays(1);
                } else if (fileName.matches(".*cmorph.*h.*ly.*")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusHours(1);
                } else if (fileName.matches("ge.*\\.pgrb2.*\\.0p.*\\.f.*\\..*")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusHours(3);
                } else if (fileName.matches("prec.[0-9]{4}.nc")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusDays(1);
                } else if (fileName.matches("gfs.nc")) {
                    zonedDateTimes[1] = convert(dates[1]).plusMinutes(90);
                } else {
                    zonedDateTimes[1] = convert(dates[1]);
                }

                list.add(zonedDateTimes);
            }
            return list;
        }

        if (gcs.hasTimeAxis()) {
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

        // No time axes found. Try reading times from attributes.
        Attribute startDateAttribute = gridDatatype.findAttributeIgnoreCase("start_date");
        Attribute endDateAttribute = gridDatatype.findAttributeIgnoreCase("stop_date");
        if (startDateAttribute != null && endDateAttribute != null) {
            String startTimeString = startDateAttribute.getStringValue();
            if (startTimeString == null) return Collections.emptyList();
            ZonedDateTime startTime = ZonedDateTime.parse(startTimeString,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

            String endTimeString = endDateAttribute.getStringValue();
            if (endTimeString == null) return Collections.emptyList();
            ZonedDateTime endTime = ZonedDateTime.parse(endTimeString,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

            ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
            zonedDateTimes[0] = startTime;
            zonedDateTimes[1] = endTime;

            list.add(zonedDateTimes);
            return list;
        }

        Attribute nominalProductTimeAttribute = gridDataset.findGlobalAttributeIgnoreCase("nominal_product_time");
        if (nominalProductTimeAttribute != null) {
            String timeString = nominalProductTimeAttribute.getStringValue();
            if (timeString == null) return Collections.emptyList();

            CalendarDate calendarDate = CalendarDate.parseISOformat(null, timeString);
            LocalDateTime ldt = LocalDateTime.parse(calendarDate.toString(), DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime time = ZonedDateTime.of(ldt, ZoneId.of("UTC"));

            ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
            zonedDateTimes[0] = time;
            zonedDateTimes[1] = time;

            list.add(zonedDateTimes);
            return list;
        }

        return Collections.emptyList();
    }

    private boolean isSpecialTimeBounds() {
        String fileName = new File(path).getName().toLowerCase();

        String[] regexPatterns = {
                ".*gaugecorr.*qpe.*01h.*grib2",
                ".*radaronly.*qpe.*01h.*grib2",
                ".*multisensor.*qpe.*01h.*grib2",
                "mrms_preciprate.*",
                "preciprate_.*\\.grib2",
                ".*hhr\\.ms\\.mrg.*hdf.*",
                ".*aorc.*apcp.*nc4.*",
                ".*aorc.*tmp.*nc4.*",
                "[0-9]{2}.nc",
                "nldas_fora0125_h.a.*",
                "hrrr.*wrfsfcf.*",
                ".*cmorph.*h.*ly.*",
                "ge.*\\.pgrb2.*\\.0p.*\\.f.*\\..*",
                "prec.[0-9]{4}.nc"
        };

        if (fileName.matches("nldas_fora0125_h.a.*") && variableName.equals("APCP")
                || fileName.matches("gfs.nc") && variableName.equals("Precipitation_rate_surface")) {
            return true;
        }

        return Arrays.stream(regexPatterns).anyMatch(fileName::matches);
    }

    private List<ZonedDateTime[]> getTimeInstants(CoordinateAxis1DTime timeAxis) {
        return timeAxis.getCalendarDates().stream()
                .map(TimeConverter::toZonedDateTime)
                .map(t -> new ZonedDateTime[] {t, t})
                .collect(Collectors.toList());
    }

    private static ZonedDateTime convert(CalendarDate date) {
        return ZonedDateTime.parse(date.toString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    private static Grid getGrid(GridCoordSystem coordinateSystem) {
        AtomicReference<Grid> grid = new AtomicReference<>();

        CoordinateAxis xAxis = coordinateSystem.getXHorizAxis();
        CoordinateAxis yAxis = coordinateSystem.getYHorizAxis();

        int nx;
        int ny;

        double[] edgesX;
        double[] edgesY;

        if (xAxis instanceof CoordinateAxis1D
                && yAxis instanceof CoordinateAxis1D) {
            nx = (int) xAxis.getSize();
            ny = (int) yAxis.getSize();
            edgesX = ((CoordinateAxis1D) xAxis).getCoordEdges();
            edgesY = ((CoordinateAxis1D) yAxis).getCoordEdges();
        } else if (xAxis instanceof CoordinateAxis2D xAxis2D
                && yAxis instanceof CoordinateAxis2D yAxis2D) {
            int shapeX = xAxis2D.getEdges().getShape()[1] - 1;
            double minX = xAxis2D.getMinValue();
            double maxX = xAxis2D.getMaxValue();
            double dx = (maxX - minX) / (shapeX - 1);

            int shapeY = xAxis2D.getEdges().getShape()[0] - 1;
            double minY = yAxis2D.getMinValue();
            double maxY = yAxis2D.getMaxValue();
            double dy = (maxY - minY) / (shapeY - 1);

            double cellSize = (dx + dy) / 2;

            nx = (int) Math.round((maxX - minX) / cellSize);
            ny = (int) Math.round((maxY - minY) / cellSize);

            edgesX = new double[nx];
            for (int i = 0; i < nx; i++) {
                edgesX[i] = minX + i * cellSize;
            }

            edgesY = new double[ny];
            for (int i = 0; i < ny; i++) {
                edgesY[i] = minY + i * cellSize;
            }
        } else {
            throw new IllegalStateException();
        }

        double ulx = edgesX[0];
        double urx = edgesX[edgesX.length - 1];
        double dx = (urx - ulx) / nx;

        double uly = edgesY[0];
        double lly = edgesY[edgesY.length - 1];
        double dy = (lly - uly) / ny;

        String wkt = getWkt(coordinateSystem.getProjection());

        grid.set(Grid.builder()
                .nx(nx)
                .ny(ny)
                .dx(dx)
                .dy(dy)
                .originX(ulx)
                .originY(uly)
                .crs(wkt)
                .build());

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
            case "degrees":
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
                csUnits = KILO(METRE);
                break;
            default:
                csUnits = ONE;
        }

        if (cellUnits.isCompatible(csUnits) && !cellUnits.equals(csUnits)) {
            grid.set(scaleGrid(grid.get(), cellUnits, csUnits));
        }

        return grid.get();
    }

    private static void shiftGrid(Grid grid) {
        String crs = grid.getCrs();
        boolean isGeographic = ReferenceUtils.isGeographic(crs);
        if (isGeographic && (grid.getOriginX() > 180 || grid.getTerminusX() > 180))
            grid.shift(-360, 0);
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

    private int getDtoCount(GridDataset dataset, String variable) {
        GridDatatype gridDatatype = dataset.findGridDatatype(variable);
        Dimension timeDim = gridDatatype.getTimeDimension();
        if (timeDim != null)
            return timeDim.getLength();

        return 1;

    }

    private int getDtoCount(VariableDS variableDS) {
        List<Dimension> dimensions = variableDS.getDimensions();
        for (Dimension dimension : dimensions) {
            if (dimension.getShortName().equals("time")) {
                return dimension.getLength();
            }
        }

        return 1;
    }

    private VortexData getData(GridDataset gridDataset, String variable, int idx) {
        GridDatatype gridDatatype = gridDataset.findGridDatatype(variable);
        GridCoordSystem gcs = gridDatatype.getCoordinateSystem();

        VariableDS variableDS = gridDatatype.getVariable();
        double noDataValue = variableDS.getFillValue();

        Grid grid = getGrid(gcs);
        String wkt = getWkt(gcs.getProjection());

        List<ZonedDateTime[]> times = getTimeBounds(gridDataset, variable);

        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        if (!times.isEmpty()) {
            startTime = times.get(idx)[0];
            endTime = times.get(idx)[1];
            interval = Duration.between(startTime, endTime);
        } else {
            startTime = null;
            endTime = null;
            interval = null;
        }

        float[] slice;
        try {
            Array array = gridDatatype.readDataSlice(idx, -1, -1, -1);
            slice = getFloatArray(array);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
            return null;
        }

        float[] data = getData(slice, gcs, grid, noDataValue);

        String units;
        if (variable.toLowerCase().contains("var209-6")) {
            units = "mm";
        } else {
            units = gridDatatype.getUnitsString();
        }

        // Grid must be shifted after getData call since getData uses the original locations
        // to map values.
        shiftGrid(grid);

        VortexDataType vortexDataType = getVortexDataType(variableDS);

        return VortexGrid.builder()
                .dx(grid.getDx())
                .dy(grid.getDy())
                .nx(grid.getNx())
                .ny(grid.getNy())
                .originX(grid.getOriginX())
                .originY(grid.getOriginY())
                .wkt(wkt)
                .data(data)
                .noDataValue(noDataValue)
                .units(units)
                .fileName(gridDataset.getLocation())
                .shortName(gridDatatype.getShortName())
                .fullName(gridDatatype.getFullName())
                .description(gridDatatype.getDescription())
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval)
                .dataType(vortexDataType)
                .build();
    }

    private float[] getData(float[] slice, GridCoordSystem gcs, Grid gridDefinition, double noDataValue) {
        if (gcs.isRegularSpatial())
            return slice;

        IndexSearcher indexSearcher = IndexSearcherFactory.INSTANCE.getOrCreate(gcs);
        indexSearcher.addPropertyChangeListener(support::firePropertyChange);

        Coordinate[] coordinates = gridDefinition.getGridCellCentroidCoords();
        indexSearcher.cacheCoordinates(coordinates);

        float[] data = new float[coordinates.length];
        int count = data.length;
        for (int i = 0; i < count; i++) {
            Coordinate coordinate = coordinates[i];
            int index = indexSearcher.getIndex(coordinate.x, coordinate.y);
            data[i] = index >= 0 ? slice[index] : (float) noDataValue;
        }

        return data;
    }

    private VortexDataType getVortexDataType(VariableDS variableDS) {
        String cellMethods = variableDS.findAttributeString(CF.CELL_METHODS, "");
        return VortexDataType.fromString(cellMethods);
    }
}
