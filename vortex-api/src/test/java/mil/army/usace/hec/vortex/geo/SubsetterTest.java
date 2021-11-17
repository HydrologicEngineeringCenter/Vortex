package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

class SubsetterTest {

    @Test
    void subset() {
        VortexGrid in = VortexGrid.builder()
                .originX(1000)
                .originY(1000)
                .dx(100)
                .dy(-100)
                .nx(4)
                .ny(4)
                .wkt(WktFactory.shg())
                .data(new float[16])
                .build();

        GeometryFactory factory = new GeometryFactory();

        double originX = 1100;
        double originY = 900;
        double terminusX = 1300;
        double terminusY = 700;

        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(originX, originY),
                new Coordinate(terminusX, originY),
                new Coordinate(terminusX, terminusY),
                new Coordinate(originX, terminusY),
                new Coordinate(originX, originY)
        };

        Geometry geometry = factory.createPolygon(coordinates);
        Envelope envelope = geometry.getEnvelopeInternal();

        Subsetter subsetter = Subsetter.builder()
                .setGrid(in)
                .setEnvelope(envelope)
                .build();

        VortexGrid subset = subsetter.subset();

        Assertions.assertEquals(4, subset.data().length);
    }
}