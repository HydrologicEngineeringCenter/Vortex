package mil.army.usace.hec.vortex;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Logger;

public class TestUtil {
    private static final Logger logger = Logger.getLogger(TestUtil.class.getName());

    public static File getResourceFile(String pathFromResource) {
        URL url = TestUtil.class.getResource(pathFromResource);
        return (url != null) ? new File(url.getFile()) : null;
    }

    public static String createTempFile(String fileName) {
        try {
            int index = fileName.lastIndexOf(".");
            boolean hasExtension = index > 0;
            String prefix = hasExtension ? fileName.substring(0, index) : fileName;
            String suffix = hasExtension ? fileName.substring(index) : "";
            return Files.createTempFile(prefix, suffix).toAbsolutePath().toString();
        } catch (IOException e) {
            logger.warning(e.getMessage());
            return null;
        }
    }
}
