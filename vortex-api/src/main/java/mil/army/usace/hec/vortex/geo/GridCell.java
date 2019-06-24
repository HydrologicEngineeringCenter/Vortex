package mil.army.usace.hec.vortex.geo;

import java.awt.geom.Point2D;

public class GridCell {
    private int index;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private GridCell(GridCellBuilder builder){
        this.index = builder.index;
        this.minX = builder.minX;
        this.maxX = builder.maxX;
        this.minY = builder.minY;
        this.maxY = builder.maxY;
    }

    public static class GridCellBuilder{
        private int index;
        private double minX;
        private double maxX;
        private double minY;
        private double maxY;

        public GridCellBuilder index (int index){
            this.index = index;
            return this;
        }

        public GridCellBuilder minX (double minX){
            this.minX = minX;
            return this;
        }

        public GridCellBuilder maxX (double maxX){
            this.maxX = maxX;
            return this;
        }

        public GridCellBuilder minY (double minY){
            this.minY = minY;
            return this;
        }

        public GridCellBuilder maxY (double maxY){
            this.maxY = maxY;
            return this;
        }

        public GridCell build(){
            return new GridCell(this);
        }
    }

    public static GridCellBuilder builder(){
        return new GridCellBuilder();
    }

    public int getIndex() {
        return index;
    }

    public boolean contains(Point2D point){
        double x = point.getX();
        double y = point.getY();

        return (x > minX && x < maxX
        && y > minY && y < maxY);
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }
}


