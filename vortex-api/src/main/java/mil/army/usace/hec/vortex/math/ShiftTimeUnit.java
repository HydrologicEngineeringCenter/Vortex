package mil.army.usace.hec.vortex.math;

public enum ShiftTimeUnit {
    DAYS("Days", 86400),
    HOURS("Hours", 3600),
    MINUTES("Minutes", 60),
    SECONDS("Seconds", 1);

    private final String displayString;
    private final int toSeconds;

    ShiftTimeUnit(String displayString, int toSeconds) {
        this.displayString = displayString;
        this.toSeconds = toSeconds;
    }

    public int toSeconds() {
        return toSeconds;
    }

    @Override
    public String toString() {
        return displayString;
    }

    public static ShiftTimeUnit fromString(String string) {
        for (ShiftTimeUnit value : values()) {
            String displayString = value.toString();
            if (displayString.equalsIgnoreCase(string))
                return value;
        }
        throw new IllegalArgumentException("Time unit string + \"" + string + "\" not recognized");
    }
}
