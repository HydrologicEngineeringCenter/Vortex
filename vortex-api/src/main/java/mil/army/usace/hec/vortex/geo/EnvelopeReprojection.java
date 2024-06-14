package mil.army.usace.hec.vortex.geo;

import org.locationtech.jts.geom.Envelope;

import java.util.Objects;

class EnvelopeReprojection {
    private final String fromWkt;
    private final String toWkt;
    private final Envelope envelope;

    private EnvelopeReprojection(Envelope envelope, String fromWkt, String toWkt) {
        this.fromWkt = fromWkt;
        this.toWkt = toWkt;
        this.envelope = envelope;
    }

    static EnvelopeReprojection of(Envelope envelope, String fromWkt, String toWkt) {
        return new EnvelopeReprojection(envelope, fromWkt, toWkt);
    }

    Envelope reproject() {
        Reprojector reprojector = Reprojector.builder()
                .from(fromWkt)
                .to(toWkt)
                .build();

        return reprojector.reproject(envelope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvelopeReprojection that = (EnvelopeReprojection) o;
        return Objects.equals(fromWkt, that.fromWkt)
                && Objects.equals(toWkt, that.toWkt)
                && Objects.equals(envelope, that.envelope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromWkt, toWkt, envelope);
    }
}
