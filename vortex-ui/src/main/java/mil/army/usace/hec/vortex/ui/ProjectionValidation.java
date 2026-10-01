package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.VectorUtils;
import mil.army.usace.hec.vortex.io.RasterProjectionValidation;
import mil.army.usace.hec.vortex.io.Validation;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.Collection;

/** Presents projection failures consistently and prevents advancing the wizard. */
final class ProjectionValidation {
    private ProjectionValidation() {}

    static boolean validateSource(Component parent, String path, Collection<String> variables) {
        return showResult(parent, RasterProjectionValidation.validateSource(path.trim(), variables));
    }

    static boolean validateRaster(Component parent, String path) {
        return showResult(parent, RasterProjectionValidation.validateRaster(path.trim()));
    }

    static boolean validateVector(Component parent, String path) {
        if (path.isBlank()) {
            JOptionPane.showMessageDialog(parent, Text.format("Error_InputRequired"),
                    Text.format("Error_MissingField_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return showResult(parent, VectorUtils.validateProjection(Path.of(path.trim())));
    }

    private static boolean showResult(Component parent, Validation validation) {
        if (validation.isValid()) {
            return true;
        }
        JOptionPane.showMessageDialog(parent, String.join(System.lineSeparator(), validation.getMessages()),
                Text.format("Warning_Title"), JOptionPane.WARNING_MESSAGE);
        return false;
    }
}
