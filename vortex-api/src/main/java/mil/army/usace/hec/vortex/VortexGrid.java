package mil.army.usace.hec.vortex;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;

public class VortexGrid implements VortexData, Serializable {

    private double dx;
    private double dy;
    private int nx;
    private int ny;
    private double originX;
    private double originY;
    private String wkt;
    private float [] data;
    private String units;
    private String fileName;
    private String shortName;
    private String fullName;
    private String description;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Duration interval;

    private VortexGrid(VortexGridBuilder builder) {

        this.dx = builder.dx;
        this.dy = builder.dy;
        this.nx = builder.nx;
        this.ny = builder.ny;
        this.originX = builder.originX;
        this.originY = builder.originY;
        this.wkt = builder.wkt;
        this.data = builder.data;
        this.units = builder.units;
        this.fileName = builder.fileName;
        this.shortName = builder.shortName;
        this.fullName = builder.fullName;
        this.description = builder.description;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.interval = builder.interval;

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

    public String wkt() {
        return wkt;
    }

    public float[] data() {
        return data;
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

