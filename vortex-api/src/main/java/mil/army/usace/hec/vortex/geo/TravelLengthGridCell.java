package mil.army.usace.hec.vortex.geo;

public class TravelLengthGridCell {
    private int indexI;
    private int indexJ;
    private String subbasin;
    private double area;
    private double travelLength;

    private TravelLengthGridCell(TravelLengthCellBuilder builder){
        this.indexI = builder.indexI;
        this.indexJ = builder.indexJ;
        this.subbasin = builder.subbasin;
        this.area = builder.area;
        this.travelLength = builder.travelLength;
    }

    public static class TravelLengthCellBuilder {
        private int indexI;
        private int indexJ;
        private String subbasin;
        private double travelLength;
        private double area;

        public TravelLengthCellBuilder indexI (int indexI){
            this.indexI = indexI;
            return this;
        }

        public TravelLengthCellBuilder indexJ (int indexJ){
            this.indexJ = indexJ;
            return this;
        }

        public TravelLengthCellBuilder subbasin (String subbasin){
            this.subbasin = subbasin;
            return this;
        }

        public TravelLengthCellBuilder area (double area){
            this.area = area;
            return this;
        }
        public TravelLengthCellBuilder travelLength (double travelLength){
            this.travelLength = travelLength;
            return this;
        }


        public TravelLengthGridCell build(){
            return new TravelLengthGridCell(this);
        }
    }

    public static TravelLengthCellBuilder builder(){
        return new TravelLengthCellBuilder();
    }

    public int getIndexI() {
        return indexI;
    }

    public int getIndexJ() {
        return indexJ;
    }

    public String getSubbasin() {
        return subbasin;
    }

    public double getArea() {
        return area;
    }

    public double getTravelLength() {
        return travelLength;
    }
}
