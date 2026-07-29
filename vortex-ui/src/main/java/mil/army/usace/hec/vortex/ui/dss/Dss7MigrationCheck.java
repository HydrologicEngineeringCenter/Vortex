package mil.army.usace.hec.vortex.ui.dss;

import mil.army.usace.hec.vortex.io.DssVersion;
import mil.army.usace.hec.vortex.ui.Text;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Guards the wizards against DSS version 6 files.
 *
 * <p>Reading or writing a version 6 file is not supported here: the native
 * layer upgrades one in place on open, which rewrites a file the user did not
 * ask to have rewritten. Stop before that happens and send them to the DSS 7
 * Migration wizard, where the conversion is explicit and lists what it will
 * touch.
 */
public final class Dss7MigrationCheck {

    private Dss7MigrationCheck() {
    }

    /**
     * Returns true if any path is an existing DSS 6 file, having first told the
     * user which ones and what to do about it. A true return means the caller
     * should abort.
     *
     * <p>Blank entries and paths that do not exist are ignored -- a destination
     * the wizard is about to create is not a version 6 file.
     */
    public static boolean blockIfDss6(Component parent, Collection<String> paths) {
        List<String> dss6 = findDss6(paths);
        if (dss6.isEmpty()) {
            return false;
        }

        JOptionPane.showMessageDialog(
                parent,
                Text.format("Dss7Mig_Dss6Detected", String.join("\n", dss6)),
                Text.format("Dss7Mig_Dss6Detected_Title"),
                JOptionPane.ERROR_MESSAGE);
        return true;
    }

    /**
     * The subset of {@code paths} that are existing DSS version 6 files.
     *
     * <p>The version test itself lives in {@link DssVersion}, so the wizards and
     * the API refuse exactly the same set of files -- two probes that could
     * disagree is the failure mode worth avoiding here.
     */
    public static List<String> findDss6(Collection<String> paths) {
        if (paths == null) {
            return List.of();
        }
        return paths.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .distinct()
                .filter(DssVersion::isDss6)
                .collect(Collectors.toList());
    }
}
