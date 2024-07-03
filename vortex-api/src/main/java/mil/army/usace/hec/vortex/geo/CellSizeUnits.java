package mil.army.usace.hec.vortex.geo;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import static systems.uom.common.USCustomary.FOOT;
import static tech.units.indriya.unit.Units.METRE;

public enum CellSizeUnits {
    METERS("Meters", METRE),
    FEET("Feet", FOOT);

    private final String displayString;
    private final Unit<Length> units;

    CellSizeUnits(String displayString, Unit<Length> units) {
        this.displayString = displayString;
        this.units = units;
    }

    public static CellSizeUnits of(String displayString) {
        for (CellSizeUnits value : values()) {
            if (value.displayString.equals(displayString)) {
                return value;
            }
        }
        throw new IllegalArgumentException("display string not recognized");
    }

    public static String[] getDisplayStrings() {
        CellSizeUnits[] values = values();
        String[] displayStrings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            displayStrings[i] = values[i].displayString;
        }
        return displayStrings;
    }

    public Unit<Length> getUnits() {
        return units;
    }
}
