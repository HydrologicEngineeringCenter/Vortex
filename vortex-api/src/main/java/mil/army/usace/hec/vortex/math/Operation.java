package mil.army.usace.hec.vortex.math;

public enum Operation {
    MULTIPLY ("Multiply"),
    DIVIDE ("Divide"),
    ADD ("Add"),
    SUBTRACT ("Subtract");

    private final String displayString;

    Operation(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static Operation fromDisplayString(String displayString) {
        for (Operation operation : values()) {
            if (operation.getDisplayString().equals(displayString)) {
                return operation;
            }
        }
        return null;
    }
}
