package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;

import java.nio.file.Path;

public class RasterUtils {
    static {
        GdalRegister.getInstance();
    }

    private RasterUtils(){}

    public static String getWkt(Path path){
        Dataset raster = gdal.Open(path.toString());
        String wkt = raster.GetProjection();
        raster.delete();
        return wkt;
    }

    public static Dataset getDatasetFromVortexGrid(VortexGrid dto){
        Driver driver = gdal.GetDriverByName("MEM");
        double dx = dto.dx();
        double dy = dto.dy();
        int nx = dto.nx();
        int ny = dto.ny();
        double x = dto.originX();
        double y = dto.originY();

        Dataset dataset = driver.Create("", nx, ny, 1, gdalconst.GDT_Float32);
        double[] geoTransform = new double[]{x, dx, 0, y, 0, dy};
        dataset.SetGeoTransform(geoTransform);
        Band band = dataset.GetRasterBand(1);
        float[] data = dto.data();
        band.WriteRaster(0, 0, nx, ny, data);
        band.SetNoDataValue(dto.noDataValue());

        SpatialReference srs = new SpatialReference();
        srs.ImportFromWkt(dto.wkt());
        srs.MorphFromESRI();
        dataset.SetProjection(srs.ExportToWkt());
        srs.delete();

        band.FlushCache();

        return dataset;
    }

    public static VortexGrid resetNoDataValue(VortexGrid grid, double noDataValue) {
        float[] data = grid.data();
        double noDataValueIn = grid.noDataValue();
        for (int i = 0; i < data.length; i++) {
            if (Double.compare(data[i], noDataValueIn) == 0) {
                data[i] = (float) noDataValue;
            }
        }

        return VortexGrid.builder()
                .dx(grid.dx())
                .dy(grid.dy())
                .nx(grid.nx())
                .ny(grid.ny())
                .originX(grid.originX())
                .originY(grid.originY())
                .wkt(grid.wkt())
                .data(data)
                .noDataValue(noDataValue)
                .units(grid.units())
                .fileName(grid.fileName())
                .shortName(grid.shortName())
                .fullName(grid.fullName())
                .description(grid.description())
                .startTime(grid.startTime())
                .endTime(grid.endTime())
                .interval(grid.interval())
                .build();
    }
}
