package mil.army.usace.hec.vortex.util;

public class FilenameUtil {

    private FilenameUtil(){}

    public static String removeExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }
}
