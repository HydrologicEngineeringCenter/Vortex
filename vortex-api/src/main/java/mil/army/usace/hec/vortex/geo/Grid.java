package mil.army.usace.hec.vortex.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Grid {
    private double originX;
    private double originY;
    private double dx;
    private double dy;
    private int nx;
    private int ny;
    private List<GridCell> gridCells;

    private Grid (GridBuilder builder){
        this.originX = builder.originX;
        this.originY = builder.originY;
        this.dx = builder.dx;
        this.dy = builder.dy;
        this.nx = builder.nx;
        this.ny = builder.ny;
    }

    public static class GridBuilder{
        private double originX;
        private double originY;
        private double dx;
        private double dy;
        private int nx;
        private int ny;

        public GridBuilder originX (double originX){
            this.originX = originX;
            return this;
        }

        public GridBuilder originY (double originY){
            this.originY = originY;
            return this;
        }

        public GridBuilder dx (double dx){
            this.dx = dx;
            return this;
        }

        public GridBuilder dy (double dy){
            this.dy = dy;
            return this;
        }

        public GridBuilder nx (int nx){
            this.nx = nx;
            return this;
        }

        public GridBuilder ny (int ny){
            this.ny = ny;
            return this;
        }

        public Grid build() {
            return new Grid(this);
        }
    }

    public static GridBuilder builder() {
        return new GridBuilder();
    }

    public List<GridCell> getGridCells(){
        if (gridCells == null){
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

    public Grid copy() {
        return Grid.builder()
                .originX(this.originX)
                .originY(this.originY)
                .dx(this.dx)
                .dy(this.dy)
                .nx(this.nx)
                .ny(this.ny)
                .build();
    }
}
