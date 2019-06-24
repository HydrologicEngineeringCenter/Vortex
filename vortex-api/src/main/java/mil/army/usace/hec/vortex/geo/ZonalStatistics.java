package mil.army.usace.hec.vortex.geo;

public class ZonalStatistics {
    private String id;
    private double average;

    private ZonalStatistics(ZonalStatisticsBuilder builder){
        this.id = builder.id;
        this.average = builder.average;
    }

    public static class ZonalStatisticsBuilder{
        private String id;
        private double average;

        public ZonalStatisticsBuilder id (String id){
            this.id = id;
            return this;
        }

        public ZonalStatisticsBuilder average(double average){
            this.average = average;
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
        return this.id;
    }

    public double getAverage(){
        return this.average;
    }
}
