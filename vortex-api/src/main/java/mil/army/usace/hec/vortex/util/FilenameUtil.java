package mil.army.usace.hec.vortex.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilenameUtil {
    private static final Logger LOGGER = Logger.getLogger(FilenameUtil.class.getName());

    private FilenameUtil(){}

    public static String removeExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

    public static boolean endsWithExtensions(String filename, String... extensions) {
        for (String extension : extensions) {
            if (filename.toLowerCase().endsWith(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSameFile(String filepath1, String filepath2) {
        Path path1 = Path.of(filepath1);
        Path path2 = Path.of(filepath2);

        if (Files.notExists(path1) || Files.notExists(path2)) {
            return Objects.equals(filepath1, filepath2);
        }

        try {
            return Files.isSameFile(path1, path2);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }

        return false;
    }
}
