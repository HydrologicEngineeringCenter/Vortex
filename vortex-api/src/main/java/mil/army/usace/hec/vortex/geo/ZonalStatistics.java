package mil.army.usace.hec.vortex.geo;

public class ZonalStatistics {
    private final String id;
    private final double average;
    private final double min;
    private final double max;
    private final double median;
    private final double firstQuartile;
    private final double thirdQuartile;
    private final double pctCellsGreaterThanZero;
    private final double pctCellsGreaterThanFirstQuartile;

    private ZonalStatistics(ZonalStatisticsBuilder builder){
        this.id = builder.id;
        this.average = builder.average;
        this.min = builder.min;
        this.max = builder.max;
        this.median = builder.median;
        this.firstQuartile = builder.firstQuartile;
        this.thirdQuartile = builder.thirdQuartile;
        this.pctCellsGreaterThanZero = builder.pctCellsGreaterThanZero;
        this.pctCellsGreaterThanFirstQuartile = builder.pctCellsGreaterThanFirstQuartile;
    }

    public static class ZonalStatisticsBuilder{
        private String id;
        private double average;
        private double min;
        private double max;
        private double median;
        private double firstQuartile;
        private double thirdQuartile;
        private double pctCellsGreaterThanZero;
        private double pctCellsGreaterThanFirstQuartile;

        public ZonalStatisticsBuilder id (String id){
            this.id = id;
            return this;
        }

        public ZonalStatisticsBuilder average(double average){
           this.average = average;
           return this;
        }

        public ZonalStatisticsBuilder min(double min) {
            this.min = min;
            return this;
        }

        public ZonalStatisticsBuilder max(double max) {
            this.max = max;
            return this;
        }

        public ZonalStatisticsBuilder median(double median) {
            this.median = median;
            return this;
        }

        public ZonalStatisticsBuilder firstQuartile(double firstQuartile) {
            this.firstQuartile = firstQuartile;
            return this;
        }

        public ZonalStatisticsBuilder thirdQuartile(double thirdQuartile) {
            this.thirdQuartile = thirdQuartile;
            return this;
        }

        public ZonalStatisticsBuilder pctCellsGreaterThanZero(double pctCellsGreaterThanZero) {
            this.pctCellsGreaterThanZero = pctCellsGreaterThanZero;
            return this;
        }

        public ZonalStatisticsBuilder pctCellsGreaterThanFirstQuartile(double pctCellsGreaterThanFirstQuartile) {
            this.pctCellsGreaterThanFirstQuartile = pctCellsGreaterThanFirstQuartile;
            return this;
        }

        public ZonalStatistics build(){
            return new ZonalStatistics(this);
        }
    }

    public static ZonalStatisticsBuilder builder(){
        return new ZonalStatisticsBuilder();
    }

    public String getId(){
        return id;
    }

    public double getAverage(){
        return average;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMedian() {
        return median;
    }

    public double getFirstQuartile() {
        return firstQuartile;
    }

    public double getThirdQuartile() {
        return thirdQuartile;
    }

    public double getPctCellsGreaterThanZero() {
        return pctCellsGreaterThanZero;
    }

    public double getPctCellsGreaterThanFirstQuartile() {
        return pctCellsGreaterThanFirstQuartile;
    }
}
