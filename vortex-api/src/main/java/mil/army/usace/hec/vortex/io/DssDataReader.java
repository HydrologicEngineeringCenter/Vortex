package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileAccess;
import hec.heclib.dss.HecDssCatalog;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GridUtilities;
import hec.heclib.grid.GriddedData;
import hec.heclib.util.Heclib;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.geo.WktFactory;
import mil.army.usace.hec.vortex.util.MatrixUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

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
                dtos.add(dssToDto(gridData));
            }

        });
        return dtos;
    }

    private VortexGrid dssToDto(GridData gridData){
        GridInfo info = gridData.getGridInfo();
        String wkt;
        if (ReferenceUtils.isShg(info)){
            wkt = WktFactory.shg();
        } else {
            wkt = ReferenceUtils.enhanceWkt(info.getSpatialReferenceSystem());
        }
        float cellSize = info.getCellSize();
        int nx = info.getNumberOfCellsX();
        int ny = info.getNumberOfCellsY();
        double ulx = info.getLowerLeftCellX() * cellSize;
        double lly = info.getLowerLeftCellY() * cellSize;
        int direction = ReferenceUtils.getUlyDirection(wkt, ulx, lly);
        double uly = lly + direction * ny * cellSize;
        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        if (!info.getStartTime().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
            startTime = ZonedDateTime.of(LocalDateTime.parse(info.getStartTime(), formatter), ZoneId.of("UTC"));
            try {
                endTime = ZonedDateTime.of(LocalDateTime.parse(info.getEndTime(), formatter), ZoneId.of("UTC"));
            } catch (DateTimeParseException e) {
                endTime = startTime;
            }
            interval = Duration.between(startTime, endTime);
        } else {
            startTime = null;
            endTime = null;
            interval = null;
        }
        DSSPathname dssPathname = new DSSPathname(variableName);
        String pathName = dssPathname.getPathname();
        String variable = dssPathname.cPart();
        float[] data = MatrixUtils.flipArray(gridData.getData(), nx, ny);

        return  VortexGrid.builder()
                .dx(cellSize)
                .dy(-cellSize)
                .nx(nx).ny(ny)
                .originX(ulx)
                .originY(uly)
                .wkt(wkt)
                .data(data)
                .noDataValue(Heclib.UNDEFINED_FLOAT)
                .units(info.getDataUnits())
                .fileName(path)
                .shortName(variable)
                .description(variable)
                .fullName(pathName)
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval)
                .build();
    }

    public static Set<String> getVariables(String pathToDss){
        HecDSSFileAccess.setDefaultDSSFileName(pathToDss);
        HecDssCatalog catalog = new HecDssCatalog();
        String[] paths = catalog.getCatalog(false, null);
        return new HashSet<>(Arrays.asList(paths));
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
            return dssToDto(gridData);
        }
        return null;
    }
}
