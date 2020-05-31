package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.math.Sanitizer;
import org.gdal.gdal.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;

import static org.gdal.gdalconst.gdalconstConstants.GDT_Float32;

public class Transposer {
    private VortexGrid grid;
    private double angle;
    private Double stormCenterX;
    private Double stormCenterY;
    private boolean debug;
    private Path tempDir;

    private Transposer(TransposerBuilder builder){
        this.grid = builder.grid;
        this.angle = builder.angle;
        this.stormCenterX = builder.stormCenterX;
        this.stormCenterY = builder.stormCenterY;
        this.debug = builder.debug;
        this.tempDir = builder.tempDir;
    }

    public static class TransposerBuilder{
        private VortexGrid grid;
        private double angle;
        private Double stormCenterX;
        private Double stormCenterY;
        private boolean debug;
        private Path tempDir;

        public TransposerBuilder grid(VortexGrid grid){
            this.grid = grid;
            return this;
        }

        public TransposerBuilder angle (double angle){
            this.angle = angle;
            return this;
        }

        public TransposerBuilder stormCenterX (Double stormCenterX){
            this.stormCenterX = stormCenterX;
            return this;
        }

        public TransposerBuilder stormCenterY (Double stormCenterY){
            this.stormCenterY = stormCenterY;
            return this;
        }

        public TransposerBuilder debug (boolean debug){
            this.debug = debug;
            return this;
        }

        public TransposerBuilder tempDir(final String tempDir) {
            Path pathToTempDir = Paths.get(tempDir);
            if (pathToTempDir.toFile().exists()) {
                this.tempDir = pathToTempDir;
            }
            return this;
        }

        public Transposer build(){
            if(grid == null){
                throw new IllegalArgumentException("Transposer requires input grid");
            }
            if (tempDir == null) {
                tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            }
            if (Double.isNaN(angle)){
                angle = 0.0;
            }
            return new Transposer(this);
        }
    }

    public static TransposerBuilder builder(){return new TransposerBuilder();}

    public VortexGrid transpose(){
        if (angle == 0 && (stormCenterX == null || stormCenterX.isNaN())
                && (stormCenterY == null || stormCenterY.isNaN())){
            return grid;
        }

        VortexGrid sanatized;
        String desc = grid.description().toLowerCase();
        if (desc.contains("precipitation")
                || desc.contains("precip")
                || desc.contains("precip") && desc.contains("rate")
                || desc.contains("qpe01h")
                || ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation"))
                || ((desc.contains("snow")) && (desc.contains("water")) && (desc.contains("equivalent")))
                || desc.contains("albedo")) {

            sanatized = Sanitizer.builder()
                    .inputGrid(grid)
                    .minimumThreshold(0)
                    .minimumReplacementValue(Float.NaN)
                    .build()
                    .sanitize();
        } else {
            sanatized = grid;
        }

        Dataset datasetIn = RasterUtils.getDatasetFromVortexGrid(sanatized);

        if (debug) {
            ArrayList<String> options = new ArrayList<>();
            options.add("-of");
            options.add("Gtiff");
            TranslateOptions translateOptions1 = new TranslateOptions(new Vector<>(options));
            Path pathToPreTransposed = Paths.get(tempDir.toString(), "pre-transpose.tif");
            Dataset out = gdal.Translate(pathToPreTransposed.toString(), datasetIn, translateOptions1);
            out.FlushCache();
            out.delete();
        }

        double[] geoTransform = datasetIn.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double originX = geoTransform[0];
        double originY = geoTransform[3];
        int nx = datasetIn.GetRasterXSize();
        int ny = datasetIn.GetRasterYSize();
        double terminusX = originX + dx * nx;
        double terminusY = originY + dy * ny;
        double width = Math.abs(terminusX - originX);
        double height = Math.abs(terminusY - originY);
        double centerX = originX + Math.signum(dx) * 0.5 * width;
        double centerY = originY + Math.signum(dy) * 0.5 * height;
        double x1 = originX - centerX;
        double y1 = originY - centerY;

        Band band = datasetIn.GetRasterBand(1);
        float[] data = new float[nx * ny];
        band.ReadRaster(0, 0, nx, ny, GDT_Float32, data);
        band.delete();

        double angleRad = Math.toRadians(angle);

        double adjustedCenterX;
        if (stormCenterX != null && !Double.isNaN(stormCenterX)){
            adjustedCenterX = stormCenterX;
        } else {
            adjustedCenterX = centerX;
        }

        double adjustedCenterY;
        if (stormCenterY != null && !Double.isNaN(stormCenterY)){
            adjustedCenterY = stormCenterY;
        } else {
            adjustedCenterY = centerY;
        }

        double[] transposedGeoTransform = new double[]{
                adjustedCenterX + Math.signum(dx) * (x1 * Math.cos(angleRad) + y1 * Math.sin(angleRad)),
                Math.cos(angleRad) * dx,
                -Math.sin(angleRad) * dx,
                adjustedCenterY + Math.signum(dy) * (x1 * Math.sin(angleRad) - y1 * Math.cos(angleRad)),
                Math.sin(angleRad) * dy,
                Math.cos(angleRad) * dy
        };

        Dataset transposed = gdal.GetDriverByName("MEM").Create("", nx, ny, 1, GDT_Float32);
        transposed.SetGeoTransform(transposedGeoTransform);
        transposed.SetProjection(datasetIn.GetProjection());
        Band transposedBand = transposed.GetRasterBand(1);
        transposedBand.WriteRaster(0, 0, nx, ny, data);
        transposedBand.FlushCache();
        transposed.FlushCache();

        datasetIn.delete();

        if (debug) {
            ArrayList<String> options = new ArrayList<>();
            options.add("-of");
            options.add("Gtiff");
            TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
            Path pathToTransposed = Paths.get(tempDir.toString(), "transposed.tif");
            Dataset out = gdal.Translate(pathToTransposed.toString(), transposed, translateOptions);
            out.FlushCache();
            translateOptions.delete();
            out.delete();
        }

        ArrayList<String> options = new ArrayList<>();
        options.add("-of");
        options.add("MEM");
        options.add("-r");
        options.add("bilinear");
        WarpOptions warpOptions = new WarpOptions(new Vector<>(options));
        Dataset[] datasets = new Dataset[]{transposed};
        Dataset transposedWarped = gdal.Warp(
                "",
                datasets,
                warpOptions
        );
        transposedWarped.FlushCache();
        warpOptions.delete();
        transposed.delete();

        double[] transposedWarpedGeoTransform = transposedWarped.GetGeoTransform();
        double transposedDx = transposedWarpedGeoTransform[1];
        double transposedDy = transposedWarpedGeoTransform[5];
        double transposedOriginX = transposedWarpedGeoTransform[0];
        double transposedOriginY = transposedWarpedGeoTransform[3];
        int transposedNx = transposedWarped.GetRasterXSize();
        int transposedNy = transposedWarped.GetRasterYSize();
        double transposedTerminusX = transposedOriginX + transposedDx * transposedNx;
        double transposedTerminusY = transposedOriginY + transposedDy * transposedNy;

        //Specify resampling parameters
        double cellSize = grid.dx();
        double maxX = Math.ceil(Math.max(transposedOriginX, transposedTerminusX) / cellSize) * cellSize;
        double minX = Math.floor(Math.min(transposedOriginX, transposedTerminusX) / cellSize) * cellSize;
        double maxY = Math.ceil(Math.max(transposedOriginY, transposedTerminusY) / cellSize) * cellSize;
        double minY = Math.floor(Math.min(transposedOriginY, transposedTerminusY) / cellSize) * cellSize;

        options.clear();
        options.add("-of");
        options.add("MEM");
        options.add("-te");
        options.add(Double.toString(minX));
        options.add(Double.toString(minY));
        options.add(Double.toString(maxX));
        options.add(Double.toString(maxY));
        options.add("-tr");
        options.add(Double.toString(cellSize));
        options.add(Double.toString(cellSize));
        options.add("-r");
        options.add("bilinear");
        Dataset transposedResampled = gdal.Warp(
                "",
                new Dataset[]{transposedWarped},
                new WarpOptions(new Vector<>(options))
        );
        transposedResampled.FlushCache();
        transposedWarped.delete();

        double[] resampledGeoTransform = transposedResampled.GetGeoTransform();
        double resampledDx = resampledGeoTransform[1];
        double resampledDy = resampledGeoTransform[5];
        double resampledOriginX = resampledGeoTransform[0];
        double resampledOriginY = resampledGeoTransform[3];
        int resampledNx = transposedResampled.GetRasterXSize();
        int resampledNy = transposedResampled.GetRasterYSize();

        Band resampledBand = transposedResampled.GetRasterBand(1);
        float[] resampledData = new float[resampledNx * resampledNy];
        resampledBand.ReadRaster(0, 0, resampledNx, resampledNy, resampledData);
        transposedResampled.delete();
        resampledBand.delete();

        VortexGrid resampledGrid = VortexGrid.builder()
                .dx(resampledDx).dy(resampledDy)
                .nx(resampledNx).ny(resampledNy)
                .originX(resampledOriginX).originY(resampledOriginY)
                .wkt(grid.wkt()).data(resampledData).units(grid.units())
                .fileName(grid.fileName()).shortName(grid.shortName())
                .fullName(grid.shortName()).description(grid.description())
                .startTime(grid.startTime()).endTime(grid.endTime()).interval(grid.interval())
                .build();

        if (debug) {
            Dataset transposedResampledGrid = RasterUtils.getDatasetFromVortexGrid(resampledGrid);
            options.clear();
            options.add("-of");
            options.add("Gtiff");
            TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
            Path pathToTransposedResampled = Paths.get(tempDir.toString(), "transposed-resampled.tif");
            Dataset out = gdal.Translate(pathToTransposedResampled.toString(), transposedResampledGrid, translateOptions);
            out.FlushCache();
            out.delete();
        }

        return resampledGrid;
    }
}
