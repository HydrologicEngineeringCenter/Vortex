package mil.army.usace.hec.vortex.geo;

public enum ResamplingMethod {
    NEAREST_NEIGHBOR("near", "Nearest Neighbor"),
    BILINEAR("bilinear", "Bilinear"),
    AVERAGE("average", "Average");

    private final String keyString;
    private final String displayString;

    ResamplingMethod(String keyString, String displayString) {
        this.keyString = keyString;
        this.displayString = displayString;
    }

    public String getKeyString() {
        return keyString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static ResamplingMethod fromDisplayString(String displayString) {
        for (ResamplingMethod method : values()) {
            if (method.getDisplayString().equals(displayString)) {
                return method;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return keyString;
    }
}
