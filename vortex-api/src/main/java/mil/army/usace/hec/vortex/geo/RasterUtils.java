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

        Dataset raster = driver.Create("", nx, ny, 1, gdalconst.GDT_Float32);
        double[] geoTransform = new double[]{x, dx, 0, y, 0, dy};
        raster.SetGeoTransform(geoTransform);
        Band band = raster.GetRasterBand(1);
        float[] data = dto.data();
        band.WriteRaster(0, 0, nx, ny, data);
        band.SetNoDataValue(dto.noDataValue());
        SpatialReference srs = new SpatialReference(dto.wkt());
        srs.MorphFromESRI();
        raster.SetProjection(srs.ExportToWkt());
        band.FlushCache();

        driver.delete();

        return raster;
    }
}
