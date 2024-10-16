package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.WarpOptions;
import org.gdal.gdal.gdal;
import org.gdal.osr.SpatialReference;
import org.locationtech.jts.geom.Envelope;

import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.gdal.gdalconst.gdalconstConstants.GDT_Float32;

public class Resampler {
    private static final Logger logger = Logger.getLogger(Resampler.class.getName());
    private final VortexGrid grid;
    private final Envelope env;
    private final String envWkt;
    private final String targetWkt;
    private final Quantity<Length> cellSize;
    private final ResamplingMethod method;

    private static final Map<EnvelopeReprojection, Envelope> envelopeReprojections = new HashMap<>();

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
        private Envelope env;
        private String envWkt;
        private String targetWkt;
        private Quantity<Length> cellSize;
        private ResamplingMethod method = ResamplingMethod.BILINEAR;

        public ResamplerBuilder grid(VortexGrid grid){
            this.grid = grid;
            return this;
        }

        public ResamplerBuilder envelope(Envelope envelope){
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

        public ResamplerBuilder cellSize (Quantity<Length> cellSize){
            this.cellSize = cellSize;
            return this;
        }

        public ResamplerBuilder method (String method){
            this.method = ResamplingMethod.fromString(method);
            return this;
        }

        public ResamplerBuilder method (ResamplingMethod method){
            this.method = method;
            return this;
        }

        public Resampler build(){
            if(grid == null){
                throw new IllegalArgumentException("Resampler requires input grid");
            }

            if (envWkt == null) {
                // If envelope WKT is not provided, assume it is the same as the grid WKT
                envWkt = grid.wkt();
            }
            return new Resampler(this);
        }
    }

    public static ResamplerBuilder builder(){return new ResamplerBuilder();}

    public VortexGrid resample(){
        SpatialReference envSrs = new SpatialReference();
        if (envWkt != null) {
            envSrs.ImportFromWkt(envWkt);
            envSrs.MorphFromESRI();
        }

        Dataset dataset = RasterUtils.getDatasetFromVortexGrid(grid);

        SpatialReference destSrs = new SpatialReference();
        if (targetWkt != null) {
            destSrs.ImportFromWkt(targetWkt);
            destSrs.MorphFromESRI();
        } else {
            destSrs.ImportFromWkt(dataset.GetProjection());
        }

        double targetCellSize = getTargetCellSize();

        Dataset resampled = resample(dataset, env, envSrs, destSrs, targetCellSize, method);

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
        envSrs.delete();
        destSrs.delete();

        return VortexGrid.toBuilder(grid)
                .dx(dx).dy(dy)
                .nx(nx).ny(ny)
                .originX(originX).originY(originY)
                .wkt(wkt)
                .data(data)
                .build();
    }

    private static Dataset resample(Dataset dataset, Envelope env, SpatialReference envSrs, SpatialReference targetSrs, double cellSize, ResamplingMethod method){

        targetSrs.MorphFromESRI();

        Map<String,Double> envelope = new HashMap<>();

        if (env != null) {
            String envWkt = envSrs.ExportToWkt();
            String toWkt = targetSrs.ExportToWkt();

            EnvelopeReprojection envelopeReprojection = EnvelopeReprojection.of(env, envWkt, toWkt);

            Envelope reprojected = envelopeReprojections.computeIfAbsent(
                    envelopeReprojection, r -> envelopeReprojection.reproject()
            );

            double maxX = reprojected.getMaxX();
            double minX = reprojected.getMinX();
            double maxY = reprojected.getMaxY();
            double minY = reprojected.getMinY();

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

        options.add("-r");
        options.add(method.toString());

        WarpOptions warpOptions = new WarpOptions(new Vector<>(options));
        Dataset[] datasets = new Dataset[]{dataset};
        Dataset warped = gdal.Warp("warped", datasets, warpOptions);
        warped.FlushCache();

        if (envSrs != null)
            envSrs.delete();

        sourceSrs.delete();
        targetSrs.delete();
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

    private double getTargetCellSize() {
        // If no cell size is specified but dx/dy and linear units are the same, preserve cell size
        // If targetWkt is null, the projection of the input dataset is used guaranteeing common linear units
        if (cellSize == null && grid.dx() == grid.dy()
                && (targetWkt == null || ReferenceUtils.getLinearUnits(grid.wkt()).equals(ReferenceUtils.getLinearUnits(targetWkt))))
            return grid.dx();

        if (cellSize == null)
            return Double.NaN;

        Unit<?> targetUnit = ReferenceUtils.getLinearUnits(targetWkt);

        Unit<Length> cellSizeUnit = cellSize.getUnit();

        if (!cellSizeUnit.isCompatible(targetUnit))
            return Double.NaN;

        try {
            UnitConverter converter = cellSizeUnit.getConverterToAny(targetUnit);
            double cellSizeValue = cellSize.getValue().doubleValue();
            return converter.convert(cellSizeValue);
        } catch (IncommensurableException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
            return Double.NaN;
        }
    }
}
