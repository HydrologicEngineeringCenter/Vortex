package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.CREATE;

public class FileBrowseButton extends JButton {
    private static final Logger logger = Logger.getLogger(FileBrowseButton.class.getName());
    private static final String BROWSE_LOCATION = "browse_location";

    private final String uniqueId;

    public FileBrowseButton(String className, String buttonName) {
        super(buttonName);
        this.uniqueId = className + "." + buttonName;
    }

    public void setPersistedBrowseLocation(File file) {
        Path pathToProperties = Paths.get(Util.getCacheDir() + File.separator + uniqueId + ".properties");

        if (Files.notExists(pathToProperties.getParent())) {
            try {
                Files.createDirectory(pathToProperties.getParent());
            } catch (IOException e) {
                logger.log(Level.INFO, e.toString());
            }
        }

        try (OutputStream output = Files.newOutputStream(pathToProperties, CREATE)) {
            Properties properties = new Properties();
            String pathToFile = toString(file);

            if (pathToFile != null)
                properties.setProperty(BROWSE_LOCATION, pathToFile);

            properties.store(output, null);
        } catch (IOException e) {
            logger.log(Level.INFO, e.toString());
        }
    }

    public File getPersistedBrowseLocation() {
        String filename = uniqueId + ".properties";
        Path pathToProperties = Paths.get(Util.getCacheDir() + File.separator + filename);

        if (!Files.exists(pathToProperties))
            Util.migrateFromOldCacheDir(filename);

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String browseLocation = properties.getProperty(BROWSE_LOCATION);

                if (browseLocation == null)
                    return null;

                return toFile(browseLocation);
            } catch (IOException e) {
                logger.log(Level.INFO, e.toString());
                return null;
            }
        }
        return null;
    }

    private static File toFile(String pathToFile) {
        try {
            Path path = Path.of(pathToFile);
            if (Files.exists(path))
                return new File(pathToFile);
        } catch (InvalidPathException e) {
            logger.log(Level.INFO, e.toString());
            return null;
        }
        return null;
    }

    private static String toString(File file) {
        try {
            Path path = Path.of(file.getPath());
            return path.toString();
        } catch (InvalidPathException e) {
            logger.log(Level.INFO, e.toString());
            return null;
        }
    }
}
