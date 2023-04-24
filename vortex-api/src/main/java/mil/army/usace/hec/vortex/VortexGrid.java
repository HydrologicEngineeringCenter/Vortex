package mil.army.usace.hec.vortex;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

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

    public double[] xCoordinates() {
        double[] xCoordinates = new double[nx];
        // xCoordinates[i] = midpoint of xEdges[i] and xEdges[i + 1]
        for (int i = 0; i < nx; i++) xCoordinates[i] = originX + (i + 1) * dx - (dx / 2);
        return xCoordinates;
    }

    public double[] yCoordinates() {
        double[] yCoordinates = new double[ny];
        // yCoordinates[i] = midpoint of yEdges[i] and yEdges[i + 1]
        for (int i = 0; i < ny; i++) yCoordinates[i] = originY + (i + 1) * dy - (dy / 2);
        return yCoordinates;
    }

    public float[][] data2D() {
        float[][] data2D = new float[ny][nx];
        for (int y = 0; y < ny; y++) System.arraycopy(data, y * nx, data2D[y], 0, nx);
        return data2D;
    }

    @Override
    public boolean equals(Object o) {
        /* Temporary */
        if (this == o) return true;
        if (!(o instanceof VortexGrid)) return false;

        VortexGrid that = (VortexGrid) o;

        if (Double.compare(that.dx, dx) != 0)
            return false;
        if (Double.compare(that.dy, dy) != 0)
            return false;
        if (nx != that.nx)
            return false;
        if (ny != that.ny)
            return false;
        if (Double.compare(that.originX, originX) != 0)
            return false;
        if (Double.compare(that.originY, originY) != 0)
            return false;
//        if (Double.compare(that.noDataValue, noDataValue) != 0)
//            return false;
        if (Double.compare(that.terminusX, terminusX) != 0)
            return false;
        if (Double.compare(that.terminusY, terminusY) != 0)
            return false;
        if (!Objects.equals(wkt, that.wkt))
            return false;
        if (!Arrays.equals(data, that.data))
            return false;
        if (!Objects.equals(units, that.units))
            return false;
        if (!Objects.equals(shortName, that.shortName))
            return false;
        if (!Objects.equals(fullName, that.fullName))
            return false;
        if (!Objects.equals(description, that.description))
            return false;
        if (!Objects.equals(startTime, that.startTime))
            return false;
        if (!Objects.equals(endTime, that.endTime))
            return false;
        return Objects.equals(interval, that.interval);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(dx);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(dy);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + nx;
        result = 31 * result + ny;
        temp = Double.doubleToLongBits(originX);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(originY);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (wkt != null ? wkt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(data);
        temp = Double.doubleToLongBits(noDataValue);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (units != null ? units.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (shortName != null ? shortName.hashCode() : 0);
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (interval != null ? interval.hashCode() : 0);
        temp = Double.doubleToLongBits(terminusX);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(terminusY);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}

