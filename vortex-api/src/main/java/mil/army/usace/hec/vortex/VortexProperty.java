package mil.army.usace.hec.vortex;

public enum VortexProperty {
    STATUS("status"),
    PROGRESS("progress"),
    COMPLETE("complete"),
    WARNING("warning"),
    ERROR("error"),
    NONE("none");

    private final String name;

    VortexProperty(String name) {
        this.name = name;
    }

    public static VortexProperty parse(String str) {
        for (VortexProperty value : values()) {
            if (value.name.equalsIgnoreCase(str))
                return value;
        }
        return NONE;
    }

    @Override
    public String toString() {
        return name;
    }
}
