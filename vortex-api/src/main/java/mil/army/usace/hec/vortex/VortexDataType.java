package mil.army.usace.hec.vortex;

public enum VortexDataType {
    INSTANTANEOUS("INST-VAL", "point"),
    ACCUMULATION("PER-CUM", "sum"),
    AVERAGE("PER-AVER", "mean");

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
            if (dataTypeString.equals(type.getDssString()) || dataTypeString.equals(type.getNcString()))
                return type;
        }
        throw new IllegalArgumentException("dataTypeString not recognized");
    }

    public String getDssString() {
        return dssString;
    }

    public String getNcString() {
        return ncString;
    }
}
