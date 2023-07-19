package mil.army.usace.hec.vortex.geo;

import org.locationtech.jts.geom.*;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexSearcher {
    private final Map<Point, Integer> cache = new HashMap<>();
    private final List<IndexedGeometry> geometries = new ArrayList<>();
    IndexSearcher(GridCoordSystem gcs) {

        CoordinateAxis xAxis = gcs.getXHorizAxis();
        CoordinateAxis yAxis = gcs.getYHorizAxis();

        GeometryFactory factory = new GeometryFactory();

        if (xAxis instanceof CoordinateAxis1D xAxis1D
                && yAxis instanceof CoordinateAxis1D yAxis1D) {

            double[] edgesX = xAxis1D.getCoordEdges();
            double[] edgesY = yAxis1D.getCoordEdges();

            int index = 0;
            for (int i = 0; i < edgesY.length - 1; i++) {
                for (int j = 0; j < edgesX.length - 1; j++) {
                    Coordinate[] coordinates = new Coordinate[5];
                    coordinates[0] = new Coordinate(edgesX[i], edgesY[i]);
                    coordinates[1] = new Coordinate(edgesX[i + 1], edgesY[i]);
                    coordinates[2] = new Coordinate(edgesX[i + 1], edgesY[i + 1]);
                    coordinates[3] = new Coordinate(edgesX[i], edgesY[i + 1]);
                    coordinates[4] = new Coordinate(edgesX[i], edgesY[i]);
                    LinearRing ring = factory.createLinearRing(coordinates);
                    Geometry geometry = factory.createPolygon(ring);

                    geometries.add(new IndexedGeometry(index, geometry));
                    index++;
                }
            }

        } else if (xAxis instanceof CoordinateAxis2D xAxis2D
                && yAxis instanceof CoordinateAxis2D yAxis2D) {

            double[] edgesX = (double[]) xAxis2D.getEdges().copyTo1DJavaArray();
            double[] edgesY = (double[]) yAxis2D.getEdges().copyTo1DJavaArray();

            int nx = xAxis2D.getEdges().getShape()[1];

            int index = 0;
            for (int i = 0; i < edgesX.length - nx; i++) {
                if ((i + 1) % nx == 0)
                    continue;

                Coordinate[] coordinates = new Coordinate[5];
                coordinates[0] = new Coordinate(edgesX[i], edgesY[i]);
                coordinates[1] = new Coordinate(edgesX[i + 1], edgesY[i + 1]);
                coordinates[2] = new Coordinate(edgesX[i + nx + 1], edgesY[i + nx + 1]);
                coordinates[3] = new Coordinate(edgesX[i + nx], edgesY[i + nx]);
                coordinates[4] = new Coordinate(edgesX[i], edgesY[i]);
                LinearRing ring = factory.createLinearRing(coordinates);
                Geometry geometry = factory.createPolygon(ring);

                geometries.add(new IndexedGeometry(index, geometry));
                index++;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public int getIndex(double x, double y) {
        GeometryFactory factory = new GeometryFactory();
        Point point = factory.createPoint(new Coordinate(x, y));

        if (cache.containsKey(point))
            return cache.get(point);

        int index = geometries.parallelStream()
                .filter(g -> g.geometry().contains(point))
                .findAny()
                .map(IndexedGeometry::index)
                .orElse(-1);

        cache.put(point, index);

        return index;
    }

    private record IndexedGeometry(int index, Geometry geometry) {}
}