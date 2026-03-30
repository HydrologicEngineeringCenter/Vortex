package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexProperty;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IndexSearcher {
    private static final GeometryFactory FACTORY = new GeometryFactory();
    private final Map<Coordinate, Integer> cache = new ConcurrentHashMap<>();
    private final STRtree spatialIndex = new STRtree();
    private final Envelope domain = new Envelope();

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    IndexSearcher(GridCoordSystem gcs) {
        CoordinateAxis xAxis = gcs.getXHorizAxis();
        CoordinateAxis yAxis = gcs.getYHorizAxis();

        if (xAxis instanceof CoordinateAxis1D xAxis1D
                && yAxis instanceof CoordinateAxis1D yAxis1D) {
            buildFrom1DAxes(xAxis1D, yAxis1D);
        } else if (xAxis instanceof CoordinateAxis2D xAxis2D
                && yAxis instanceof CoordinateAxis2D yAxis2D) {
            buildFrom2DAxes(xAxis2D, yAxis2D);
        } else {
            throw new IllegalStateException();
        }

        spatialIndex.build();
    }

    private void buildFrom1DAxes(CoordinateAxis1D xAxis, CoordinateAxis1D yAxis) {
        double[] edgesX = xAxis.getCoordEdges();
        double[] edgesY = yAxis.getCoordEdges();

        int index = 0;
        for (int i = 0; i < edgesY.length - 1; i++) {
            for (int j = 0; j < edgesX.length - 1; j++) {
                Coordinate[] ring = createRing(
                        edgesX[j], edgesY[i],
                        edgesX[j + 1], edgesY[i],
                        edgesX[j + 1], edgesY[i + 1],
                        edgesX[j], edgesY[i + 1]);
                insertCell(index++, ring);
            }
        }
    }

    private void buildFrom2DAxes(CoordinateAxis2D xAxis, CoordinateAxis2D yAxis) {
        double[] edgesX = (double[]) xAxis.getEdges().copyTo1DJavaArray();
        double[] edgesY = (double[]) yAxis.getEdges().copyTo1DJavaArray();
        int nx = xAxis.getEdges().getShape()[1];

        int index = 0;
        for (int i = 0; i < edgesX.length - nx; i++) {
            if ((i + 1) % nx == 0)
                continue;

            Coordinate[] ring = createRing(
                    edgesX[i], edgesY[i],
                    edgesX[i + 1], edgesY[i + 1],
                    edgesX[i + nx + 1], edgesY[i + nx + 1],
                    edgesX[i + nx], edgesY[i + nx]);
            insertCell(index++, ring);
        }
    }

    private static Coordinate[] createRing(double x0, double y0, double x1, double y1,
                                           double x2, double y2, double x3, double y3) {
        return new Coordinate[]{
                new Coordinate(x0, y0), new Coordinate(x1, y1),
                new Coordinate(x2, y2), new Coordinate(x3, y3),
                new Coordinate(x0, y0)};
    }

    private void insertCell(int index, Coordinate[] ring) {
        Geometry geometry = FACTORY.createPolygon(FACTORY.createLinearRing(ring));
        Envelope envelope = geometry.getEnvelopeInternal();
        spatialIndex.insert(envelope, new IndexedGeometry(index, geometry));
        domain.expandToInclude(envelope);
    }

    public synchronized void cacheCoordinates(Coordinate[] coordinates) {
        boolean allCached = Arrays.stream(coordinates).allMatch(cache::containsKey);
        if (allCached)
            return;

        support.firePropertyChange(VortexProperty.STATUS.toString(), null, "ReIndexing");

        int count = coordinates.length;
        int lastProgress = -1;
        for (int i = 0; i < count; i++) {
            getIndex(coordinates[i]);
            int progress = (int) (((float) i / count) * 100);
            if (progress != lastProgress) {
                support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, progress);
                lastProgress = progress;
            }
        }
    }

    public synchronized int getIndex(double x, double y) {
        Coordinate coordinate = new Coordinate(x, y);
        Integer cached = cache.get(coordinate);
        if (cached != null)
            return cached;

        return getIndex(coordinate);
    }

    private int getIndex(Coordinate coordinate) {
        if (!domain.contains(coordinate)) {
            cache.put(coordinate, -1);
            return -1;
        }

        Point point = FACTORY.createPoint(coordinate);

        @SuppressWarnings("unchecked")
        List<IndexedGeometry> candidates = spatialIndex.query(new Envelope(coordinate));

        for (IndexedGeometry candidate : candidates) {
            if (candidate.geometry().contains(point)) {
                cache.put(coordinate, candidate.index());
                return candidate.index();
            }
        }

        cache.put(coordinate, -1);
        return -1;
    }

    private record IndexedGeometry(int index, Geometry geometry) {
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
}