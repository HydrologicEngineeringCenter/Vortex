package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileAccess;
import hec.heclib.dss.HecDssCatalog;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GridUtilities;
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
            GridData gridData = GridUtilities.retrieveGridFromDss(this.path, path, status);
            if (gridData != null) {
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
        ZonedDateTime startTime = ZonedDateTime.of(LocalDateTime.parse(info.getStartTime(), formatter), ZoneId.of("UTC"));
        ZonedDateTime endTime = ZonedDateTime.of(LocalDateTime.parse(info.getEndTime(), formatter), ZoneId.of("UTC"));
        Duration interval = Duration.between(startTime, endTime);
        DSSPathname dssPathname = new DSSPathname(variableName);
        String pathName = dssPathname.getPathname();
        String variable = dssPathname.cPart();
        float[] data = MatrixUtils.flipArray(gridData.getData(), nx, ny);

        return  VortexGrid.builder()
                .dx(cellSize).dy(-cellSize).nx(nx).ny(ny)
                .originX(ulx).originY(uly)
                .wkt(wkt).data(data).units(info.getDataUnits())
                .fileName(path).shortName(variable)
                .description(variable).fullName(pathName)
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval).build();
    }

    public static Set<String> getVariables(String pathToDss){
        HecDSSFileAccess.setDefaultDSSFileName(pathToDss);
        HecDssCatalog catalog = new HecDssCatalog();
        String[] paths = catalog.getCatalog(false, "/*/*/*/*/*/*/");
        Set<String> variables = new HashSet<>();
        Arrays.stream(paths).map(DSSPathname::new).forEach(path -> variables.add(path.pathname()));
        return variables;
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
