package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.*;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Resampler {
    private VortexGrid grid;
    private Rectangle2D env;
    private String envWkt;
    private String targetWkt;
    private Double cellSize;

    private Resampler(ResamplerBuilder builder){
        this.grid = builder.grid;
        this.env = builder.env;
        this.envWkt = builder.envWkt;
        this.targetWkt = builder.targetWkt;
        this.cellSize = builder.cellSize;
    }

    public static class ResamplerBuilder{
        private VortexGrid grid;
        private Rectangle2D env;
        private String envWkt;
        private String targetWkt;
        private Double cellSize;

        public ResamplerBuilder grid(VortexGrid grid){
            this.grid = grid;
            return this;
        }

        public ResamplerBuilder envelope(Rectangle2D envelope){
            this.env = envelope;
            return this;
        }

        public ResamplerBuilder envelopeWkt(String wkt){
            this.envWkt = wkt;
            return this;
        }

        public ResamplerBuilder targetWkt(String wkt){
            this.targetWkt = wkt;
            return this;
        }

        public ResamplerBuilder cellSize (Double cellSize){
            this.cellSize = cellSize;
            return this;
        }

        public Resampler build(){
            if(grid == null){
                System.out.println("Resampler requires input grid");
            }
            if(cellSize == null){
                System.out.println("Resampler requires cell size");
            }
            return new Resampler(this);
        }
    }

    public static ResamplerBuilder builder(){return new ResamplerBuilder();}

    public VortexGrid resample(){
        SpatialReference envSrs;
        if (envWkt != null) {
            envSrs = new SpatialReference(envWkt);
            envSrs.MorphFromESRI();
        } else {
            envSrs = new SpatialReference();
        }

        SpatialReference destSrs = new SpatialReference(targetWkt);
        destSrs.MorphFromESRI();

        Dataset dataset = RasterUtils.getRasterFromVortexGrid(grid);
        Dataset resampled = resample(dataset, env, envSrs, destSrs, cellSize);

        double[] geoTransform = resampled.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double originX = geoTransform[0];
        double originY = geoTransform[3];
        int nx = resampled.GetRasterXSize();
        int ny = resampled.GetRasterYSize();
        String wkt = resampled.GetProjection();
        Band band = resampled.GetRasterBand(1);
        float[] data = new float[nx * ny];
        band.ReadRaster(0, 0, nx, ny, gdalconst.GDT_Float32, data);

        dataset.delete();
        resampled.delete();
        band.delete();

        return  VortexGrid.builder()
                .dx(dx).dy(dy).nx(nx).ny(ny)
                .originX(originX).originY(originY)
                .wkt(wkt).data(data).units(grid.units())
                .fileName(grid.fileName()).shortName(grid.shortName())
                .fullName(grid.shortName()).description(grid.description())
                .startTime(grid.startTime()).endTime(grid.endTime()).interval(grid.interval())
                .build();
    }

    private static Dataset resample(Dataset dataset, Rectangle2D env, SpatialReference envSrs, SpatialReference destSrs, double cellSize){

        destSrs.MorphFromESRI();

        Map<String,Double> envelope = new HashMap<>();

        if (env != null) {
            CoordinateTransformation transform = new CoordinateTransformation(envSrs, destSrs);

            double[] lowerLeft = transform.TransformPoint(env.getMinX(), env.getMinY());
            double minX = lowerLeft[0];
            double minY = lowerLeft[1];
            double[] upperRight = transform.TransformPoint(env.getMaxX(), env.getMaxY());
            double maxX = upperRight[0];
            double maxY = upperRight[1];

            envelope.put("maxX", Math.ceil(maxX / cellSize) * cellSize);
            envelope.put("maxY", Math.ceil(maxY / cellSize) * cellSize);
            envelope.put("minX", Math.floor(minX / cellSize) * cellSize);
            envelope.put("minY", Math.floor(minY / cellSize) * cellSize);
        }

        SpatialReference srData = new SpatialReference(dataset.GetProjection());
        srData.MorphFromESRI();
        ArrayList<String> options = new ArrayList<>();
        options.add("-of");
        options.add("MEM");
        options.add("-s_srs");
        options.add(srData.ExportToWkt());
        options.add("-t_srs");
        options.add(destSrs.ExportToWkt());
        if (env != null) {
            options.add("-te");
            options.add(Double.toString(envelope.get("minX")));
            options.add(Double.toString(envelope.get("maxY")));
            options.add(Double.toString(envelope.get("maxX")));
            options.add(Double.toString(envelope.get("minY")));
        }
        options.add("-tr");
        options.add(Double.toString(cellSize));
        options.add(Double.toString(cellSize));
        WarpOptions warpOptions = new WarpOptions(new Vector<>(options));
        Dataset[] datasets = new Dataset[]{dataset};
        Dataset warped = gdal.Warp("warped", datasets, warpOptions);
        warped.FlushCache();

        warpOptions.delete();

        return warped;
    }

}
