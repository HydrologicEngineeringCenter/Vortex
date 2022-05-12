package mil.army.usace.hec.vortex;

import java.io.File;
import java.net.URL;

public class TestUtil {
    public static File getResourceFile(String pathFromResource) {
        URL url = TestUtil.class.getResource(pathFromResource);
        return (url != null) ? new File(url.getFile()) : null;
    }
}
