package mil.army.usace.hec.vortex.util;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexVariable;

public final class VortexGridUtils {
    private VortexGridUtils() {
        // Utility class
    }

    public static VortexVariable inferVortexVariableFromNames(VortexGrid vortexGrid) {
        return VortexVariable.fromNames(
                vortexGrid.shortName(),
                vortexGrid.fullName(),
                vortexGrid.description()
        );
    }
}
