package mil.army.usace.hec.vortex;

import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.util.UnitUtil;

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
    private final VortexDataType dataType;
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
        this.dataType = builder.dataType;

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
        private VortexDataType dataType;

        public VortexGridBuilder() {
            // Empty constructor
        }

        private VortexGridBuilder(VortexGrid copy) {
            dx = copy.dx;
            dy = copy.dy;
            nx = copy.nx;
            ny = copy.ny;
            originX = copy.originX;
            originY = copy.originY;
            wkt = copy.wkt;
            data = copy.data;
            noDataValue = copy.noDataValue;
            units = copy.units;
            fileName = copy.fileName;
            shortName = copy.shortName;
            fullName = copy.fullName;
            description = copy.description;
            startTime = copy.startTime;
            endTime = copy.endTime;
            interval = copy.interval;
            dataType = copy.dataType;
        }

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

        public VortexGridBuilder dataType (final VortexDataType dataType) {
            this.dataType = dataType;
            return this;
        }

        public VortexGrid build() {
            return new VortexGrid(this);
        }
    }

    /**
     * Creates a {@link VortexGridBuilder} instance that is initialized with the properties of the specified {@code vortexGrid}.
     * This method facilitates the creation of a new {@code VortexGrid} object that is a modified version of the existing one
     * by copying its current state into a builder. Modifications can then be made on the builder before constructing a new
     * {@code VortexGrid} instance.
     *
     * @param vortexGrid The {@link VortexGrid} instance from which to copy properties.
     * @return A {@code VortexGridBuilder} initialized with the properties copied from the provided {@code vortexGrid}.
     */
    public static VortexGridBuilder toBuilder(VortexGrid vortexGrid) {
        return new VortexGridBuilder(vortexGrid);
    }

    public static VortexGridBuilder builder(){
        return new VortexGridBuilder();
    }

    public static VortexGrid noDataGrid() {
        return VortexGrid.builder()
                .shortName("")
                .fullName("")
                .description("")
                .fileName("")
                .nx(0).ny(0)
                .dx(0).dy(0)
                .wkt("")
                .data(new float[0])
                .noDataValue(Double.NaN)
                .units("")
                .dataType(VortexDataType.UNDEFINED)
                .build();
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

    public VortexDataType dataType() {
        return dataType != null ? dataType : inferDataType();
    }

    public float[][][] data3D() {
        float[][][] data3D = new float[1][ny][nx];
        for (int y = 0; y < ny; y++) System.arraycopy(data, y * nx, data3D[0][y], 0, nx);
        return data3D;
    }

    private VortexDataType inferDataType() {
        if (interval == null || interval.isZero()) return VortexDataType.INSTANTANEOUS;

        return switch (VortexVariable.fromName(shortName)) {
            case PRECIPITATION -> VortexDataType.ACCUMULATION;
            case TEMPERATURE, SHORTWAVE_RADIATION, WINDSPEED, PRESSURE -> VortexDataType.AVERAGE;
            default -> VortexDataType.INSTANTANEOUS;
        };
    }

    public double getValue(int index) {
        return data[index];
    }

    public boolean isNoDataValue(double value) {
        return value == noDataValue || Double.isNaN(value);
    }

    public boolean hasTime() {
        return startTime != null && endTime != null;
    }

    public boolean hasSameData(VortexGrid that) {
        float[] thisData = this.data();
        float[] thatData = that.data();

        if (thisData.length != thatData.length) {
            return false;
        }

        for (int i = 0; i < thisData.length; i++) {
            float thisValue = thisData[i];
            float thatValue = thatData[i];

            boolean sameFloatValue = thisValue == thatValue;
            boolean bothAreNoData = this.isNoDataValue(thisValue) && that.isNoDataValue(thatValue);

            if (!sameFloatValue && !bothAreNoData) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VortexGrid that)) return false;
        if (Double.compare(that.dx, dx) != 0) return false;
        if (Double.compare(that.dy, dy) != 0) return false;
        if (nx != that.nx) return false;
        if (ny != that.ny) return false;
        if (Double.compare(that.originX, originX) != 0) return false;
        if (Double.compare(that.originY, originY) != 0) return false;
        if (!ReferenceUtils.equals(wkt, that.wkt)) return false;
        if (!this.hasSameData(that)) return false;
        if (!UnitUtil.equals(units, that.units)) return false;
        if (!startTime.isEqual(that.startTime)) return false;
        if (!endTime.isEqual(that.endTime)) return false;
        if (!Objects.equals(interval, that.interval)) return false;
        return dataType == that.dataType;
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
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (units != null ? units.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (interval != null ? interval.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        return result;
    }

    // Add to help with debugging (when testing VortexGrid::equals)
    @Override
    public String toString() {
        return "VortexGrid{" +
                "dx=" + dx +
                ", dy=" + dy +
                ", nx=" + nx +
                ", ny=" + ny +
                ", originX=" + originX +
                ", originY=" + originY +
                ", wkt='" + wkt + '\'' +
                ", data=" + Arrays.toString(data) +
                ", noDataValue=" + noDataValue +
                ", units='" + units + '\'' +
                ", fileName='" + fileName + '\'' +
                ", shortName='" + shortName + '\'' +
                ", fullName='" + fullName + '\'' +
                ", description='" + description + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", interval=" + interval +
                ", dataType=" + dataType +
                ", terminusX=" + terminusX +
                ", terminusY=" + terminusY +
                '}';
    }
}

