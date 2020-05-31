package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import org.gdal.ogr.Geometry;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import java.awt.geom.Point2D;

import static org.gdal.ogr.ogrConstants.wkbPoint;

public class CoordinateConverter {
    static {
        GdalRegister.getInstance();
    }
    
    private final String fromCrs;
    private final String toCrs;
    
    private CoordinateConverter(Builder builder) {
        fromCrs = builder.fromCrs;
        toCrs = builder.toCrs;
    }
    
    public static class Builder {
        private String fromCrs;
        private String toCrs;

        public Builder setFromCrs(String fromCrs) {
            this.fromCrs = fromCrs;
            return this;
        }

        public Builder setToCrs(String toCrs) {
            this.toCrs = toCrs;
            return this;
        }
        
        public CoordinateConverter build() {
            if (fromCrs == null || fromCrs.isEmpty()) {
                throw new IllegalArgumentException("From CRS must be provided");
            }
            if (toCrs == null || toCrs.isEmpty()) {
                throw new IllegalArgumentException("To CRS must be provided");
            }
            return new CoordinateConverter(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Point2D convert (Point2D point) {
        Geometry geometry = new Geometry(wkbPoint);
        geometry.AddPoint(point.getX(), point.getY());

        SpatialReference from = new SpatialReference(fromCrs);
        SpatialReference to = new SpatialReference(toCrs);
        CoordinateTransformation transformation = osr.CreateCoordinateTransformation(from, to);
        geometry.Transform(transformation);
        transformation.delete();

        Point2D transformed = new Point2D.Double(geometry.GetX(), geometry.GetY());

        from.delete();
        to.delete();
        transformation.delete();

        return transformed;
    }
}
