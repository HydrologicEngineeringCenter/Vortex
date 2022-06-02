package mil.army.usace.hec.vortex.ui.util;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class OsUtil {
    private static final Logger logger = Logger.getLogger(OsUtil.class.getName());

    private static final String OS = System.getProperty("os.name").toLowerCase();
    public static final boolean WINDOWS = (OS.contains("win"));
    public static final boolean MACOS = (OS.contains("mac"));
    public static final boolean UNIX = (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));

    private OsUtil() {
        throw new IllegalStateException("Utility class");
    }

    /***
     * Execute command via command line
     * @param command The command to execute
     */
    public static void exec(String command) {
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    /***
     * Attempt to open folder containing file via Explorer/Finder/etc. and select the file via native OS call
     * @param pathToFile File to be opened and selected
     */
    public static void browseFileDirectory(Path pathToFile) {
        if(Files.notExists(pathToFile)) {
            logger.warning("File does not exist");
            return;
        }

        // Attempt to use Java default method first (currently does not support Windows 10/11 or Linux)
        Desktop desktop = Desktop.getDesktop();
        if(desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            desktop.browseFileDirectory(pathToFile.toFile());
            return;
        }

        // Using native calls for each OS
        String command = "";
        if(WINDOWS) command = "explorer.exe /select," + pathToFile.toAbsolutePath();
        if(MACOS) command = "open -R " + pathToFile.toAbsolutePath();
        if(UNIX) command = "nautilus --select " + pathToFile.toAbsolutePath();
        exec(command);
    }
}