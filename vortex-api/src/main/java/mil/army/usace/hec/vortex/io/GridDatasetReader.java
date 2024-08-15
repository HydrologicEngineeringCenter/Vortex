package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.temporal.VortexTimeRecord;
import mil.army.usace.hec.vortex.geo.*;
import mil.army.usace.hec.vortex.util.TimeConverter;
import mil.army.usace.hec.vortex.util.UnitUtil;
import org.locationtech.jts.geom.Coordinate;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;

import javax.measure.Unit;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static mil.army.usace.hec.vortex.io.GridDatasetReader.SpecialFileType.*;
import static tech.units.indriya.unit.Units.METRE;

public class GridDatasetReader extends NetcdfDataReader {
    private static final Logger logger = Logger.getLogger(GridDatasetReader.class.getName());

    private final GridDataset gridDataset;
    private final SpecialFileType specialFileType;
    private final GridDatatype gridDatatype;
    private final GridCoordSystem gridCoordSystem;
    private final VariableDS variableDS;
    private final Grid gridDefinition;
    private final List<VortexTimeRecord> timeBounds;

    enum SpecialFileType {
        ABRSC_GAUGE(".*gaugecorr.*qpe.*01h.*grib2", ".*"),
        ABRSC_RADAR(".*radaronly.*qpe.*01h.*grib2", ".*"),
        MRMS_IDP(".*multisensor.*qpe.*01h.*grib2", ".*"),
        MRMS_PRECIP("mrms_preciprate.*", ".*"),
        MRMS_PRECIP_2_MIN("preciprate_.*\\.grib2", ".*"),
        GPM(".*hhr\\.ms\\.mrg.*hdf.*", ".*"),
        AORC_APCP(".*aorc.*apcp.*nc4.*", ".*"),
        AORC_TMP(".*aorc.*tmp.*nc4.*", ".*"),
        UA_SWE("[0-9]{2}.nc", ".*"),
        NLDAS_APCP("nldas_fora0125_h.a.*", "APCP"),
        CMORPH(".*cmorph.*h.*ly.*", ".*"),
        GEFS("ge.*\\.pgrb2.*\\.0p.*\\.f.*\\..*", ".*"),
        LIVNEH_PRECIP("prec.\\d{4}.nc", ".*"),
        HRRR_WRFSFCF("hrrr.*wrfsfcf.*", ".*"),
        GFS("gfs.nc", "Precipitation_rate_surface"),
        UNDEFINED("", ".*");

        private final Pattern filenamePattern;
        private final Pattern variableNamePattern;

        SpecialFileType(String filenameRegex, String variableRegex) {
            this.filenamePattern = Pattern.compile(filenameRegex);
            this.variableNamePattern = Pattern.compile(variableRegex);
        }

        public boolean matches(String fileName, String variableName) {
            return filenamePattern.matcher(fileName).matches() && variableNamePattern.matcher(variableName).matches();
        }
    }

    /* Constructor */
    public GridDatasetReader(GridDataset gridDataset, String variableName) {
        super(new DataReaderBuilder().path(gridDataset.getLocation()).variable(variableName));
        this.gridDataset = gridDataset;
        specialFileType = determineSpecialFileType(gridDataset.getLocation());

        gridDatatype = gridDataset.findGridDatatype(variableName);
        gridCoordSystem = gridDatatype.getCoordinateSystem();
        variableDS = gridDatatype.getVariable();

        gridDefinition = getGridDefinition(gridCoordSystem);
        timeBounds = getTimeBounds(gridCoordSystem);
    }

    private SpecialFileType determineSpecialFileType(String pathToFile) {
        String filename = Path.of(pathToFile).getFileName().toString().toLowerCase();
        return Arrays.stream(values())
                .filter(t -> t.matches(filename, variableName))
                .findFirst()
                .orElse(UNDEFINED);
    }

    /* Public */
    @Override
    public List<VortexData> getDtos() {
        Dimension timeDim = gridDatatype.getTimeDimension();
        Dimension endDim = gridDatatype.getEnsembleDimension();
        Dimension rtDim = gridDatatype.getRunTimeDimension();

        if (timeDim != null && endDim != null && rtDim != null) {
            return getMultiDimensionGrids(timeDim, endDim, rtDim);
        } else if (timeDim != null) {
            return getTemporalGrids();
        } else {
            return getStaticGrid();
        }
    }

    @Override
    public VortexData getDto(int timeIndex) {
        return createGridFromDataSlice(0, 0, timeIndex);
    }

    @Override
    public int getDtoCount() {
        Dimension timeDim = gridDatatype.getTimeDimension();
        return timeDim != null ? timeDim.getLength() : 1;
    }

    @Override
    public List<VortexTimeRecord> getTimeRecords() {
        return timeBounds;
    }

    /* Grid Data Helpers */
    private List<VortexData> getMultiDimensionGrids(Dimension timeDim, Dimension endDim, Dimension rtDim) {
        List<VortexData> gridList = new ArrayList<>();

        for (int rtIndex = 0; rtIndex < rtDim.getLength(); rtIndex++) {
            for (int endIndex = 0; endIndex < endDim.getLength(); endIndex ++) {
                for (int timeIndex = 0; timeIndex < timeDim.getLength(); timeIndex++) {
                    VortexGrid vortexGrid = createGridFromDataSlice(rtIndex, endIndex, timeIndex);
                    // Note: still add to list even if vortexGrid is null
                    gridList.add(vortexGrid);
                }
            }
        }

        return gridList;
    }

    private List<VortexData> getTemporalGrids() {
        List<VortexData> gridList = new ArrayList<>();
        for (int i = 0; i < timeBounds.size(); i++) {
            VortexData vortexGrid = getDto(i);
            gridList.add(vortexGrid);
        }
        return gridList;
    }

    private List<VortexData> getStaticGrid() {
        VortexData staticGrid = createGridFromDataSlice(0, 0, 0);
        return staticGrid != null ? List.of(staticGrid) : Collections.emptyList();
    }

    /**
     * Creates a VortexGrid object by reading a specific data slice and assembling it into a grid.
     * This method retrieves a slice of data based on specified indices for runtime, ensemble, and time dimensions,
     * converts the raw data into a float array, and then builds a grid using the processed data and associated time record.
     * @param rtIndex the runtime dimension index. If < 0, all runtimes are included.
     * @param endIndex the ensemble dimension index. If < 0, all ensembles are included.
     * @param timeIndex the time dimension index. Specifies a particular time slice to include.
     * @return a VortexGrid object representing the specified data slice, or null if an IOException occurs.
     */
    private VortexGrid createGridFromDataSlice(int rtIndex, int endIndex, int timeIndex) {
        try {
            Array array = gridDatatype.readDataSlice(rtIndex, endIndex, timeIndex, -1, -1, -1);
            float[] slice = getFloatArray(array);
            float[] data = getGridData(slice);
            VortexTimeRecord timeRecord = (timeIndex < timeBounds.size()) ? timeBounds.get(timeIndex) : VortexTimeRecord.UNDEFINED;
            return buildGrid(data, timeRecord);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    private float[] getFloatArray(Array array) {
        // Check commit [939d691a] for details
        return (float[]) array.get1DJavaArray(DataType.FLOAT);
    }

    private float[] getGridData(float[] slice) {
        if (gridCoordSystem.isRegularSpatial()) {
            return slice;
        }

        IndexSearcher indexSearcher = IndexSearcherFactory.INSTANCE.getOrCreate(gridCoordSystem);
        indexSearcher.addPropertyChangeListener(support::firePropertyChange);

        Coordinate[] coordinates = gridDefinition.getGridCellCentroidCoords();
        indexSearcher.cacheCoordinates(coordinates);

        float[] data = new float[coordinates.length];
        for (int i = 0; i < data.length; i++) {
            Coordinate coordinate = coordinates[i];
            int index = indexSearcher.getIndex(coordinate.x, coordinate.y);
            data[i] = index >= 0 ? slice[index] : (float) variableDS.getFillValue();
        }

        return data;
    }

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
                .units(getUnits(variableDS))
                .fileName(path)
                .shortName(gridDatatype.getShortName())
                .fullName(gridDatatype.getFullName())
                .description(gridDatatype.getDescription())
                .startTime(timeRecord.startTime())
                .endTime(timeRecord.endTime())
                .interval(timeRecord.getRecordDuration())
                .dataType(getVortexDataType(variableDS))
                .build();
    }

    /* Grid Definition Helpers */
    private static Grid getGridDefinition(GridCoordSystem coordinateSystem) {
        AtomicReference<Grid> grid = new AtomicReference<>();

        CoordinateAxis xAxis = coordinateSystem.getXHorizAxis();
        CoordinateAxis yAxis = coordinateSystem.getYHorizAxis();

        int nx;
        int ny;

        double[] edgesX;
        double[] edgesY;

        if (xAxis instanceof CoordinateAxis1D xCoord && yAxis instanceof CoordinateAxis1D yCoord) {
            nx = (int) xAxis.getSize();
            ny = (int) yAxis.getSize();
            edgesX = xCoord.getCoordEdges();
            edgesY = yCoord.getCoordEdges();
        } else if (xAxis instanceof CoordinateAxis2D xAxis2D && yAxis instanceof CoordinateAxis2D yAxis2D) {
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

        String wkt = WktFactory.createWkt(coordinateSystem.getProjection());

        grid.set(Grid.builder()
                .nx(nx).ny(ny)
                .dx(dx).dy(dy)
                .originX(ulx).originY(uly)
                .crs(wkt)
                .build());

        String xAxisUnits = Objects.requireNonNull(xAxis).getUnitsString();
        Unit<?> cellUnits = UnitUtil.getUnits(xAxisUnits.toLowerCase());
        Unit<?> csUnits = ReferenceUtils.getLinearUnits(wkt);

        // This will scale the grid if cellUnits and csUnits do not align
        // e.g. cellUnits are in meters but csUnits are in kilometers
        // isCompatible is simply checking if the units are of type length so that no scaling is attempted
        // between DEGREE_ANGLE and ONE
        if (cellUnits.isCompatible(METRE) && csUnits.isCompatible(METRE) && !cellUnits.equals(csUnits)) {
            grid.set(scaleGrid(grid.get(), cellUnits, csUnits));
        }

        return grid.get();
    }

    private String getUnits(VariableDS variableDS) {
        // Add handling logic for MRMS IDP version 12.0.0
        if (variableName.toLowerCase().contains("var209-6")) {
            return "mm";
        }

        return variableDS.getUnitsString();
    }

    /* Time Record Helpers */
    private List<VortexTimeRecord> getTimeBounds(GridCoordSystem gcs) {
        if (gcs.hasTimeAxis1D()) {
            return getTimeRecordsWithTimeAxis1D(gcs.getTimeAxis1D());
        } else if (gcs.hasTimeAxis()) {
            return getTimeRecords();
        } else {
            VortexTimeRecord timeRecord = getTimeRecordFromAttributes();
            return timeRecord != null ? List.of(timeRecord) : Collections.emptyList();
        }
    }

    private VortexTimeRecord getTimeRecordFromAttributes() {
        // No time axes found. Try reading times from attributes.
        VortexTimeRecord startStopDateRecord = getTimeRecordFromStartStopDate();
        return startStopDateRecord != null ? startStopDateRecord : getTimeRecordFromNominalProductTime();
    }

    private VortexTimeRecord getTimeRecordFromStartStopDate() {
        Attribute startDateAttribute = gridDatatype.findAttributeIgnoreCase("start_date");
        Attribute endDateAttribute = gridDatatype.findAttributeIgnoreCase("stop_date");

        String startDateString = getAttributeStringValue(startDateAttribute);
        String endDateString = getAttributeStringValue(endDateAttribute);
        if (startDateString == null || endDateString == null) return null;

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        ZonedDateTime startDate = ZonedDateTime.parse(startDateString, dateTimeFormatter);
        ZonedDateTime endDate = ZonedDateTime.parse(endDateString, dateTimeFormatter);
        return VortexTimeRecord.of(startDate, endDate);
    }

    private VortexTimeRecord getTimeRecordFromNominalProductTime() {
        Attribute nominalProductTimeAttribute = gridDataset.findGlobalAttributeIgnoreCase("nominal_product_time");
        String timeString = getAttributeStringValue(nominalProductTimeAttribute);
        if (timeString == null) return null;

        CalendarDate calendarDate = CalendarDate.parseISOformat(null, timeString);
        LocalDateTime ldt = LocalDateTime.parse(calendarDate.toString(), DateTimeFormatter.ISO_DATE_TIME);
        ZonedDateTime time = ZonedDateTime.of(ldt, ZoneId.of("UTC"));
        return VortexTimeRecord.of(time, time);
    }

    private String getAttributeStringValue(Attribute attribute) {
        return Optional.ofNullable(attribute).map(Attribute::getStringValue).orElse(null);
    }

    private List<VortexTimeRecord> getTimeRecordsWithTimeAxis1D(CoordinateAxis1DTime tAxis) {
        List<VortexTimeRecord> list = new ArrayList<>();
        if (!tAxis.isInterval() && !isSpecialTimeBounds()) {
            return getTimeInstants(tAxis);
        }

        for (int i = 0; i < (int) tAxis.getSize(); i++) {
            VortexTimeRecord timeRecord = adjustTimeForSpecialFile(tAxis, i);
            list.add(timeRecord);
        }

        return list;
    }

    private VortexTimeRecord adjustTimeForSpecialFile(CoordinateAxis1DTime tAxis, int time) {
        CalendarDate[] dates = tAxis.getCoordBoundsDate(time);
        ZonedDateTime startTime = TimeConverter.toZonedDateTime(dates[0]);
        ZonedDateTime endTime = TimeConverter.toZonedDateTime(dates[1]);
        ZonedDateTime instantTime = TimeConverter.toZonedDateTime(tAxis.getCalendarDate(time));
        ZonedDateTime initialTime = TimeConverter.toZonedDateTime(tAxis.getCalendarDate(0));

        ZonedDateTime adjustedStart = switch (specialFileType) {
            case ABRSC_GAUGE, ABRSC_RADAR, MRMS_IDP, NLDAS_APCP -> startTime.minusHours(1);
            case MRMS_PRECIP -> startTime.minusMinutes(5);
            case MRMS_PRECIP_2_MIN -> startTime.minusMinutes(2);
            case GPM, AORC_TMP, LIVNEH_PRECIP -> instantTime;
            case AORC_APCP -> instantTime.minusHours(1);
            case UA_SWE -> initialTime;
            case GFS -> startTime.plusMinutes(90);
            default -> startTime;
        };

        ZonedDateTime adjustedEnd = switch (specialFileType) {
            case HRRR_WRFSFCF, CMORPH -> adjustedStart.plusHours(1);
            case GPM -> adjustedStart.plusMinutes(30);
            case AORC_APCP, AORC_TMP -> instantTime;
            case UA_SWE, LIVNEH_PRECIP -> adjustedStart.plusDays(1);
            case GEFS -> adjustedStart.plusHours(3);
            case GFS -> endTime.plusMinutes(90);
            default -> endTime;
        };

        return VortexTimeRecord.of(adjustedStart, adjustedEnd);
    }

    private boolean isSpecialTimeBounds() {
        return specialFileType != null && specialFileType != UNDEFINED;
    }

    private List<VortexTimeRecord> getTimeInstants(CoordinateAxis1DTime timeAxis) {
        return timeAxis.getCalendarDates().stream()
                .map(TimeConverter::toZonedDateTime)
                .map(t -> VortexTimeRecord.of(t, t))
                .toList();
    }
}
