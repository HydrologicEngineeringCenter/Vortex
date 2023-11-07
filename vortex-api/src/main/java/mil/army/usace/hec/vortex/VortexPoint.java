package mil.army.usace.hec.vortex;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;

public class VortexPoint implements VortexData, Serializable {
    private double originX;
    private double originY;
    private String id;
    private String wkt;
    private final float average;
    private final float min;
    private final float max;
    private final float median;
    private final float firstQuartile;
    private final float thirdQuartile;
    private final double pctCellsGreaterThanZero;
    private final double pctCellsGreaterThanFirstQuartile;
    private String units;
    private String fileName;
    private String shortName;
    private String fullName;
    private String description;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Duration interval;

    private VortexPoint(VortexPointBuilder builder) {

        this.originX = builder.originX;
        this.originY = builder.originY;
        this.id = builder.id;
        this.wkt = builder.wkt;
        this.average = builder.average;
        this.min = builder.min;
        this.max = builder.max;
        this.median = builder.median;
        this.firstQuartile = builder.firstQuartile;
        this.thirdQuartile = builder.thirdQuartile;
        this.pctCellsGreaterThanZero = builder.pctCellsGreaterThanZero;
        this.pctCellsGreaterThanFirstQuartile = builder.pctCellsGreaterThanFirstQuartile;
        this.units = builder.units;
        this.fileName = builder.fileName;
        this.shortName = builder.shortName;
        this.fullName = builder.fullName;
        this.description = builder.description;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.interval = builder.interval;
    }

    public static class VortexPointBuilder {
        private double originX;
        private double originY;
        private String id;
        private String wkt;
        private float average;
        private float min;
        private float max;
        private float median;
        private float firstQuartile;
        private float thirdQuartile;
        private double pctCellsGreaterThanZero;
        private double pctCellsGreaterThanFirstQuartile;
        private String units;
        private String fileName;
        private String shortName;
        private String fullName;
        private String description;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private Duration interval;

        public VortexPointBuilder originX(final double originX) {
            this.originX = originX;
            return this;
        }

        public VortexPointBuilder originY(final double originY) {
            this.originY = originY;
            return this;
        }

        public VortexPointBuilder id (final String id){
            this.id = id;
            return this;
        }

        public VortexPointBuilder wkt (final String wkt) {
            this.wkt = wkt;
            return this;
        }

        public VortexPointBuilder average(final float average) {
            this.average = average;
            return this;
        }

        public VortexPointBuilder min (final float min) {
            this.min = min;
            return this;
        }

        public VortexPointBuilder max (final float max) {
            this.max = max;
            return this;
        }

        public VortexPointBuilder median (final float median) {
            this.median = median;
            return this;
        }

        public VortexPointBuilder firstQuartile (final float firstQuartile) {
            this.firstQuartile = firstQuartile;
            return this;
        }

        public VortexPointBuilder thirdQuartile (final float thirdQuartile) {
            this.thirdQuartile = thirdQuartile;
            return this;
        }

        public VortexPointBuilder pctCellsGreaterThanZero (final double pctCellsGreaterThanZero) {
            this.pctCellsGreaterThanZero = pctCellsGreaterThanZero;
            return this;
        }

        public VortexPointBuilder pctCellsGreaterThanFirstQuartile (final double pctCellsGreaterThanFirstQuartile) {
            this.pctCellsGreaterThanFirstQuartile = pctCellsGreaterThanFirstQuartile;
            return this;
        }

        public VortexPointBuilder units (final String units) {
            this.units = units;
            return this;
        }

        public VortexPointBuilder fileName (final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public VortexPointBuilder shortName (final String shortName) {
            this.shortName = shortName;
            return this;
        }

        public VortexPointBuilder fullName (final String fullName) {
            this.fullName = fullName;
            return this;
        }

        public VortexPointBuilder description (final String description) {
            this.description = description;
            return this;
        }

        public VortexPointBuilder startTime (final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public VortexPointBuilder endTime (final ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public VortexPointBuilder interval (final Duration interval) {
            this.interval = interval;
            return this;
        }

        public VortexPoint build() {
            return new VortexPoint(this);
        }

    }

    public static VortexPointBuilder builder(){
        return new VortexPointBuilder();
    }

    public double originX() {
        return originX;
    }

    public double originY() {
        return originY;
    }

    public String id() {return id;}

    public String wkt() {
        return wkt;
    }

    public float getAverage() {
        return average;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getMedian() {
        return median;
    }

    public float getFirstQuartile() {
        return firstQuartile;
    }

    public float getThirdQuartile() {
        return thirdQuartile;
    }

    public double getPctCellsGreaterThanZero() {
        return pctCellsGreaterThanZero;
    }

    public double pctCellsGreaterThanFirstQuartile() {
        return pctCellsGreaterThanFirstQuartile;
    }

    public String units() {
        return units;
    }

    public String fileName() {
        return fileName;
    }

    public String shortName() {
        return shortName;
    }

    public String fullName() {
        return fullName;
    }

    public String description() {
        return description;
    }

    public ZonedDateTime startTime() {
        return startTime;
    }

    public ZonedDateTime endTime() {
        return endTime;
    }

    public long startTimeMilli(){
        return startTime.toInstant().toEpochMilli();
    }

    public long endTimeMilli(){
        return endTime.toInstant().toEpochMilli();
    }

    public Duration interval() {
        return interval;
    }

}
