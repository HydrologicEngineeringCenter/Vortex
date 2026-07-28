package mil.army.usace.hec.vortex.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JOptionPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class VortexUi {

    public static void main(String[] args) {
        installUncaughtExceptionHandler();
        reexecWithCorrectedPathIfNeeded(args);

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
        } else if (set.contains("-time-shifter")) {
            wizard = new TimeShifterWizard(null);
        } else if (set.contains("-time-step-resampler")) {
            wizard = new TimeStepResamplerWizard(null);
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

    // jpackage builds these launchers without a console window (matching the
    // old Launch4j gui header type), so an uncaught exception on the main
    // thread or the EDT would otherwise exit the process with no visible
    // trace at all. Surface it as a dialog and a log file instead.
    private static void installUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Path logFile = writeCrashLog(throwable);
            String message = throwable
                    + (logFile != null ? "\n\nDetails written to:\n" + logFile : "");
            System.err.println(message);
            JOptionPane.showMessageDialog(null, message, "Vortex encountered an error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private static Path writeCrashLog(Throwable throwable) {
        try {
            Path logFile = Path.of(System.getProperty("java.io.tmpdir"), "vortex-error.log");
            StringWriter stackTrace = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stackTrace));
            String entry = "[" + LocalDateTime.now() + "]\n" + stackTrace + "\n";
            Files.writeString(logFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return logFile;
        } catch (IOException e) {
            return null;
        }
    }

    /*
     * gdal302.dll's own dependency chain (via geos_c.dll and others) only
     * resolves correctly when its directory *and* the netcdf directory are on
     * the OS PATH -- java.library.path and same-folder DLL colocation are not
     * enough (confirmed empirically: colocating the missing DLLs still left
     * "The specified procedure could not be found" errors, while PATH-based
     * resolution works cleanly). jpackage launchers can't set PATH the way
     * Launch4j could, and setting it on the already-running process via the
     * Win32 API doesn't affect this process's own native loader (it only
     * takes effect for genuinely new processes) -- so a real new process is
     * required. We re-exec this exact launcher once, as a child with PATH
     * corrected, and let the child do the actual work.
     */
    private static void reexecWithCorrectedPathIfNeeded(String[] args) {
        if (System.getenv("VORTEX_REEXECED") != null) {
            return;
        }
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null) {
            return;
        }

        File launcher = new File(appPath);
        File appDir = new File(launcher.getParentFile(), "app");
        File gdalDir = new File(appDir, "gdal");
        File netcdfDir = new File(appDir, "netcdf");
        if (!gdalDir.isDirectory() || !netcdfDir.isDirectory()) {
            return;
        }

        try {
            List<String> command = new ArrayList<>();
            command.add(appPath);
            command.addAll(Arrays.asList(args));

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().put("VORTEX_REEXECED", "1");
            String existingPath = builder.environment().getOrDefault("PATH", "");
            builder.environment().put(
                    "PATH",
                    gdalDir.getAbsolutePath() + ";" + netcdfDir.getAbsolutePath() + ";" + existingPath
            );
            builder.inheritIO();

            Process child = builder.start();
            int exitCode = child.waitFor();
            System.exit(exitCode);
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to re-exec with corrected PATH, continuing in-process: " + e);
        }
    }
}
