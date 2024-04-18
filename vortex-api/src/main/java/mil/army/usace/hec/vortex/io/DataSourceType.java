package mil.army.usace.hec.vortex.io;

public enum DataSourceType {
    ASC(".*\\.(asc|tif|tiff)$"),
    SNODAS_TAR(".*snodas.*\\.tar"),
    SNODAS_DAT(".*\\.dat"),
    BIL(".*\\.bil"),
    BIL_ZIP(".*bil\\.zip"),
    ASC_ZIP(".*asc\\.zip"),
    DSS(".*\\.dss"),
    NETCDF(".*\\.(nc|nc4)$"),
    UNSUPPORTED("");

    private final String pattern;

    DataSourceType(String pattern) {
        this.pattern = pattern;
    }

    public boolean matches(String path) {
        return !this.equals(UNSUPPORTED) && path.matches(pattern);
    }

    public static DataSourceType fromPathToFile(String pathToFile) {
        for (DataSourceType type : DataSourceType.values()) {
            if (type.matches(pathToFile)) {
                return type;
            }
        }

        return UNSUPPORTED;
    }
}
