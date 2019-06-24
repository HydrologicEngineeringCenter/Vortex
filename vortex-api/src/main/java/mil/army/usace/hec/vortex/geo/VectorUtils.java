package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;

import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.util.*;

public class VectorUtils {
    static {
        GdalRegister.getInstance();
    }

    private VectorUtils(){}

    public static String getWkt(Path pathToShp){
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        DataSource dataSource = driver.Open(pathToShp.toString(), 0);
        Layer layer = dataSource.GetLayer(0);
        SpatialReference src = layer.GetSpatialRef();
        src.MorphFromESRI();
        String wkt = src.ExportToPrettyWkt();

        driver.delete();
        dataSource.delete();
        layer.delete();
        src.delete();

        return wkt;
    }

    public static Rectangle2D getEnvelope(Path shapefile){
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        DataSource dataSource = driver.Open(shapefile.toString(), 0);
        Rectangle2D envelope = getEnvelope(dataSource);

        driver.delete();
        dataSource.delete();

        return envelope;
    }

    private static Rectangle2D getEnvelope(DataSource dataSource){
        Layer layer = dataSource.GetLayer(0);
        Rectangle2D envelope =  getEnvelope(layer);

        layer.delete();

        return envelope;
    }

    private static Rectangle2D getEnvelope(Layer layer){
        Geometry geometry = null;
        Feature feature = null;
        double minx = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        long count = layer.GetFeatureCount();
        for (int i=0; i<count; i++){
            feature = layer.GetFeature(i);
            geometry = feature.GetGeometryRef();
            double [] envelope = new double[4];
            geometry.GetEnvelope(envelope);
            minx = Math.min(minx, envelope[0]);
            maxx = Math.max(maxx, envelope[1]);
            miny = Math.min(miny, envelope[2]);
            maxy = Math.max(maxy, envelope[3]);
        }

        Optional.ofNullable(geometry).ifPresent(Geometry::delete);
        Optional.ofNullable(feature).ifPresent(Feature::delete);

        return new Rectangle2D.Double(minx, miny, maxx-minx, maxy-miny);
    }

    public static Set<String> getValues(Path pathToFeatures, String field){
        DataSource dataSource = ogr.Open(pathToFeatures.toString());
        Layer layer = dataSource.GetLayer(0);
        long count = layer.GetFeatureCount();
        Set<String> values = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Feature feature = layer.GetFeature(i);
            values.add(feature.GetFieldAsString(field));
        }
        return values;
    }

    public static Set<String> getFields(Path pathToFeatures){
        DataSource dataSource = ogr.Open(pathToFeatures.toString());
        Layer layer = dataSource.GetLayer(0);
        FeatureDefn featureDefn = layer.GetLayerDefn();
        long count = featureDefn.GetFieldCount();

        Set<String> fields = new HashSet<>();
        for (int i = 0; i < count; i++) {
            fields.add(featureDefn.GetFieldDefn(i).GetName());
        }

        dataSource.delete();
        layer.delete();
        featureDefn.delete();

        return fields;
    }
}
