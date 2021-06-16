package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.CREATE;

public class FileBrowseButton extends JButton {
    private final String uniqueId;
    private static final Logger logger = Logger.getLogger(FileBrowseButton.class.getName());

    public FileBrowseButton(String className, String buttonName) {
        super(buttonName);
        this.uniqueId = className + "." + buttonName;
    }

    public void setPersistedBrowseLocation(File file) {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".hms" + File.separator + uniqueId + ".properties" );

        if(Files.notExists(pathToProperties.getParent())){
            try {
                Files.createDirectory(pathToProperties.getParent());
            } catch (IOException e) {
                logger.log(Level.INFO, e.toString());
            }
        }

        try(OutputStream output = Files.newOutputStream(pathToProperties, CREATE)){
            Properties properties = new Properties();
            properties.setProperty("browse_location", file.getPath());
            properties.store(output,null);
        } catch (IOException e) {
            logger.log(Level.INFO, e.toString());
        }
    }

    public File getPersistedBrowseLocation() {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".hms" + File.separator + uniqueId + ".properties" );

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String outFilePath = properties.getProperty("browse_location");
                if (outFilePath == null){
                    return null;
                }
                if (Files.exists(Paths.get(outFilePath))) {
                    return new File(outFilePath);
                }
                return null;
            } catch (IOException e) {
                logger.log(Level.INFO, e.toString());
                return null;
            }
        }
        return null;
    }
}
