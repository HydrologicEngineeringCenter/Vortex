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

import static hec.heclib.dss.HecDSSDataAttributes.*;

class DssDataReader extends DataReader {

    DssDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {
        HecDSSFileAccess.setDefaultDSSFileName(path);
        String[] paths;
        if (variableName.contains("*")) {
            HecDssCatalog catalog = new HecDssCatalog();
            paths = catalog.getCatalog(true, variableName);
        } else {
            paths = new String[1];
            paths[0] = variableName;
        }
        List<VortexData> dtos = new ArrayList<>();
        Arrays.stream(paths).forEach(path -> {
            int[] status = new int[1];
            GriddedData griddedData = new GriddedData();
            griddedData.setDSSFileName(this.path);
            griddedData.setPathname(path);
            GridData gridData = new GridData();
            griddedData.retrieveGriddedData(true, gridData, status);
            if (status[0] == 0) {
                dtos.add(dssToDto(gridData, path));
            }

        });
        return dtos;
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
        HecDSSFileAccess.setDefaultDSSFileName(path);
        String[] paths;
        if (variableName.contains("*")) {
            HecDssCatalog catalog = new HecDssCatalog();
            paths = catalog.getCatalog(true, variableName);
        } else {
            paths = new String[1];
            paths[0] = variableName;
        }
        return paths.length;
    }

    @Override
    public VortexData getDto(int idx) {
        HecDSSFileAccess.setDefaultDSSFileName(path);
        String[] paths;
        if (variableName.contains("*")) {
            HecDssCatalog catalog = new HecDssCatalog();
            paths = catalog.getCatalog(true, variableName);
        } else {
            paths = new String[1];
            paths[0] = variableName;
        }
        String dssPath = paths[idx];
        int[] status = new int[1];
        GridData gridData = GridUtilities.retrieveGridFromDss(this.path, dssPath, status);
        if (gridData != null) {
            return dssToDto(gridData, dssPath);
        }
        return null;
    }
}
