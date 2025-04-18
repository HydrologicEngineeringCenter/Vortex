package mil.army.usace.hec.vortex.ui;

import com.formdev.flatlaf.FlatLightLaf;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.util.Set;

public class VortexUi {

    public static void main(String[] args) {
        FlatLightLaf.setup();

        Set<String> set = Set.of(args);
        VortexWizard wizard;
        if (set.contains("-calculator")) {
            wizard = new CalculatorWizard(null);
        } else if (set.contains("-clipper")) {
            wizard = new ClipperWizard(null);
        } else if (set.contains("-grid-to-point")) {
            wizard = new GridToPointWizard(null);
        } else if (set.contains("-gap-filler")) {
            wizard = new GapFillerWizard(null);
        } else if (set.contains("-image-exporter")) {
            wizard = new ImageExporterWizard(null);
        } else if (set.contains("-importer")) {
            wizard = new ImportMetWizard(null);
        } else if (set.contains("-normalizer")) {
            wizard = new NormalizerWizard(null);
        } else if (set.contains("-sanitizer")) {
            wizard = new SanitizerWizard(null);
        } else if (set.contains("-shifter")) {
            wizard = new ShifterWizard(null);
        } else {
            wizard = new AnyWizard(null);
        }

        wizard.buildAndShowUI();

        WindowListener listener = new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exit();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                exit();
            }
        };

        wizard.addWindowListener(listener);
    }

    private static void exit() {
        System.exit(0);
    }
}
