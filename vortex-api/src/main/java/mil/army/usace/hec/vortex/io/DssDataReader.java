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
import mil.army.usace.hec.vortex.VortexTimeRecord;
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

import static hec.heclib.dss.HecDSSDataAttributes.*;

class DssDataReader extends DataReader {
    private final List<DSSPathname> catalogPathnameList;

    DssDataReader(DataReaderBuilder builder) {
        super(builder);
        catalogPathnameList = getCatalogPathnames(path, variableName);
    }

    private static List<DSSPathname> getCatalogPathnames(String path, String variableName) {
        if (!variableName.contains("*")) {
            return List.of(new DSSPathname(variableName));
        }

        HecDSSDataAttributes attributes = new HecDSSDataAttributes();
        attributes.setDSSFileName(path);
        String[] dssPathnames = attributes.getCatalog(true, variableName);
        return Arrays.stream(dssPathnames).map(DSSPathname::new).toList();
    }

    @Override
    public List<VortexData> getDtos() {
        List<VortexData> dtos = new ArrayList<>();
        catalogPathnameList.forEach(path -> {
            GridData gridData = retrieveGriddedData(this.path, path.getPathname());
            if (gridData != null) {
                dtos.add(dssToDto(gridData, path.getPathname()));
            }
        });
        return dtos;
    }

    private static GridData retrieveGriddedData(String dssFileName, String dssPathname) {
        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(dssFileName);
        griddedData.setPathname(dssPathname);
        GridData gridData = new GridData();

        try {
            griddedData.retrieveGriddedData(true, gridData, status);
        } catch (Exception e) {
            return null;
        }

        return gridData;
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

        if (dPart.isEmpty() && gridInfo.getStartTime().equals("31 December 1899, 00:00")) {
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
                .toFormatter();
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
    public VortexData getDto(int idx) {
        String dssPath = catalogPathnameList.get(idx).pathname();
        int[] status = new int[1];
        GridData gridData = GridUtilities.retrieveGridFromDss(this.path, dssPath, status);
        if (gridData != null) {
            return dssToDto(gridData, dssPath);
        }
        return null;
    }

    @Override
    public List<VortexTimeRecord> getTimeRecords() {
        return catalogPathnameList.stream()
                .map(VortexTimeRecord::of)
                .toList();
    }
}
