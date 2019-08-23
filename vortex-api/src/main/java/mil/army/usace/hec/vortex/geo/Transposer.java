package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.Vector;

public class Transposer {
    private VortexGrid grid;
    private double angle;
    private double x;
    private double y;

    private Transposer(TransposerBuilder builder){
        this.grid = builder.grid;
        this.angle = builder.angle;
        this.x = builder.x;
        this.y = builder.y;
    }

    public static class TransposerBuilder{
        private VortexGrid grid;
        private double angle;
        private double x;
        private double y;

        public TransposerBuilder grid(VortexGrid grid){
            this.grid = grid;
            return this;
        }

        public TransposerBuilder angle (double angle){
            this.angle = angle;
            return this;
        }

        public TransposerBuilder x (double x){
            this.x = x;
            return this;
        }

        public TransposerBuilder y (double y){
            this.y = y;
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
        Dataset dataset1 = gdal.Translate("C:/Temp/datasetIn.tiff", dataset, translateOptions1);
        dataset1.FlushCache();

        double[] geoTransform = dataset.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double originX = geoTransform[0];
        double originY = geoTransform[3];
        int nx = dataset.GetRasterXSize();
        int ny = dataset.GetRasterYSize();

        double height = dy * ny;
        double width = dx * nx;

        double originToCenter = Math.sqrt(Math.pow(0.5*height, 2) + Math.pow(0.5*width, 2));

        double angleRadians = Math.toRadians(angle);
        double newOriginX = originX + originToCenter * Math.sin(angleRadians);
        double newOriginY = originY + originToCenter * Math.cos(angleRadians);

        double[] geoTransformRotated = new double[]{
                newOriginX,
                Math.cos(angleRadians) * dx,
                -Math.sin(angleRadians) * dx,
                newOriginY,
                Math.sin(angleRadians) * dy,
                Math.cos(angleRadians) * dy
        };
        dataset.SetGeoTransform(geoTransformRotated);

        ArrayList<String>  options = new ArrayList<>();
        options.add("-of");
        options.add("Gtiff");
        TranslateOptions translateOptions = new TranslateOptions(new Vector<>(options));
        Dataset translated = gdal.Translate("C:/Temp/translated.tiff", dataset, translateOptions);
        translated.FlushCache();

        return VortexGrid.builder().build();
    }
}
