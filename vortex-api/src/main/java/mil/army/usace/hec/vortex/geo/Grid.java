package mil.army.usace.hec.vortex.geo;

import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Grid {
    private double originX;
    private double originY;
    private final double dx;
    private final double dy;
    private final int nx;
    private final int ny;

    private final String crs;
    private List<GridCell> gridCells;

    private Grid(Builder builder) {
        this.originX = builder.originX;
        this.originY = builder.originY;
        this.dx = builder.dx;
        this.dy = builder.dy;
        this.nx = builder.nx;
        this.ny = builder.ny;
        this.crs = builder.crs;
    }

    public static class Builder {
        private double originX;
        private double originY;
        private double dx;
        private double dy;
        private int nx;
        private int ny;
        private String crs;

        public Builder() {
            // Empty constructor
        }

        public Builder(Grid copyGrid) {
            this.originX = copyGrid.getOriginX();
            this.originY = copyGrid.getOriginY();
            this.dx = copyGrid.getDx();
            this.dy = copyGrid.getDy();
            this.nx = copyGrid.getNx();
            this.ny = copyGrid.getNy();
            this.crs = copyGrid.getCrs();
        }

        public Builder originX(double originX) {
            this.originX = originX;
            return this;
        }

        public Builder originY(double originY) {
            this.originY = originY;
            return this;
        }

        public Builder dx(double dx) {
            this.dx = dx;
            return this;
        }

        public Builder dy(double dy) {
            this.dy = dy;
            return this;
        }

        public Builder nx(int nx) {
            this.nx = nx;
            return this;
        }

        public Builder ny(int ny) {
            this.ny = ny;
            return this;
        }

        public Builder crs(String crs) {
            this.crs = crs;
            return this;
        }

        public Grid build() {
            return new Grid(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder toBuilder(Grid grid) {
        return new Builder(grid);
    }

    public List<GridCell> getGridCells() {
        if (gridCells == null) {
            gridCells = new ArrayList<>();
            AtomicInteger index = new AtomicInteger();
            IntStream.range(0, ny).forEach(y -> IntStream.range(0, nx).forEach(x -> {
                double cellOriginX = originX + x * dx;
                double cellOriginY = originY + y * dy;
                double minX = Math.min(cellOriginX, cellOriginX + dx);
                double maxX = Math.max(cellOriginX, cellOriginX + dx);
                double minY = Math.min(cellOriginY, cellOriginY + dy);
                double maxY = Math.max(cellOriginY, cellOriginY + dy);
                GridCell gridCell = GridCell.builder()
                        .index(index.getAndIncrement())
                        .minX(minX)
                        .maxX(maxX)
                        .minY(minY)
                        .maxY(maxY)
                        .build();

                gridCells.add(gridCell);
            }));
        }
        return gridCells;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public double getTerminusX() {
        return originX + dx * nx;
    }

    public double getTerminusY() {
        return originY + dy * ny;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public int getNx() {
        return nx;
    }

    public int getNy() {
        return ny;
    }

    public String getCrs() {
        return crs;
    }

    public double[] getPoint(int index) {
        int row = index / nx;
        int column = index % nx;

        double x = originX + column * dx + 0.5 * dx;
        double y = originY + row * dy + 0.5 * dy;
        return new double[]{x, y};
    }

    public Coordinate getCoordinate(int index) {
        double[] xy = getPoint(index);
        return new Coordinate(xy[0], xy[1]);
    }

    public Coordinate[] getGridCellCentroidCoords() {
        int size = nx * ny;
        Coordinate[] coordinates = new Coordinate[size];
        for (int i = 0; i < size; i++) {
            coordinates[i] = getCoordinate(i);
        }
        return coordinates;
    }

    public void shift(double shiftX, double shiftY) {
        originX += shiftX;
        originY += shiftY;
    }
}
