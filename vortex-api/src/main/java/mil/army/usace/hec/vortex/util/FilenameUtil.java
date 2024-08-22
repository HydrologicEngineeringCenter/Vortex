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

    public static boolean endsWithExtensions(String filename, String... extensions) {
        for (String extension : extensions) {
            if (filename.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
