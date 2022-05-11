package mil.army.usace.hec.vortex;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;

public class VortexGrid implements VortexData, Serializable {

    private final double dx;
    private final double dy;
    private final int nx;
    private final int ny;
    private final double originX;
    private final double originY;
    private final String wkt;
    private final float [] data;
    private final double noDataValue;
    private final String units;
    private final String fileName;
    private final String shortName;
    private final String fullName;
    private final String description;
    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;
    private final Duration interval;

    private final double terminusX;
    private final double terminusY;

    private VortexGrid(VortexGridBuilder builder) {

        this.dx = builder.dx;
        this.dy = builder.dy;
        this.nx = builder.nx;
        this.ny = builder.ny;
        this.originX = builder.originX;
        this.originY = builder.originY;
        this.wkt = builder.wkt;
        this.data = builder.data;
        this.noDataValue = builder.noDataValue;
        this.units = builder.units;
        this.fileName = builder.fileName;
        this.shortName = builder.shortName;
        this.fullName = builder.fullName;
        this.description = builder.description;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.interval = builder.interval;

        terminusX = originX + dx * nx;
        terminusY = originY + dy * ny;
    }

    public static class VortexGridBuilder {
        private double dx;
        private double dy;
        private int nx;
        private int ny;
        private double originX;
        private double originY;
        private String wkt;
        private float [] data;
        private double noDataValue = Double.NaN;
        private String units;
        private String fileName;
        private String shortName;
        private String fullName;
        private String description;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private Duration interval;

        public VortexGridBuilder dx (final double dx) {
            this.dx = dx;
            return this;
        }

        public VortexGridBuilder dy (final double dy) {
            this.dy = dy;
            return this;
        }

        public VortexGridBuilder nx (final int nx) {
            this.nx = nx;
            return this;
        }

        public VortexGridBuilder ny (final int ny) {
            this.ny = ny;
            return this;
        }

        public VortexGridBuilder originX(final double originX) {
            this.originX = originX;
            return this;
        }

        public VortexGridBuilder originY(final double originY) {
            this.originY = originY;
            return this;
        }

        public VortexGridBuilder wkt (final String wkt) {
            this.wkt = wkt;
            return this;
        }

        public VortexGridBuilder data (final float[] data) {
            this.data = data;
            return this;
        }

        public VortexGridBuilder noDataValue (final double noDataValue) {
            this.noDataValue = noDataValue;
            return this;
        }

        public VortexGridBuilder units (final String units) {
            this.units = units;
            return this;
        }

        public VortexGridBuilder fileName (final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public VortexGridBuilder shortName (final String shortName) {
            this.shortName = shortName;
            return this;
        }

        public VortexGridBuilder fullName (final String fullName) {
            this.fullName = fullName;
            return this;
        }

        public VortexGridBuilder description (final String description) {
            this.description = description;
            return this;
        }

        public VortexGridBuilder startTime (final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public VortexGridBuilder endTime (final ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public VortexGridBuilder interval (final Duration interval) {
            this.interval = interval;
            return this;
        }

        public VortexGrid build() {
            return new VortexGrid(this);
        }

    }

    public static VortexGridBuilder builder(){
        return new VortexGridBuilder();
    }

    public double dx() {
        return dx;
    }

    public double dy() {
        return dy;
    }

    public int nx() {
        return nx;
    }

    public int ny() {
        return ny;
    }

    public double originX() {
        return originX;
    }

    public double originY() {
        return originY;
    }

    public double getTerminusX() {
        return terminusX;
    }

    public double terminusY() {
        return terminusY;
    }

    public String wkt() {
        return wkt;
    }

    public float[] data() {
        return data;
    }

    public double noDataValue() {
        return noDataValue;
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

