package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class Reprojector {
    static {
        GdalRegister.getInstance();
    }

    private final String from;
    private final String to;

    private Reprojector(Builder builder) {
        from = builder.from;
        to = builder.to;
    }

    public static class Builder {
        private String from;
        private String to;

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Reprojector build() {
            return new Reprojector(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Geometry reproject(Geometry geometry) {
        SpatialReference fromSrs = new SpatialReference(from);
        fromSrs.MorphFromESRI();
        SpatialReference toSrs = new SpatialReference(to);
        toSrs.MorphFromESRI();
        org.gdal.ogr.Geometry ogrGeometry = GeometryConverter.convertToOgrGeometry(geometry);

        if (fromSrs.IsSame(toSrs) != 1) {
            CoordinateTransformation transformation = osr.CreateCoordinateTransformation(fromSrs, toSrs);
            ogrGeometry.Transform(transformation);
            transformation.delete();
        }

        Geometry jtsGeometry = GeometryConverter.convertToJtsGeometry(ogrGeometry);

        fromSrs.delete();
        toSrs.delete();
        ogrGeometry.delete();

        return jtsGeometry;
    }

    public Envelope reproject(Envelope envelope) {
        GeometryFactory factory = new GeometryFactory();
        Geometry geometry = factory.toGeometry(envelope);
        Geometry reprojected = reproject(geometry);
        return reprojected.getEnvelopeInternal();
    }

}
