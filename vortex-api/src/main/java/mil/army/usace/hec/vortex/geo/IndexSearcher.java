package mil.army.usace.hec.vortex.geo;

import org.locationtech.jts.geom.*;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexSearcher {
    private final Map<Point, Integer> cache = new ConcurrentHashMap<>();
    private final List<IndexedGeometry> geometries = new ArrayList<>();
    IndexSearcher(CoordinateAxes coordinateAxes) {

        CoordinateAxis xAxis = coordinateAxes.xAxis();
        CoordinateAxis yAxis = coordinateAxes.yAxis();

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

                Set<Double> values = new HashSet<>();
                for (Coordinate coordinate : coordinates) {
                    values.add(coordinate.getX());
                    values.add(coordinate.getY());
                }

                if (!values.contains(Double.NaN)) {
                    LinearRing ring = factory.createLinearRing(coordinates);
                    Geometry geometry = factory.createPolygon(ring);
                    geometries.add(new IndexedGeometry(index, geometry));
                }

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

    public int[] getIndices(Coordinate[] coordinates) {
        int[] indices = new int[coordinates.length];
        Arrays.fill(indices, -1);

        GeometryFactory factory = new GeometryFactory();

        List<IndexedPoint> indexedPoints = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < coordinates.length; i++) {
            Point point = factory.createPoint(coordinates[i]);
            indexedPoints.add(new IndexedPoint(i, point));
        }

        AtomicInteger processed = new AtomicInteger();

        int count = indexedPoints.size();

        indexedPoints.parallelStream().forEach(ip -> {
            int index = geometries.parallelStream()
                    .filter(ig -> ig.geometry.contains(ip.point))
                    .findFirst()
                    .map(ig -> ig.index)
                    .orElse(-1);

            indices[ip.index] = index;

            processed.incrementAndGet();
            System.out.println(count - processed.get());
        });

        return indices;
    }

    private record IndexedGeometry(int index, Geometry geometry) {}

    private record IndexedPoint(int index, Point point) {}
}