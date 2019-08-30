package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.*;
import org.gdal.gdalconst.gdalconst;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import static org.gdal.gdalconst.gdalconstConstants.GDT_Float32;

public class Transposer {
    private VortexGrid grid;
    private double angle;
    private double stormCenterX;
    private double stormCenterY;

    private Transposer(TransposerBuilder builder){
        this.grid = builder.grid;
        this.angle = builder.angle;
        this.stormCenterX = builder.stormCenterX;
        this.stormCenterX = builder.stormCenterY;
    }

    public static class TransposerBuilder{
        private VortexGrid grid;
        private double angle;
        private double stormCenterX;
        private double stormCenterY;

        public TransposerBuilder grid(VortexGrid grid){
            this.grid = grid;
            return this;
        }

        public TransposerBuilder angle (double angle){
            this.angle = angle;
            return this;
        }

        public TransposerBuilder stormCenterX (double stormCenterX){
            this.stormCenterX = stormCenterX;
            return this;
        }

        public TransposerBuilder stormCenterY (double stormCenterY){
            this.stormCenterY = stormCenterY;
            return this;
        }

        public Transposer build(){
            if(grid == null){
                throw new IllegalArgumentException("Transposer requires input grid");
            }
            return new Transposer(this);
        }
    }

    public static TransposerBuilder builder(){return new TransposerBuilder();}

    public VortexGrid transpose(){
        Dataset dataset = RasterUtils.getRasterFromVortexGrid(grid);

        ArrayList<String>  options1 = new ArrayList<>();
        options1.add("-of");
        options1.add("Gtiff");
        TranslateOptions translateOptions1 = new TranslateOptions(new Vector<>(options1));
        Dataset dataset1 = gdal.Translate("C:/Temp/datasetIn.tif", dataset, translateOptions1);
        dataset1.FlushCache();

        double[] geoTransform = dataset.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double originX = geoTransform[0];
        double originY = geoTransform[3];
        int nx = dataset.GetRasterXSize();
        int ny = dataset.GetRasterYSize();
        double terminusX = originX + dx * nx;
        double terminusY = originY + dy * ny;

        Band band = dataset.GetRasterBand(1);
        float[] data = new float[nx * ny];
        band.ReadRaster(0, 0, nx, ny, gdalconst.GDT_Float32, data);

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(originX, originY),
                new Coordinate(terminusX, originY),
                new Coordinate(terminusX, terminusY),
                new Coordinate(originX, terminusY),
                new Coordinate(originX, originY)
        };
        Polygon rectangle = factory.createPolygon(coordinates);

        AffineTransformation transformation = new AffineTransformation();
        transformation.rotate(Math.toRadians(angle));
        Geometry transformed = transformation.transform(rectangle);

        Coordinate[] transformedCoordinates = transformed.getEnvelope().getCoordinates();
        Coordinate upperLeft = transformedCoordinates[0];
        Coordinate upperRight = transformedCoordinates[1];
        Coordinate lowerRight = transformedCoordinates[2];
        Coordinate lowerLeft = transformedCoordinates[3];

        double[] outGeoTransform = new double[]{
                0,
                Math.cos(Math.toRadians(angle)) * dx,
                -Math.sin(Math.toRadians(angle)) * dx,
                0,
                Math.sin(Math.toRadians(angle) * dy),
                Math.cos(Math.toRadians(angle) * dy)
        };

        Dataset transposed = gdal.GetDriverByName("MEM").Create("", nx, ny, 1, GDT_Float32);
        transposed.SetGeoTransform(outGeoTransform);
        transposed.SetProjection(dataset.GetProjection());
        Band outBand = transposed.GetRasterBand(1);
        outBand.WriteRaster(0, 0, nx, ny, data);
        outBand.FlushCache();
        transposed.FlushCache();

        ArrayList<String>  options = new ArrayList<>();
        options.add("-of");
        options.add("Gtiff");
//        options.addAll(Arrays.asList("-gcp", "0", "0", Double.toString(upperLeft.x), Double.toString(upperLeft.y)));
//        options.addAll(Arrays.asList("-gcp", Integer.toString(nx), "0", Double.toString(upperRight.x), Double.toString(upperRight.y)));
//        options.addAll(Arrays.asList("-gcp", Integer.toString(nx), Integer.toString(ny), Double.toString(lowerRight.x), Double.toString(lowerRight.y)));
//        options.addAll(Arrays.asList("-gcp", "0", Integer.toString(ny), Double.toString(lowerLeft.x), Double.toString(lowerLeft.y)));
        TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
        Dataset translated = gdal.Translate("C:/Temp/translated.tif", transposed, translateOptions);
        translated.FlushCache();

        dataset.delete();
        transposed.delete();
        translated.delete();

        return VortexGrid.builder().build();
    }
}
