package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.*;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import static org.gdal.gdalconst.gdalconstConstants.GDT_Float32;

public class Resampler {
    private static final Logger logger = Logger.getLogger(Resampler.class.getName());
    private final VortexGrid grid;
    private final Rectangle2D env;
    private final String envWkt;
    private final String targetWkt;
    private final Double cellSize;
    private final String method;

    private Resampler(ResamplerBuilder builder){
        this.grid = builder.grid;
        this.env = builder.env;
        this.envWkt = builder.envWkt;
        this.targetWkt = builder.targetWkt;
        this.cellSize = builder.cellSize;
        this.method = builder.method;
    }

    public static class ResamplerBuilder{
        private VortexGrid grid;
        private Rectangle2D env;
        private String envWkt;
        private String targetWkt;
        private Double cellSize;
        private String method;

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

        public ResamplerBuilder method (String method){
            this.method = method;
            return this;
        }

        public Resampler build(){
            if(grid == null){
                throw new IllegalArgumentException("Resampler requires input grid");
            }
            if(cellSize == null){
                throw new IllegalArgumentException("Resampler requires cell size");
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

        Dataset dataset = RasterUtils.getDatasetFromVortexGrid(grid);

        SpatialReference destSrs = new SpatialReference();
        if (targetWkt != null) {
            destSrs.ImportFromWkt(targetWkt);
            destSrs.MorphFromESRI();
        } else {
            destSrs.ImportFromWkt(dataset.GetProjection());
        }

        Dataset resampled = resample(dataset, env, envSrs, destSrs, cellSize, method);

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
        band.ReadRaster(0, 0, nx, ny, GDT_Float32, data);

        dataset.delete();
        resampled.delete();
        band.delete();

        return VortexGrid.builder()
                .dx(dx)
                .dy(dy)
                .nx(nx)
                .ny(ny)
                .originX(originX)
                .originY(originY)
                .wkt(wkt)
                .data(data)
                .noDataValue(grid.noDataValue())
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

    private static Dataset resample(Dataset dataset, Rectangle2D env, SpatialReference envSrs, SpatialReference targetSrs, double cellSize, String method){

        targetSrs.MorphFromESRI();

        Map<String,Double> envelope = new HashMap<>();

        if (env != null) {
            CoordinateTransformation transform = new CoordinateTransformation(envSrs, targetSrs);

            double[] lowerLeft = transform.TransformPoint(env.getMinX(), env.getMinY());
            double[] upperRight = transform.TransformPoint(env.getMaxX(), env.getMaxY());
            double minX = Math.min(lowerLeft[0], upperRight[0]);
            double minY = Math.min(lowerLeft[1], upperRight[1]);
            double maxX = Math.max(lowerLeft[0], upperRight[0]);
            double maxY = Math.max(lowerLeft[1], upperRight[1]);

            transform.delete();

            if (!Double.isNaN(cellSize)) {
                envelope.put("maxX", Math.ceil(maxX / cellSize) * cellSize);
                envelope.put("maxY", Math.ceil(maxY / cellSize) * cellSize);
                envelope.put("minX", Math.floor(minX / cellSize) * cellSize);
                envelope.put("minY", Math.floor(minY / cellSize) * cellSize);
            } else {
                envelope.put("maxX", maxX);
                envelope.put("maxY", maxY);
                envelope.put("minX", minX);
                envelope.put("minY", minY);
            }
        }

        double noDataValue = getNoDataValue(dataset);

        SpatialReference sourceSrs = new SpatialReference(dataset.GetProjection());

        ArrayList<String> options = new ArrayList<>();
        options.add("-of");
        options.add("MEM");

        try {
            sourceSrs.Validate();
        } catch (RuntimeException e) {
            logger.warning("Invalid Coordinate Referencing System");
        }

        try {
            targetSrs.Validate();
        } catch (RuntimeException e) {
            logger.warning("Invalid Coordinate Referencing System");
        }

        options.add("-s_srs");
        options.add(sourceSrs.ExportToWkt());
        options.add("-t_srs");
        options.add(targetSrs.ExportToWkt());

        options.add("-srcnodata");
        options.add(Double.toString(noDataValue));
        if (env != null) {
            options.add("-te");
            options.add(Double.toString(envelope.get("minX")));
            options.add(Double.toString(envelope.get("minY")));
            options.add(Double.toString(envelope.get("maxX")));
            options.add(Double.toString(envelope.get("maxY")));
        }
        if (!Double.isNaN(cellSize)) {
            options.add("-tr");
            options.add(Double.toString(cellSize));
            options.add(Double.toString(cellSize));
        }
        if (method.equals("Bilinear")) {
            options.add("-r");
            options.add("bilinear");
        }
        if (method.equals("Average")) {
            options.add("-r");
            options.add("average");
        }
        if (method.equals("Nearest Neighbor")){
            options.add("-r");
            options.add("near");
        }
        WarpOptions warpOptions = new WarpOptions(new Vector<>(options));
        Dataset[] datasets = new Dataset[]{dataset};
        Dataset warped = gdal.Warp("warped", datasets, warpOptions);
        warped.FlushCache();

        warpOptions.delete();

        return warped;
    }

    private static double getNoDataValue(Dataset dataset) {
        Band band = dataset.GetRasterBand(1);
        Double[] value = new Double[1];
        band.GetNoDataValue(value);

        band.delete();

        return value[0];
    }

}
