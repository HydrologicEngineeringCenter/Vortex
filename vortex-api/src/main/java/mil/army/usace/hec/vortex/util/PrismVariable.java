package mil.army.usace.hec.vortex.util;

public enum PrismVariable {
    PPT("ppt", "precipitation"),
    TMIN("tmin", "minimum temperature"),
    TMAX("tmax", "maximum temperature"),
    TMEAN("tmean", "mean temperature"),
    TDMEAN("tdmean", "mean dewpoint"),
    VPDMIN("vpdmin", "minimum vapor pressure deficit"),
    VPDMAX("vpdmax", "maximum vapor pressure deficit"),
    UNKNOWN("unknown", "unknown variable");

    private final String code;
    private final String name;

    PrismVariable(String code, String description) {
        this.code = code;
        this.name = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * Extract the variable name from a PRISM filename.
     * PRISM filename format: PRISM_<var>_<region>_<resolution>_<date>.zip
     *
     * @param filename The PRISM filename to parse
     * @return PrismVariable enum representing the variable
     */
    public static PrismVariable parse(String filename) {
        String basename = filename
                .replace(".zip", "")
                .replace(".bil", "")
                .replace(".asc", "")
                .replace(".tif", "")
                .replace(".tiff", "");

        int lastSlash = Math.max(basename.lastIndexOf('/'), basename.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            basename = basename.substring(lastSlash + 1);
        }

        String[] parts = basename.split("_");

        // Variable is typically the second part (index 1) after "PRISM"
        if (parts.length < 2) {
            return PrismVariable.UNKNOWN;
        }

        String varCode = parts[1].toLowerCase();

        return switch (varCode) {
            case "ppt" -> PrismVariable.PPT;
            case "tmin" -> PrismVariable.TMIN;
            case "tmax" -> PrismVariable.TMAX;
            case "tmean" -> PrismVariable.TMEAN;
            case "tdmean" -> PrismVariable.TDMEAN;
            case "vpdmin" -> PrismVariable.VPDMIN;
            case "vpdmax" -> PrismVariable.VPDMAX;
            default -> PrismVariable.UNKNOWN;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}