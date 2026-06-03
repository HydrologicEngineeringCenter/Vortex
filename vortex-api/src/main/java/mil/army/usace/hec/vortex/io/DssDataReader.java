package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.*;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GridUtilities;
import hec.heclib.grid.GriddedData;
import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.geo.WktFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hec.heclib.dss.HecDSSDataAttributes.*;

class DssDataReader extends DataReader {
    private static final Logger logger = Logger.getLogger(DssDataReader.class.getName());

    private final List<DSSPathname> catalogPathnameList;

    DssDataReader(DataReaderBuilder builder) {
        super(builder);
        catalogPathnameList = getCatalogPathnames(path, variableName);
    }

    private static List<DSSPathname> getCatalogPathnames(String path, String variableName) {
        if (!variableName.contains("*")) {
            return List.of(new DSSPathname(variableName));
        }

        HecDssCatalog hecDssCatalog = new HecDssCatalog();
        hecDssCatalog.setDSSFileName(path);
        String[] dssPathnames = hecDssCatalog.getCatalog(true, variableName);
        hecDssCatalog.done();

        return Arrays.stream(dssPathnames).map(DSSPathname::new).toList();
    }

    @Override
    public List<VortexData> getDtos() throws DataReadException {
        List<VortexData> dtos = new ArrayList<>(catalogPathnameList.size());
        for (DSSPathname pathname : catalogPathnameList) {
            GridData gridData;
            try {
                gridData = retrieveGriddedData(this.path, pathname.getPathname());
            } catch (DataReadException e) {
                // Bulk catalog reads silently skip records that don't exist
                // (MISSING_RECORD) or aren't grids (UNSUPPORTED) — matching
                // the pre-DataReadException behavior where null grids were
                // dropped. Only genuine I/O failures escape.
                if (e.getKind() == DataReadException.Kind.IO_ERROR) {
                    throw e;
                }
                logger.log(Level.FINE, e, e::getMessage);
                continue;
            }
            dtos.add(dssToDto(gridData, pathname.getPathname()));
        }
        return dtos;
    }

    private static GridData retrieveGriddedData(String dssFileName, String dssPathname) throws DataReadException {
        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(dssFileName);
        griddedData.setPathname(dssPathname);
        GridData gridData = new GridData();

        try {
            griddedData.retrieveGriddedData(true, gridData, status);
        } catch (Exception e) {
            throw DataReadException.ioError(dssFileName, dssPathname,
                    "Failed to read DSS grid record [" + dssFileName + " : " + dssPathname + "]: " + e.getMessage(),
                    e);
        } finally {
            griddedData.done();
        }

        if (status[0] != 0) {
            throw dssFailure(dssFileName, dssPathname, status[0]);
        }

        return gridData;
    }

    /**
     * Maps a HEC-DSS {@code retrieveGridFromDss} / {@code retrieveGriddedData}
     * status code to a {@link DataReadException} with an appropriate
     * {@link DataReadException.Kind}. Centralizes the only place that needs
     * to know what specific DSS status codes mean.
     *
     * <p>Known mappings (see {@code GridUtilities.retrieveGridFromDss}):
     * <ul>
     *   <li>{@code -1} — set-file or set-pathname failed → {@code IO_ERROR}</li>
     *   <li>{@code -2} — record does not exist → {@code MISSING_RECORD}</li>
     *   <li>{@code -3} — record exists but isn't a grid type → {@code UNSUPPORTED}</li>
     *   <li>{@code 0} with null payload — native success status but no data
     *       returned, almost certainly a JNI-level failure → {@code IO_ERROR}</li>
     *   <li>Other negative — native retrieve failure → {@code IO_ERROR}</li>
     * </ul>
     */
    // Package-private so the classification table can be pinned down by a
    // unit test; the method itself is otherwise an implementation detail.
    static DataReadException dssFailure(String dssFileName, String dssPathname, int statusCode) {
        DataReadException.Kind kind = switch (statusCode) {
            case -2 -> DataReadException.Kind.MISSING_RECORD;
            case -3 -> DataReadException.Kind.UNSUPPORTED;
            default -> DataReadException.Kind.IO_ERROR;
        };
        // Status 0 conventionally means success in HEC-DSS; reaching this
        // method with status 0 means the native call returned no data despite
        // reporting success — surface that explicitly instead of an opaque
        // "status=0".
        String detail = statusCode == 0
                ? "native call returned no data despite status=0 (likely JNI-level failure)"
                : "status=" + statusCode;
        String message = "Failed to read DSS grid record [" + dssFileName + " : " + dssPathname
                + "], " + detail + ", kind=" + kind;
        return DataReadException.of(kind, dssFileName, dssPathname, statusCode, message, null);
    }

    private VortexGrid dssToDto(GridData gridData, String pathname){
        GridInfo gridInfo = gridData.getGridInfo();
        String wkt = WktFactory.fromGridInfo(gridInfo);
        float cellSize = gridInfo.getCellSize();
        int nx = gridInfo.getNumberOfCellsX();
        int ny = gridInfo.getNumberOfCellsY();
        double ulx = gridInfo.getLowerLeftCellX() * cellSize;
        double lly = gridInfo.getLowerLeftCellY() * cellSize;
        int direction = ReferenceUtils.getUlyDirection(wkt, ulx, lly);
        double uly = lly + direction * ny * cellSize;

        DSSPathname dssPathname = new DSSPathname(pathname);
        String pathName = dssPathname.getPathname();
        String variable = dssPathname.cPart();

        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;

        String dPart = dssPathname.getDPart();

        if (dPart.isEmpty() && (gridInfo.getStartTime().isBlank() || gridInfo.getStartTime().equals("31 December 1899, 00:00"))) {
            startTime = null;
            endTime = null;
            interval = null;
        } else {
            DateTimeFormatter formatter = getDateTimeFormatter();

            LocalDateTime ldtStart = LocalDateTime.parse(dPart, formatter);
            startTime = ZonedDateTime.of(ldtStart, ZoneId.of("UTC"));

            String ePart = dssPathname.getEPart();
            endTime = getEndTime(ePart);
            if (endTime == null)
                endTime = startTime;

            interval = Duration.between(startTime, endTime);
        }

        float[] data = RasterUtils.flipVertically(gridData.getData(), nx);

        String dssTypeString = DssDataType.fromInt(gridInfo.getDataType()).toString();
        VortexDataType dataType = VortexDataType.fromString(dssTypeString);

        return  VortexGrid.builder()
                .dx(cellSize)
                .dy(-cellSize)
                .nx(nx).ny(ny)
                .originX(ulx)
                .originY(uly)
                .wkt(wkt)
                .data(data)
                .noDataValue(Heclib.UNDEFINED_FLOAT)
                .units(gridInfo.getDataUnits())
                .fileName(path)
                .shortName(variable)
                .description(variable)
                .fullName(pathName)
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval)
                .dataType(dataType)
                .build();
    }

    private ZonedDateTime getEndTime(String ePart) {
        if (ePart.isBlank())
            return null;

        DateTimeFormatter formatter = getDateTimeFormatter();

        try {
            LocalDateTime ldtEnd;
            if (ePart.endsWith(":2400")) {
                String ePart0000 = ePart.replaceAll(":2400", ":0000");
                ldtEnd = LocalDateTime.parse(ePart0000, formatter);
                return ZonedDateTime.of(ldtEnd.plusDays(1), ZoneId.of("UTC"));
            } else {
                ldtEnd = LocalDateTime.parse(ePart, formatter);
                return ZonedDateTime.of(ldtEnd, ZoneId.of("UTC"));
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private DateTimeFormatter getDateTimeFormatter() {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("ddMMMuuuu:HHmm")
                .toFormatter(Locale.ENGLISH);
    }

    public static Set<String> getVariables(String pathToDss){
        HecDSSDataAttributes attributes = new HecDSSDataAttributes();
        attributes.setDSSFileName(pathToDss);
        String[] dssPathnames = attributes.getCatalog(false, null);

        Set<Integer> gridRecordTypes = getGridRecordTypes();

        List<String> griddedRecords = new ArrayList<>();
        for(String dssPathname : dssPathnames) {
            int recordType = attributes.recordType(dssPathname);
            if (gridRecordTypes.contains(recordType))
                griddedRecords.add(dssPathname);
        }

        attributes.done();

        return new HashSet<>(griddedRecords);
    }

    private static Set<Integer> getGridRecordTypes() {
        Set<Integer> gridRecordTypes = new HashSet<>();
        gridRecordTypes.add(UNDEFINED_GRID_WITH_TIME);
        gridRecordTypes.add(UNDEFINED_GRID);
        gridRecordTypes.add(HRAP_GRID_WITH_TIME);
        gridRecordTypes.add(HRAP_GRID);
        gridRecordTypes.add(ALBERS_GRID_WITH_TIME);
        gridRecordTypes.add(ALBERS_GRID);
        gridRecordTypes.add(SPECIFIED_GRID_WITH_TIME);
        gridRecordTypes.add(SPECIFIED_GRID);
        return gridRecordTypes;
    }

    @Override
    public int getDtoCount() {
        return catalogPathnameList.size();
    }

    @Override
    public VortexData getDto(int idx) throws DataReadException {
        Objects.checkIndex(idx, catalogPathnameList.size());

        String dssPath = catalogPathnameList.get(idx).pathname();
        int[] status = new int[1];
        GridData gridData;
        try {
            gridData = GridUtilities.retrieveGridFromDss(this.path, dssPath, status);
        } catch (Exception e) {
            throw DataReadException.ioError(this.path, dssPath,
                    "Failed to read DSS grid record [" + this.path + " : " + dssPath + "]: " + e.getMessage(),
                    e);
        }
        if (gridData == null) {
            throw dssFailure(this.path, dssPath, status[0]);
        }
        return dssToDto(gridData, dssPath);
    }

    @Override
    public List<VortexDataInterval> getDataIntervals() {
        return catalogPathnameList.stream()
                .map(DSSPathname::toString)
                .map(VortexDataInterval::of)
                .toList();
    }

    @Override
    public Validation isValid() {
        return Validation.of(true);
    }

    @Override
    public void close() throws Exception {
        // No op - done() is called on accessors after an opening call is made
    }
}
