package mil.army.usace.hec.vortex;

public enum VortexDataType {
    INSTANTANEOUS("INST-VAL", "point"),
    ACCUMULATION("PER-CUM", "sum"),
    AVERAGE("PER-AVER", "mean"),
    UNDEFINED("", "");

    // https://www.hec.usace.army.mil/confluence/display/dssJavaprogrammer/Units+and+Type
    private final String dssString;

    // https://cfconventions.org/cf-conventions/cf-conventions.html#cell-methods
    private final String ncString;

    VortexDataType(String dssString, String ncString) {
        this.dssString = dssString;
        this.ncString = ncString;
    }

    public static VortexDataType fromString(String dataTypeString) {
        for (VortexDataType type : values()) {
            boolean isDssString = type.getDssString().equalsIgnoreCase(dataTypeString);
            boolean isNcString = type.getNcString().equalsIgnoreCase(dataTypeString);
            if (isDssString || isNcString) {
                return type;
            }
        }
        return UNDEFINED;
    }

    public String getDssString() {
        return dssString;
    }

    public String getNcString() {
        return ncString;
    }
}
