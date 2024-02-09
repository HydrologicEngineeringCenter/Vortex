package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.*;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static hec.heclib.dss.HecDSSDataAttributes.*;

class DssDataReader extends DataReader {
    private static final Logger logger = Logger.getLogger(DssDataReader.class.getName());
    private List<String> catalogPathnameList = null;

    DssDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {
        return IntStream.range(0, getDtoCount())
                .mapToObj(this::getDto)
                .toList();
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

        if (dssPathname.getDPart().isEmpty() && gridInfo.getStartTime().equals("31 December 1899, 00:00")) {
            startTime = null;
            endTime = null;
            interval = null;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
            startTime = ZonedDateTime.of(LocalDateTime.parse(gridInfo.getStartTime(), formatter), ZoneId.of("UTC"));
            try {
                endTime = ZonedDateTime.of(LocalDateTime.parse(gridInfo.getEndTime(), formatter), ZoneId.of("UTC"));
                if (endTime.isEqual(ZonedDateTime.parse("1899-12-31T00:00Z[UTC]")))
                    endTime = startTime;
            } catch (DateTimeParseException e) {
                endTime = startTime;
            }
            interval = Duration.between(startTime, endTime);
        }

        VortexTimeRecord timeRecord = VortexTimeRecord.of(dssPathname);
        if (timeRecord != null) {
            startTime = timeRecord.startTime();
            endTime = timeRecord.endTime();
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
        return getRecordPathnameList().size();
    }

    @Override
    public VortexData getDto(int idx) {
        String pathname = getRecordPathnameList().get(idx);
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(path);
        griddedData.setPathname(pathname);

        GridData gridData = new GridData();
        int[] status = new int[1];
        griddedData.retrieveGriddedData(true, gridData, status);

        if (status[0] == 0) {
            return dssToDto(gridData, pathname);
        } else {
            logger.warning("Failed to retrieve gridded data");
            return null;
        }
    }

    @Override
    public List<VortexTimeRecord> getTimeRecords() {
        return getRecordPathnameList().stream()
                .map(DSSPathname::new)
                .map(VortexTimeRecord::of)
                .toList();
    }

    private List<String> getRecordPathnameList() {
        if (catalogPathnameList == null) {
            HecDataManager dataManager = new HecDataManager(path);
            String pathnameFilter = getPathnameFilter();
            String[] paths = dataManager.getCatalog(true, pathnameFilter);
            dataManager.done();

            catalogPathnameList = Arrays.stream(paths)
                    .sorted(this::comparePathname)
                    .toList();
        }

        return catalogPathnameList;
    }

    private String getPathnameFilter() {
        if (variableName.equals("*")) return variableName;
        DSSPathname dssPathname = new DSSPathname(variableName);
        // Getting records for all times
        dssPathname.setDPart("*");
        dssPathname.setEPart("*");
        return dssPathname.getPathname();
    }

    private int comparePathname(String pathname1, String pathname2) {
        VortexTimeRecord record1 = VortexTimeRecord.of(new DSSPathname(pathname1));
        VortexTimeRecord record2 = VortexTimeRecord.of(new DSSPathname(pathname2));

        if (record1 == null || record2 == null) return 0;

        long startTime1 = record1.startTime().toEpochSecond();
        long startTime2 = record2.startTime().toEpochSecond();

        long interval1 = record1.endTime().toEpochSecond() - startTime1;
        long interval2 = record2.endTime().toEpochSecond() - startTime2;

        int startTimeComparison = Long.compare(startTime1, startTime2);

        return (startTimeComparison != 0) ? startTimeComparison : Long.compare(interval1, interval2);
    }
}
