package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.io.Validation;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.locationtech.jts.geom.Envelope;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class VectorUtils {
    static {
        GdalRegister.getInstance();
    }

    private VectorUtils(){}

    /** Validates each layer's projection without reading feature geometry. */
    public static Validation validateProjection(Path path) {
        DataSource source = null;
        try {
            source = ogr.Open(path.toString(), 0);
            if (source == null || source.GetLayerCount() == 0) {
                return Validation.of(false, Message.format("error_invalid_file", path));
            }
            for (int i = 0; i < source.GetLayerCount(); i++) {
                Layer layer = source.GetLayer(i);
                try {
                    SpatialReference srs = layer.GetSpatialRef();
                    if (srs == null) {
                        return Validation.of(false, Message.format("warn_vector_missing_projection", path));
                    }
                    try {
                        String wkt = srs.ExportToWkt();
                        if (wkt == null || wkt.isBlank()) {
                            return Validation.of(false, Message.format("warn_vector_missing_projection", path));
                        }
                        if (!ReferenceUtils.isValidProjection(wkt)) {
                            return Validation.of(false, Message.format("warn_vector_invalid_projection", path));
                        }
                    } finally {
                        srs.delete();
                    }
                } finally {
                    layer.delete();
                }
            }
            return Validation.of(true);
        } catch (RuntimeException e) {
            return Validation.of(false, Message.format("error_projection_read", path, e.getMessage()));
        } finally {
            if (source != null) {
                source.delete();
            }
        }
    }

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

    public static Envelope getEnvelope(Path shapefile){
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        DataSource dataSource = driver.Open(shapefile.toString(), 0);
        Envelope envelope = getEnvelope(dataSource);

        driver.delete();
        dataSource.delete();

        return envelope;
    }

    private static Envelope getEnvelope(DataSource dataSource){
        Layer layer = dataSource.GetLayer(0);
        Envelope envelope =  getEnvelope(layer);

        layer.delete();

        return envelope;
    }

    private static Envelope getEnvelope(Layer layer){
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

        return new Envelope(minx, maxx, miny, maxy);
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
